package com.adaptris.interlok.azure.onedrive;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * Implementation of a file producer that can place files into a
 * Microsoft One Drive account, using their Graph API and OAuth2.
 *
 * @config one-drive-producer
 */
@XStreamAlias("one-drive-producer")
@AdapterComponent
@ComponentProfile(summary = "Place files into a Microsoft Office 365 One Drive account using the Microsoft Graph API", tag = "producer,file,o365,microsoft,office,365,one drive")
@DisplayOrder(order = { "username" })
public class OneDriveProducer extends ProduceOnlyProducerImp
{
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String username;

  @Override
  public void prepare()
  {
    /* do nothing */
  }

  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException
  {
    String user = adaptrisMessage.resolve(username);

    log.debug("Pushing file to One Drive as user " + user);

    try
    {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      IGraphServiceClient graphClient = connection.getClientConnection();

    }
    catch (Exception e)
    {
      log.error("Exception processing One Drive file", e);
      throw new ProduceException(e);
    }
  }

  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage)
  {
    return adaptrisMessage.resolve(username);
  }
}
