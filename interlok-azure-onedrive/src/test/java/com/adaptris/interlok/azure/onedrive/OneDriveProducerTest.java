package com.adaptris.interlok.azure.onedrive;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
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

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OneDriveProducerTest extends ExampleProducerCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String FILENAME = "cupcake.txt";
  private static final String MESSAGE = "Cupcake ipsum dolor sit amet sesame snaps. Caramels carrot cake cookie sesame snaps muffin gummi bears sweet. Carrot cake macaroon jelly ice cream muffin pastry. Jelly bonbon icing toffee macaroon topping liquorice ice cream chocolate. Powder dragée wafer sugar plum chupa chups cheesecake. Cheesecake tiramisu ice cream marshmallow gingerbread sesame snaps. Lemon drops chocolate wafer cake toffee bonbon toffee soufflé danish. Muffin caramels tootsie roll dragée candy canes danish apple pie fruitcake. Caramels chocolate bar cotton candy. Candy canes halvah cotton candy. Fruitcake jelly candy canes bear claw croissant jelly-o cookie. Pie dragée apple pie toffee dragée topping tiramisu jujubes. Cookie oat cake jujubes chocolate cake.";

  private AzureConnection connection;
  private OneDriveProducer producer;

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

    producer = new OneDriveProducer();
    producer.registerConnection(connection);
    producer.setUsername(properties.getProperty("USERNAME", USERNAME));
    producer.setFilename(FILENAME);
  }

  @Test
  public void testLiveProducer() throws Exception
  {
    Assume.assumeTrue(liveTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Test
  public void testMockProducerOverwrite() throws Exception
  {
    Assume.assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    producer.registerConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    IGraphServiceClient client = mock(IGraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    IUserRequestBuilder userRequestBuilder = mock(IUserRequestBuilder.class);
    when(client.users(USERNAME)).thenReturn(userRequestBuilder);
    IDriveRequestBuilder driveRequestBuilder = mock(IDriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);

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
    fileReference.name = FILENAME;
    fileReference.size = (long)MESSAGE.length();
    when(migraine.getCurrentPage()).thenReturn(Arrays.asList(fileReference));

    when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
    IDriveItemContentStreamRequestBuilder streamRequestBuilder = mock(IDriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    IDriveItemContentStreamRequest streamRequest = mock(IDriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(streamRequest, times(1)).put(MESSAGE.getBytes(Charset.defaultCharset()));
  }

  @Test
  public void testMockProducerNoOverwrite() throws Exception
  {
    Assume.assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    producer.registerConnection(connection);
    producer.setOverwrite(false);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    IGraphServiceClient client = mock(IGraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    IUserRequestBuilder userRequestBuilder = mock(IUserRequestBuilder.class);
    when(client.users(USERNAME)).thenReturn(userRequestBuilder);
    IDriveRequestBuilder driveRequestBuilder = mock(IDriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);

    IDriveItemCollectionRequestBuilder driveItemsRequestBuilder = mock(IDriveItemCollectionRequestBuilder.class);
    when(driveRequestBuilder.items()).thenReturn(driveItemsRequestBuilder);
    IDriveItemCollectionRequest driveItemRequest = mock(IDriveItemCollectionRequest.class);
    when(driveItemsRequestBuilder.buildRequest()).thenReturn(driveItemRequest);

    DriveItem newItem = new DriveItem();
    newItem.id = "";
    when(driveItemRequest.post(any(DriveItem.class))).thenReturn(newItem);

    IDriveItemRequestBuilder driveItemRequestBuilder = mock(IDriveItemRequestBuilder.class);
    when(driveRequestBuilder.items(newItem.id)).thenReturn(driveItemRequestBuilder);
    IDriveItemContentStreamRequestBuilder streamRequestBuilder = mock(IDriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    IDriveItemContentStreamRequest streamRequest = mock(IDriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(streamRequest, times(1)).put(MESSAGE.getBytes(Charset.defaultCharset()));
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
