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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Queues a reindex of every file that carries a transcript. Elasticsearch
 * refused the index document of a file whose transcript was longer than its
 * keyword term limit (EXO-92202), so the copy of the previous file index made
 * when the mapping version changes holds either no document or a stale one for
 * such a file. The queued operations are processed once the index upgrade has
 * switched the alias to the new index.
 */
public class TranscribedFilesReindexUpgradePlugin extends UpgradeProductPlugin {

  /** JCR-SQL statement returning the nodes that carry the transcript mixin. */
  public static final String    TRANSCRIBED_FILES_QUERY = "SELECT * FROM " + NodetypeConstant.EXO_TRANSCRIPTION;

  /** Workspace holding the files the file index covers. */
  private static final String   WORKSPACE               = "collaboration";

  /** Logger. */
  private static final Log      LOG                     = ExoLogger.getLogger(TranscribedFilesReindexUpgradePlugin.class);

  /** Repository the transcribed files are read from. */
  private final RepositoryService repositoryService;

  /** Queue the reindex operations are added to. */
  private final IndexingService   indexingService;

  /**
   * Builds the plugin with the services it reads from and writes to.
   *
   * @param initParams the upgrade plugin parameters
   * @param repositoryService the JCR repository service
   * @param indexingService the indexing queue
   */
  public TranscribedFilesReindexUpgradePlugin(InitParams initParams,
                                              RepositoryService repositoryService,
                                              IndexingService indexingService) {
    super(initParams);
    this.repositoryService = repositoryService;
    this.indexingService = indexingService;
  }

  /**
   * Opens a request life cycle, then queues the reindex of every transcribed
   * file through a system session. A failure is thrown, so that the upgrade
   * service does not record the plugin as executed: the next startup runs it
   * again when the failed one was an upgrade to a new product version, and
   * not when the product version was unchanged.
   *
   * @param oldVersion the previous product version
   * @param newVersion the new product version
   */
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      reindexTranscribedFiles(WCMCoreUtils.getSystemSessionProvider());
    } finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * Opens a session of the files workspace, queues the reindex of every
   * transcribed file and logs the session out.
   *
   * @param sessionProvider the provider the session is opened with
   * @return the number of queued reindex operations
   * @throws IllegalStateException when the repository cannot be read
   */
  int reindexTranscribedFiles(SessionProvider sessionProvider) {
    Session session = null;
    try {
      session = sessionProvider.getSession(WORKSPACE, repositoryService.getCurrentRepository());
      int count = reindexTranscribedFiles(session);
      LOG.info("Queued the reindex of {} transcribed files", count);
      return count;
    } catch (RepositoryException e) {
      throw new IllegalStateException("Error while queuing the reindex of transcribed files", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  /**
   * Queues a reindex operation for each node returned by
   * {@link #TRANSCRIBED_FILES_QUERY}. A repository that does not define the
   * transcript mixin holds no transcribed file, so nothing is queued.
   *
   * @param session the JCR session the query runs in
   * @return the number of queued reindex operations
   * @throws RepositoryException when the query cannot be run
   */
  int reindexTranscribedFiles(Session session) throws RepositoryException {
    try {
      session.getWorkspace().getNodeTypeManager().getNodeType(NodetypeConstant.EXO_TRANSCRIPTION);
    } catch (NoSuchNodeTypeException e) {
      LOG.info("Node type {} is not defined, no transcribed file to reindex", NodetypeConstant.EXO_TRANSCRIPTION);
      return 0;
    }
    Query query = session.getWorkspace().getQueryManager().createQuery(TRANSCRIBED_FILES_QUERY, Query.SQL);
    NodeIterator nodes = query.execute().getNodes();
    int count = 0;
    while (nodes.hasNext()) {
      NodeImpl node = (NodeImpl) nodes.nextNode();
      indexingService.reindex(FileindexingConnector.TYPE, node.getInternalIdentifier());
      count++;
    }
    return count;
  }
}
