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

package com.adaptris.interlok.azure.onedrive;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import javax.validation.constraints.NotBlank;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Implementation of a file consumer that can retrieve files from
 * Microsoft One Drive, using their Graph API and OAuth2.
 *
 * @config azure-one-drive-consumer
 */
@XStreamAlias("azure-one-drive-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup files from a Microsoft Office 365 One Drive account using the Microsoft Graph API", tag = "consumer,file,o365,microsoft,office,365,one drive")
@DisplayOrder(order = { "username" })
public class OneDriveConsumer extends AdaptrisPollingConsumer
{
  /**
   * The username for which One Drive will be polled.
   */
  @Getter
  @Setter
  @NotBlank
  private String username;

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void prepareConsumer()
  {
    /* do nothing */
  }

  /**
   * Poll the given One Drive for files.
   *
   * @return The number of files found.
   */
  @Override
  protected int processMessages()
  {
    log.debug("Polling for files in One Drive as user " + username);

    int count = 0;
    try
    {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      IGraphServiceClient graphClient = connection.getClientConnection();

      Drive oneDrive = graphClient.users(username).drive().buildRequest().get();
      /*
       * TODO If paths work in the conventional sense, then it might be
       *  useful to allow the end user to choose a path
       */
      IDriveItemCollectionPage children = graphClient.users(username).drives(oneDrive.id).root().children().buildRequest().get();

      List<DriveItem> currentPage = children.getCurrentPage();
      log.debug("One Drive {} for user {} has {} items", oneDrive.name, username, currentPage.size());
      for (DriveItem driveItem : currentPage)
      {
        AdaptrisMessage adaptrisMessage = getMessageFactory().newMessage();
        InputStream remoteStream = graphClient.users(username).drives(oneDrive.id).items(driveItem.id).content().buildRequest().get();
        OutputStream outputStream = adaptrisMessage.getOutputStream();
        IOUtils.copy(remoteStream, outputStream);
        outputStream.close();

        adaptrisMessage.addMetadata("filename", driveItem.name);

        retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);

        count++;
      }

    }
    catch (Throwable e)
    {
      log.error("Exception processing One Drive file", e);
    }

    return count;
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }
}

