package com.adaptris.interlok.azure.onedrive;

import bsh.This;
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
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Retrieve the contents of an item in a specific format. Not all files
 * can be converted into all formats.
 *
 * @config azure-one-drive-document-transform-service
 */
@XStreamAlias("azure-one-drive-document-transform-service")
@AdapterComponent
@ComponentProfile(summary = "Retrieve the contents of an item in a specific format. Not all files can be converted into all formats.", tag = "file,o365,microsoft,office,365,one drive,transform")
@DisplayOrder(order = { "username", "filename" })
public class DocumentTransformService extends ServiceImp
{
  /**
   * According to https://docs.microsoft.com/en-us/graph/api/driveitem-get-content-format?view=graph-rest-1.0&tabs=http
   * the only currently supported format is PDF.
   */
  private static final String FORMAT = "pdf";

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
   * The name of the file to be downloaded and transformed.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
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
   * Retrieve the contents of an item in a specific format.
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

      /*
       * This is the only difference with the document-download-service;
       * there should be a way to simplify/refactor.
       */
      List<Option> requestOptions = new LinkedList<>();
      requestOptions.add(new QueryOption("format", FORMAT));

      InputStream remoteStream = graphClient.users(username).drives(oneDrive.id).items(driveItem.id).content().buildRequest(requestOptions).get();
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
