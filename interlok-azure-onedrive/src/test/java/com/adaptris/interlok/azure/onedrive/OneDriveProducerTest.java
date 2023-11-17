package com.adaptris.interlok.azure.onedrive;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemCollectionRequest;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
import com.microsoft.graph.requests.DriveItemContentStreamRequestBuilder;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.DriveRequest;
import com.microsoft.graph.requests.DriveRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;

public class OneDriveProducerTest extends ExampleProducerCase {
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String FILENAME = "cupcake.txt";
  private static final String MESSAGE = "Cupcake ipsum dolor sit amet sesame snaps. Caramels carrot cake cookie sesame snaps muffin gummi bears sweet. Carrot cake macaroon jelly ice cream muffin pastry. Jelly bonbon icing toffee macaroon topping liquorice ice cream chocolate. Powder dragée wafer sugar plum chupa chups cheesecake. Cheesecake tiramisu ice cream marshmallow gingerbread sesame snaps. Lemon drops chocolate wafer cake toffee bonbon toffee soufflé danish. Muffin caramels tootsie roll dragée candy canes danish apple pie fruitcake. Caramels chocolate bar cotton candy. Candy canes halvah cotton candy. Fruitcake jelly candy canes bear claw croissant jelly-o cookie. Pie dragée apple pie toffee dragée topping tiramisu jujubes. Cookie oat cake jujubes chocolate cake.";

  private GraphAPIConnection connection;
  private OneDriveProducer producer;

  private boolean liveTests = false;

  @BeforeEach
  public void setUp() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(this.getClass().getResource("onedrive.properties").getFile()));
      liveTests = true;
    } catch (Exception e) {
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
  public void testLiveProducer() throws Exception {
    assumeTrue(liveTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Test
  public void testLiveProducerLarge() throws Exception {
    assumeTrue(liveTests);

    producer.setFilename("big-data.txt");

    StringBuilder bigData = new StringBuilder();
    do {
      bigData.append(MESSAGE).append("\n");
    } while (bigData.length() < 4 * FileUtils.ONE_MB);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(bigData.toString());
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Test
  public void testMockProducerOverwrite() throws Exception {
    assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    producer.registerConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(USERNAME)).thenReturn(userRequestBuilder);
    DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);

    DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
    when(driveRequestBuilder.root()).thenReturn(driveItemRequestBuilder);
    DriveItemCollectionRequestBuilder childrenRequest = mock(DriveItemCollectionRequestBuilder.class);
    when(driveItemRequestBuilder.children()).thenReturn(childrenRequest);
    DriveItemCollectionRequest children = mock(DriveItemCollectionRequest.class);
    when(childrenRequest.buildRequest()).thenReturn(children);
    DriveItemCollectionPage migraine = mock(DriveItemCollectionPage.class);
    when(children.get()).thenReturn(migraine);

    DriveItem fileReference = new DriveItem();
    fileReference.id = "73271d08680510a91aec95295ed03b90";
    fileReference.name = FILENAME;
    fileReference.size = (long) MESSAGE.length();
    when(migraine.getCurrentPage()).thenReturn(Arrays.asList(fileReference));

    when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
    DriveItemContentStreamRequestBuilder streamRequestBuilder = mock(DriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    DriveItemContentStreamRequest streamRequest = mock(DriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(streamRequest, times(1)).put(MESSAGE.getBytes(Charset.defaultCharset()));
  }

  @Test
  public void testMockProducerNoOverwrite() throws Exception {
    assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    producer.registerConnection(connection);
    producer.setOverwrite(false);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(USERNAME)).thenReturn(userRequestBuilder);
    DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);

    DriveItemCollectionRequestBuilder driveItemsRequestBuilder = mock(DriveItemCollectionRequestBuilder.class);
    when(driveRequestBuilder.items()).thenReturn(driveItemsRequestBuilder);
    DriveItemCollectionRequest driveItemRequest = mock(DriveItemCollectionRequest.class);
    when(driveItemsRequestBuilder.buildRequest()).thenReturn(driveItemRequest);

    DriveItem newItem = new DriveItem();
    newItem.id = "";
    when(driveItemRequest.post(any(DriveItem.class))).thenReturn(newItem);

    DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
    when(driveRequestBuilder.items(newItem.id)).thenReturn(driveItemRequestBuilder);
    DriveItemContentStreamRequestBuilder streamRequestBuilder = mock(DriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    DriveItemContentStreamRequest streamRequest = mock(DriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(streamRequest, times(1)).put(MESSAGE.getBytes(Charset.defaultCharset()));
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    return new StandaloneProducer(connection, producer);
  }

  @Override
  protected List<StandaloneProducer> retrieveObjectsForSampleConfig() {
    return Collections.singletonList((StandaloneProducer) retrieveObjectForSampleConfig());
  }

}
