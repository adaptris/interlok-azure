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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.validation.Valid;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.mail.IgnoreMailHeaders;
import com.adaptris.core.mail.MailHeaderHandler;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Implementation of an email consumer that is geared towards Microsoft Office 365, using their Graph API and OAuth2. The raw MimeMessage
 * will not be parsed, and the contents of the entire MimeMessage will be used to create a single AdaptrisMessage instance for processing.
 * Additionally, any configured encoder will be ignored..
 *
 * @config azure-office-365-raw-mail-consumer
 */
@XStreamAlias("azure-office-365-raw-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup mime email from a Microsoft Office 365 account using the Microsoft Graph API", tag = "consumer,email,mime,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "username", "delete", "folder", "filter", "search" })
public class O365RawMailConsumer extends O365MailConsumerImpl {

  /**
   * Specify whether to use the email unique id as the AdaptrisMessage unique ID.
   *
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean useEmailMessageIdAsUniqueId;

  /**
   * Specify how to handle mails headers
   *
   * @param headerHandler
   *          the handler, defaults to {@link IgnoreMailHeaders}.
   */
  @Getter
  @Setter
  @NonNull
  @AdvancedConfig
  @Valid
  @InputFieldDefault(value = "ignore-mail-headers")
  private MailHeaderHandler headerHandler;

  public O365RawMailConsumer() {
    setHeaderHandler(new IgnoreMailHeaders());
  }

  @Override
  protected AdaptrisMessage processMessage(GraphServiceClient<?> graphClient, Message outlookMessage) throws Exception {
    InputStream inputStream = graphClient.users(getUsername()).messages(outlookMessage.id).content().buildRequest().get();

    Session session = Session.getDefaultInstance(new Properties(), null);
    MimeMessage mime = new MimeMessage(session, inputStream);

    AdaptrisMessage adaptrisMessage = defaultIfNull(getMessageFactory()).newMessage();
    String uuid = adaptrisMessage.getUniqueId();

    try (OutputStream out = adaptrisMessage.getOutputStream()) {
      mime.writeTo(out);
    }
    if (useEmailMessageIdAsUniqueId()) {
      adaptrisMessage.setUniqueId(StringUtils.defaultIfBlank(mime.getMessageID(), uuid));
    }

    log.debug("Processing email from {}: {}", outlookMessage.from.emailAddress.address, outlookMessage.subject);

    headerHandler().handle(mime, adaptrisMessage);

    return adaptrisMessage;
  }

  boolean useEmailMessageIdAsUniqueId() {
    return BooleanUtils.toBooleanDefaultIfNull(getUseEmailMessageIdAsUniqueId(), false);
  }

  protected MailHeaderHandler headerHandler() {
    return getHeaderHandler();
  }

}
