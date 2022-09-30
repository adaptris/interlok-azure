package com.adaptris.interlok.azure.mail;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.MultiPayloadMessageFactory;

public class MultiPayloadAdaptrisMessageWrapperTest {

  private static String PAYLOAD = "Some payload";

  @Test
  public void testAddPayloadMessageHeader() {

    MultiPayloadAdaptrisMessageWrapper multiPayloadAdaptrisMessageWrapper = new MultiPayloadAdaptrisMessageWrapper(
        (MultiPayloadAdaptrisMessage) new MultiPayloadMessageFactory().newMessage(PAYLOAD));

    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("key", "value");
    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("another-payload", "another-key", "another-value");

    assertEquals("value", multiPayloadAdaptrisMessageWrapper.getMetadataValue("PAYLOAD_default-payload_key"));
    assertEquals("another-value", multiPayloadAdaptrisMessageWrapper.getMetadataValue("PAYLOAD_another-payload_another-key"));
  }

  @Test
  public void testGetPayloadMessageHeaders() {

    MultiPayloadAdaptrisMessageWrapper multiPayloadAdaptrisMessageWrapper = new MultiPayloadAdaptrisMessageWrapper(
        (MultiPayloadAdaptrisMessage) new MultiPayloadMessageFactory().newMessage(PAYLOAD));

    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("key", "value");
    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("another-payload", "key", "value2");
    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("another-payload", "another-key", "another-value");

    Map<String, String> payloadMessageHeaders = multiPayloadAdaptrisMessageWrapper.getPayloadMessageHeaders();
    assertEquals(1, payloadMessageHeaders.size());
    assertEquals("value", payloadMessageHeaders.get("key"));
  }

}
