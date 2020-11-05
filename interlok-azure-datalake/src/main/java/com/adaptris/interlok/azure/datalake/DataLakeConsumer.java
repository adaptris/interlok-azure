/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.interlok.azure.datalake;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.interlok.azure.AzureConnection;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("data-lake-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup data frmo Azure Data Lake", tag = "consumer,azure,data lake,data,lake")
@DisplayOrder(order = { })
public class DataLakeConsumer extends AdaptrisPollingConsumer
{
  @Override
  protected void prepareConsumer()
  {
    /* do nothing */
  }

  @Override
  protected int processMessages()
  {
    log.debug("Polling for data in Azure Data Lake");

    int count = 0;
    try
    {
      AzureConnection connection = retrieveConnection(AzureConnection.class);


      count++;
    }
    catch (Throwable e)
    {
      log.error("Exception in Data Lake", e);
    }

    return count;
  }

  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
  }
}

