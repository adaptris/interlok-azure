package com.adaptris.interlok.azure.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    assertEquals("value", multiPayloadAdaptrisMessageWrapper.getPayloadMessageHeaderValue("key"));
    assertEquals("value2", multiPayloadAdaptrisMessageWrapper.getPayloadMessageHeaderValue("another-payload", "key"));
  }

  @Test
  public void testPayloadHeadersContainsKey() {
    MultiPayloadAdaptrisMessageWrapper multiPayloadAdaptrisMessageWrapper = new MultiPayloadAdaptrisMessageWrapper(
        (MultiPayloadAdaptrisMessage) new MultiPayloadMessageFactory().newMessage(PAYLOAD));

    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("key", "value");
    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("another-payload", "key", "value2");

    assertTrue(multiPayloadAdaptrisMessageWrapper.payloadHeadersContainsKey("key"));
    assertTrue(multiPayloadAdaptrisMessageWrapper.payloadHeadersContainsKey("another-payload", "key"));
  }

  @Test
  public void testRemovePayloadMessageHeader() {
    MultiPayloadAdaptrisMessageWrapper multiPayloadAdaptrisMessageWrapper = new MultiPayloadAdaptrisMessageWrapper(
        (MultiPayloadAdaptrisMessage) new MultiPayloadMessageFactory().newMessage(PAYLOAD));

    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("key", "value");
    multiPayloadAdaptrisMessageWrapper.addPayloadMessageHeader("another-payload", "key", "value2");

    multiPayloadAdaptrisMessageWrapper.removePayloadMessageHeader("key");
    multiPayloadAdaptrisMessageWrapper.removePayloadMessageHeader("another-payload", "key");

    assertFalse(multiPayloadAdaptrisMessageWrapper.payloadHeadersContainsKey("key"));
    assertFalse(multiPayloadAdaptrisMessageWrapper.payloadHeadersContainsKey("another-payload", "key"));
  }

}
