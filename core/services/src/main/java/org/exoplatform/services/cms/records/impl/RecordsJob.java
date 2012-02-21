/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.records.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.BaseJob;
import org.exoplatform.services.scheduler.JobContext;

public class RecordsJob extends BaseJob {

  private static final String QUERY = "SELECT * FROM rma:filePlan";

  /**
   * Logger.
   */
  private static final Log          LOG  = ExoLogger.getLogger("job.RecordsJob");

  private RepositoryService   repositoryService_;

  private RecordsService      recordsService_;

  /**
   * {@inheritDoc}
   */
  public void execute(JobContext context) throws Exception {
    Session session = null;
    try {
      if (LOG.isDebugEnabled())
        LOG.debug("File plan job started");
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      repositoryService_ = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
      recordsService_ = (RecordsService) container.getComponentInstanceOfType(RecordsService.class);
      ManageableRepository repository = repositoryService_.getCurrentRepository();

      if (repository.getState() != ManageableRepository.OFFLINE) {
        String[] workspaces = repository.getWorkspaceNames();
        for (int i = 0; i < workspaces.length; i++) {
          String workspaceName = workspaces[i];
          if (LOG.isDebugEnabled())
            LOG.debug("Search File plans in workspace : " + workspaceName);

          session = repository.getSystemSession(workspaceName);
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          Query query = queryManager.createQuery(QUERY, Query.SQL);
          QueryResult results = query.execute();
          NodeIterator iter = results.getNodes();
          if (LOG.isDebugEnabled())
            LOG.debug("File plan nodes : " + iter.getSize());
          while (iter.hasNext()) {
            Node filePlan = iter.nextNode();
            try {
              recordsService_.computeCutoffs(filePlan);
              recordsService_.computeHolds(filePlan);
              recordsService_.computeTransfers(filePlan);
              recordsService_.computeAccessions(filePlan);
              recordsService_.computeDestructions(filePlan);
            } catch (RepositoryException ex) {
              if (LOG.isErrorEnabled()) {
                LOG.error(ex.getMessage(), ex);
              }
            }
          }
          session.logout();
        }
      } else {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Repository '" + repository.getConfiguration().getName()
              + "' is not started. Execution skipped.");
        }
      }
    } catch (Exception e) {
      if (session != null) {
        session.logout();
      }
    }
    if (LOG.isDebugEnabled())
      LOG.debug("File plan job done");
  }
}
