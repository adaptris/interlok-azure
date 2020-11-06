package com.adaptris.interlok.azure.datalake;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.FixedIntervalPoller;
import com.adaptris.core.Poller;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.DataLakeConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleConsumerCase;
import com.adaptris.util.TimeInterval;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DataLakeConsumerTest extends ExampleConsumerCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String ACCOUNT = "example";
  private static final String FILE_SYSTEM = "some-fs";
  private static final String PATH = "some/path";

  private static final Poller[] POLLERS =
  {
    new FixedIntervalPoller(new TimeInterval(60L, TimeUnit.SECONDS)),
    new QuartzCronPoller("0 */5 * * * ?"),
  };

  private AzureConnection connection;
  private DataLakeConsumer consumer;

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

    consumer = new DataLakeConsumer();
    consumer.setMessageFactory(AdaptrisMessageFactory.getDefaultInstance());
    consumer.setFileSystem(properties.getProperty("FILE_SYSTEM", FILE_SYSTEM));
    consumer.setPath(properties.getProperty("PATH", PATH));
  }

  @Test
  public void testConsumer() throws Exception
  {
    Assume.assumeTrue(runTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try
    {
      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 25000);
    }
    catch (InterruptedIOException | InterruptedException e)
    {
      // Ignore these as they're occasionally thrown by the Graph SDK when the connection is closed while it's still processing
    }
    finally
    {
      stop(standaloneConsumer);
    }

    List<AdaptrisMessage> messages = mockMessageListener.getMessages();
    System.out.println("Received " + messages.size() + " messages");
    for (AdaptrisMessage message : messages)
    {
      System.out.println(message.getMetadataValue("filename") + " [" + message.getMetadataValue("size") + "] : " + message.getContent());
    }

  }

  @Override
  protected Object retrieveObjectForSampleConfig()
  {
    return new StandaloneConsumer(connection, consumer);
  }

  @Override
  protected List<StandaloneConsumer> retrieveObjectsForSampleConfig()
  {
    List<StandaloneConsumer> result = new ArrayList<>();
    for (Poller poller : POLLERS)
    {
      StandaloneConsumer standaloneConsumer = (StandaloneConsumer)retrieveObjectForSampleConfig();
      ((DataLakeConsumer)standaloneConsumer.getConsumer()).setPoller(poller);
      result.add(standaloneConsumer);
    }
    return result;
  }
}
