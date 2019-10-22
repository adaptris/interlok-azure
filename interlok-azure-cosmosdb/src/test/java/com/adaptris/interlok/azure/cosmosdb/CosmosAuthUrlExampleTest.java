package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.core.ServiceCase;

// to avoid more work when ServiceCase no longer extends TestCase
public class CosmosAuthUrlExampleTest extends ServiceCase {

  @Override
  protected CosmosAuthorizationHeaderFromUrl retrieveObjectForSampleConfig() {
    return new CosmosAuthorizationHeaderFromUrl()
        .withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/MyDatabase/colls/MyCollection")
        .withMasterKey("my-master-key (could be encoded)")
        .withHttpVerb("PUT");
  }

}
