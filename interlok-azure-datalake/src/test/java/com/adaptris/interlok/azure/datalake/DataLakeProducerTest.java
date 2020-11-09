package com.adaptris.interlok.azure.datalake;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.DataLakeConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataLakeProducerTest extends ExampleProducerCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String ACCOUNT = "example";
  private static final String FILE_SYSTEM = "some-fs";
  private static final String PATH = "some/path";
  private static final String NAME = "file001";

  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";

  private AzureConnection connection;
  private DataLakeProducer producer;

  private boolean runTests = false;

  @Before
  public void setUp()
  {
    Properties properties = new Properties();
    try
    {
      properties.load(new FileInputStream(this.getClass().getResource("datalake.properties").getFile()));
      runTests = true;
      connection = new DataLakeConnection();
    }
    catch (Exception e)
    {
      connection = mock(DataLakeConnection.class);
    }

    connection.setApplicationId(properties.getProperty("APPLICATION_ID", APPLICATION_ID));
    connection.setTenantId(properties.getProperty("TENANT_ID", TENANT_ID));
    connection.setClientSecret(properties.getProperty("CLIENT_SECRET", CLIENT_SECRET));

    ((DataLakeConnection)connection).setAccount(properties.getProperty("ACCOUNT", ACCOUNT));

    producer = new DataLakeProducer();
    producer.registerConnection(connection);
    producer.setFileSystem(properties.getProperty("FILE_SYSTEM", FILE_SYSTEM));
    producer.setPath(properties.getProperty("PATH", PATH));
    producer.setFilename(properties.getProperty("FILE_NAME", NAME));
  }

  @Test
  public void testLiveProducer() throws Exception
  {
    Assume.assumeTrue(runTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Test
  public void testMockProducer() throws Exception
  {
    Assume.assumeFalse(runTests);

    producer.registerConnection(connection);
    when(connection.retrieveConnection(any())).thenReturn(connection);
    DataLakeServiceClient client = mock(DataLakeServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    DataLakeFileSystemClient fsClient = mock(DataLakeFileSystemClient.class);
    when(client.getFileSystemClient(FILE_SYSTEM)).thenReturn(fsClient);
    DataLakeDirectoryClient dirClient = mock(DataLakeDirectoryClient.class);
    when(fsClient.getDirectoryClient(PATH)).thenReturn(dirClient);
    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(dirClient.createFile(NAME)).thenReturn(fileClient);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(fileClient, times(1)).append(any(InputStream.class), anyLong(), anyLong());
    verify(fileClient, times(1)).flush(MESSAGE.length());
  }

  @Override
  protected Object retrieveObjectForSampleConfig()
  {
    return new StandaloneProducer(connection, producer);
  }

  @Override
  protected List<StandaloneProducer> retrieveObjectsForSampleConfig()
  {
    return Collections.singletonList((StandaloneProducer)retrieveObjectForSampleConfig());
  }
}
