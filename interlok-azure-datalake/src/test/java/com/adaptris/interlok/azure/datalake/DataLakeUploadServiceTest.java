package com.adaptris.interlok.azure.datalake;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.DataLakeConnection;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataLakeUploadServiceTest extends ExampleServiceCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String ACCOUNT = "example";
  private static final String FILE_SYSTEM = "some-fs";
  private static final String PATH = "some/path";
  private static final String NAME = "file002";

  private static final String MESSAGE = "Cupcake ipsum dolor sit. Amet gummi bears cake sesame snaps. Gummi bears halvah icing sweet roll cake lollipop pastry cake pastry. Oat cake jelly beans lollipop muffin wafer marzipan. Tart biscuit tiramisu jujubes. Apple pie sweet roll wafer carrot cake cookie sugar plum chocolate bar cupcake. Dessert topping bear claw icing chocolate cake apple pie lemon drops topping. Cake carrot cake sugar plum apple pie chupa chups. Bonbon marzipan jelly beans gingerbread dessert biscuit. Cake apple pie sweet roll. Dessert dessert chocolate bar lemon drops sweet sweet roll dessert dessert marshmallow. Danish toffee brownie apple pie.";

  private AzureConnection connection;
  private DataLakeUploadService service;

  private boolean runTests = false;

  @Before
  public void setUp()
  {
    Properties properties = new Properties();
    try
    {
      properties.load(new FileInputStream(this.getClass().getResource("datalake.properties").getFile()));
      runTests = true;
    }
    catch (Exception e)
    {
      // do nothing
    }

    connection = new DataLakeConnection();
    connection.setApplicationId(properties.getProperty("APPLICATION_ID", APPLICATION_ID));
    connection.setTenantId(properties.getProperty("TENANT_ID", TENANT_ID));
    connection.setClientSecret(properties.getProperty("CLIENT_SECRET", CLIENT_SECRET));

    ((DataLakeConnection)connection).setAccount(properties.getProperty("ACCOUNT", ACCOUNT));

    service = new DataLakeUploadService();
    service.setConnection(connection);
    service.setFileSystem(properties.getProperty("FILE_SYSTEM", FILE_SYSTEM));
    service.setPath(properties.getProperty("PATH", PATH));
    service.setFilename(properties.getProperty("FILE_NAME", NAME));
  }

  @Test
  public void testLiveService() throws Exception
  {
    Assume.assumeTrue(runTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    service.doService(message);
  }

  @Test
  public void testMockService() throws Exception
  {
    Assume.assumeFalse(runTests);

    connection = mock(DataLakeConnection.class);
    service.setConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    DataLakeServiceClient client = mock(DataLakeServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    DataLakeFileSystemClient fsClient = mock(DataLakeFileSystemClient.class);
    when(client.getFileSystemClient(FILE_SYSTEM)).thenReturn(fsClient);
    DataLakeDirectoryClient dirClient = mock(DataLakeDirectoryClient.class);
    when(fsClient.getDirectoryClient(PATH)).thenReturn(dirClient);
    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(dirClient.createFile(NAME, true)).thenReturn(fileClient);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    service.doService(message);

    verify(fileClient, times(1)).append(any(InputStream.class), anyLong(), anyLong());
    verify(fileClient, times(1)).flush(MESSAGE.length());
  }

  @Override
  protected Object retrieveObjectForSampleConfig()
  {
    return service;
  }
}
