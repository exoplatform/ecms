/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.connector;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.search.base.EcmsSearchResult;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WCMCoreUtils.class)
@PowerMockIgnore({"javax.management.*","jdk.internal.reflect.*"})
public class TestFileSearchServiceConnector {

  public static final String ES_RESPONSE_EMPTY = "{ \"hits\": { \"hits\": [] } }";

  public static final String ES_RESPONSE_ONE_DOC = "{ \"hits\": { \"hits\":  [\n" +
          "           {\n" +
          "            \"_index\": \"file\",\n" +
          "            \"_type\": \"file\",\n" +
          "            \"_id\": \"7b9b54017f00010102ba5027fa2c5944\",\n" +
          "            \"_score\": 0.45138216,\n" +
          "            \"_source\": {\n" +
          "               \"workspace\": \"collaboration\",\n" +
          "               \"author\": \"john\",\n" +
          "               \"dc:creator\": \"cairo 1.9.5 (http://cairographics.org)\",\n" +
          "               \"repository\": \"repository\",\n" +
          "               \"title\": \"exo-documentation.pdf\",\n" +
          "               \"tags\": [\n" +
          "               \"exo-tag-doc-john\",\n" +
          "               ],\n" +
          "               \"path\": \"/sites/intranet/documents/exo-documentation.pdf\",\n" +
          "               \"lastUpdatedDate\": 1505312333066,\n" +
          "               \"createdDate\": \"1505310310756\",\n" +
          "               \"attachment\": {\n" +
          "                  \"content\": \"eXo Platform Documentation\"\n" +
          "               },\n" +
          "               \"fileSize\": \"94842\",\n" +
          "               \"permissions\": [\n" +
          "                  \"john\",\n" +
          "                  \"*:/platform/web-contributors\",\n" +
          "                  \"*:/platform/administrators\",\n" +
          "                  \"any\"\n" +
          "               ],\n" +
          "               \"name\": \"exo-documentation.pdf\",\n" +
          "               \"exo:internalUse\": \"false\",\n" +
          "               \"fileType\": \"application/pdf\",\n" +
          "               \"dc:publisher\": \"cairo 1.9.5 (http://cairographics.org)\"\n" +
          "            }\n" +
          "          }\n" +
          "        ]\n" +
          "      } }";

  public static final String ES_RESPONSE_TWO_DOCS = "{ \"hits\": { \"hits\":  [\n" +
          "           {\n" +
          "            \"_index\": \"file\",\n" +
          "            \"_type\": \"file\",\n" +
          "            \"_id\": \"7b9b54017f00010102ba5027fa2c5944\",\n" +
          "            \"_score\": 0.45138216,\n" +
          "            \"_source\": {\n" +
          "               \"workspace\": \"collaboration\",\n" +
          "               \"author\": \"john\",\n" +
          "               \"dc:creator\": \"cairo 1.9.5 (http://cairographics.org)\",\n" +
          "               \"repository\": \"repository\",\n" +
          "               \"title\": \"exo-documentation.pdf\",\n" +
          "               \"tags\": [\n" +
          "               \"exo-tag-doc-john\",\n" +
          "               ],\n" +
          "               \"path\": \"/sites/intranet/documents/exo-documentation.pdf\",\n" +
          "               \"lastUpdatedDate\": 1505312333066,\n" +
          "               \"createdDate\": \"1505310310756\",\n" +
          "               \"attachment\": {\n" +
          "                  \"content\": \"eXo Platform Documentation\"\n" +
          "               },\n" +
          "               \"fileSize\": \"94842\",\n" +
          "               \"permissions\": [\n" +
          "                  \"john\",\n" +
          "                  \"*:/platform/web-contributors\",\n" +
          "                  \"*:/platform/administrators\",\n" +
          "                  \"any\"\n" +
          "               ],\n" +
          "               \"name\": \"exo-documentation.pdf\",\n" +
          "               \"exo:internalUse\": \"false\",\n" +
          "               \"fileType\": \"application/pdf\",\n" +
          "               \"dc:publisher\": \"cairo 1.9.5 (http://cairographics.org)\"\n" +
          "            }\n" +
          "          },\n" +
          "           {\n" +
          "            \"_index\": \"file\",\n" +
          "            \"_type\": \"file\",\n" +
          "            \"_id\": \"7b9b54017f00010102ba5027fa2c5945\",\n" +
          "            \"_score\": 0.65568216,\n" +
          "            \"_source\": {\n" +
          "               \"workspace\": \"collaboration\",\n" +
          "               \"author\": \"mary\",\n" +
          "               \"repository\": \"repository\",\n" +
          "               \"title\": \"exo-training.pdf\",\n" +
          "               \"tags\": [\n" +
          "               \"exo-training-john\",\n" +
          "               ],\n" +
          "               \"path\": \"/sites/intranet/documents/exo-training.pdf\",\n" +
          "               \"lastUpdatedDate\": 1505312333166,\n" +
          "               \"createdDate\": \"1505312330856\",\n" +
          "               \"attachment\": {\n" +
          "                  \"content\": \"eXo Platform Training\"\n" +
          "               },\n" +
          "               \"fileSize\": \"250842\",\n" +
          "               \"permissions\": [\n" +
          "                  \"mary\",\n" +
          "                  \"any\"\n" +
          "               ],\n" +
          "               \"name\": \"exo-training.pdf\",\n" +
          "               \"exo:internalUse\": \"false\",\n" +
          "               \"fileType\": \"application/pdf\",\n" +
          "            }\n" +
          "          }\n" +
          "        ]\n" +
          "      } }";
  
