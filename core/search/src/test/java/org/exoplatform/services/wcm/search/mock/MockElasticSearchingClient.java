package org.exoplatform.services.wcm.search.mock;

import org.apache.http.conn.HttpClientConnectionManager;
import org.exoplatform.commons.search.es.client.ElasticIndexingAuditTrail;
import org.exoplatform.commons.search.es.client.ElasticResponse;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;

public class MockElasticSearchingClient extends ElasticSearchingClient {
  public MockElasticSearchingClient(ElasticIndexingAuditTrail auditTrail) {
    super(auditTrail);
  }

  @Override
  public String sendRequest(String esQuery, String index, String type) {
    return "{ \"hits\" : { \"hits\" : [] } }";
  }

  @Override
  protected String getEsUsernameProperty() {
    return "";
  }

  @Override
  protected String getEsPasswordProperty() {
    return "";
  }

  @Override
  protected HttpClientConnectionManager getClientConnectionManager() {
    return null;
  }

  @Override
  protected ElasticResponse sendHttpPostRequest(String url, String content) {
    return new ElasticResponse("", 200);
  }

  @Override
  protected ElasticResponse sendHttpPutRequest(String url, String content) {
    return new ElasticResponse("", 200);
  }

  @Override
  protected ElasticResponse sendHttpDeleteRequest(String url) {
    return new ElasticResponse("", 200);
  }

  @Override
  protected ElasticResponse sendHttpGetRequest(String url) {
    return null;
  }
}
