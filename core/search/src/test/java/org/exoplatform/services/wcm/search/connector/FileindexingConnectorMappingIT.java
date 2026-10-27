/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hands the file index mapping to a real Elasticsearch: the engine accepts a
 * document whose transcript is far over the Lucene keyword term limit (32766
 * bytes), and finds it by a word said at its end. Runs under the run-its
 * profile only ({@code mvn verify -Prun-its}), on a container of
 * {@code es.testcontainer.image}, or on the engine named by {@code -Des.url}.
 * A plain or CI build does not run it.
 */
public class FileindexingConnectorMappingIT {

  /** Elasticsearch image started when no engine is named by es.url. */
  private static final String                 IMAGE          = System.getProperty("es.testcontainer.image",
                                                                                  "docker.elastic.co/elasticsearch/elasticsearch:9.3.4");

  /** Word said only at the end of the transcript. */
  private static final String                 LAST_WORD      = "zanzibarique";

  /** Name of the indexed file. */
  private static final String                 FILE_NAME      = "Product Review long.mp4";

  /** Shared JSON mapper, strict by default. */
  private static final JsonMapper             JSON           = JsonMapper.builder().build();

  /** HTTP client sending the requests to the engine. */
  private static final HttpClient             HTTP           = HttpClient.newHttpClient();

  /** Container started on demand, null when es.url names an engine. */
  private static ElasticsearchContainer       container;

  /** Base URL of the engine. */
  private static String                       url;

  /** Index created by the running test, deleted after it. */
  private String                              index;

  /**
   * Stops the container started for the class, if any.
   */
  @AfterClass
  public static void stopContainer() {
    if (container != null) {
      container.stop();
    }
  }

  /**
   * Deletes the index the test created.
   *
   * @throws Exception when the engine cannot be reached
   */
  @After
  public void deleteIndex() throws Exception {
    if (index != null) {
      send("DELETE", "/" + index, null);
    }
  }

  /**
   * With the connector's mapping, a file whose transcript is about 135 KB is
   * indexed, found by its name and by the last word of its transcript, and
   * that word is highlighted.
   *
   * @throws Exception when the engine cannot be reached
   */
  @Test
  public void testLongTranscriptIsIndexedAndFoundByItsLastWord() throws Exception {
    createIndex(connectorMapping());

    HttpResponse<String> indexed = send("PUT", "/" + index + "/_doc/1?refresh=true", fileDocument());
    assertEquals(indexed.body(), 201, indexed.statusCode());

    JsonNode byWord = search("""
        {"query":{"query_string":{"fields":["name","title","transcription"],"query":"*%s*"}},
         "highlight":{"fields":{"transcription":{"fragment_size":100,"number_of_fragments":3}}}}
        """.formatted(LAST_WORD));
    assertEquals(1, byWord.get("hits").get("total").get("value").asInt());
    assertTrue(byWord.get("hits").get("hits").get(0).get("highlight").get("transcription").get(0).asString()
                     .contains("<em>" + LAST_WORD + "</em>"));

    JsonNode byName = search("""
        {"query":{"query_string":{"fields":["name","title","transcription"],"query":"review"}}}
        """);
    assertEquals(1, byName.get("hits").get("total").get("value").asInt());
  }

  /**
   * Control: the same document is refused by a mapping that keeps a keyword
   * copy of the transcript, which is the refusal the connector's mapping must
   * not reproduce.
   *
   * @throws Exception when the engine cannot be reached
   */
  @Test
  public void testLongTranscriptIsRefusedWithAKeywordCopy() throws Exception {
    JsonNode mapping = connectorMapping();
    ((ObjectNode) mapping.get("properties").get("transcription").get("fields")).set("raw",
                                                                                    JSON.readTree("{\"type\":\"keyword\"}"));
    createIndex(mapping);

    HttpResponse<String> refused = send("PUT", "/" + index + "/_doc/1?refresh=true", fileDocument());
    assertEquals(400, refused.statusCode());
    assertTrue(refused.body(), refused.body().contains("transcription.raw"));
  }

