/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.services.wcm.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;
import org.exoplatform.social.rest.api.RestUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the unified search of files with the real query builder and reads the
 * query sent to the search engine: what a non-admin may find outside the space
 * drives and their own drive. The query's shape is checked with a strict
 * parser; no search engine runs here, as no test harness of this module starts
 * one, so how the clauses match on a real index is checked on a running
 * platform.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestFileSearchRestService {

  private static final String           ES_RESPONSE_EMPTY = "{ \"hits\": { \"hits\": [] } }";

  private static final String           PRIVATE_PATH      = "/Users/b___/bo___/bob/Private";

  @Mock
  private ElasticSearchingClient        client;

  @Mock
  private RepositoryService             repositoryService;

  @Mock
  private DocumentService               documentService;

  @Mock
  private NodeHierarchyCreator          nodeHierarchyCreator;

  @Mock
  private UserACL                       userACL;

  private MockedStatic<PortalContainer> portalContainerStatic;

  private MockedStatic<RestUtils>       restUtilsStatic;

  private Identity                      bob;

  private FileSearchRestService         fileSearchRestService;

  @Before
  public void setUp() {
    PortalContainer portalContainer = mock(PortalContainer.class);
    portalContainerStatic = mockStatic(PortalContainer.class);
    portalContainerStatic.when(PortalContainer::getInstance).thenReturn(portalContainer);
    when(portalContainer.getComponentInstanceOfType(UserACL.class)).thenReturn(userACL);
    restUtilsStatic = mockStatic(RestUtils.class);
    restUtilsStatic.when(RestUtils::getCurrentUserIdentityId).thenReturn(1L);

    bob = new Identity("bob", Arrays.asList(new MembershipEntry("/spaces/marketing", "member")));
    ConversationState.setCurrent(new ConversationState(bob));
    when(client.sendRequest(anyString(), nullable(String.class))).thenReturn(ES_RESPONSE_EMPTY);

    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(buildInitParams(),
                                                                                           client,
                                                                                           repositoryService,
                                                                                           documentService);
    fileSearchRestService = new FileSearchRestService(fileSearchServiceConnector, nodeHierarchyCreator);
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
    portalContainerStatic.close();
    restUtilsStatic.close();
  }

  @Test
  public void aNonAdminSearchesTheirDrivesAndWhatIsSharedWithThemByNameOrGroupButNotWhatAnyUserMayRead() throws Exception {
    when(userACL.isSuperUser(bob)).thenReturn(false);
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    when(userACL.isUserInGroup(bob, "/platform/administrators")).thenReturn(false);
    mockPrivateNode();

    fileSearchRestService.searchRecentDocuments("hello", false, null, null, 10, false, null, null);

    JsonNode scope = getScopeClauses(getSentQuery());
    assertEquals(4, scope.size());
    assertEquals("/Groups/spaces.*", getValue(scope, 0, "regexp", "path"));
    assertEquals(PRIVATE_PATH + ".*", getValue(scope, 1, "regexp", "path"));
    assertEquals("bob", getValue(scope, 2, "term", "permissions"));
    assertEquals(Set.of("member:/spaces/marketing", "*:/spaces/marketing"),
                 new HashSet<>(Arrays.asList(getValue(scope, 3, "regexp", "permissions").split("\\|"))));
    for (JsonNode clause : scope) {
      assertFalse("The scope never lets 'any' in: " + clause, clause.toString().contains("\"any\""));
    }
  }

  @Test
  public void anAdministratorSearchesWithNoScope() throws Exception {
    when(userACL.isSuperUser(bob)).thenReturn(false);
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    when(userACL.isUserInGroup(bob, "/platform/administrators")).thenReturn(true);
    mockPrivateNodeLeniently();

    fileSearchRestService.searchRecentDocuments("hello", false, null, null, 10, false, null, null);

    assertNull(getScopeClauses(getSentQuery()));
  }

  @Test
  public void theSuperUserSearchesWithNoScope() throws Exception {
    when(userACL.isSuperUser(bob)).thenReturn(true);
    mockPrivateNodeLeniently();

    fileSearchRestService.searchRecentDocuments("hello", false, null, null, 10, false, null, null);

    assertNull(getScopeClauses(getSentQuery()));
  }

  private void mockPrivateNode() throws Exception {
    Node userNode = mock(Node.class);
    Node privateNode = mock(Node.class);
    when(nodeHierarchyCreator.getUserNode(any(SessionProvider.class), eq("bob"))).thenReturn(userNode);
    when(userNode.getNode("Private")).thenReturn(privateNode);
    when(privateNode.getPath()).thenReturn(PRIVATE_PATH);
  }

  /**
   * An admin's search reads no private node; the stubs are there so that a
   * search that wrongly scopes it fails on the scope, not on a missing node.
   */
  private void mockPrivateNodeLeniently() throws Exception {
    Node userNode = mock(Node.class);
    Node privateNode = mock(Node.class);
    lenient().when(nodeHierarchyCreator.getUserNode(any(SessionProvider.class), eq("bob"))).thenReturn(userNode);
    lenient().when(userNode.getNode("Private")).thenReturn(privateNode);
    lenient().when(privateNode.getPath()).thenReturn(PRIVATE_PATH);
    lenient().when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
  }

  /**
   * Parses the query sent to the search engine with a strict parser, which
   * refuses what the search engine refuses, such as a trailing comma.
   */
  private JsonNode getSentQuery() {
    ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(query.capture(), nullable(String.class));
    return JsonMapper.builder().build().readTree(query.getValue());
  }

  /**
   * @return the clauses of the filter whose should matches on the path, or
   *         null when the query has none
   */
  private JsonNode getScopeClauses(JsonNode query) {
    JsonNode filters = query.path("query").path("bool").path("filter");
    JsonNode musts = filters.get(filters.size() - 1).path("bool").path("must");
    for (JsonNode must : musts) {
      JsonNode should = must.path("bool").path("should");
      if (should.isArray() && should.toString().contains("\"path\"")) {
        return should;
      }
    }
    return null;
  }

  private String getValue(JsonNode clauses, int index, String queryType, String field) {
    JsonNode clause = clauses.get(index);
    assertTrue("Clause " + index + " is a " + queryType + ": " + clause, clause.has(queryType));
    return clause.path(queryType).path(field).asString();
  }

  private InitParams buildInitParams() {
    InitParams initParams = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setProperty("searchType", FileindexingConnector.TYPE);
    propertiesParam.setProperty("displayName", "Files");
    propertiesParam.setProperty("searchFields", "name,tag,title,attachment.content");
    propertiesParam.setProperty("index_current", "index_name");
    initParams.put("constructor.params", propertiesParam);
    return initParams;
  }
}
