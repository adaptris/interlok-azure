package com.adaptris.interlok.azure.onedrive;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.FixedIntervalPoller;
import com.adaptris.core.MultiPayloadMessageFactory;
import com.adaptris.core.Poller;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleConsumerCase;
import com.adaptris.util.TimeInterval;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.DriveRequest;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequest;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.extensions.IDriveItemContentStreamRequest;
import com.microsoft.graph.requests.extensions.IDriveItemContentStreamRequestBuilder;
import com.microsoft.graph.requests.extensions.IDriveItemRequestBuilder;
import com.microsoft.graph.requests.extensions.IDriveRequestBuilder;
import com.microsoft.graph.requests.extensions.IUserRequestBuilder;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OneDriveConsumerTest extends ExampleConsumerCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String CONTENT = "A bud light makes love to a Bacardi Silver about the Octoberfest. Now and then, the funny Amarillo Pale Ale seldom makes love to the steam engine of another girl scout. If the Pilsner beyond a burglar ale slurly satiates a shabby girl scout, then the change reads a magazine. A psychotic St. Pauli Girl dies, because the fat satellite brewery drunkenly finds lice on the financial Pilsner. When a Heineken near a Bridgeport ESB leaves, the air hocky table hesitates.";

  private static final Poller[] POLLERS =
  {
    new FixedIntervalPoller(new TimeInterval(60L, TimeUnit.SECONDS)),
    new QuartzCronPoller("0 */5 * * * ?"),
  };

  private AzureConnection connection;
  private OneDriveConsumer consumer;

  private boolean liveTests = false;

  @Before
  public void setUp()
  {
    Properties properties = new Properties();
    try
    {
      properties.load(new FileInputStream(this.getClass().getResource("onedrive.properties").getFile()));
      liveTests = true;
    }
    catch (Exception e)
    {
      // do nothing
    }

    connection = new GraphAPIConnection();
    connection.setApplicationId(properties.getProperty("APPLICATION_ID", APPLICATION_ID));
    connection.setTenantId(properties.getProperty("TENANT_ID", TENANT_ID));
    connection.setClientSecret(properties.getProperty("CLIENT_SECRET", CLIENT_SECRET));

    consumer = new OneDriveConsumer();
    consumer.setUsername(properties.getProperty("USERNAME", USERNAME));
    consumer.setMessageFactory(new MultiPayloadMessageFactory());
  }

  @Test
  public void testLiveConsumer() throws Exception
  {
    Assume.assumeTrue(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try
    {
      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 25000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      System.out.println("Found " + messages.size() + " files");
      Thread.sleep(5000); // sleep for 5 seconds, otherwise the Graph SDK complains we disconnected while waiting for a response
    }
    catch (InterruptedIOException | InterruptedException e)
    {
      // Ignore these as they're occasionally thrown by the Graph SDK when the connection is closed while it's still processing
    }
    finally
    {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testMockConsumer() throws Exception
  {
    Assume.assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    consumer.registerConnection(connection);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try
    {
      when(connection.retrieveConnection(any())).thenReturn(connection);

      IGraphServiceClient client = mock(IGraphServiceClient.class);
      when(connection.getClientConnection()).thenReturn(client);
      IUserRequestBuilder userRequestBuilder = mock(IUserRequestBuilder.class);
      when(client.users(USERNAME)).thenReturn(userRequestBuilder);
      IDriveRequestBuilder driveRequestBuilder = mock(IDriveRequestBuilder.class);
      when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
      DriveRequest driveRequest = mock(DriveRequest.class);
      when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);
      Drive drive = new Drive();
      drive.id = "8e7a00f1daf1c2b7015459dd686856c2";
      when(driveRequest.get()).thenReturn(drive);
      when(userRequestBuilder.drives(drive.id)).thenReturn(driveRequestBuilder);
      IDriveItemRequestBuilder driveItemRequestBuilder = mock(IDriveItemRequestBuilder.class);
      when(driveRequestBuilder.root()).thenReturn(driveItemRequestBuilder);
      IDriveItemCollectionRequestBuilder childrenRequest = mock(IDriveItemCollectionRequestBuilder.class);
      when(driveItemRequestBuilder.children()).thenReturn(childrenRequest);
      IDriveItemCollectionRequest children = mock(IDriveItemCollectionRequest.class);
      when(childrenRequest.buildRequest()).thenReturn(children);
      IDriveItemCollectionPage migraine = mock(IDriveItemCollectionPage.class);
      when(children.get()).thenReturn(migraine);
      DriveItem fileReference = new DriveItem();
      fileReference.id = "73271d08680510a91aec95295ed03b90";
      fileReference.name = "beer";
      fileReference.size = (long)CONTENT.length();
      when(migraine.getCurrentPage()).thenReturn(Arrays.asList(fileReference));
      when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
      IDriveItemContentStreamRequestBuilder streamRequestBuilder = mock(IDriveItemContentStreamRequestBuilder.class);
      when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
      IDriveItemContentStreamRequest streamRequest = mock(IDriveItemContentStreamRequest.class);
      when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);
      when(streamRequest.get()).thenReturn(new ByteArrayInputStream(CONTENT.getBytes(Charset.defaultCharset())));

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(CONTENT, messages.get(0).getContent());
    }
    catch (InterruptedIOException | InterruptedException e)
    {
      // Ignore these as they're occasionally thrown by the Graph SDK when the connection is closed while it's still processing
    }
    finally
    {
      stop(standaloneConsumer);
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
    List<StandaloneConsumer> result = new ArrayList();
    for (Poller poller : POLLERS)
    {
      StandaloneConsumer standaloneConsumer = (StandaloneConsumer)retrieveObjectForSampleConfig();
      ((OneDriveConsumer)standaloneConsumer.getConsumer()).setPoller(poller);
      result.add(standaloneConsumer);
    }
    return result;
  }
}
