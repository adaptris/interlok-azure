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

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.CoreException;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.util.DestinationHelper;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.models.extensions.Attachment;
import com.microsoft.graph.models.extensions.FileAttachment;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.InternetMessageHeader;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IAttachmentCollectionPage;
import com.microsoft.graph.requests.extensions.IAttachmentRequest;
import com.microsoft.graph.requests.extensions.IMessageCollectionPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Collections;

/**
 * Implementation of an email consumer that is geared towards Microsoft
 * Office 365, using their Graph API and OAuth2.
 *
 * @config office-365-mail-consumer
 */
@XStreamAlias("office-365-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup email from a Microsoft Office 365 account using the Microsoft Graph API", tag = "consumer,email,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret", "username", "delete" })
public class O365MailConsumer extends AdaptrisPollingConsumer
{
  private static final String SCOPE = "https://graph.microsoft.com/.default";

  @Getter
  @Setter
  @NotBlank
  private String applicationId;

  @Getter
  @Setter
  @NotBlank
  private String tenantId;

  @Getter
  @Setter
  @NotBlank
  private String clientSecret;

  @Getter
  @Setter
  @NotBlank
  private String username;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "Delete messages instead of marking them as read?")
  @InputFieldDefault("false")
  private Boolean delete;

  private transient ConfidentialClientApplication confidentialClientApplication;

  @Override
  protected void prepareConsumer() throws CoreException
  {
    try
    {
      confidentialClientApplication = ConfidentialClientApplication.builder(applicationId,
          ClientCredentialFactory.createFromSecret(clientSecret))
          .authority(tenant())
          .build();
    }
    catch (Exception e)
    {
      log.error("Could not identify Azure application or tenant", e);
      throw new CoreException(e);
    }
  }

  @Override
  protected int processMessages()
  {
    log.debug("Polling for mail in Office365 as user " + username);

    int count = 0;
    try
    {
      IAuthenticationResult iAuthResult = confidentialClientApplication.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE)).build()).join();
      log.trace("Access token: " + iAuthResult.accessToken());
      IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + iAuthResult.accessToken())).buildClient();

      // TODO Allow the end user to choose the folder and filter themselves
      IMessageCollectionPage messages = graphClient.users(username).mailFolders("inbox").messages().buildRequest().filter("isRead eq false").get();

      // TODO handle multiple pages...
      log.debug("Found {} messages", messages.getCurrentPage().size());
      for (Message outlookMessage : messages.getCurrentPage())
      {
        String id = outlookMessage.id;
        AdaptrisMessage adaptrisMessage = getMessageFactory().newMessage(outlookMessage.body.content);

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage)
        {
          ((MultiPayloadAdaptrisMessage)adaptrisMessage).setCurrentPayloadId(id);
        }
        adaptrisMessage.addMetadata("EmailID", id);
        adaptrisMessage.addMetadata("Subject", outlookMessage.subject);
        adaptrisMessage.addMetadata("To", outlookMessage.toRecipients.stream().map(r -> r.emailAddress.address).reduce((a, b) -> a + "," + b).get() );
        adaptrisMessage.addMetadata("From", outlookMessage.from.emailAddress.address);
        adaptrisMessage.addMetadata("CC", String.join(",", outlookMessage.ccRecipients.stream().map(r -> r.emailAddress.address).toArray(String[]::new)));

        log.debug("Processing email from {}: {}", outlookMessage.from.emailAddress.address, outlookMessage.subject);

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage && outlookMessage.hasAttachments)
        {
          MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage = (MultiPayloadAdaptrisMessage)adaptrisMessage;
          IAttachmentCollectionPage attachments = graphClient.users(username).messages(id).attachments().buildRequest().get();
          log.debug("Message has {} attachments", attachments.getCurrentPage().size());
          for (Attachment reference : attachments.getCurrentPage())
          {
            log.debug("Attachment {} is of type {} with size {}", reference.name, reference.oDataType, reference.size);
            IAttachmentRequest request = graphClient.users(username).messages(id).attachments(reference.id).buildRequest();//new QueryOption("$value", ""));
            log.debug("URL: {}", request.getRequestUrl());
            Attachment attachment = request.get();
            if (attachment instanceof FileAttachment)
            {
              FileAttachment file = (FileAttachment)attachment;
              log.debug("File {} :: {} :: {}", file.name, file.contentType, file.size);
              if (file.contentType.startsWith("multipart"))
              {
                MimeMultipart mimeMultipart = new MimeMultipart(new ByteArrayDataSource(file.contentBytes, file.contentType));
                parseMimeMultiPart(multiPayloadAdaptrisMessage, mimeMultipart);
              }
              else
              {
                addAttachmentToAdaptrisMessage(multiPayloadAdaptrisMessage, file.name, file.contentBytes);
              }
            }
          }
          multiPayloadAdaptrisMessage.switchPayload(id);
        }

        /*
         * The internetMessageHeaders need to be requested explicitly with a SELECT.
         */
        outlookMessage = graphClient.users(username).messages(id).buildRequest().select("internetMessageHeaders").get();
        if (outlookMessage.internetMessageHeaders != null)
        {
          for (InternetMessageHeader header : outlookMessage.internetMessageHeaders)
          {
            adaptrisMessage.addMetadata(header.name, header.value);
          }
        }

        retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);

        if (delete())
        {
          graphClient.users(username).messages(id).buildRequest().delete();
        }
        else
        {
          /*
           * With PATCH, only send what we've changed, in this instance
           * we're marking the mail as read.
           */
          outlookMessage = new Message();
          outlookMessage.isRead = true;
          graphClient.users(username).messages(id).buildRequest().patch(outlookMessage);
        }

        count++;
      }

    }
    catch (Throwable e)
    {
      log.error("Exception processing Outlook message", e);
    }

    return count;
  }

  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }

  private String tenant()
  {
    return String.format("https://login.microsoftonline.com/%s", tenantId);
  }

  private boolean delete()
  {
    return BooleanUtils.toBooleanDefaultIfNull(delete, false);
  }

  private void addAttachmentToAdaptrisMessage(MultiPayloadAdaptrisMessage message, String name, byte[] attachment)
  {
    try
    {
      /*
       * The Graph API documentation says that contentBytes
       * is Base64 encoded, but that doesn't always appear to
       * be the case
       */
      attachment = Base64.getDecoder().decode(attachment);
    }
    catch (Exception e)
    {
      // do nothing; content wasn't base64 encoded
    }
    message.addPayload(name, attachment);
  }

  private void parseMimeMultiPart(MultiPayloadAdaptrisMessage message, MimeMultipart mimeMultipart) throws Exception
  {
    for (int i = 0; i < mimeMultipart.getCount(); i++)
    {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      String name = bodyPart.getFileName();
      Object content = bodyPart.getContent();
      if (content instanceof MimeMultipart)
      {
        parseMimeMultiPart(message, (MimeMultipart)content);
      }
      else if (name == null)
      {
        // do nothing; this is the email body
      }
      else
      {
        byte[] bytes = null;
        if (content instanceof ByteArrayInputStream)
        {
          bytes = ((ByteArrayInputStream)content).readAllBytes();
        }
        else if (content instanceof String)
        {
          bytes = ((String)content).getBytes();
        }
        else
        {
          log.warn("Unsupported MIME part {}", bodyPart.getContentType());
        }
        addAttachmentToAdaptrisMessage(message, name, bytes);
      }
    }
  }
}

