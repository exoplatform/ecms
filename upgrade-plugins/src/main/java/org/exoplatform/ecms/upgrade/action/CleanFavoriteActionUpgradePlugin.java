/*
 * Copyright (C) 2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.ecms.upgrade.action;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Aymen Boughzela aboughzela@exoplatform.com
 * This upgrade will be used to clean allexo:addToFavoriteAction (Favorite action) created under favorite user folder.
 * The removed actions it's replaced by one favorite action created on the rootbfolder /Users.
 *
 */
public class CleanFavoriteActionUpgradePlugin extends UpgradeProductPlugin {
  private static final Log       LOG              = ExoLogger.getLogger(CleanFavoriteActionUpgradePlugin.class.getName());

  /**
   * Define relative path for action node
   */
  private static final String    EXO_ACTIONS      = "exo:actions";

  /**
   * Define nodetype ACTION_STORAGE
   */
  private static final String    ACTION_STORAGE   = "exo:actionStorage";

  /**
   * Define nodetype favorite
   */
  private static final String    ACTION_NODE_TYPE = "exo:addToFavoriteAction";

  /**
   * Define favorite mixin
   */
  private static final String    FAVORITE_MIXIN   = "exo:favoriteFolder";

  private ActionServiceContainer actionServiceContainer;

  private NodeHierarchyCreator   nodeHierarchyCreator;

  private RepositoryService      repoService;

  public CleanFavoriteActionUpgradePlugin(RepositoryService repoService,
                                          ActionServiceContainer actionServiceContainer,
                                          NodeHierarchyCreator nodeHierarchyCreator,
                                          InitParams initParams) {
    super(initParams);

    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.actionServiceContainer = actionServiceContainer;
    this.repoService = repoService;
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion, previousVersion);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    SessionProvider sessionProvider = null;
    try {
      LOG.info("Start " + getClass().getName() + ".............");
      RequestLifeCycle.begin(PortalContainer.getInstance());
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(
                                                   this.repoService.getCurrentRepository()
                                                                   .getConfiguration()
                                                                   .getDefaultWorkspaceName(),
                                                   this.repoService.getCurrentRepository());

      String usersNodePath = this.nodeHierarchyCreator.getJcrPath("usersPath");
      if (!usersNodePath.endsWith("/")) {
        usersNodePath = usersNodePath + "/";
      }
      // Select all favorite Action
      NodeIterator nodeIter = session.getWorkspace()
                                     .getQueryManager()
                                     .createQuery("SELECT * FROM " + ACTION_NODE_TYPE + " WHERE jcr:path like '" + usersNodePath
                                             + "%'", "sql")
                                     .execute()
                                     .getNodes();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Clean unused addToFavorite count :  " + nodeIter.getSize());
      }

      while (nodeIter.hasNext()) {
        Node favoriteActionNode = nodeIter.nextNode();
        Node actionStorageNode = favoriteActionNode.getParent();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Clean unused addToFavorite Path :  " + favoriteActionNode.getPath());
        }
        if (actionStorageNode == null || favoriteActionNode == null) {
          continue;
        }
         Node favoriteNode = actionStorageNode.getParent();
        // skip remove favorite action created on root users folder
        if (usersNodePath.concat(EXO_ACTIONS).equals(actionStorageNode.getPath()) || !favoriteNode.isNodeType(FAVORITE_MIXIN)) {
          continue;
        }
        try {
          if (this.actionServiceContainer.getAction(favoriteNode, favoriteActionNode.getName()) != null) {
            this.actionServiceContainer.removeAction(favoriteNode,
                                                     favoriteActionNode.getName(),
                                                     this.repoService.getCurrentRepository().getConfiguration().getName());
          }
          if (actionStorageNode.isNodeType(ACTION_STORAGE) && actionStorageNode.getNodes().getSize() == 0) {
            actionStorageNode.remove();
            session.save();
          }
        } catch (Exception ex) {
          LOG.error("Failed to clean addToFavorite PATH= " + favoriteActionNode.getPath(), ex);
        }
      }
      LOG.info("End " + getClass().getName() + ".............");
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(getClass().getName() + " Failed : ", e);
      }
    } finally {
      RequestLifeCycle.end();
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }
}
