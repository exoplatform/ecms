/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.migration;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Feb 17, 2011
 */
public class ActionMigrationService implements Startable {

  private RepositoryService repoService_;
  private Log log = ExoLogger.getLogger(this.getClass());

  public ActionMigrationService(RepositoryService repositoryService) {
    repoService_ = repositoryService;
  }

  @Override
  public void start() {
    String exoNodeTypeName = "exo:nodeTypeName";
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      ManageableRepository repository = repoService_.getCurrentRepository();
      Session session = null;
      for (String wsName : repository.getWorkspaceNames()) {
        try {
          session = sessionProvider.getSession(wsName, repository);
          QueryManager manager = session.getWorkspace().getQueryManager();
          String statement = "SELECT * from exo:action WHERE " + exoNodeTypeName + " IS NOT NULL";
          Query query = manager.createQuery(statement.toString(), Query.SQL);
          NodeIterator nodes = query.execute().getNodes();

          while (nodes.hasNext()) {
            Node node = (Node)nodes.next();
            if (node.getProperty(exoNodeTypeName).getValues().length == 0)
              node.setProperty(exoNodeTypeName, (Value[])null);
          }
          session.save();
          session.logout();
        } catch (Exception e) {
          if (session != null && session.isLive())
            session.logout();
          if (log.isErrorEnabled())
            log.error("An unexpected problem occurs when migrating data for action nodes in workspace "
                          + wsName,
                      e);
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error("An unexpected problem occurs when migrating data for action nodes", e);
    } finally {
      sessionProvider.close();
    }
    if (log.isInfoEnabled()) log.info("Action nodes data migrated successfully!...");
  }

  @Override
  public void stop() {

  }

}
