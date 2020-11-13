package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.azure.DataLakeConnection;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Upload files to Microsoft Data Lake.
 *
 * @config azure-data-lake-upload-service
 */
@XStreamAlias("azure-data-lake-upload-service")
@AdapterComponent
@ComponentProfile(summary = "Put data into a Azure Data Lake", tag = "service,azure,data lake,data,lake")
@DisplayOrder(order = { "fileSystem", "path", "filename" })
public class DataLakeUploadService extends ServiceImp
{
  /**
   * A connection to an Azure Data Lake.
   */
  @Getter
  @Setter
  @NotNull
  private DataLakeConnection connection;
  
  /**
   * The Data Lake file system to access.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String fileSystem;

  /**
   * The path to poll for files.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String path;

  /**
   * The name of the file to put into the Data Lake.
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
   * Upload the given message to the Data Lake.
   *
   * @param adaptrisMessage The message to upload.
   * @throws ServiceException If there was an issue uploading the file.
   */
  @Override
  public void doService(AdaptrisMessage adaptrisMessage) throws ServiceException
  {
    DataLakeProducer producer = new DataLakeProducer();
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);

    try
    {
      producer.registerConnection(connection);
      producer.setFileSystem(adaptrisMessage.resolve(fileSystem));
      producer.setPath(adaptrisMessage.resolve(path));
      producer.setFilename(adaptrisMessage.resolve(filename));

      LifecycleHelper.initAndStart(standaloneProducer, false);

      standaloneProducer.doService(adaptrisMessage);
    }
    catch (Exception e)
    {
      log.error("Could not upload file", e);
      throw new ServiceException(e);
    }
    finally
    {
      LifecycleHelper.stopAndClose(standaloneProducer, false);
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
