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

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;
import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.validation.constraints.NotBlank;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentRequest;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

/**
 * Implementation of an email consumer that is geared towards Microsoft Office 365, using their Graph API and OAuth2.
 *
 * @config azure-office-365-mail-consumer
 */
@XStreamAlias("azure-office-365-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup email from a Microsoft Office 365 account using the Microsoft Graph API", tag = "consumer,email,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "username", "delete" })
public class O365MailConsumer extends AdaptrisPollingConsumer {
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
   * filter parameters.
   */
  @Getter
  @Setter
  @AdvancedConfig(rare = true)
  @InputFieldHint(friendly = "How to filter the emails (see https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter).")
  @InputFieldDefault(DEFAULT_FILTER)
  private String filter;

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void prepareConsumer() {
    /* do nothing */
  }

  /**
   * Poll the given usernames mailbox for new messages.
   *
   * @return The number of new emails received.
   */
  @Override
  protected int processMessages() {
    log.debug("Polling for mail in Office365 as user " + username);

    int count = 0;
    try {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      GraphServiceClient<?> graphClient = connection.getClientConnection();

      MessageCollectionPage messages = graphClient.users(username).mailFolders(folder()).messages().buildRequest().filter(filter()).get();

      // TODO handle multiple pages...
      List<Message> currentPage = messages.getCurrentPage();
      log.debug("Found {} messages", currentPage.size());

      for (Message outlookMessage : currentPage) {
        String id = outlookMessage.id;
        AdaptrisMessage adaptrisMessage = decode(
            outlookMessage.body.content.getBytes(charset()));

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage) {
          ((MultiPayloadAdaptrisMessage) adaptrisMessage).setCurrentPayloadId(id);
        }

        addMessageMetadata(outlookMessage, id, adaptrisMessage);

        log.debug("Processing email from {}: {}", outlookMessage.from.emailAddress.address, outlookMessage.subject);

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage && outlookMessage.hasAttachments) {
          processMultiPayload(graphClient, outlookMessage, id, adaptrisMessage);
        }

        /*
         * The internetMessageHeaders need to be requested explicitly with a SELECT.
         */
        outlookMessage = processInternetMessageHeaders(graphClient, id, adaptrisMessage);

        retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);

        if (delete()) {
          graphClient.users(username).messages(id).buildRequest().delete();
        } else {
          /*
           * With PATCH, only send what we've changed, in this instance we're marking the mail as read.
           */
          outlookMessage = new Message();
          outlookMessage.isRead = true;
          graphClient.users(username).messages(id).buildRequest().patch(outlookMessage);
        }

        count++;
      }

    } catch (Throwable e) {
      log.error("Exception processing Outlook message", e);
    }

    return count;
  }

  private Message processInternetMessageHeaders(GraphServiceClient<?> graphClient, String id, AdaptrisMessage adaptrisMessage) {
    Message outlookMessage;
    outlookMessage = graphClient.users(username).messages(id).buildRequest().select("internetMessageHeaders").get();
    if (outlookMessage.internetMessageHeaders != null) {
      for (InternetMessageHeader header : outlookMessage.internetMessageHeaders) {
        adaptrisMessage.addMetadata(header.name, header.value);
      }
    }
    return outlookMessage;
  }

  private void processMultiPayload(GraphServiceClient<?> graphClient, Message outlookMessage, String id, AdaptrisMessage adaptrisMessage)
      throws MessagingException, Exception {

    MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage = (MultiPayloadAdaptrisMessage) adaptrisMessage;
    AttachmentCollectionPage attachments = graphClient.users(username).messages(id).attachments().buildRequest().get();
    log.debug("Message has {} attachments", attachments.getCurrentPage().size());
    for (Attachment reference : attachments.getCurrentPage()) {
      log.debug("Attachment {} is of type {} with size {}", reference.name, reference.oDataType, reference.size);
      AttachmentRequest request = graphClient.users(username).messages(id).attachments(reference.id).buildRequest();// new
      // QueryOption("value",
      // ""));
      log.debug("URL: {}", request.getRequestUrl());
      Attachment attachment = request.get();
      if (attachment instanceof FileAttachment) {
        FileAttachment file = (FileAttachment) attachment;
        log.debug("File {} :: {} :: {}", file.name, file.contentType, file.size);
        if (file.contentType.startsWith("multipart")) {
          MimeMultipart mimeMultipart = new MimeMultipart(new ByteArrayDataSource(file.contentBytes, file.contentType));
          parseMimeMultiPart(multiPayloadAdaptrisMessage, mimeMultipart);
        } else {
          addAttachmentToAdaptrisMessage(multiPayloadAdaptrisMessage, file.name, file.contentBytes);
        }
      }
    }
    multiPayloadAdaptrisMessage.switchPayload(id);
  }

  private void addMessageMetadata(Message outlookMessage, String id, AdaptrisMessage adaptrisMessage) {
    adaptrisMessage.addMetadata("EmailID", id);
    adaptrisMessage.addMetadata("Subject", outlookMessage.subject);
    adaptrisMessage.addMetadata("To", joinEmailAddresses(outlookMessage.toRecipients));
    adaptrisMessage.addMetadata("From", outlookMessage.from.emailAddress.address);
    adaptrisMessage.addMetadata("CC", joinEmailAddresses(outlookMessage.ccRecipients));
    adaptrisMessage.addMetadata("BCC", joinEmailAddresses(outlookMessage.bccRecipients));
  }

  private String joinEmailAddresses(List<Recipient> recipients) {
    return CollectionUtils.emptyIfNull(recipients).stream().map(r -> r.emailAddress.address).collect(Collectors.joining(","));
  }

  private String charset() {
    return StringUtils.defaultIfBlank(defaultIfNull(getMessageFactory()).getDefaultCharEncoding(), Charset.defaultCharset().name());
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  protected String newThreadName() {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }

  private boolean delete() {
    return BooleanUtils.toBooleanDefaultIfNull(delete, false);
  }

  private String folder() {
    return StringUtils.defaultString(folder, DEFAULT_FOLDER);
  }

  private String filter() {
    return StringUtils.defaultString(filter, DEFAULT_FILTER);
  }

  private void addAttachmentToAdaptrisMessage(MultiPayloadAdaptrisMessage message, String name, byte[] attachment) {
    try {
      /*
       * The Graph API documentation says that contentBytes is Base64 encoded, but that doesn't always appear to be the case
       */
      attachment = Base64.getDecoder().decode(attachment);
    } catch (Exception e) {
      // Do nothing; content wasn't base64 encoded
    }
    message.addPayload(name, attachment);
  }

  private void parseMimeMultiPart(MultiPayloadAdaptrisMessage message, MimeMultipart mimeMultipart) throws Exception {
    for (int i = 0; i < mimeMultipart.getCount(); i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      String name = bodyPart.getFileName();
      Object content = bodyPart.getContent();
      if (content instanceof MimeMultipart) {
        parseMimeMultiPart(message, (MimeMultipart) content);
      } else if (name == null) {
        // Do nothing; this is the email body
      } else {
        byte[] bytes = null;
        if (content instanceof ByteArrayInputStream) {
          bytes = IOUtils.toByteArray((ByteArrayInputStream) content);
        } else if (content instanceof String) {
          bytes = ((String) content).getBytes(US_ASCII); // MIME encoded emails are always 7bit ASCII right??
        } else {
          log.warn("Unsupported MIME part {}", bodyPart.getContentType());
        }
        addAttachmentToAdaptrisMessage(message, name, bytes);
      }
    }
  }

}
