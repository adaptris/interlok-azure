package com.adaptris.interlok.azure.onedrive;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnection;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Retrieve the contents of a file from OneDrive.
 *
 * @config azure-one-drive-document-download-service
 */
@XStreamAlias("azure-one-drive-document-download-service")
@AdapterComponent
@ComponentProfile(summary = "Retrieve the contents of a file from OneDrive.", tag = "file,o365,microsoft,office,365,one drive,download")
@DisplayOrder(order = { "connection", "username", "filename" })
public class DocumentDownloadService extends ServiceImp
{
  /**
   * Connection to Azure OneDrive.
   */
  @Getter
  @Setter
  @NotNull
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
   * The name of the file to be downloaded.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true, friendly = "The name and path of the file to be downloaded")
  private String filename;

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void initService()
  {
    /* do nothing */
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void closeService()
  {
    /* do nothing */
  }

  /**
   * Retrieve the contents of a file from OneDrive.
   *
   * @param adaptrisMessage the <code>AdaptrisMessage</code> to process
   * @throws ServiceException wrapping any underlying <code>Exception</code>s
   */
  @Override
  public void doService(AdaptrisMessage adaptrisMessage) throws ServiceException
  {
    String user = adaptrisMessage.resolve(username);
    String file = adaptrisMessage.resolve(filename);

    try
    {
      IGraphServiceClient graphClient = ((GraphAPIConnection)connection).getClientConnection();

      Drive oneDrive = graphClient.users(username).drive().buildRequest().get();
      DriveItem driveItem = graphClient.users(user).drives(oneDrive.id).root().itemWithPath(file).buildRequest().get();

      InputStream remoteStream = graphClient.users(username).drives(oneDrive.id).items(driveItem.id).content().buildRequest().get();
      OutputStream outputStream = adaptrisMessage.getOutputStream();
      IOUtils.copy(remoteStream, outputStream);
      outputStream.close();

      adaptrisMessage.addMetadata("filename", driveItem.name);
    }
    catch (Throwable e)
    {
      log.error("Exception processing One Drive file", e);
      throw new ServiceException(e);
    }
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public void prepare()
  {
    /* do nothing */
  }
}
