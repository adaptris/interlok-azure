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

package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.azure.AzureConnection;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.ListPathsOptions;
import com.azure.storage.file.datalake.models.PathItem;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IClientSecret;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

@XStreamAlias("data-lake-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup data frmo Azure Data Lake", tag = "consumer,azure,data lake,data,lake")
@DisplayOrder(order = { })
public class DataLakeConsumer extends AdaptrisPollingConsumer
{
  @Getter
  @Setter
  @NotBlank
  private String account;

  @Getter
  @Setter
  @NotBlank
  private String fileSystem;

  @Getter
  @Setter
  @NotBlank
  private String path;

  @Override
  protected void prepareConsumer()
  {
    /* do nothing */
  }

  @Override
  protected int processMessages()
  {
    log.debug("Polling for data in Azure Data Lake");

    int count = 0;
    try
    {
      AzureConnection connection = retrieveConnection(AzureConnection.class);

      log.debug("Scanning {}:{} for files", fileSystem, path);

      DataLakeServiceClientBuilder builder = new DataLakeServiceClientBuilder();
      DataLakeServiceClient dataLakeServiceClient = builder.credential(connection.getClientSecretCredential()).endpoint(endpoint()).buildClient();
      DataLakeFileSystemClient fileSystemClient = dataLakeServiceClient.getFileSystemClient(fileSystem);
      DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient(path);

      ListPathsOptions options = new ListPathsOptions();
      options.setPath(path);
      PagedIterable<PathItem> pagedIterable = fileSystemClient.listPaths(options, null);
      for (PathItem item : pagedIterable)
      {
        String name = FilenameUtils.getName(item.getName());
        if (item.isDirectory())
        {
          log.debug("Skipping directory {}", name);
        }
        log.debug("Found file {}", name);

        AdaptrisMessage message = getMessageFactory().newMessage();
        message.addMetadata("filename", name);
        message.addMetadata("size", String.valueOf(item.getContentLength()));

        DataLakeFileClient fileClient = directoryClient.getFileClient(name);

        OutputStream os = message.getOutputStream();
        fileClient.read(os);
        os.close();

        retrieveAdaptrisMessageListener().onAdaptrisMessage(message);
      }

      count++;
    }
    catch (Throwable e)
    {
      log.error("Exception in Data Lake", e);
    }

    return count;
  }

  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }

  private String endpoint()
  {
    return String.format("https://%s.dfs.core.windows.net", account);
  }
}

