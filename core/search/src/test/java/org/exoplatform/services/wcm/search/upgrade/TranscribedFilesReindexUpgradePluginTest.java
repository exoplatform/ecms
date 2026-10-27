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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
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
import org.exoplatform.commons.upgrade.UpgradePluginExecutionContext;
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
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;

@RunWith(MockitoJUnitRunner.class)
public class TranscribedFilesReindexUpgradePluginTest {

  /** Version the rig ran the plugin at, unchanged between restarts. */
  private static final String                  VERSION          = "7.3.x-ai-contribution-SNAPSHOT-rev20261026";

  /** Product version before an upgrade. */
  private static final String                  PREVIOUS_VERSION = "7.2.0";

  /** Product version an upgrade brings. */
  private static final String                  NEW_VERSION      = "7.3.0";

  @Mock
  private IndexingService                      indexingService;

  @Mock
  private RepositoryService                    repositoryService;

  @Mock
  private Session                              session;

  @Mock
  private Workspace                            workspace;

  @Mock
  private NodeTypeManager                      nodeTypeManager;

  private TranscribedFilesReindexUpgradePlugin plugin;

  /**
   * Builds the plugin with the parameters core-search-configuration.xml gives
   * it, on a mocked session whose workspace exposes a node type manager.
   *
   * @throws Exception when a mock setup fails
   */
  @Before
  public void setUp() throws Exception {
    InitParams initParams = new InitParams();
    initParams.addParameter(valueParam("product.group.id", "org.exoplatform.platform"));
    initParams.addParameter(valueParam("plugin.upgrade.execute.once", "true"));
    plugin = new TranscribedFilesReindexUpgradePlugin(initParams, repositoryService, indexingService);
    when(session.getWorkspace()).thenReturn(workspace);
    when(workspace.getNodeTypeManager()).thenReturn(nodeTypeManager);
  }

  /**
   * The plugin runs on an instance that never ran it, even when the product
   * version did not change, and never again once one run succeeded.
   */
  @Test
  public void testRunsOnceWhateverTheProductVersion() {
    assertTrue(plugin.shouldProceedToUpgrade(VERSION, VERSION, null));
    assertFalse(plugin.shouldProceedToUpgrade(VERSION, VERSION, new UpgradePluginExecutionContext(VERSION, 1)));
  }

  /**
   * Before running a plugin, the upgrade service stores the previous product
   * version with no execution, and keeps it when the run throws. A run that
   * failed during an upgrade to a new version is therefore run again at the
   * next startup; one that failed at an unchanged version is not.
   */
  @Test
  public void testFailedRunIsRetriedOnlyAfterAVersionChange() {
    assertTrue(plugin.shouldProceedToUpgrade(NEW_VERSION,
                                             NEW_VERSION,
                                             new UpgradePluginExecutionContext(PREVIOUS_VERSION, 0)));
    assertFalse(plugin.shouldProceedToUpgrade(VERSION, VERSION, new UpgradePluginExecutionContext(VERSION, 0)));
  }

  /**
   * A repository failure is thrown, so that the upgrade service does not
   * record the plugin as executed, and the session is logged out.
   *
   * @throws Exception when a mock setup fails
   */
  @Test
  public void testFailureIsThrownAndSessionLoggedOut() throws Exception {
    SessionProvider sessionProvider = mock(SessionProvider.class);
    when(sessionProvider.getSession(eq("collaboration"), any())).thenReturn(session);
    when(workspace.getQueryManager()).thenThrow(new RepositoryException("query failed"));

    assertThrows(IllegalStateException.class, () -> plugin.reindexTranscribedFiles(sessionProvider));

    verify(session).logout();
    verifyNoInteractions(indexingService);
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

  /**
   * Builds a value parameter.
   *
   * @param name the parameter name
   * @param value the parameter value
   * @return the parameter
   */
  private static ValueParam valueParam(String name, String value) {
    ValueParam param = new ValueParam();
    param.setName(name);
    param.setValue(value);
    return param;
  }
}
