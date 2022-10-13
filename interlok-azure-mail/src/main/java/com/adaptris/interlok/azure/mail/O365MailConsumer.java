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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of an email consumer that is geared towards Microsoft Office 365, using their Graph API and OAuth2. In 3.12.0.1 this
 * service adds a metadata 'emailpayloadtype' for each payload with a value of 'payload' or 'attachment'. It also adds extra metadata
 * 'emailattachmentfilename', 'emailattachmentcontenttype', 'emailattachmentsize' for each attachments. This new behaviour will also be
 * added to 4.6.0.
 *
 * @config azure-office-365-mail-consumer
 */
@XStreamAlias("azure-office-365-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup email from a Microsoft Office 365 account using the Microsoft Graph API", tag = "consumer,email,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "username", "delete", "folder", "filter", "search" })
public class O365MailConsumer extends O365MailConsumerImpl {

  @Override
  protected AdaptrisMessage processMessage(GraphServiceClient<?> graphClient, Message outlookMessage) throws Exception {
    String id = outlookMessage.id;

    AdaptrisMessage adaptrisMessage = decode(outlookMessage.body.content.getBytes(charset()));

    if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage) {
      ((MultiPayloadAdaptrisMessage) adaptrisMessage).setCurrentPayloadId(id);
      ((MultiPayloadAdaptrisMessage) adaptrisMessage)
      .addPayloadMessageHeader(EmailConstants.EMAIL_PAYLOAD_TYPE, EmailConstants.EMAIL_PAYLOAD_TYPE_PAYLOAD);
    }

    addMessageMetadata(outlookMessage, id, adaptrisMessage);

    log.debug("Processing email from {}: {}", outlookMessage.from.emailAddress.address, outlookMessage.subject);

    if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage && outlookMessage.hasAttachments) {
      processMultiPayload(graphClient, outlookMessage, id, (MultiPayloadAdaptrisMessage) adaptrisMessage);
    }

    /*
     * The internetMessageHeaders need to be requested explicitly with a SELECT.
     */
    processInternetMessageHeaders(graphClient, id, adaptrisMessage);

    return adaptrisMessage;
  }

  private void processInternetMessageHeaders(GraphServiceClient<?> graphClient, String id, AdaptrisMessage adaptrisMessage) {
    Message outlookMessage = graphClient.users(getUsername()).messages(id).buildRequest().select("internetMessageHeaders").get();
    if (outlookMessage.internetMessageHeaders != null) {
      for (InternetMessageHeader header : outlookMessage.internetMessageHeaders) {
        adaptrisMessage.addMetadata(header.name, header.value);
      }
    }
  }

  private void processMultiPayload(GraphServiceClient<?> graphClient, Message outlookMessage, String id, MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage)
      throws MessagingException, Exception {

    AttachmentCollectionPage attachments = graphClient.users(getUsername()).messages(id).attachments().buildRequest().get();
    log.debug("Message has {} attachments", attachments.getCurrentPage().size());
    for (Attachment attachment : attachments.getCurrentPage()) {
      log.debug("Attachment {} is of type {} with size {}", attachment.name, attachment.oDataType, attachment.size);
      // AttachmentRequest request = graphClient.users(username).messages(id).attachments(reference.id).buildRequest();
      // log.debug("URL: {}", request.getRequestUrl());
      // Attachment attachment = request.get();
      if (attachment instanceof FileAttachment) {
        FileAttachment file = (FileAttachment) attachment;
        log.debug("File {} :: {} :: {}", file.name, file.contentType, file.size);
        if (file.contentType.startsWith("multipart")) {
          MimeMultipart mimeMultipart = new MimeMultipart(new ByteArrayDataSource(file.contentBytes, file.contentType));
          parseMimeMultiPart(multiPayloadAdaptrisMessage, mimeMultipart);
        } else {
          addAttachmentToAdaptrisMessage(multiPayloadAdaptrisMessage, file.name, file.contentBytes, file.contentType, file.size);
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

  private void addAttachmentToAdaptrisMessage(MultiPayloadAdaptrisMessage message, String name, byte[] attachment,
      String contentType,
      int size) {
    try {
      /*
       * The Graph API documentation says that contentBytes is Base64 encoded, but that doesn't always appear to be the case
       */
      attachment = Base64.getDecoder().decode(attachment);
    } catch (Exception e) {
      // Do nothing; content wasn't base64 encoded
    }

    message.addPayloadMessageHeader(name, EmailConstants.EMAIL_PAYLOAD_TYPE, EmailConstants.EMAIL_PAYLOAD_TYPE_ATTACHMENT);
    message.addPayloadMessageHeader(name, EmailConstants.EMAIL_ATTACH_FILENAME, name);
    message.addPayloadMessageHeader(name, EmailConstants.EMAIL_ATTACH_CONTENT_TYPE, contentType);
    message.addPayloadMessageHeader(name, EmailConstants.EMAIL_ATTACH_SIZE, String.valueOf(size));

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
        addAttachmentToAdaptrisMessage(message, name, bytes, bodyPart.getContentType(), bodyPart.getSize());
      }
    }
  }

}