  public static final String ES_RESPONSE_IS_INDEXED = "{\"took\":250,\"timed_out\":false,\"_shards\":{\"total\":5,"
      + "\"successful\":5,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":1,\"max_score\":1.0,"
      + "\"hits\":[{\"_index\":\"file_v2\",\"_type\":\"file\",\"_id\":\"fb48c6ad7f0000017be4f0e70309e014\",\"_score\":1.0,"
      + "\"_source\":{\"workspace\":\"collaboration\",\"author\":\"root\",\"lastModifier\":\"root\","
      + "\"repository\":\"repository\",\"title\":\"test6.txt\",\"version\":\"0\",\"tags\":[],"
      + "\"path\":\"/Users/r___/ro___/roo___/root/Private/Documents/test6.txt\",\"lastUpdatedDate\":\"1610533619474\","
      + "\"createdDate\":\"1610533619375\",\"attachment\":{\"content\":\"bb\"},\"fileSize\":\"3\",\"permissions\":[\"root\"],"
      + "\"name\":\"test6.txt\",\"exo:internalUse\":\"false\",\"fileType\":\"text/plain\"}}]}}";
  
  public static final String ES_RESPONSE_IS_NOT_INDEXED = "{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":5,"
      + "\"successful\":5,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":0,\"max_score\":null,\"hits\":[]}}";
  

  @Mock
  ElasticSearchingClient elasticSearchingClient;

  @Mock
  RepositoryService repositoryService;

  @Mock
  ManageableRepository repository;

  @Mock
  DocumentService documentService;

