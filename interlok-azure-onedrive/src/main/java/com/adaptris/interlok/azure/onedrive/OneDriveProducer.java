package com.adaptris.interlok.azure.onedrive;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.interlok.azure.GraphAPIConnection;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.File;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;

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

  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(expression = true)
  private String filename;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault("true")
  private Boolean overwrite;

  @Override
  public void prepare()
  {
    /* do nothing */
  }

  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException
  {
    String user = adaptrisMessage.resolve(username);
    String file = adaptrisMessage.resolve(filename);

    log.debug("Pushing file {} to One Drive as user {}", file, user);

    try
    {
      GraphAPIConnection connection = retrieveConnection(GraphAPIConnection.class);
      IGraphServiceClient graphClient = connection.getClientConnection();

      DriveItem newItem = new DriveItem();
      newItem.file = new File();
      newItem.name = adaptrisMessage.resolve(file);

      boolean isNew = true;
      if (overwrite())
      {
        IDriveItemCollectionPage children = graphClient.users(user).drive().root().children().buildRequest().get();
        for (DriveItem driveItem : children.getCurrentPage())
        {
          if (driveItem.name.equals(newItem.name))
          {
            newItem = driveItem;
            isNew = false;
            break;
          }
        }
      }

      if (isNew)
      {
        newItem = graphClient.users(user).drive().items().buildRequest().post(newItem);
      }

      if (adaptrisMessage.getSize() < 4 * FileUtils.ONE_MB)
      {
        graphClient.users(user).drive().items(newItem.id).content().buildRequest().put(adaptrisMessage.getPayload());
      }
      else
      {
        log.warn("Large files not yet supported!");
      }
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

  private boolean overwrite()
  {
    return BooleanUtils.toBooleanDefaultIfNull(overwrite, true);
  }
}
