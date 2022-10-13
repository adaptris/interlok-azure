/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adaptris.interlok.azure.mail;

import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;

import lombok.Getter;
import lombok.Setter;

public abstract class O365MailConsumerImpl extends AdaptrisPollingConsumer {

  static final String CONSISTENCY_LEVEL_OPTION = "ConsistencyLevel";
  static final String FILTER_OPTION = "$filter";
  static final String SEARCH_OPTION = "$search";

  static final String DEFAULT_FOLDER = "inbox";
  static final String DEFAULT_FILTER = "isRead eq false";

  /**
   * The Office 365 username of the mailbox to poll for new messages.
   */
  @Getter
  @Setter
  @NotBlank
  private String username;

  /**
   * Whether emails should be deleted after reading, not just marked as read.
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "Delete messages instead of marking them as read?")
  @InputFieldDefault("false")
  private Boolean delete;

  /**
   * The mailbox folder to poll for new messages.
   *
   * The default folder is 'Inbox'.
   */
  @Getter
  @Setter
  @AdvancedConfig(rare = true)
  @InputFieldHint(friendly = "The folder to check for emails.")
  @InputFieldDefault(DEFAULT_FOLDER)
  private String folder;

  /**
   * How to filter the messages.
   *
   * The default filter is 'isRead eq false'. See https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter for further
   * filter parameters. Filter and search cannot be used at the same time.
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "How to filter the emails (see https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter).")
  @InputFieldDefault(DEFAULT_FILTER)
  private String filter;

  /**
   * You can search messages based on a value in specific message properties. If you do a search on messages and specify only a value
   * without specific message properties, the search is carried out on the default search properties of from, subject, and body. Filter and
   * search cannot be used at the same time. When search is used filter is ignored.
   *
   * filter parameters.
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "Search emails based on a value in specific message properties (see https://docs.microsoft.com/en-us/graph/search-query-parameter).")
  private String search;

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void prepareConsumer() {
    /* do nothing */
  }

  /**
   * Poll the given username's mailbox for new messages.
   *
   * @return The number of new emails received.
   */
  @Override
  protected int processMessages() {
    log.debug("Polling for mail in Office365 as user {}", getUsername());

    int count = 0;
    try {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      GraphServiceClient<?> graphClient = connection.getClientConnection();

      MessageCollectionPage messages = graphClient.users(getUsername()).mailFolders(folder()).messages().buildRequest(queryOptions()).get();

      // TODO handle multiple pages...
      List<Message> currentPage = messages.getCurrentPage();
      log.debug("Found {} messages", currentPage.size());

      for (Message outlookMessage : currentPage) {
        String id = outlookMessage.id;

        AdaptrisMessage adaptrisMessage = processMessage(graphClient, outlookMessage);

        retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);

        if (delete()) {
          graphClient.users(getUsername()).messages(id).buildRequest().delete();
        } else {
          /*
           * With PATCH, only send what we've changed, in this instance we're marking the mail as read.
           */
          outlookMessage = new Message();
          outlookMessage.isRead = true;
          graphClient.users(getUsername()).messages(id).buildRequest().patch(outlookMessage);
        }

        count++;
      }

    } catch (Throwable e) {
      log.error("Exception processing Outlook message", e);
    }

    return count;
  }

  protected abstract AdaptrisMessage processMessage(GraphServiceClient<?> graphClient, Message outlookMessage) throws Exception;

  List<Option> queryOptions() {
    LinkedList<Option> options = new LinkedList<>();
    if (StringUtils.isNotBlank(search)) {
      // This request requires the ConsistencyLevel header set to eventual to use $search
      options.add(new HeaderOption(CONSISTENCY_LEVEL_OPTION, "eventual"));
      options.add(new QueryOption(SEARCH_OPTION, search()));
    } else {
      options.add(new QueryOption(FILTER_OPTION, filter()));
    }
    return options;
  }

  @Override
  protected String newThreadName() {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }

  protected boolean delete() {
    return BooleanUtils.toBooleanDefaultIfNull(delete, false);
  }

  protected String folder() {
    return StringUtils.defaultString(folder, DEFAULT_FOLDER);
  }

  protected String filter() {
    return StringUtils.defaultString(filter, DEFAULT_FILTER);
  }

  protected String search() {
    return StringUtils.wrap(search, "\"");
  }

}
