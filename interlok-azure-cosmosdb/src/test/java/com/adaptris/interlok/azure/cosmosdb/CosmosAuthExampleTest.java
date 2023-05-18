package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;

// to avoid more work when ServiceCase no longer extends TestCase
public class CosmosAuthExampleTest extends ExampleServiceCase {

  @Override
  protected CosmosAuthorizationHeader retrieveObjectForSampleConfig() {
    return new CosmosAuthorizationHeader().withResourceId("dbs/MyDatabase/colls/MyCollection").withResourceType("colls").withHttpVerb("PUT")
        .withMasterKey("my-master-key (could be encoded)");
  }

}
