package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;

// to avoid more work when ServiceCase no longer extends TestCase
public class CosmosAuthUrlExampleTest extends ExampleServiceCase {

  @Override
  protected CosmosAuthorizationHeaderFromUrl retrieveObjectForSampleConfig() {
    return new CosmosAuthorizationHeaderFromUrl().withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/MyDatabase/colls/MyCollection")
        .withMasterKey("MyMasterKey or PW:XXX").withHttpVerb("PUT");
  }

}
