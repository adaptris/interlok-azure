package com.adaptris.interlok.azure.onedrive;

import java.util.Optional;

import javax.validation.constraints.NotBlank;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.File;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadTask;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

/**
 * Implementation of a file producer that can place files into a Microsoft One Drive account, using their Graph API and OAuth2.
 *
 * @config azure-one-drive-producer
 */
@XStreamAlias("azure-one-drive-producer")
@AdapterComponent
@ComponentProfile(summary = "Place files into a Microsoft Office 365 One Drive account using the Microsoft Graph API", tag = "producer,file,o365,microsoft,office,365,one drive")
@DisplayOrder(order = { "username" })
public class OneDriveProducer extends ProduceOnlyProducerImp {
  /**
   * The username for which One Drive will be polled.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String username;

  /**
   * The name of the file to be uploaded.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String filename;

  /**
   * Whether to overwrite an existing file of the same name. (Default is true!)
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault("true")
  private Boolean overwrite;

  /**
   * {@inheritDoc}.
   */
  @Override
  public void prepare() {
  }

  /**
   * Push the Adaptris message, as a file, to the given One Drive.
   *
   * @param adaptrisMessage
   *          The message to upload.
   * @param endpoint
   *          Ignored.
   * @throws ProduceException
   *           If there was a uploading the file.
   */
  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException {
    String user = adaptrisMessage.resolve(username);
    String file = adaptrisMessage.resolve(filename);

    log.debug("Pushing file {} to One Drive as user {}", file, user);

    try {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      GraphServiceClient<?> graphClient = connection.getClientConnection();

      DriveItem newItem = new DriveItem();
      newItem.file = new File();
      newItem.name = adaptrisMessage.resolve(file);
      newItem.size = adaptrisMessage.getSize();

      boolean isNew = true;
      if (overwrite()) {
        /*
         * By default One Drive doesn't allow files to be overwritten unless the DriveItem.id matches, so quickly get it
         */
        if (getDriveItem(graphClient, user, file).isPresent()) {
          newItem = getDriveItem(graphClient, user, file).get();
          isNew = false;
        }

      }

      if (isNew) {
        // Create what could be thought of as a directory entry
        newItem = graphClient.users(user).drive().items().buildRequest().post(newItem);
      }

      // Upload the file data
      if (adaptrisMessage.getSize() < 4 * FileUtils.ONE_MB) {
        graphClient.users(user).drive().items(newItem.id).content().buildRequest().put(adaptrisMessage.getPayload());
      } else {
        String name = newItem.name;
        UploadSession uploadSession = graphClient.users(user).drive().items(newItem.id)
            .createUploadSession(new DriveItemCreateUploadSessionParameterSet()).buildRequest().post();

        LargeFileUploadTask<DriveItem> largeFileUploadTask = new LargeFileUploadTask<>(uploadSession, graphClient,
            adaptrisMessage.getInputStream(), adaptrisMessage.getSize(), DriveItem.class);

        largeFileUploadTask.upload(0, null, new IProgressCallback() {
          @Override
          public void progress(long current, long max) {
            log.debug("Uploading file {} progress is {} / {}", name, current, max);
          }
        });

      }
    } catch (Exception e) {
      log.error("Exception processing One Drive file", e);
      throw new ProduceException(e);
    }
  }

  private Optional<DriveItem> getDriveItem(GraphServiceClient<?> graphClient, String user, String name) {
    DriveItemCollectionPage children = graphClient.users(user).drive().root().children().buildRequest().get();
    for (DriveItem driveItem : children.getCurrentPage()) {
      if (driveItem.name.equals(name)) {
        return Optional.of(driveItem);
      }
    }
    return Optional.empty();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage) {
    return adaptrisMessage.resolve(username);
  }

  private boolean overwrite() {
    return BooleanUtils.toBooleanDefaultIfNull(overwrite, true);
  }
}
