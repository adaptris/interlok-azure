package com.adaptris.interlok.azure.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.FixedIntervalPoller;
import com.adaptris.core.Poller;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.mail.MetadataMailHeaders;
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
import com.microsoft.graph.requests.MessageStreamRequest;
import com.microsoft.graph.requests.MessageStreamRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;

public class O365RawMailConsumerTest extends ExampleConsumerCase {
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String SUBJECT = "InterlokMail Office365 Test Message";
  private static final String FROM = "sender@example.com";
  private static final String TO = "recipient@example.com";
  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";
  private static final String MESSAGE_MIME = "From: " + FROM + "\nTo: " + TO + "\nSubject: " + SUBJECT + "\n"
      + "Message-ID: message-id\nMIME-Version: 1.0\nContent-Type: multipart/mixed; boundary=\"XXXXboundary_text\"\n"
      + "Content-Type: text/plain\n\n" + MESSAGE + "\n\n--XXXXboundary_text--";
  private static final String MESSAGE_MIME_ATTACHMENT = MESSAGE_MIME + "\n" + "Content-Type: text/plain; name=\"filename.txt\"\n"
      + "Content-Description: filename.txt\n" + "Content-Disposition: attachment; filename=\"filename.txt\"; size=7;\n"
      + "    creation-date=\"Thu, 01 Sep 2022 00:0:00 GMT\";\n" + "    modification-date=\"Thu, 02 Sep 2022 00:00:00 GMT\"\n"
      + "Content-Transfer-Encoding: base64\n" + "\n" + "Y29udGVudA==\n" + "\n" + "--XXXXboundary_text--";

  private static final Poller[] POLLERS = { new FixedIntervalPoller(new TimeInterval(60L, TimeUnit.SECONDS)),
      new QuartzCronPoller("0 */5 * * * ?"), };

  private GraphAPIConnection connection;
  private Properties properties;

  private boolean liveTests = false;

  @BeforeEach
  public void setUp() {
    properties = new Properties();
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
  }

