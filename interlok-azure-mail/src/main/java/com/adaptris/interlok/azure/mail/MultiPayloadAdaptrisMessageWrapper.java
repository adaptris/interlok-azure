package com.adaptris.interlok.azure.mail;

import java.util.Map;
import java.util.stream.Collectors;

import com.adaptris.annotation.Removal;
import com.adaptris.core.MultiPayloadAdaptrisMessage;

@Deprecated
@Removal(version = "4.0.0")
public class MultiPayloadAdaptrisMessageWrapper {

  public final static String PAYLOAD_METADATA_KEY_PREFIX = "PAYLOAD_";
  public final static String PAYLOAD_METADATA_KEY_FORMAT = PAYLOAD_METADATA_KEY_PREFIX + "%s_%s";

  private MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage;

  public MultiPayloadAdaptrisMessageWrapper(MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage) {
    this.multiPayloadAdaptrisMessage = multiPayloadAdaptrisMessage;
  }

  /**
   * Check if the current payload contains a metadata with the give key
   *
   * @param key
   * @return true if the current payload contains a metadata with the give key
   */
  public boolean payloadHeadersContainsKey(String key) {
    return multiPayloadAdaptrisMessage.headersContainsKey(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Check if the payload with the given id contains a metadata with the give key
   *
   * @param payloadId
   * @param key
   * @return true if the payload with the given id contains a metadata with the give key
   */
  public boolean payloadHeadersContainsKey(String payloadId, String key) {
    return multiPayloadAdaptrisMessage.headersContainsKey(payloadMessageHeaderKey(payloadId, key));
  }

  /**
   * Return metadata value for the current payload and key
   *
   * @param key
   * @return metadata value for the current payload and key
   */
  public String getPayloadMessageHeaderValue(String key) {
    return multiPayloadAdaptrisMessage.getMetadataValue(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Return metadata value for the given payload id and key
   *
   * @param payloadId
   * @param key
   * @return metadata value for the given payload id and key
   */
  public String getPayloadMessageHeaderValue(String payloadId, String key) {
    return multiPayloadAdaptrisMessage.getMetadataValue(payloadMessageHeaderKey(payloadId, key));
  }

  /**
   * Return all the metadata for the current payload removing the payload prefixes e.g. 'PAYLOAD_payload-id_key' will become 'key'
   *
   * @return all the metadata for the given payload removing the payload prefixes
   */
  public Map<String, String> getPayloadMessageHeaders() {
    return getPayloadMessageHeaders(multiPayloadAdaptrisMessage.getCurrentPayloadId());
  }

  /**
   * Return all the metadata for a given payload id removing the payload prefixes e.g. 'PAYLOAD_payload-id_key' will become 'key'
   *
   * @param payloadId
   * @return all the metadata for a given payload id removing the payload prefixes
   */
  public Map<String, String> getPayloadMessageHeaders(String payloadId) {
    String payloadKeyPrefix = String.format(PAYLOAD_METADATA_KEY_FORMAT, payloadId, "");
    return multiPayloadAdaptrisMessage.getMessageHeaders().entrySet().stream()
        .filter(e -> e.getKey().startsWith(payloadKeyPrefix))
        .collect(Collectors.toMap(e -> {
          return e.getKey().substring(payloadKeyPrefix.length());
        }, e -> {
          return e.getValue();
        }));
  }

  /**
   * Add a metadata for the current payload
   *
   * @param key
   * @param value
   */
  public void addPayloadMessageHeader(String key, String value) {
    multiPayloadAdaptrisMessage.addMessageHeader(currentPayloadMessageHeaderKey(key), value);
  }

  /**
   * Add a metadata for the given payload id
   *
   * @param payloadId
   * @param key
   * @param value
   */
  public void addPayloadMessageHeader(String payloadId, String key, String value) {
    multiPayloadAdaptrisMessage.addMessageHeader(payloadMessageHeaderKey(payloadId, key), value);
  }

  /**
   * Remove a metadata for the current payload and the given key
   *
   * @param key
   */
  public void removePayloadMessageHeader(String key) {
    multiPayloadAdaptrisMessage.removeMessageHeader(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Remove a metadata for the given payload id and the given key
   *
   * @param payloadId
   * @param key
   */
  public void removePayloadMessageHeader(String payloadId, String key) {
    multiPayloadAdaptrisMessage.removeMessageHeader(payloadMessageHeaderKey(payloadId, key));
  }

  public String getMetadataValue(String key) {
    return multiPayloadAdaptrisMessage.getMetadataValue(key);
  }

  private String currentPayloadMessageHeaderKey(String key) {
    return payloadMessageHeaderKey(multiPayloadAdaptrisMessage.getCurrentPayloadId(), key);
  }

  private String payloadMessageHeaderKey(String payloadId, String key) {
    return String.format(PAYLOAD_METADATA_KEY_FORMAT, payloadId, key);
  }

}
