package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("data-lake-producer")
@AdapterComponent
@ComponentProfile(summary = "Put data into a Azure Data Lake", tag = "producer,azure,data lake,data,lake")
@DisplayOrder(order = { })
public class DataLakeProducer extends ProduceOnlyProducerImp
{
  @Override
  public void prepare()
  {
    /* do nothing */
  }

  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException
  {
    ;
  }

  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage)
  {
    return adaptrisMessage.resolve("?");
  }
}
