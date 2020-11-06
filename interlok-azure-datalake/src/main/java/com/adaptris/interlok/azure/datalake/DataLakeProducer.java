package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.interlok.azure.DataLakeConnection;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

@XStreamAlias("data-lake-producer")
@AdapterComponent
@ComponentProfile(summary = "Put data into a Azure Data Lake", tag = "producer,azure,data lake,data,lake")
@DisplayOrder(order = { })
public class DataLakeProducer extends ProduceOnlyProducerImp
{
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String fileSystem;

  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String path;

  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String filename;

  @Override
  public void prepare()
  {
    /* do nothing */
  }

  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException
  {
    String d = adaptrisMessage.resolve(fileSystem);
    String p = adaptrisMessage.resolve(path);
    String f = adaptrisMessage.resolve(filename);
    log.debug("Placing file {} into directory {} on file system {}", f, p, d);

    try
    {
      DataLakeConnection connection = retrieveConnection(DataLakeConnection.class);
      DataLakeServiceClient dataLakeServiceClient = connection.getClientConnection();
      DataLakeFileSystemClient fileSystemClient = dataLakeServiceClient.getFileSystemClient(fileSystem);
      DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient(path);
      DataLakeFileClient fileClient = directoryClient.createFile(f);

      InputStream stream = adaptrisMessage.getInputStream();
      long fileSize = adaptrisMessage.getSize();
      fileClient.append(stream, 0, fileSize);
      fileClient.flush(fileSize);
    }
    catch (Exception e)
    {
      log.error("Could not upload file {} to {} on Data Lake file system {}", f, p, d, e);
      throw new ProduceException(e);
    }
  }

  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage)
  {
    return adaptrisMessage.resolve(filename);
  }
}