  /**
   * Creates a fresh index with the settings commons sends for every index and
   * the given mapping, as ElasticIndexingClient#sendCreateIndexRequest does.
   *
   * @param mapping the mapping to create the index with
   * @throws Exception when the engine cannot be reached or refuses the index
   */
  private void createIndex(JsonNode mapping) throws Exception {
    index = "file_it_" + UUID.randomUUID().toString().replace("-", "");
    String body = "{\"settings\": " + defaultSettings() + ", \"mappings\": " + JSON.writeValueAsString(mapping) + "}";
    HttpResponse<String> created = send("PUT", "/" + index, body);
    assertEquals(created.body(), 200, created.statusCode());
  }

  /**
   * Reads the commons default index settings, with the values
   * ElasticContentRequestBuilder#getCreateIndexRequestContent substitutes for
   * a single-node engine.
   *
   * @return the settings JSON
   * @throws Exception when the settings resource cannot be read
   */
  private String defaultSettings() throws Exception {
    try (InputStream settings = getClass().getClassLoader().getResourceAsStream("es-default-index-settings.json")) {
      return new String(settings.readAllBytes(), StandardCharsets.UTF_8).replace("shard.number", "1")
                                                                       .replace("replica.number", "0")
                                                                       .replace("max_regex.length", "65536");
    }
  }

  /**
   * Parses the mapping the connector sends at index creation.
   *
   * @return the mapping as a JSON tree
   */
  private JsonNode connectorMapping() {
    InitParams initParams = new InitParams();
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("index_alias", "file_alias");
    constructorParams.setProperty("index_current", "file_v6");
    initParams.addParameter(constructorParams);
    try (MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      commonsUtils.when(() -> CommonsUtils.getService(any())).thenReturn(null);
      return JSON.readTree(new FileindexingConnector(initParams).getMapping());
    }
  }

  /**
   * Builds a file document whose transcript is about 135 KB and ends with
   * {@link #LAST_WORD}.
   *
   * @return the document JSON
   */
  private String fileDocument() {
    String sentence = "Alors on reprend le point sur le serveur MCP et les transcriptions des réunions produit. ";
    String transcript = sentence.repeat(1500) + "Pour conclure, le mot de la fin est " + LAST_WORD + " et on se revoit jeudi.";
    ObjectNode document = JSON.createObjectNode();
    document.put("name", FILE_NAME);
    document.put("title", FILE_NAME);
    document.put("transcription", transcript);
    return JSON.writeValueAsString(document);
  }

  /**
   * Runs a search on the test index.
   *
   * @param query the search body
   * @return the parsed response
   * @throws Exception when the engine cannot be reached or refuses the search
   */
  private JsonNode search(String query) throws Exception {
    HttpResponse<String> response = send("POST", "/" + index + "/_search", query);
    assertEquals(response.body(), 200, response.statusCode());
    return JSON.readTree(response.body());
  }

  /**
   * Sends a request to the engine.
   *
   * @param method the HTTP method
   * @param path the path, starting with a slash
   * @param body the JSON body, or null for none
   * @return the response
   * @throws Exception when the engine cannot be reached
   */
  private HttpResponse<String> send(String method, String path, String body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(elasticsearchUrl() + path))
                                     .header("Content-Type", "application/json")
                                     .method(method,
                                             body == null ? HttpRequest.BodyPublishers.noBody()
                                                          : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                     .build();
    return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Returns the engine named by es.url, or starts a container once per class.
   *
   * @return the engine base URL
   */
  private static synchronized String elasticsearchUrl() {
    if (url == null) {
      String configured = System.getProperty("es.url");
      if (StringUtils.isNotBlank(configured)) {
        url = StringUtils.removeEnd(configured, "/");
      } else {
        container = new ElasticsearchContainer(DockerImageName.parse(IMAGE)).withEnv("xpack.security.enabled", "false")
                                                                            .withEnv("discovery.type", "single-node")
                                                                            .withStartupTimeout(Duration.ofMinutes(5));
        container.start();
        url = "http://" + container.getHttpHostAddress();
      }
    }
    return url;
  }
}
