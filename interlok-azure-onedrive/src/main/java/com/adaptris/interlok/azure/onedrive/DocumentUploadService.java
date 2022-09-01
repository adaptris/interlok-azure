package com.adaptris.interlok.azure.onedrive;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnection;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ConnectedService;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

/**
 * Upload the contents of a message to a file in OneDrive.
 *
 * @config azure-one-drive-document-upload-service
 */
@XStreamAlias("azure-one-drive-document-upload-service")
@AdapterComponent
@ComponentProfile(summary = "Upload the contents of a message to a file in OneDrive.", tag = "file,o365,microsoft,office,365,one drive,upload", recommended = {
    GraphAPIConnection.class })
@DisplayOrder(order = { "connection", "username", "filename" })
public class DocumentUploadService extends ServiceImp implements ConnectedService {
  /**
   * Connection to Azure OneDrive.
   */
  @Getter
  @Setter
  @NotNull
  @InputFieldHint(ofType = "com.adaptris.interlok.azure.AzureConnection")
  private AdaptrisConnection connection;

  /**
   * The username for which One Drive will be polled.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String username;

  /**
   * The name and path of the file to be uploaded.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true, friendly = "The name and path of the file to be uploaded")
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
   * <p>
   * Apply the service to the message.
   * </p>
   *
   * @param adaptrisMessage
   *          the <code>AdaptrisMessage</code> to process
   * @throws ServiceException
   *           wrapping any underlying <code>Exception</code>s
   */
  @Override
  public void doService(AdaptrisMessage adaptrisMessage) throws ServiceException {
    OneDriveProducer producer = new OneDriveProducer();
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);

    try {
      producer.registerConnection(connection);
      producer.setUsername(adaptrisMessage.resolve(username));
      producer.setFilename(adaptrisMessage.resolve(filename));
      producer.setOverwrite(overwrite);

      LifecycleHelper.initAndStart(standaloneProducer, false);

      standaloneProducer.doService(adaptrisMessage);
    } catch (Exception e) {
      log.error("Could not upload file", e);
      throw new ServiceException(e);
    } finally {
      LifecycleHelper.stopAndClose(standaloneProducer, false);
    }
  }

  /**
   * Calls LifecycleHelper#prepare for the Azure connection.
   *
   * @throws CoreException
   *           If teh connection could not be prepared.
   */
  @Override
  public void prepare() throws CoreException {
    LifecycleHelper.prepare(connection);
  }

  /**
   * Calls LifecycleHelper#init for the Azure connection.
   */
  @Override
  protected void initService() throws CoreException {
    LifecycleHelper.init(connection);
  }

  /**
   * Calls LifecycleHelper#start for the Azure connection.
   *
   * @throws CoreException
   *           If teh connection could not be prepared.
   */
  @Override
  public void start() throws CoreException {
    super.start();
    LifecycleHelper.start(connection);
  }

  /**
   * Calls LifecycleHelper#stop for the Azure connection.
   */
  @Override
  public void stop() {
    super.stop();
    LifecycleHelper.stop(connection);
  }

  /**
   * Calls LifecycleHelper#close for the Azure connection.
   */
  @Override
  protected void closeService() {
    LifecycleHelper.close(connection);
  }

}
