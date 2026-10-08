package com.adaptris.interlok.azure.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.MultiPayloadMessageFactory;
import com.adaptris.core.ProduceException;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;

public class O365MailProducerAttachmentTest {

  @Test
  public void testSendsDraftWithAttachment() throws Exception {
    GraphServiceClient<?> client = mock(GraphServiceClient.class, Answers.RETURNS_DEEP_STUBS);
    O365MailProducer producer = newProducer(client);
    producer.setCcRecipients("cc@example.com");
    producer.setBccRecipients("bcc@example.com");
    Message draft = new Message();
    draft.id = "draft-id";
    when(client.users("sender@example.com").messages().buildRequest().post(any(Message.class))).thenReturn(draft);

    byte[] attachmentBytes = "attachment content".getBytes(StandardCharsets.UTF_8);
    MultiPayloadAdaptrisMessage message = (MultiPayloadAdaptrisMessage) new MultiPayloadMessageFactory()
        .newMessage("attachment.txt", attachmentBytes);
    message.addPayload("body", "mail body".getBytes(StandardCharsets.UTF_8));

    producer.doProduce(message, null);

    ArgumentCaptor<Message> mail = ArgumentCaptor.forClass(Message.class);
    verify(client.users("sender@example.com").messages().buildRequest()).post(mail.capture());
    assertEquals("subject", mail.getValue().subject);
    assertEquals("mail body", mail.getValue().body.content);
    assertEquals(BodyType.TEXT, mail.getValue().body.contentType);
    assertTrue(mail.getValue().isDraft);
    assertEquals("recipient@example.com", mail.getValue().toRecipients.get(0).emailAddress.address);
    assertEquals("cc@example.com", mail.getValue().ccRecipients.get(0).emailAddress.address);
    assertEquals("bcc@example.com", mail.getValue().bccRecipients.get(0).emailAddress.address);

    ArgumentCaptor<FileAttachment> attachment = ArgumentCaptor.forClass(FileAttachment.class);
    verify(client.users("sender@example.com").messages("draft-id").attachments().buildRequest()).post(attachment.capture());
    assertEquals("#microsoft.graph.fileAttachment", attachment.getValue().oDataType);
    assertEquals("attachment.txt", attachment.getValue().name);
    assertEquals(attachmentBytes.length, attachment.getValue().size);
    assertEquals("application/octet-stream", attachment.getValue().contentType);
    assertArrayEquals(Base64.getEncoder().encode(attachmentBytes), attachment.getValue().contentBytes);
    verify(client.users("sender@example.com").messages("draft-id").send().buildRequest()).post();
  }

  @Test
  public void testWrapsGraphFailure() {
    GraphServiceClient<?> client = mock(GraphServiceClient.class);
    O365MailProducer producer = newProducer(client);
    IllegalStateException failure = new IllegalStateException("Graph request failed");
    when(client.users("sender@example.com")).thenThrow(failure);

    ProduceException exception = assertThrows(ProduceException.class,
        () -> producer.doProduce(AdaptrisMessageFactory.getDefaultInstance().newMessage("body"), null));

    assertSame(failure, exception.getCause());
  }

  private O365MailProducer newProducer(GraphServiceClient<?> client) {
    GraphAPIConnection connection = mock(GraphAPIConnection.class);
    when(connection.retrieveConnection(any())).thenReturn(connection);
    doReturn(client).when(connection).getClientConnection();
    O365MailProducer producer = new O365MailProducer();
    producer.registerConnection(connection);
    producer.setUsername("sender@example.com");
    producer.setSubject("subject");
    producer.setToRecipients("recipient@example.com");
    return producer;
  }
}
