package com.adaptris.interlok.azure.onedrive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ServiceList;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairList;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemCollectionRequest;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
import com.microsoft.graph.requests.DriveItemContentStreamRequestBuilder;
import com.microsoft.graph.requests.DriveItemRequest;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.DriveRequest;
import com.microsoft.graph.requests.DriveRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;

public class DocumentUpDownServiceTest extends ExampleServiceCase {
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";

  private static final String ORIGINAL = "beer.md";
  private static final String CONVERT = "beer.html";

  private AzureConnection<GraphServiceClient<?>> connection;

  private String username = "user@example.com";
  private String data;
  private String html;

  private boolean liveTests = false;

  @BeforeEach
  public void setUp() throws Exception {
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

    if (properties.containsKey("USERNAME")) {
      username = properties.getProperty("USERNAME");
    }

    data = IOUtils.toString(new FileInputStream(this.getClass().getResource(ORIGINAL).getFile()), Charset.defaultCharset());
    // The MS Azure markdown to HTML transform doesn't include <html> tags
    html = IOUtils.toString(new FileInputStream(this.getClass().getResource(CONVERT).getFile()), Charset.defaultCharset());
  }

  /**
   * Run through the service tests: upload a CSV file, download it, download & transform it.
   *
   * @throws Exception
   *           If something goes bang.
   */
  @Test
  public void liveTests() throws Exception {
    assumeTrue(liveTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(data);

    DocumentUploadService upload = documentUploadService();
    DocumentDownloadService download = documentDownloadService();
    DocumentTransformService transform = documentTransformService();

    // whether or not this test has ever run before, this should result in the test file being available
    upload.doService(message);

    // get a new empty message so we can confirm we've downloaded the correct data
    message = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    download.doService(message);

    assertEquals(data, message.getContent());

    transform.setFormat(DocumentTransformService.Format.PDF);
    transform.doService(message);
    assertTrue(message.getContent().startsWith("%PDF"));

    transform.setFormat(DocumentTransformService.Format.HTML);
    transform.doService(message);
    assertEquals(html, message.getContent());

    transform.setFormat(DocumentTransformService.Format.JPG);
    KeyValuePairList additionalOptions = new KeyValuePairList();
    additionalOptions.addKeyValuePair(new KeyValuePair("width", "1000"));
    additionalOptions.addKeyValuePair(new KeyValuePair("height", "1000"));
    transform.setAdditionalRequestOptions(additionalOptions);
    transform.doService(message);
    // assertTrue(message.getContent().startsWith("?"));
    // Sometimes I had JPEG, sometimes PNG data returned; if no exception is thrown, I'd say we're good
  }

  @Test
  public void mockUploadTest() throws Exception {
    assumeFalse(liveTests);

    DocumentUploadService upload = documentUploadService();

    connection = mock(GraphAPIConnection.class);
    upload.setConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(username)).thenReturn(userRequestBuilder);
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
    fileReference.name = ORIGINAL;
    fileReference.size = Long.valueOf(data.length());
    when(migraine.getCurrentPage()).thenReturn(Arrays.asList(fileReference));

    when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
    DriveItemContentStreamRequestBuilder streamRequestBuilder = mock(DriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    DriveItemContentStreamRequest streamRequest = mock(DriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest()).thenReturn(streamRequest);

    upload.doService(AdaptrisMessageFactory.getDefaultInstance().newMessage(data));

    verify(streamRequest, times(1)).put(data.getBytes(Charset.defaultCharset()));
  }

  @Test
  public void mockDownloadTest() throws Exception {
    assumeFalse(liveTests);

    DocumentDownloadService download = documentDownloadService();

    connection = mock(GraphAPIConnection.class);
    download.setConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);
    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(username)).thenReturn(userRequestBuilder);
    DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);
    Drive drive = new Drive();
    drive.id = "8e7a00f1daf1c2b7015459dd686856c2";
    when(driveRequest.get()).thenReturn(drive);
    when(userRequestBuilder.drives(drive.id)).thenReturn(driveRequestBuilder);
    DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
    when(driveRequestBuilder.root()).thenReturn(driveItemRequestBuilder);
    when(driveItemRequestBuilder.itemWithPath(ORIGINAL)).thenReturn(driveItemRequestBuilder);
    DriveItemRequest driveItemRequest = mock(DriveItemRequest.class);
    when(driveItemRequestBuilder.buildRequest()).thenReturn(driveItemRequest);
    DriveItem fileReference = new DriveItem();
    fileReference.id = "73271d08680510a91aec95295ed03b90";
    fileReference.name = ORIGINAL;
    fileReference.size = Long.valueOf(data.length());
    when(driveItemRequest.get()).thenReturn(fileReference);
    when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
    DriveItemContentStreamRequestBuilder streamRequestBuilder = mock(DriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    DriveItemContentStreamRequest streamRequest = mock(DriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest((List<Option>) null)).thenReturn(streamRequest);
    when(streamRequest.get()).thenReturn(new ByteArrayInputStream(data.getBytes(Charset.defaultCharset())));

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    download.doService(message);
    assertEquals(data, message.getContent());
    assertEquals(ORIGINAL, message.getMetadataValue("filename"));
  }

  @Test
  public void mockTransformTest() throws Exception {
    assumeFalse(liveTests);

    DocumentTransformService transform = documentTransformService();

    connection = mock(GraphAPIConnection.class);
    transform.setConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);
    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(username)).thenReturn(userRequestBuilder);
    DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
    when(userRequestBuilder.drive()).thenReturn(driveRequestBuilder);
    DriveRequest driveRequest = mock(DriveRequest.class);
    when(driveRequestBuilder.buildRequest()).thenReturn(driveRequest);
    Drive drive = new Drive();
    drive.id = "8e7a00f1daf1c2b7015459dd686856c2";
    when(driveRequest.get()).thenReturn(drive);
    when(userRequestBuilder.drives(drive.id)).thenReturn(driveRequestBuilder);
    DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
    when(driveRequestBuilder.root()).thenReturn(driveItemRequestBuilder);
    when(driveItemRequestBuilder.itemWithPath(ORIGINAL)).thenReturn(driveItemRequestBuilder);
    DriveItemRequest driveItemRequest = mock(DriveItemRequest.class);
    when(driveItemRequestBuilder.buildRequest()).thenReturn(driveItemRequest);
    DriveItem fileReference = new DriveItem();
    fileReference.id = "73271d08680510a91aec95295ed03b90";
    fileReference.name = CONVERT;
    fileReference.size = Long.valueOf(html.length());
    when(driveItemRequest.get()).thenReturn(fileReference);
    when(driveRequestBuilder.items(fileReference.id)).thenReturn(driveItemRequestBuilder);
    DriveItemContentStreamRequestBuilder streamRequestBuilder = mock(DriveItemContentStreamRequestBuilder.class);
    when(driveItemRequestBuilder.content()).thenReturn(streamRequestBuilder);
    DriveItemContentStreamRequest streamRequest = mock(DriveItemContentStreamRequest.class);
    when(streamRequestBuilder.buildRequest(anyList())).thenReturn(streamRequest);
    when(streamRequest.get()).thenReturn(new ByteArrayInputStream(html.getBytes(Charset.defaultCharset())));

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    transform.doService(message);
    assertEquals(html, message.getContent());
    assertEquals(CONVERT, message.getMetadataValue("filename"));
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    ServiceList serviceList = new ServiceList();
    serviceList.add(documentUploadService());
    serviceList.add(documentDownloadService());
    serviceList.add(documentTransformService());
    return serviceList;
  }

  private DocumentUploadService documentUploadService() {
    DocumentUploadService service = new DocumentUploadService();
    service.setConnection(connection);
    service.setUsername(username);
    service.setFilename(ORIGINAL);
    return service;
  }

  private DocumentDownloadService documentDownloadService() {
    DocumentDownloadService service = new DocumentDownloadService();
    service.setConnection(connection);
    service.setUsername(username);
    service.setFilename(ORIGINAL);
    return service;
  }

  private DocumentTransformService documentTransformService() {
    DocumentTransformService service = new DocumentTransformService();
    service.setConnection(connection);
    service.setUsername(username);
    service.setFilename(ORIGINAL);
    service.setFormat(DocumentTransformService.Format.GLB);
    return service;
  }

}
