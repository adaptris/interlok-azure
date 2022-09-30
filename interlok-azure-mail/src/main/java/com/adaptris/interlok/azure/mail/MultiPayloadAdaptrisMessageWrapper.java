package com.adaptris.interlok.azure.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.adaptris.annotation.Removal;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageEncoder;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.CoreException;
import com.adaptris.core.MessageEventGenerator;
import com.adaptris.core.MessageLifecycleEvent;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.interlok.types.MessageWrapper;

@Deprecated
@Removal(version = "4.0.0")
public class MultiPayloadAdaptrisMessageWrapper implements MultiPayloadAdaptrisMessage {

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
    return headersContainsKey(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Check if the payload with the given id contains a metadata with the give key
   *
   * @param payloadId
   * @param key
   * @return true if the payload with the given id contains a metadata with the give key
   */
  public boolean payloadHeadersContainsKey(String payloadId, String key) {
    return headersContainsKey(payloadMessageHeaderKey(payloadId, key));
  }

  /**
   * Return metadata value for the current payload and key
   *
   * @param key
   * @return metadata value for the current payload and key
   */
  public String getPayloadMessageHeaderValue(String key) {
    return getMetadataValue(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Return metadata value for the given payload id and key
   *
   * @param payloadId
   * @param key
   * @return metadata value for the given payload id and key
   */
  public String getPayloadMessageHeaderValue(String payloadId, String key) {
    return getMetadataValue(payloadMessageHeaderKey(payloadId, key));
  }

  /**
   * Return all the metadata for the current payload removing the payload prefixes e.g. 'PAYLOAD_payload-id_key' will become 'key'
   *
   * @return all the metadata for the given payload removing the payload prefixes
   */
  public Map<String, String> getPayloadMessageHeaders() {
    return getPayloadMessageHeaders(getCurrentPayloadId());
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
    addMessageHeader(currentPayloadMessageHeaderKey(key), value);
  }

  /**
   * Add a metadata for the given payload id
   *
   * @param payloadId
   * @param key
   * @param value
   */
  public void addPayloadMessageHeader(String payloadId, String key, String value) {
    addMessageHeader(payloadMessageHeaderKey(payloadId, key), value);
  }

  /**
   * Remove a metadata for the current payload and the given key
   *
   * @param key
   */
  public void removePayloadMessageHeader(String key) {
    removeMessageHeader(currentPayloadMessageHeaderKey(key));
  }

  /**
   * Remove a metadata for the given payload id and the given key
   *
   * @param payloadId
   * @param key
   */
  public void removePayloadMessageHeader(String payloadId, String key) {
    removeMessageHeader(payloadMessageHeaderKey(payloadId, key));
  }

  private String currentPayloadMessageHeaderKey(String key) {
    return payloadMessageHeaderKey(getCurrentPayloadId(), key);
  }

  private String payloadMessageHeaderKey(String payloadId, String key) {
    return String.format(PAYLOAD_METADATA_KEY_FORMAT, payloadId, key);
  }

  @Override
  public String getUniqueId() {
    return multiPayloadAdaptrisMessage.getUniqueId();
  }

  @Override
  public void setPayload(byte[] payload) {
    multiPayloadAdaptrisMessage.setPayload(payload);
  }

  @Override
  public byte[] getPayload() {
    return multiPayloadAdaptrisMessage.getPayload();
  }

  @Override
  public void switchPayload(@NotNull String id) {
    multiPayloadAdaptrisMessage.switchPayload(id);
  }

  @Override
  public long getSize() {
    return multiPayloadAdaptrisMessage.getSize();
  }

  @Override
  public boolean hasPayloadId(@NotNull String id) {
    return multiPayloadAdaptrisMessage.hasPayloadId(id);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void setStringPayload(String payload) {
    multiPayloadAdaptrisMessage.setStringPayload(payload);
  }

  @Override
  public void setUniqueId(String uniqueId) {
    multiPayloadAdaptrisMessage.setUniqueId(uniqueId);
  }

  @Override
  public String getCurrentPayloadId() {
    return multiPayloadAdaptrisMessage.getCurrentPayloadId();
  }

  @Override
  public Set<String> getPayloadIDs() {
    return multiPayloadAdaptrisMessage.getPayloadIDs();
  }

  @Override
  public void setCurrentPayloadId(@NotNull String id) {
    multiPayloadAdaptrisMessage.setCurrentPayloadId(id);
  }

  @Override
  public String getContent() {
    return multiPayloadAdaptrisMessage.getContent();
  }

  @Override
  public void setContent(String payload, String encoding) {
    multiPayloadAdaptrisMessage.setContent(payload, encoding);
  }

  @Override
  public Map<String, String> getMessageHeaders() {
    return multiPayloadAdaptrisMessage.getMessageHeaders();
  }

  @Override
  public void addPayload(@NotNull String id, byte[] payload) {
    multiPayloadAdaptrisMessage.addPayload(id, payload);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void setStringPayload(String payload, String charEncoding) {
    multiPayloadAdaptrisMessage.setStringPayload(payload, charEncoding);
  }

  @Override
  public void deletePayload(@NotNull String id) {
    multiPayloadAdaptrisMessage.deletePayload(id);
  }

  @Override
  public byte[] getPayload(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getPayload(id);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void setMessageHeaders(Map<String, String> metadata) {
    multiPayloadAdaptrisMessage.setMessageHeaders(metadata);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public String getStringPayload() {
    return multiPayloadAdaptrisMessage.getStringPayload();
  }

  @Override
  public long getSize(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getSize(id);
  }

  @Override
  public int getPayloadCount() {
    return multiPayloadAdaptrisMessage.getPayloadCount();
  }

  @Override
  public void addContent(@NotNull String id, String content) {
    multiPayloadAdaptrisMessage.addContent(id, content);
  }

  @Override
  public void addContent(@NotNull String id, String content, String encoding) {
    multiPayloadAdaptrisMessage.addContent(id, content, encoding);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void setCharEncoding(String charEncoding) {
    multiPayloadAdaptrisMessage.setCharEncoding(charEncoding);
  }

  @Override
  public void setContent(@NotNull String id, String content, String encoding) {
    multiPayloadAdaptrisMessage.setContent(id, content, encoding);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public String getCharEncoding() {
    return multiPayloadAdaptrisMessage.getCharEncoding();
  }

  @Override
  public String getContent(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getContent(id);
  }

  @Override
  public void setContentEncoding(@NotNull String id, String encoding) {
    multiPayloadAdaptrisMessage.setContentEncoding(id, encoding);
  }

  @Override
  public String getMetadataValue(String key) {
    return multiPayloadAdaptrisMessage.getMetadataValue(key);
  }

  @Override
  public String getContentEncoding(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getContentEncoding(id);
  }

  @Override
  public InputStream getInputStream(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getInputStream(id);
  }

  @Override
  public void addMessageHeader(String key, String value) {
    multiPayloadAdaptrisMessage.addMessageHeader(key, value);
  }

  @Override
  public void removeMessageHeader(String key) {
    multiPayloadAdaptrisMessage.removeMessageHeader(key);
  }

  @Override
  public String getContentEncoding() {
    return multiPayloadAdaptrisMessage.getContentEncoding();
  }

  @Override
  public void setContentEncoding(String payloadEncoding) {
    multiPayloadAdaptrisMessage.setContentEncoding(payloadEncoding);
  }

  @Override
  public OutputStream getOutputStream(@NotNull String id) {
    return multiPayloadAdaptrisMessage.getOutputStream(id);
  }

  @Override
  public Reader getReader() throws IOException {
    return multiPayloadAdaptrisMessage.getReader();
  }

  @Override
  public MetadataElement getMetadata(String key) {
    return multiPayloadAdaptrisMessage.getMetadata(key);
  }

  @Override
  public Writer getWriter(@NotNull String id) throws IOException {
    return multiPayloadAdaptrisMessage.getWriter(id);
  }

  @Override
  public Writer getWriter() throws IOException {
    return multiPayloadAdaptrisMessage.getWriter();
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public boolean containsKey(String key) {
    return multiPayloadAdaptrisMessage.containsKey(key);
  }

  @Override
  public Writer getWriter(@NotNull String id, String encoding) throws IOException {
    return multiPayloadAdaptrisMessage.getWriter(id, encoding);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return multiPayloadAdaptrisMessage.getInputStream();
  }

  @Override
  public void addMetadata(String key, String value) {
    multiPayloadAdaptrisMessage.addMetadata(key, value);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return multiPayloadAdaptrisMessage.getOutputStream();
  }

  @Override
  public void addMetadata(MetadataElement metadata) {
    multiPayloadAdaptrisMessage.addMetadata(metadata);
  }

  @Override
  public void addObjectHeader(Object key, Object object) {
    multiPayloadAdaptrisMessage.addObjectHeader(key, object);
  }

  @Override
  public void removeMetadata(MetadataElement element) {
    multiPayloadAdaptrisMessage.removeMetadata(element);
  }

  @Override
  public Map<Object, Object> getObjectHeaders() {
    return multiPayloadAdaptrisMessage.getObjectHeaders();
  }

  @Override
  public Set<MetadataElement> getMetadata() {
    return multiPayloadAdaptrisMessage.getMetadata();
  }

  @Override
  public boolean headersContainsKey(String key) {
    return multiPayloadAdaptrisMessage.headersContainsKey(key);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void setMetadata(Set<MetadataElement> metadata) {
    multiPayloadAdaptrisMessage.setMetadata(metadata);
  }

  @Override
  public String resolve(String s) {
    return multiPayloadAdaptrisMessage.resolve(s);
  }

  @Override
  public String resolve(String s, boolean multiline) {
    return multiPayloadAdaptrisMessage.resolve(s, multiline);
  }

  @Override
  public void clearMetadata() {
    multiPayloadAdaptrisMessage.clearMetadata();
  }

  @Override
  public AdaptrisMessageFactory getFactory() {
    return multiPayloadAdaptrisMessage.getFactory();
  }

  @Override
  public <T> T wrap(MessageWrapper<T> wrapper) throws Exception {
    return multiPayloadAdaptrisMessage.wrap(wrapper);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return multiPayloadAdaptrisMessage.clone();
  }

  @Override
  public void addEvent(MessageEventGenerator meg, boolean wasSuccessful) {
    multiPayloadAdaptrisMessage.addEvent(meg, wasSuccessful);
  }

  @Override
  public MessageLifecycleEvent getMessageLifecycleEvent() {
    return multiPayloadAdaptrisMessage.getMessageLifecycleEvent();
  }

  @Override
  public byte[] encode(AdaptrisMessageEncoder encoder) throws CoreException {
    return multiPayloadAdaptrisMessage.encode(encoder);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public void addObjectMetadata(String key, Object object) {
    multiPayloadAdaptrisMessage.addObjectMetadata(key, object);
  }

  @Override
  @Deprecated
  @Removal(version = "5.0.0")
  public Map getObjectMetadata() {
    return multiPayloadAdaptrisMessage.getObjectMetadata();
  }

  @Override
  public String getPayloadForLogging() {
    return multiPayloadAdaptrisMessage.getPayloadForLogging();
  }

  @Override
  public String getNextServiceId() {
    return multiPayloadAdaptrisMessage.getNextServiceId();
  }

  @Override
  public void setNextServiceId(String uniqueId) {
    multiPayloadAdaptrisMessage.setNextServiceId(uniqueId);
  }

  @Override
  public String getMetadataValueIgnoreKeyCase(String key) {
    return multiPayloadAdaptrisMessage.getMetadataValueIgnoreKeyCase(key);
  }

  @Override
  public boolean equivalentForTracking(AdaptrisMessage other) {
    return multiPayloadAdaptrisMessage.equivalentForTracking(other);
  }


}
