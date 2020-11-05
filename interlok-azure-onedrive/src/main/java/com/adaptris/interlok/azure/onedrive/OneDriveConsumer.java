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
import com.adaptris.interlok.azure.AzureConnection;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequest;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import javax.validation.constraints.NotBlank;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementation of a file consumer that can retrieve files from
 * Microsoft One Drive, using their Graph API and OAuth2.
 *
 * @config one-drive-consumer
 */
@XStreamAlias("one-drive-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup files from a Microsoft Office 365 One Drive account using the Microsoft Graph API", tag = "consumer,file,o365,microsoft,office,365,one drive")
@DisplayOrder(order = { "username" })
public class OneDriveConsumer extends AdaptrisPollingConsumer
{
  @Getter
  @Setter
  @NotBlank
  private String username;

  @Override
  protected void prepareConsumer()
  {
    /* do nothing */
  }

  @Override
  protected int processMessages()
  {
    log.debug("Polling for files in One Drive as user " + username);

    int count = 0;
    try
    {
      AzureConnection connection = retrieveConnection(AzureConnection.class);
      IGraphServiceClient graphClient = connection.getClient();

      Drive drive = graphClient.users(username).drive().buildRequest().get();
      IDriveItemCollectionPage children = graphClient.users(username).drives(drive.id).root().children().buildRequest().get();

      List<DriveItem> currentPage = children.getCurrentPage();
      log.debug("One Drive {} for user {} has {} items", drive.name, username, currentPage.size());
      for (DriveItem driveItem : currentPage)
      {
        AdaptrisMessage adaptrisMessage = getMessageFactory().newMessage();
        InputStream remoteStream = graphClient.users(username).drives(drive.id).items(driveItem.id).content().buildRequest().get();
        IOUtils.copy(remoteStream, adaptrisMessage.getOutputStream());

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

  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }
}

