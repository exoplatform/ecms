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
package org.exoplatform.services.wcm.search.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.LocationStepQueryNode;
import org.exoplatform.services.jcr.impl.core.query.NodeTypeQueryNode;
import org.exoplatform.services.jcr.impl.core.query.QueryNode;
import org.exoplatform.services.jcr.impl.core.query.QueryRootNode;
import org.exoplatform.services.jcr.impl.core.query.sql.JCRSQLQueryBuilder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;

@RunWith(MockitoJUnitRunner.class)
public class TranscribedFilesReindexUpgradePluginTest {

  @Mock
  private IndexingService                      indexingService;

  @Mock
  private Session                              session;

  @Mock
  private Workspace                            workspace;

  @Mock
  private NodeTypeManager                      nodeTypeManager;

  private TranscribedFilesReindexUpgradePlugin plugin;

  /**
   * Builds the plugin on a mocked session whose workspace exposes a node type
   * manager.
   *
   * @throws Exception when a mock setup fails
   */
  @Before
  public void setUp() throws Exception {
    InitParams initParams = new InitParams();
    ValueParam groupId = new ValueParam();
    groupId.setName("product.group.id");
    groupId.setValue("org.exoplatform.platform");
    initParams.addParameter(groupId);
    plugin = new TranscribedFilesReindexUpgradePlugin(initParams, mock(RepositoryService.class), indexingService);
    when(session.getWorkspace()).thenReturn(workspace);
    when(workspace.getNodeTypeManager()).thenReturn(nodeTypeManager);
  }

  /**
   * Every node the query returns is queued for a reindex of the file index
   * under its internal identifier, the id of its Elasticsearch document.
   *
   * @throws Exception when a mock setup fails
   */
  @Test
  public void testReindexTranscribedFilesQueuesEachFileById() throws Exception {
    NodeImpl first = mock(NodeImpl.class);
    when(first.getInternalIdentifier()).thenReturn("id-1");
    NodeImpl second = mock(NodeImpl.class);
    when(second.getInternalIdentifier()).thenReturn("id-2");
    NodeIterator nodes = mock(NodeIterator.class);
    when(nodes.hasNext()).thenReturn(true, true, false);
    when(nodes.nextNode()).thenReturn(first, second);
    QueryResult result = mock(QueryResult.class);
    when(result.getNodes()).thenReturn(nodes);
    Query query = mock(Query.class);
    when(query.execute()).thenReturn(result);
    QueryManager queryManager = mock(QueryManager.class);
    when(queryManager.createQuery(TranscribedFilesReindexUpgradePlugin.TRANSCRIBED_FILES_QUERY, Query.SQL)).thenReturn(query);
    when(workspace.getQueryManager()).thenReturn(queryManager);

    int count = plugin.reindexTranscribedFiles(session);

    assertEquals(2, count);
    verify(indexingService).reindex(FileindexingConnector.TYPE, "id-1");
    verify(indexingService).reindex(FileindexingConnector.TYPE, "id-2");
  }

  /**
   * A repository without the transcript mixin, which the Documents addon
   * defines, has no transcribed file: no query is run and nothing is queued.
   *
   * @throws Exception when a mock setup fails
   */
  @Test
  public void testReindexTranscribedFilesWithoutTranscriptNodeType() throws Exception {
    when(nodeTypeManager.getNodeType(NodetypeConstant.EXO_TRANSCRIPTION)).thenThrow(new NoSuchNodeTypeException());

    int count = plugin.reindexTranscribedFiles(session);

    assertEquals(0, count);
    verify(workspace, never()).getQueryManager();
    verifyNoInteractions(indexingService);
  }

  /**
   * The statement is parsed by the JCR's own SQL parser and selects the nodes
   * of type exo:transcription, with no other predicate.
   *
   * @throws Exception when the statement is refused by the parser
   */
  @Test
  public void testTranscribedFilesQueryIsParsedByTheJcrEngine() throws Exception {
    QueryRootNode root = JCRSQLQueryBuilder.createQuery(TranscribedFilesReindexUpgradePlugin.TRANSCRIBED_FILES_QUERY,
                                                        new LocationFactory(new NamespaceRegistryImpl()),
                                                        new DefaultQueryNodeFactory());

    LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
    assertEquals(1, steps.length);
    QueryNode[] predicates = steps[0].getPredicates();
    assertEquals(1, predicates.length);
    assertTrue(predicates[0] instanceof NodeTypeQueryNode);
    InternalQName nodeType = ((NodeTypeQueryNode) predicates[0]).getValue();
    assertEquals("transcription", nodeType.getName());
    assertEquals("http://www.exoplatform.com/jcr/exo/1.0", nodeType.getNamespace());
  }
}
