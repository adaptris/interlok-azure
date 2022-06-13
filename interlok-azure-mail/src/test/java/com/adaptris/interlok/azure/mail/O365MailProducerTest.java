package com.adaptris.interlok.azure.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;
import com.microsoft.graph.requests.UserSendMailRequest;
import com.microsoft.graph.requests.UserSendMailRequestBuilder;

public class O365MailProducerTest extends ExampleProducerCase {
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String SUBJECT = "InterlokMail Office365 Test Message";
  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";

  private GraphAPIConnection connection;
  private O365MailProducer producer;

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

    producer = new O365MailProducer();
    producer.registerConnection(connection);
    producer.setUsername(properties.getProperty("USERNAME", USERNAME));
    producer.setSubject(SUBJECT);
    producer.setToRecipients(properties.getProperty("USERNAME", USERNAME)); // send it to ourself so we're not spamming anyone else
    producer.setSave(true);
  }

  @Test
  public void testLiveProducer() throws Exception {
    Assume.assumeTrue(liveTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Test
  public void testMockProducer() throws Exception {
    Assume.assumeFalse(liveTests);

    connection = mock(GraphAPIConnection.class);
    producer.registerConnection(connection);

    when(connection.retrieveConnection(any())).thenReturn(connection);
    GraphServiceClient client = mock(GraphServiceClient.class);
    when(connection.getClientConnection()).thenReturn(client);

    UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
    when(client.users(USERNAME)).thenReturn(userRequestBuilder);
    UserSendMailRequestBuilder sendMailBuilder = mock(UserSendMailRequestBuilder.class);
    when(userRequestBuilder.sendMail(any(UserSendMailParameterSet.class))).thenReturn(sendMailBuilder);
    UserSendMailRequest messageRequest = mock(UserSendMailRequest.class);
    when(sendMailBuilder.buildRequest()).thenReturn(messageRequest);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);

    verify(messageRequest, times(1)).post();
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
