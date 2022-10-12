package com.adaptris.interlok.azure.mail;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.Removal;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.services.conditional.ForEach;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of the {@link ForEach} service which add a PAYLOAD_ID metadata when iterating though payloads. It also add renamed payload
 * metada e.g. 'PAYLOAD_payload-id_emailattachmentcontenttype' will give a new metadata 'emailattachmentcontenttype'
 *
 */
@Deprecated
@Removal(version = "4.0.0", message = "This service will be removed in 4.0.0 and should be replaced by for-each-payload service in 4.6.0. In versions 4.0.0 to 4.5.0 the for-each-payload service does not add renamed metadata")
@XStreamAlias("azure-office-365-for-each")
@AdapterComponent
@ComponentProfile(summary = "Runs the configured service/list for each multi-payload message payload.", tag = "for,each,for each,for-each,then,multi-payload,outlook,365", since = "3.12.0")
@DisplayOrder(order = { "then", "threadCount" })
public class O365ForEach extends ForEach {

  private static final transient Logger log = LoggerFactory.getLogger(O365ForEach.class.getName());

  private static final String PAYLOAD_ID = "PAYLOAD_ID";

  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    ThreadPoolExecutor executor = null;

    try {
      log.info("Starting for-each");
      if (!(msg instanceof MultiPayloadAdaptrisMessage)) {
        log.warn("Message [{}] is not a multi-payload message!", msg.getUniqueId());
        iterate(msg);
      } else {
        MultiPayloadAdaptrisMessage message = (MultiPayloadAdaptrisMessage) msg;

        int threads = getThreadCount();
        if (threads == 0) {
          // use as many threads as necessary
          threads = message.getPayloadCount();
        }
        log.trace("Using {} thread{}", threads, threads > 1 ? "s" : "");
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

        for (String id : message.getPayloadIDs()) {
          try {
            message.switchPayload(id);
            MultiPayloadAdaptrisMessageWrapper messageWrapper = new MultiPayloadAdaptrisMessageWrapper(message);
            AdaptrisMessage each = DefaultMessageFactory.getDefaultInstance().newMessage(message, null);
            each.setPayload(message.getPayload());
            each.addMessageHeader(PAYLOAD_ID, id);
            messageWrapper.getPayloadMessageHeaders(id).entrySet().forEach(e -> each.addMessageHeader(e.getKey(), e.getValue()));
            executor.execute(() -> iterate(each));
          } catch (CloneNotSupportedException e) {
            log.error("Could not clone message [{}]", id, e);
          }
        }
      }
    } finally {
      if (executor != null) {
        executor.shutdown();
        try {
          while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS)) {
            /* wait for all threads to complete */
          }
        } catch (InterruptedException e) {
          log.warn("Interrupted while waiting for tasks to finish!", e);
        }
      }
      log.info("Finished for-each");
    }
  }

  /**
   * Perform a single iteration of the then service on the given message.
   *
   * @param message
   *          The message to iterate over.
   */
  private void iterate(AdaptrisMessage message) {
    String id = message.getUniqueId();
    try {
      log.debug("Iterating over message [{}}]", id);
      getThen().getService().doService(message);
    } catch (Exception e) {
      log.error("Message [{}}] failed!", id, e);
    } finally {
      log.debug("Done with message [{}]", id);
    }
  }

}