  @Test
  public void testLiveConsumer() throws Exception {
    assumeTrue(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    O365RawMailConsumer consumer = newConsumer();
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
    assumeTrue(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    O365RawMailConsumer consumer = newConsumer();
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
    assumeFalse(liveTests);

    GraphAPIConnection connection = mock(GraphAPIConnection.class);
    O365RawMailConsumer consumer = newConsumer();
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
      when(messageCollectionRequestBuilder.buildRequest(anyList())).thenReturn(messageCollectionRequest);
      when(messageCollectionRequest.filter(O365MailConsumer.DEFAULT_FILTER)).thenReturn(messageCollectionRequest);
      MessageCollectionPage messageResponse = mock(MessageCollectionPage.class);
      when(messageCollectionRequest.get()).thenReturn(messageResponse);
      Message message = newMessage(false);
      when(messageResponse.getCurrentPage()).thenReturn(Arrays.asList(message));

      MessageRequestBuilder messageRequestBuilder = mock(MessageRequestBuilder.class);
      when(userRequestBuilder.messages(anyString())).thenReturn(messageRequestBuilder);
      MessageRequest messageRequest = mock(MessageRequest.class);
      when(messageRequestBuilder.buildRequest()).thenReturn(messageRequest);
      when(messageRequest.get()).thenReturn(message);

      InputStream inputStream = new ByteArrayInputStream(MESSAGE_MIME.getBytes());
      MessageStreamRequestBuilder messageStreamRequestBuilder = mock(MessageStreamRequestBuilder.class);
      when(messageRequestBuilder.content()).thenReturn(messageStreamRequestBuilder);
      MessageStreamRequest messageStreamRequest = mock(MessageStreamRequest.class);
      when(messageStreamRequestBuilder.buildRequest()).thenReturn(messageStreamRequest);
      when(messageStreamRequest.get()).thenReturn(inputStream);

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(MESSAGE_MIME, messages.get(0).getContent().replaceAll("\r\n", "\n"));
      assertEquals(0, messages.get(0).getMessageHeaders().size());
      assertNotEquals("message-id", messages.get(0).getUniqueId());
      // verifyReadMessageCalled(messageRequest);
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testMockConsumerWithHeaderAndUseEmailMessageIdAsUniqueId() throws Exception {
    assumeFalse(liveTests);

    GraphAPIConnection connection = mock(GraphAPIConnection.class);
    O365RawMailConsumer consumer = newConsumer();
    consumer.registerConnection(connection);
    consumer.setHeaderHandler(new MetadataMailHeaders("prefix."));
    consumer.setUseEmailMessageIdAsUniqueId(true);

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
      when(messageCollectionRequestBuilder.buildRequest(anyList())).thenReturn(messageCollectionRequest);
      when(messageCollectionRequest.filter(O365MailConsumer.DEFAULT_FILTER)).thenReturn(messageCollectionRequest);
      MessageCollectionPage messageResponse = mock(MessageCollectionPage.class);
      when(messageCollectionRequest.get()).thenReturn(messageResponse);
      Message message = newMessage(false);
      when(messageResponse.getCurrentPage()).thenReturn(Arrays.asList(message));

      MessageRequestBuilder messageRequestBuilder = mock(MessageRequestBuilder.class);
      when(userRequestBuilder.messages(anyString())).thenReturn(messageRequestBuilder);
      MessageRequest messageRequest = mock(MessageRequest.class);
      when(messageRequestBuilder.buildRequest()).thenReturn(messageRequest);
      when(messageRequest.get()).thenReturn(message);

      InputStream inputStream = new ByteArrayInputStream(MESSAGE_MIME.getBytes());
      MessageStreamRequestBuilder messageStreamRequestBuilder = mock(MessageStreamRequestBuilder.class);
      when(messageRequestBuilder.content()).thenReturn(messageStreamRequestBuilder);
      MessageStreamRequest messageStreamRequest = mock(MessageStreamRequest.class);
      when(messageStreamRequestBuilder.buildRequest()).thenReturn(messageStreamRequest);
      when(messageStreamRequest.get()).thenReturn(inputStream);

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(MESSAGE_MIME, messages.get(0).getContent().replaceAll("\r\n", "\n"));
      assertTrue(messages.get(0).getMessageHeaders().size() > 3);
      assertEquals(FROM, messages.get(0).getMetadataValue("prefix.From"));
      assertEquals(TO, messages.get(0).getMetadataValue("prefix.To"));
      assertEquals(SUBJECT, messages.get(0).getMetadataValue("prefix.Subject"));
      assertEquals("message-id", messages.get(0).getUniqueId());
      // verifyReadMessageCalled(messageRequest);
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testMockConsumerWithAttachment() throws Exception {
    assumeFalse(liveTests);

    MockMessageListener mockMessageListener = new MockMessageListener(10);
    GraphAPIConnection connection = mock(GraphAPIConnection.class);
    O365RawMailConsumer consumer = newConsumer();
    consumer.registerConnection(connection);
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
      when(messageCollectionRequestBuilder.buildRequest(anyList())).thenReturn(messageCollectionRequest);
      when(messageCollectionRequest.filter(O365MailConsumer.DEFAULT_FILTER)).thenReturn(messageCollectionRequest);
      MessageCollectionPage messageResponse = mock(MessageCollectionPage.class);
      when(messageCollectionRequest.get()).thenReturn(messageResponse);
      Message message = newMessage(true);
      when(messageResponse.getCurrentPage()).thenReturn(Arrays.asList(message));

      MessageRequestBuilder messageRequestBuilder = mock(MessageRequestBuilder.class);
      when(userRequestBuilder.messages(anyString())).thenReturn(messageRequestBuilder);
      MessageRequest messageRequest = mock(MessageRequest.class);
      when(messageRequestBuilder.buildRequest()).thenReturn(messageRequest);
      when(messageRequest.get()).thenReturn(message);

      InputStream inputStream = new ByteArrayInputStream(MESSAGE_MIME_ATTACHMENT.getBytes());
      MessageStreamRequestBuilder messageStreamRequestBuilder = mock(MessageStreamRequestBuilder.class);
      when(messageRequestBuilder.content()).thenReturn(messageStreamRequestBuilder);
      MessageStreamRequest messageStreamRequest = mock(MessageStreamRequest.class);
      when(messageStreamRequestBuilder.buildRequest()).thenReturn(messageStreamRequest);
      when(messageStreamRequest.get()).thenReturn(inputStream);

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(MESSAGE_MIME_ATTACHMENT, messages.get(0).getContent().replaceAll("\r\n", "\n"));
      assertEquals(0, messages.get(0).getMessageHeaders().size());
      assertNotEquals("message-id", messages.get(0).getUniqueId());
      // verifyReadMessageCalled(messageRequest);
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testMockConsumerDelete() throws Exception {
    assumeFalse(liveTests);

    GraphAPIConnection connection = mock(GraphAPIConnection.class);
    O365RawMailConsumer consumer = newConsumer();
    consumer.registerConnection(connection);
    consumer.setDelete(true);

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
      when(messageCollectionRequestBuilder.buildRequest(anyList())).thenReturn(messageCollectionRequest);
      when(messageCollectionRequest.filter(O365MailConsumer.DEFAULT_FILTER)).thenReturn(messageCollectionRequest);
      MessageCollectionPage messageResponse = mock(MessageCollectionPage.class);
      when(messageCollectionRequest.get()).thenReturn(messageResponse);
      Message message = newMessage(false);
      when(messageResponse.getCurrentPage()).thenReturn(Arrays.asList(message));

      MessageRequestBuilder messageRequestBuilder = mock(MessageRequestBuilder.class);
      when(userRequestBuilder.messages(anyString())).thenReturn(messageRequestBuilder);
      MessageRequest messageRequest = mock(MessageRequest.class);
      when(messageRequestBuilder.buildRequest()).thenReturn(messageRequest);
      when(messageRequest.get()).thenReturn(message);

      InputStream inputStream = new ByteArrayInputStream(MESSAGE_MIME.getBytes());
      MessageStreamRequestBuilder messageStreamRequestBuilder = mock(MessageStreamRequestBuilder.class);
      when(messageRequestBuilder.content()).thenReturn(messageStreamRequestBuilder);
      MessageStreamRequest messageStreamRequest = mock(MessageStreamRequest.class);
      when(messageStreamRequestBuilder.buildRequest()).thenReturn(messageStreamRequest);
      when(messageStreamRequest.get()).thenReturn(inputStream);

      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 1, 1000);

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      assertEquals(1, messages.size());
      assertEquals(MESSAGE_MIME, messages.get(0).getContent().replaceAll("\r\n", "\n"));
      assertEquals(0, messages.get(0).getMessageHeaders().size());
      assertNotEquals("message-id", messages.get(0).getUniqueId());
      // verify(messageRequest).delete();
    } finally {
      stop(standaloneConsumer);
    }
  }

  @Test
  public void testQueryOptionWithSearch() throws Exception {
    O365RawMailConsumer consumer = newConsumer();
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
    return new StandaloneConsumer(connection, newConsumer());
  }

  @Override
  protected List<StandaloneConsumer> retrieveObjectsForSampleConfig() {
    List<StandaloneConsumer> result = new ArrayList<>();
    for (Poller poller : POLLERS) {
      StandaloneConsumer standaloneConsumer = (StandaloneConsumer) retrieveObjectForSampleConfig();
      ((O365RawMailConsumer) standaloneConsumer.getConsumer()).setPoller(poller);
      result.add(standaloneConsumer);
    }
    return result;
  }

  private O365RawMailConsumer newConsumer() {
    O365RawMailConsumer consumer = new O365RawMailConsumer();
    consumer.registerConnection(connection);
    consumer.setUsername(properties.getProperty("USERNAME", USERNAME));
    consumer.setMessageFactory(DefaultMessageFactory.getDefaultInstance());
    return consumer;
  }

  private Message newMessage(boolean hasAttachments) {
    Message message = new Message();
    message.id = "7cbd04e3e8be1a706b19377ba82bd6b4";
    ItemBody body = new ItemBody();
    body.content = MESSAGE;
    message.body = body;
    message.subject = SUBJECT;
    Recipient recipient = new Recipient();
    EmailAddress emailAddress = new EmailAddress();
    emailAddress.address = TO;
    recipient.emailAddress = emailAddress;
    message.toRecipients = Arrays.asList(recipient);
    Recipient sender = new Recipient();
    emailAddress = new EmailAddress();
    emailAddress.address = FROM;
    sender.emailAddress = emailAddress;
    message.from = sender;
    message.ccRecipients = new ArrayList<>();
    message.hasAttachments = hasAttachments;
    return message;
  }

  // private void verifyReadMessageCalled(MessageRequest messageRequest) {
  // verify(messageRequest).patch(isA(Message.class));
  // }

}
