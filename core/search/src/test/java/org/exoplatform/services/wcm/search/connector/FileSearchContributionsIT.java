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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.ecms.legacy.search.es.ElasticSearchFilter;
import org.exoplatform.ecms.legacy.search.es.ElasticSearchFilterType;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The files keyword search with contributed ids on a real Elasticsearch (run
 * with {@code -Prun-its}): an index with the file mapping
 * ({@link FileindexingConnector#getMapping()}) and the platform's default
 * settings, and the requests {@link FileSearchServiceConnector} builds, sent
 * as built, a refused request failing the test. A mock cannot tell a request
 * the engine refuses, nor whether a contributed file outside the permissions
 * or a filter comes back.
 */
public class FileSearchContributionsIT {

  private static final String           IMAGE = System.getProperty("es.testcontainer.image",
                                                                   "docker.elastic.co/elasticsearch/elasticsearch:9.3.4");

  private static final String           INDEX = "file_it";

  private static final JsonMapper       JSON  = JsonMapper.builder().build();

  private static final HttpClient       HTTP  = HttpClient.newHttpClient();

  private static ElasticsearchContainer container;

  private static String                 url;

  private static QueryBuilder           builder;

  /**
   * Starts the engine, creates the file index and indexes four files: a1
   * « Northwind contract » and b2 « Order form » readable by user1, c3
   * « Secret Northwind » by user2 only, d4 « Sales plan » by the sales space's
   * members, its tag {@code roadmap}.
   *
   * @throws Exception when the engine does not answer
   */
  @BeforeClass
  public static void startElasticsearch() throws Exception {
    String configured = System.getProperty("es.url");
    if (StringUtils.isNotBlank(configured)) {
      url = StringUtils.removeEnd(configured, "/");
    } else {
      container = new ElasticsearchContainer(DockerImageName.parse(IMAGE)).withEnv("xpack.security.enabled", "false")
                                                                          .withEnv("discovery.type", "single-node")
                                                                          .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                                                                          .withStartupTimeout(Duration.ofMinutes(5));
      container.start();
      url = "http://" + container.getHttpHostAddress();
    }
    send("PUT", "/" + INDEX, "{\"settings\": " + defaultSettings() + ", \"mappings\": " + fileMapping() + "}");
    index("a1", "Northwind contract", "user1", "");
    index("b2", "Order form", "user1", "");
    index("c3", "Secret Northwind", "user2", "");
    index("d4", "Sales plan", "*:/spaces/sales", "roadmap");
    send("POST", "/" + INDEX + "/_refresh", "");

    InitParams initParams = new InitParams();
    PropertiesParam params = new PropertiesParam();
    params.setName("constructor.params");
    params.setProperty("index", INDEX);
    params.setProperty("titleField", "title");
    params.setProperty("searchFields", "name,title,attachment.content,tags");
    initParams.addParameter(params);
    builder = new QueryBuilder(initParams);
    ConversationState.setCurrent(new ConversationState(new Identity("user1",
                                                                    List.of(new MembershipEntry("/spaces/sales", "member")))));
  }

  /**
   * Stops the engine and clears the current user.
   */
  @AfterClass
  public static void stopElasticsearch() {
    ConversationState.setCurrent(null);
    if (container != null) {
      container.stop();
    }
  }

  /**
   * Contributed ids add the user's readable files to the keyword's, and never
   * a file the user may not read (c3). Mutant: the ids clause outside the
   * keyword's {@code must}.
   */
  @Test
  public void testKeywordAndContributedIdsWithinThePermissions() throws Exception {
    assertEquals(Set.of("a1"), search("Northwind", List.of(), List.of()));
    assertEquals(Set.of("a1", "b2"), search("Northwind", List.of(), List.of("b2", "c3")));
  }

  /**
   * A contributed file is found by its id alone, the keyword matching nothing,
   * through a membership too.
   */
  @Test
  public void testContributedIdsAlone() throws Exception {
    assertEquals(Set.of("b2", "d4"), search("Zzyzx", List.of(), List.of("b2", "c3", "d4")));
  }

  /**
   * A contributed file passes the other filters too: under a tag filter, only
   * the tagged one comes back.
   */
  @Test
  public void testContributedIdsUnderAFilter() throws Exception {
    ElasticSearchFilter tag = new ElasticSearchFilter(ElasticSearchFilterType.FILTER_BY_TERM, "tags", "roadmap");
    assertEquals(Set.of("d4"), search("Zzyzx", List.of(tag), List.of("b2", "d4")));
  }

  /**
   * Odd ids are dropped before the request is written, odd sort orders are
   * rewritten, and a keyword with a backslash, a tab or a leading separator
   * is split into words: the engine accepts every such request. A keyword of
   * separators only searches as no keyword, every readable file. Mutants: the
   * id shape check removed, the order written as sent, the backslash kept in
   * the words, the JSON escape removed, the empty words kept.
   */
  @Test
  public void testRequestsTheEngineMustAccept() throws Exception {
    assertEquals(Set.of("a1", "b2"), search("Northwind", List.of(), List.of("b2", "bad\"id", "x\\y")));
    String request = builder.query("Northwind", new ArrayList<>(), 0, 10, "date", "asc\"}},{\"x\":\"", List.of("b2"));
    send("POST", "/" + INDEX + "/_search", request);
    assertEquals(Set.of("a1"), search("Northwind\\", List.of(), List.of()));
    assertEquals(Set.of("a1", "b2"), search("+Northwind\tcontract\\", List.of(), List.of("b2")));
    assertEquals(Set.of("a1", "b2", "d4"), search("\\ +", List.of(), List.of("b2")));
  }

  /**
   * Searches as unified search's Files: the user's permissions, the given
   * filters, the keyword or the contributed ids.
   *
   * @param keyword the keyword
   * @param filters the filters
   * @param contributedIds the contributed ids
   * @return the ids of the files found
   */
  private static Set<String> search(String keyword, List<ElasticSearchFilter> filters, List<String> contributedIds) throws Exception {
    String request = builder.query(keyword, new ArrayList<>(filters), 0, 10, null, null, contributedIds);
    JsonNode hits = JSON.readTree(send("POST", "/" + INDEX + "/_search", request)).path("hits").path("hits");
    Set<String> ids = new HashSet<>();
    hits.forEach(hit -> ids.add(hit.path("_id").asString()));
    return ids;
  }

  /**
   * Indexes one file document.
   *
   * @param id the node id
   * @param title the title
   * @param permission the one permission
   * @param tag the one tag, none when empty
   */
  private static void index(String id, String title, String permission, String tag) throws Exception {
    send("PUT",
         "/" + INDEX + "/_doc/" + id,
         "{\"title\":\"" + title + "\",\"name\":\"" + title + "\",\"permissions\":[\"" + permission + "\"]"
             + (tag.isEmpty() ? "" : ",\"tags\":[\"" + tag + "\"]") + ",\"lastUpdatedDate\":1700000000000}");
  }

  /**
   * Sends a request to the engine.
   *
   * @param method the method
   * @param path the path
   * @param body the body, empty for none
   * @return the answer's body
   * @throws IOException when the engine answers anything but 2xx
   */
  private static String send(String method, String path, String body) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                                     .header("Content-Type", "application/json")
                                     .timeout(Duration.ofSeconds(30))
                                     .method(method,
                                             body.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                                                            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                     .build();
    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() / 100 != 2) {
      throw new IOException(method + " " + path + " answered " + response.statusCode() + ": " + response.body() + "\nrequest: " + body);
    }
    return response.body();
  }

  /**
   * @return commons' default index settings, with a test's values
   */
  private static String defaultSettings() throws IOException {
    try (InputStream settings = FileSearchContributionsIT.class.getClassLoader().getResourceAsStream("es-default-index-settings.json")) {
      assertNotNull("the platform's default index settings are on the test classpath", settings);
      return new String(settings.readAllBytes(), StandardCharsets.UTF_8).replace("shard.number", "1")
                                                                        .replace("replica.number", "0")
                                                                        .replace("max_regex.length", "1000");
    }
  }

  /**
   * @return the file connector's mapping
   */
  private static String fileMapping() {
    InitParams initParams = new InitParams();
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("index_alias", "file_alias");
    constructorParams.setProperty("index_current", "file_v6");
    initParams.addParameter(constructorParams);
    try (MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      commonsUtils.when(() -> CommonsUtils.getService(any())).thenReturn(null);
      return new FileindexingConnector(initParams).getMapping();
    }
  }

  /**
   * The file search connector, with its request builder reachable: the
   * request it builds is what the test sends.
   */
  private static final class QueryBuilder extends FileSearchServiceConnector {

    /**
     * @param initParams the connector's parameters
     */
    QueryBuilder(InitParams initParams) {
      super(initParams, mock(ElasticSearchingClient.class), null, null);
    }

    /**
     * @return the request the connector sends for these arguments
     */
    String query(String keyword,
                 List<ElasticSearchFilter> filters,
                 int offset,
                 int limit,
                 String sort,
                 String order,
                 Collection<String> contributedIds) {
      return buildFilteredQuery(keyword, null, filters, offset, limit, sort, order, List.copyOf(contributedIds), getContributedBoost());
    }
  }

}
