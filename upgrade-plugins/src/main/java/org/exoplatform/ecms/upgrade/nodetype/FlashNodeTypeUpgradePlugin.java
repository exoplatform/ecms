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
package org.exoplatform.ecms.upgrade.nodetype;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tanhq@exoplatform.com
 * Sep 10, 2013  
 */

public class FlashNodeTypeUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(FlashNodeTypeUpgradePlugin.class.getName());
  private RepositoryService repositoryService_;
  final static public String EXO_RISIZEABLE = "exo:documentSize";
  final static public String FLASH_MIMETYPE = "flash";

  public FlashNodeTypeUpgradePlugin(RepositoryService repoService, InitParams initParams) {
    super(initParams);
    this.repositoryService_ = repoService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      String[] workspaces = repositoryService_.getCurrentRepository().getWorkspaceNames();
      if(workspaces.length > 0) {
        for (String workspace : workspaces) {
          Session session = sessionProvider.getSession(workspace, repositoryService_.getCurrentRepository());
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          String queryStatement = "SELECT * FROM nt:resource WHERE jcr:mimeType IS NOT NULL AND jcr:mimeType LIKE '%"+
          FLASH_MIMETYPE+"%'";
          Query query = queryManager.createQuery(queryStatement, Query.SQL);
          NodeIterator iter = query.execute().getNodes();
          while (iter.hasNext()) {
            Node node = iter.nextNode();  
            Node flashNode = node.getParent();
            if(flashNode.canAddMixin(EXO_RISIZEABLE)) {
              flashNode.addMixin(EXO_RISIZEABLE);
              flashNode.save();
            }
          }
        }
      }      
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating flash node type.", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

}