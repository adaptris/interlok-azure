package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.core.ServiceCase;

// to avoid more work when ServiceCase no longer extends TestCase
public class CosmosAuthExampleTest extends ServiceCase {

  @Override
  protected CosmosAuthorizationHeader retrieveObjectForSampleConfig() {
    return new CosmosAuthorizationHeader().withMasterKey("my-master-key (could be encoded)")
        .withResourceId("dbs/MyDatabase/colls/MyCollection").withResourceType("colls")
        .withHttpVerb("PUT");
  }

}
