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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.search.model.FileSearchMatchRequest;
import org.exoplatform.services.wcm.search.model.FileSearchMatches;
import org.exoplatform.services.wcm.search.model.SearchFieldMatches;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The files keyword search with contributed ids: the request the connector
 * builds, read with a strict parser, and the merge of the contributors'
 * answers. The request's grammar on a real engine is
 * {@code FileSearchContributionsIT}'s.
 */
public class FileSearchContributionsTest {

  private static final JsonMapper     MAPPER = JsonMapper.builder().build();

  private ElasticSearchingClient      client;

  private PropertiesParam             params;

  private FileSearchServiceConnector  connector;

  /**
   * A connector on a mocked client, for a user with one space membership.
   */
  @Before
  public void setUp() {
    client = mock(ElasticSearchingClient.class);
    when(client.sendRequest(anyString(), anyString())).thenReturn("{\"hits\":{\"hits\":[]}}");
    params = mock(PropertiesParam.class);
    when(params.getProperty("index")).thenReturn("file_alias");
    when(params.getProperty("searchFields")).thenReturn("name,title");
    when(params.getProperty("titleField")).thenReturn("title");
    InitParams initParams = mock(InitParams.class);
    when(initParams.getPropertiesParam("constructor.params")).thenReturn(params);
    connector = new FileSearchServiceConnector(initParams, client, null, null);
    ConversationState.setCurrent(new ConversationState(new Identity("user1",
                                                                    List.of(new MembershipEntry("/spaces/sales", "*")))));
  }

  /**
   * Clears the current user.
   */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * Contributed ids are an alternative to the keyword inside the
   * {@code must}; the permission filter stays in the {@code filter}, beside
   * it, so it applies to them. Mutants: the ids clause dropped, or written
   * without the keyword alternative.
   */
  @Test
  public void testContributedIdsAreAnAlternativeToTheKeywordInsideTheMust() {
    JsonNode bool = request("Northwind", List.of("abc-1", "def2", "abc-1")).path("query").path("bool");

    JsonNode alternative = bool.path("must").path("bool");
    assertEquals(1, alternative.path("minimum_should_match").asInt());
    JsonNode should = alternative.path("should");
    assertEquals(2, should.size());
    assertTrue(should.get(0).has("query_string"));
    assertEquals(List.of("abc-1", "def2"), values(should.get(1).path("ids").path("values")));
    assertEquals(FileSearchServiceConnector.DEFAULT_CONTRIBUTED_BOOST, should.get(1).path("ids").path("boost").asDouble(), 0d);
    assertTrue(bool.path("filter").toString().contains("permissions"));
  }

  /**
   * Without contributed ids, the keyword clause is the one the connector
   * always wrote.
   */
  @Test
  public void testNoContributedIdsKeepsTheKeywordClause() {
    JsonNode must = request("Northwind", List.of()).path("query").path("bool").path("must");
    assertTrue(must.has("query_string"));
    assertFalse(must.has("bool"));
  }

  /**
   * An id a request may not carry is dropped, the others kept in order: a
   * contributor's output is written into a hand-built request. Mutant: the
   * shape check removed (an id with a quote breaks the request).
   */
  @Test
  public void testIdsOutsideTheShapeAreDropped() {
    JsonNode request = request("Northwind", Arrays.asList("ok-1", "bad\"id", "../etc", null, "x".repeat(65), "ok2"));
    assertEquals(List.of("ok-1", "ok2"),
                 values(request.path("query").path("bool").path("must").path("bool").path("should").get(1).path("ids").path("values")));
  }

  /**
   * With no keyword (a favorites-only or tags-only search), no id is written:
   * the search keeps its shape. Mutant: the ids written without a keyword
   * clause.
   */
  @Test
  public void testNoKeywordWritesNoIds() {
    JsonNode request = request("", List.of("abc-1"));
    assertFalse(request.toString().contains("\"ids\""));
  }

