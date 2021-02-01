package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
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
import org.apache.commons.lang3.BooleanUtils;

import javax.validation.constraints.NotBlank;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of a file producer that can upload files to Microsoft
 * Data Lake.
 *
 * @config azure-data-lake-producer
 */
@XStreamAlias("azure-data-lake-producer")
@AdapterComponent
@ComponentProfile(summary = "Put data into a Azure Data Lake", tag = "producer,azure,data lake,data,lake")
@DisplayOrder(order = { "fileSystem", "path", "filename" })
public class DataLakeProducer extends ProduceOnlyProducerImp
{
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
   * Whether to overwrite existing files. Defaults to true.
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
  public void prepare()
  {
    /* do nothing */
  }

  /**
   * Upload the given Adaptris message to the Data Lake.
   *
   * @param adaptrisMessage The message to upload.
   * @param endpoint Ignored.
   *
   * @throws ProduceException If there is a problem uploading the file.
   */
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
      DataLakeFileSystemClient fileSystemClient = dataLakeServiceClient.getFileSystemClient(d);
      String hierarchy = "";
      for (String dir : p.split("/")) {
        hierarchy += dir;
        if (!fileSystemClient.getDirectoryClient(hierarchy).exists()) {
          fileSystemClient.createDirectory(hierarchy);
        }
        hierarchy += "/";
      }
      DataLakeDirectoryClient directoryClient = fileSystemClient.getDirectoryClient(p);

      DataLakeFileClient fileClient = directoryClient.createFile(f, overwrite());

      try (InputStream stream = adaptrisMessage.getInputStream())
      {
        long fileSize = adaptrisMessage.getSize();
        fileClient.append(stream, 0, fileSize);
        fileClient.flush(fileSize);
      }
    }
    catch (Exception e)
    {
      log.error("Could not upload file {} to {} on Data Lake file system {}", f, p, d, e);
      throw new ProduceException(e);
    }
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage)
  {
    return adaptrisMessage.resolve(filename);
  }

  private boolean overwrite()
  {
    return BooleanUtils.toBooleanDefaultIfNull(overwrite, true);
  }
}
