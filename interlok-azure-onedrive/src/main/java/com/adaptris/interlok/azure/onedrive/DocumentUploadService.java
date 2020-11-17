package com.adaptris.interlok.azure.onedrive;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnection;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Upload the contents of a message to a file in OneDrive.
 *
 * @config azure-one-drive-document-upload-service
 */
@XStreamAlias("azure-one-drive-document-upload-service")
@AdapterComponent
@ComponentProfile(summary = "Upload the contents of a message to a file in OneDrive.", tag = "file,o365,microsoft,office,365,one drive,upload")
@DisplayOrder(order = { "connection", "username", "filename" })
public class DocumentUploadService extends ServiceImp
{
    /**
     * Connection to Azure OneDrive.
     */
    @Getter
    @Setter
    @NotNull
    private AdaptrisConnection connection;

    /**
     * The username for which One Drive will be polled.
     */
    @Getter
    @Setter
    @NotBlank
    @InputFieldHint(expression = true)
    private String username;

    /**
     * The name and path of the file to be uploaded.
     */
    @Getter
    @Setter
    @NotBlank
    @InputFieldHint(expression = true, friendly = "The name and path of the file to be uploaded")
    private String filename;

    /**
     * Whether to overwrite an existing file of the same name. (Default is true!)
     */
    @Getter
    @Setter
    @AdvancedConfig
    @InputFieldDefault("true")
    private Boolean overwrite;

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void initService()
    {
        /* do nothing */
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void closeService()
    {
        /* do nothing */
    }

    /**
     * <p>
     * Apply the service to the message.
     * </p>
     *
     * @param adaptrisMessage the <code>AdaptrisMessage</code> to process
     * @throws ServiceException wrapping any underlying <code>Exception</code>s
     */
    @Override
    public void doService(AdaptrisMessage adaptrisMessage) throws ServiceException
    {
        OneDriveProducer producer = new OneDriveProducer();
        StandaloneProducer standaloneProducer = new StandaloneProducer(connection, producer);

        try
        {
            producer.registerConnection(connection);
            producer.setUsername(adaptrisMessage.resolve(username));
            producer.setFilename(adaptrisMessage.resolve(filename));
            producer.setOverwrite(overwrite);

            LifecycleHelper.initAndStart(standaloneProducer, false);

            standaloneProducer.doService(adaptrisMessage);
        }
        catch (Exception e)
        {
            log.error("Could not upload file", e);
            throw new ServiceException(e);
        }
        finally
        {
            LifecycleHelper.stopAndClose(standaloneProducer, false);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void prepare()
    {
        /* do nothing */
    }
}
