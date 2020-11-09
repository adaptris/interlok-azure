package com.adaptris.interlok.azure.onedrive;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.azure.AzureConnection;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class OneDriveProducerTest extends ExampleProducerCase
{
  private static final String APPLICATION_ID = "47ea49b0-670a-47c1-9303-0b45ffb766ec";
  private static final String TENANT_ID = "cbf4a38d-3117-48cd-b54b-861480ee93cd";
  private static final String CLIENT_SECRET = "NGMyYjY0MTEtOTU0Ny00NTg0LWE3MzQtODg2ZDAzZGVmZmY1Cg==";
  private static final String USERNAME = "user@example.com";

  private static final String SUBJECT = "InterlokMail Office365 Test Message";
  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";

  private AzureConnection connection;
  private OneDriveProducer producer;

  private boolean runTests = false;

  @Before
  public void setUp()
  {
    Properties properties = new Properties();
    try
    {
      properties.load(new FileInputStream(this.getClass().getResource("o365.properties").getFile()));
      runTests = true;
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
  }

  @Test
  public void testProducer() throws Exception
  {
    Assume.assumeTrue(runTests);

    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);
    ExampleServiceCase.execute(standaloneProducer, message);
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