  /**
   * The sort order is asc or desc, whatever the client sends. Mutant: the
   * order written as sent.
   */
  @Test
  public void testSortOrderIsAscOrDescOnly() {
    connector.filteredSearch(null, "x", new ArrayList<>(), null, 0, 10, "date", "asc\"}},{\"x\":\"", List.of(), 2d);
    ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(sent.capture(), anyString());
    JsonNode sort = MAPPER.readTree(sent.getValue()).path("sort").get(0);
    assertEquals("desc", sort.path("lastUpdatedDate").path("order").asString());
  }

  /**
   * Answers are merged in the contributors' order: an id once, its first
   * line kept, an id outside the shape dropped, a failing contributor
   * skipped, at most {@value FileSearchServiceConnector#MAX_CONTRIBUTED_FILES}
   * ids in all. Mutants: the catch removed, the cap removed, the shape check
   * removed, the first line replaced.
   */
  @Test
  public void testContributionsMerged() {
    SearchFieldMatches first = new SearchFieldMatches("a", "a", List.of(), 0, "");
    SearchFieldMatches second = new SearchFieldMatches("b", "b", List.of(), 0, "");
    FileSearchMatchPlugin failing = mock(FileSearchMatchPlugin.class);
    when(failing.search(any())).thenThrow(new IllegalStateException("index down"));
    FileSearchMatchPlugin one = mock(FileSearchMatchPlugin.class);
    when(one.search(any())).thenReturn(new FileSearchMatches(Arrays.asList("a1", "bad id", null, "b2"), Map.of("a1", first)));
    FileSearchMatchPlugin two = mock(FileSearchMatchPlugin.class);
    when(two.search(any())).thenReturn(new FileSearchMatches(List.of("b2", "a1", "c3"), Map.of("a1", second, "c3", second)));

    FileSearchMatches merged = connector.collectContributions(List.of(failing, one, two), request());

    assertEquals(List.of("a1", "b2", "c3"), merged.fileIds());
    assertSame(first, merged.matches().get("a1"));
    assertSame(second, merged.matches().get("c3"));

    FileSearchMatchPlugin many = mock(FileSearchMatchPlugin.class);
    when(many.search(any())).thenReturn(new FileSearchMatches(IntStream.range(0, FileSearchServiceConnector.MAX_CONTRIBUTED_FILES + 5)
                                                                       .mapToObj(i -> "id" + i)
                                                                       .toList(),
                                                              Map.of()));
    assertEquals(FileSearchServiceConnector.MAX_CONTRIBUTED_FILES,
                 connector.collectContributions(List.of(many), request()).fileIds().size());
  }

  /**
   * The contributed files' boost is read from the configuration, from 0.1 to
   * 100; anything else keeps the default. Mutant: the range check removed.
   */
  @Test
  public void testContributedBoostIsReadWithinItsRange() {
    Map<String, Double> expected = new LinkedHashMap<>();
    expected.put(null, 5.0d);
    expected.put("5", 5.0d);
    expected.put("0", 5.0d);
    expected.put("100.5", 5.0d);
    expected.put("NaN", 5.0d);
    expected.put("abc", 5.0d);
    expected.forEach((value, boost) -> assertEquals(value,
                                                     boost,
                                                     FileSearchServiceConnector.parseContributedBoost(value),
                                                     0d));
  }

  /**
   * @param query the keyword
   * @param contributedIds the contributed ids
   * @return the request the connector sends, read strictly
   */
  private JsonNode request(String query, List<String> contributedIds) {
    connector.filteredSearch(null, query, new ArrayList<>(), null, 0, 10, null, null, contributedIds, 2d);
    ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(sent.capture(), anyString());
    return MAPPER.readTree(sent.getValue());
  }

  /**
   * @param array a JSON array of strings
   * @return its values
   */
  private static List<String> values(JsonNode array) {
    List<String> result = new ArrayList<>();
    array.forEach(value -> result.add(value.asString()));
    return result;
  }

  /**
   * @return a request for the merge tests
   */
  private static FileSearchMatchRequest request() {
    return new FileSearchMatchRequest(new Identity("user1"), "x", List.of(), 10, Locale.ENGLISH);
  }

}
