package com.adaptris.interlok.azure.mail;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.FixedIntervalPoller;
import com.adaptris.core.MultiPayloadMessageFactory;
import com.adaptris.core.Poller;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleConsumerCase;
import com.adaptris.util.TimeInterval;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MailFolderRequestBuilder;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.MessageCollectionRequest;
import com.microsoft.graph.requests.MessageCollectionRequestBuilder;
import com.microsoft.graph.requests.MessageRequest;
import com.microsoft.graph.requests.MessageRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;

public class O365MailConsumerTest extends ExampleConsumerCase {
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String SUBJECT = "InterlokMail Office365 Test Message";
  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";

  private static final Poller[] POLLERS = { new FixedIntervalPoller(new TimeInterval(60L, TimeUnit.SECONDS)),
      new QuartzCronPoller("0 */5 * * * ?"), };

  private GraphAPIConnection connection;
  private O365MailConsumer consumer;

  private boolean liveTests = false;

  @Before
  public void setUp() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(this.getClass().getResource("o365.properties").getFile()));
      liveTests = true;
    } catch (Exception e) {
      // do nothing
    }

    connection = new GraphAPIConnection();
    connection.setApplicationId(properties.getProperty("APPLICATION_ID", APPLICATION_ID));
    connection.setTenantId(properties.getProperty("TENANT_ID", TENANT_ID));
    connection.setClientSecret(properties.getProperty("CLIENT_SECRET", CLIENT_SECRET));

    consumer = new O365MailConsumer();
    consumer.registerConnection(connection);
    consumer.setUsername(properties.getProperty("USERNAME", USERNAME));
    consumer.setMessageFactory(new MultiPayloadMessageFactory());
  }

  @Test
  public void testLiveConsumer() throws Exception {
    Assume.assumeTrue(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try {
      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 5, 10000); // Wait until we get five new emails or for 10 seconds

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      System.out.println("Found " + messages.size() + " emails");
      Thread.sleep(5000); // Sleep for 5 seconds, otherwise the Graph SDK complains we disconnected while waiting for a response
    } catch (InterruptedIOException | InterruptedException e) {
      // Ignore these as they're occasionally thrown by the Graph SDK when the connection is closed while it's still processing
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testLiveConsumerWithSearch() throws Exception {
    Assume.assumeTrue(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    consumer.setSearch("subject:A subject");
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try {
      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 5, 10000); // Wait until we get five new emails or for 10 seconds

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      System.out.println("Found " + messages.size() + " emails");
      Thread.sleep(5000); // Sleep for 5 seconds, otherwise the Graph SDK complains we disconnected while waiting for a response
    } catch (InterruptedIOException | InterruptedException e) {
      // Ignore these as they're occasionally thrown by the Graph SDK when the connection is closed while it's still processing
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testMockConsumer() throws Exception {
    Assume.assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    consumer.registerConnection(connection);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(connection, consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try {
      when(connection.retrieveConnection(any())).thenReturn(connection);

      GraphServiceClient client = mock(GraphServiceClient.class);
      when(connection.getClientConnection()).thenReturn(client);
      UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
      when(client.users(USERNAME)).thenReturn(userRequestBuilder);
      MailFolderRequestBuilder mailRequestBuilder = mock(MailFolderRequestBuilder.class);
      when(userRequestBuilder.mailFolders(O365MailConsumer.DEFAULT_FOLDER)).thenReturn(mailRequestBuilder);
      MessageCollectionRequestBuilder messageCollectionRequestBuilder = mock(MessageCollectionRequestBuilder.class);
      when(mailRequestBuilder.messages()).thenReturn(messageCollectionRequestBuilder);
      MessageCollectionRequest messageCollectionRequest = mock(MessageCollectionRequest.class);
      when(messageCollectionRequestBuilder.buildRequest()).thenReturn(messageCollectionRequest);
      when(messageCollectionRequest.filter(O365MailConsumer.DEFAULT_FILTER)).thenReturn(messageCollectionRequest);
      MessageCollectionPage messageResponse = mock(MessageCollectionPage.class);
      when(messageCollectionRequest.get()).thenReturn(messageResponse);
      Message message = new Message();
      message.id = "7cbd04e3e8be1a706b19377ba82bd6b4";
      ItemBody body = new ItemBody();
      body.content = MESSAGE;
      message.body = body;
      message.subject = SUBJECT;
      Recipient recipient = new Recipient();
      EmailAddress emailAddress = new EmailAddress();
      emailAddress.address = "recipient@example.com";
      recipient.emailAddress = emailAddress;
      message.toRecipients = Arrays.asList(recipient);
      Recipient sender = new Recipient();
      emailAddress = new EmailAddress();
      emailAddress.address = "sender@example.com";
      sender.emailAddress = emailAddress;
      message.from = sender;
      message.ccRecipients = new ArrayList<>();
      message.hasAttachments = false;
      when(messageResponse.getCurrentPage()).thenReturn(Arrays.asList(message));

      MessageRequestBuilder messageRequestBuilder = mock(MessageRequestBuilder.class);
      when(userRequestBuilder.messages(anyString())).thenReturn(messageRequestBuilder);
      MessageRequest messageRequest = mock(MessageRequest.class);
      when(messageRequestBuilder.buildRequest()).thenReturn(messageRequest);
      when(messageRequest.select(anyString())).thenReturn(messageRequest);
      when(messageRequest.get()).thenReturn(message);

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(MESSAGE, messages.get(0).getContent());
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testQueryOptionWithSearch() throws Exception {
    O365MailConsumer consumer = new O365MailConsumer();
    consumer.setSearch("subject:\"subject\"");

    assertEquals(2, consumer.queryOptions().size());
    assertEquals(O365MailConsumer.CONSISTENCY_LEVEL_OPTION, consumer.queryOptions().get(0).getName());
    assertEquals("eventual", consumer.queryOptions().get(0).getValue());
    assertEquals(O365MailConsumer.SEARCH_OPTION, consumer.queryOptions().get(1).getName());
    assertEquals("\"subject:\"subject\"\"", consumer.queryOptions().get(1).getValue());
  }

  @Test
  public void testQueryOptionNoSearch() throws Exception {
    O365MailConsumer consumer = new O365MailConsumer();

    assertEquals(1, consumer.queryOptions().size());
    assertEquals(O365MailConsumer.FILTER_OPTION, consumer.queryOptions().get(0).getName());
    assertEquals(O365MailConsumer.DEFAULT_FILTER, consumer.queryOptions().get(0).getValue());
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    return new StandaloneConsumer(connection, consumer);
  }

  @Override
  protected List<StandaloneConsumer> retrieveObjectsForSampleConfig() {
    List<StandaloneConsumer> result = new ArrayList<>();
    for (Poller poller : POLLERS) {
      StandaloneConsumer standaloneConsumer = (StandaloneConsumer) retrieveObjectForSampleConfig();
      ((O365MailConsumer) standaloneConsumer.getConsumer()).setPoller(poller);
      result.add(standaloneConsumer);
    }
    return result;
  }
}