  @Before
  public void setUp() throws RepositoryException {
    PowerMockito.mockStatic(WCMCoreUtils.class);
    when(WCMCoreUtils.getRestContextName()).thenReturn("rest");

    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setName("repository");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void shouldReturnNoResultWhenNoDocInES() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_EMPTY);

    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);

    // When
    Collection<SearchResult> search = fileSearchServiceConnector.search(new SearchContext(new Router(new ControllerDescriptor()), "site"), "*", null, 0, -1, "", "");

    // Then
    assertNotNull(search);
    assertEquals(0, search.size());
  }

  @Test
  public void shouldReturnOneResultWhenOneDocInES() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_ONE_DOC);

    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);

    // When
    Collection<SearchResult> searchResults = fileSearchServiceConnector.search(new SearchContext(new Router(new ControllerDescriptor()), "site"), "", null, 0, -1, "", "");

    // Then
    assertNotNull(searchResults);
    assertEquals(1, searchResults.size());
    SearchResult searchResult = searchResults.iterator().next();
    assertEquals("exo-documentation.pdf", searchResult.getTitle());
    assertEquals("exo-tag-doc-john", ((EcmsSearchResult) searchResult).getTags().get(0));
    assertEquals(1505312333066L, searchResult.getDate());
    assertEquals("/rest/thumbnailImage/medium/repository/collaboration/sites/intranet/documents/exo-documentation.pdf", searchResult.getImageUrl());
  }

  @Test
  public void shouldReturnTwoResultsWhenTwoDocsInES() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_TWO_DOCS);

    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);

    // When
    Collection<SearchResult> searchResults = fileSearchServiceConnector.search(new SearchContext(new Router(new ControllerDescriptor()), "site"), "", null, 0, -1, "", "");

    // Then
    assertNotNull(searchResults);
    assertEquals(2, searchResults.size());
    Iterator<SearchResult> searchResultIterator = searchResults.iterator();
    SearchResult searchResult1 = searchResultIterator.next();
    assertEquals("exo-documentation.pdf", searchResult1.getTitle());
    assertEquals("exo-tag-doc-john", ((EcmsSearchResult) searchResult1).getTags().get(0));
    assertEquals(1505312333066L, searchResult1.getDate());
    assertEquals("/rest/thumbnailImage/medium/repository/collaboration/sites/intranet/documents/exo-documentation.pdf", searchResult1.getImageUrl());
    SearchResult searchResult2 = searchResultIterator.next();
    assertEquals("exo-training.pdf", searchResult2.getTitle());
    assertEquals("exo-training-john", ((EcmsSearchResult) searchResult2).getTags().get(0));
    assertEquals(1505312333166L, searchResult2.getDate());
    assertEquals("/rest/thumbnailImage/medium/repository/collaboration/sites/intranet/documents/exo-training.pdf", searchResult2.getImageUrl());
  }

  @Test
  public void searchByTagShouldReturnOneResult() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_ONE_DOC);

    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);

    // When
    Collection<SearchResult> searchResults = fileSearchServiceConnector.search(new SearchContext(new Router(new ControllerDescriptor()), "site"), "exo-tag-doc-john~", null, 0, -1, "relevancy", "desc");

    // Then
    assertNotNull(searchResults);
    assertEquals(1, searchResults.size());
    SearchResult searchResult = searchResults.iterator().next();
    assertEquals("exo-documentation.pdf", searchResult.getTitle());
    assertEquals("exo-tag-doc-john", ((EcmsSearchResult) searchResult).getTags().get(0));
    assertEquals(1505312333066L, searchResult.getDate());
    assertEquals("/rest/thumbnailImage/medium/repository/collaboration/sites/intranet/documents/exo-documentation.pdf", searchResult.getImageUrl());
  }
  
  @Test
  public void shouldReturnIsIndexedInES() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_IS_INDEXED);
    
    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);
    
    String uuid="123456789";
    // When
    boolean isIndexed = fileSearchServiceConnector.isIndexed(new SearchContext(new Router(new ControllerDescriptor()), ""),uuid);
    
    // Then
    assertTrue(isIndexed);
  }
  
  @Test
  public void shouldReturnIsNotIndexedInES() throws Exception {
    // Given
    startSessionAs("john");
    InitParams initParams = buildInitParams();
    when(elasticSearchingClient.sendRequest(anyString(), anyString(), anyString())).thenReturn(ES_RESPONSE_IS_NOT_INDEXED);
    
    FileSearchServiceConnector fileSearchServiceConnector = new FileSearchServiceConnector(initParams, elasticSearchingClient, repositoryService, documentService);
    
    String uuid="123456789";
    // When
    boolean isIndexed = fileSearchServiceConnector.isIndexed(new SearchContext(new Router(new ControllerDescriptor()), ""),uuid);
    
    // Then
    assertFalse(isIndexed);
  }

  /**
   * Build default init params
   * @return The InitParams object for search connector
   */
  public InitParams buildInitParams() {
    InitParams initParams = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setProperty("searchType", FileindexingConnector.TYPE);
    propertiesParam.setProperty("displayName", "Files");
    propertiesParam.setProperty("searchFields", "name,tag,title,attachment.content");
    initParams.put("constructor.params", propertiesParam);
    return initParams;
  }

  /**
   * Start a session with the given username
   *
   * @param username Username of the user to start a session for
   */
  private void startSessionAs(String username) {
    ConversationState.setCurrent(new ConversationState(new Identity(username)));
  }
}
