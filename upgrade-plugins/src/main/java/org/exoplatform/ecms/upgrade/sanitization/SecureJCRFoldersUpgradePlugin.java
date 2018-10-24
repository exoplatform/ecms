
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
package org.exoplatform.ecms.upgrade.sanitization;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityConstants;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.Iterator;

public class SecureJCRFoldersUpgradePlugin extends UpgradeProductPlugin {

  private OrganizationService orgService;
  private DMSConfiguration dmsConfiguration;
  private RepositoryService repoService;
  private NodeHierarchyCreator nodeHierarchyCreator;

  private static final Log LOG = ExoLogger.getLogger(SecureJCRFoldersUpgradePlugin.class.getName());

  public SecureJCRFoldersUpgradePlugin(OrganizationService orgService, RepositoryService repoService, DMSConfiguration dmsConfiguration,
                                       NodeHierarchyCreator nodeHierarchyCreator, InitParams initParams) {
    super(initParams);
    this.orgService = orgService;
    this.repoService = repoService;
    this.dmsConfiguration = dmsConfiguration;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }

    migrateECMSystem();

    migrateSites();

    migrateGroups();

    migrateDigitalAssets();
  }

  private void migrateECMSystem() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(dmsConfiguration.getConfig().getSystemWorkspace(),
          repoService.getCurrentRepository());

      Node ecmNode = (Node)session.getItem("/exo:ecm");

      String[] relPaths = {null, "exo:taxonomyTrees/definition", "exo:folksonomies/exo:tagStyles",
          "templates", "scripts", "metadata", "queries", "scripts/ecm-explorer",
          "scripts/ecm-explorer/action", "scripts/ecm-explorer/interceptor", "scripts/ecm-explorer/widget",
          "views", "views/templates", "views/userviews", "views/templates/ecm-explorer"};
      for (String path: relPaths) {
        removePermission(ecmNode, path, IdentityConstants.ANY);
      }

      removePermission(ecmNode, "exo:taxonomyTrees/storage", "*:/platform/users");
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate /exo:ecm storage system", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  private void migrateSites() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();;
    try {
      String ws = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(ws, repoService.getCurrentRepository());

      removePermission((Node) session.getItem(nodeHierarchyCreator.getJcrPath("siteContent")),
          null, IdentityConstants.ANY);
      removePermission((Node) session.getItem(nodeHierarchyCreator.getJcrPath("liveSiteContent")),
          null, IdentityConstants.ANY);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate Sites", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  private void migrateGroups() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      String ws = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(ws, repoService.getCurrentRepository());

      Node rootedNode = (Node) session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH));
      removePermission(rootedNode, null, "*:/platform/users");

      migrateGroup(rootedNode, null); // migrate from the root (top) of groups
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate /Groups", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  private void migrateGroup(Node groupNode, Group group) throws Exception {
    // If it is NOT the root group
    if (group != null) {
      removePermission(groupNode,"ApplicationData/Tags", IdentityConstants.ANY);
      removePermission(groupNode,"SharedData", IdentityConstants.ANY);
    }

    GroupHandler groupHandler = orgService.getGroupHandler();
    Collection<Group> children = groupHandler.findGroups(group);
    if (children != null) {
      for (Group childGroup: children) {
        try {
          Node childGroupNode = groupNode.getNode(group.getGroupName());
          migrateGroup(childGroupNode, childGroup);
        } catch (PathNotFoundException e) {
          LOG.warn("Could not find the group node: " + groupNode.getPath() + group.getGroupName());
        }
      }
    }
  }

  private void migrateDigitalAssets() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      String ws = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(ws, repoService.getCurrentRepository());

      String[] EXO_DIGITAL_PATH_ALIAS = new String[]{
          "digitalVideoPath", "digitalAudioPath", "digitalAssetsPath", "digitalPicturePath"
      };
      for (String digitalNodePath: EXO_DIGITAL_PATH_ALIAS){
        String exoDigitalNodePath = nodeHierarchyCreator.getJcrPath(digitalNodePath);
        removePermission((Node) session.getItem(exoDigitalNodePath), null, "*:/platform/users");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate /Digital Assets", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  private void removePermission(Node rootedNode, String relativePath, String permission) throws RepositoryException {
    ExtendedNode node;
    if (relativePath != null) {
      node = (ExtendedNode) rootedNode.getNode(relativePath);
    } else {
      node = (ExtendedNode) rootedNode;
    }

    if (node != null) {
      node.removePermission(permission);
      node.save();
    } else {
      LOG.warn("Could not find the node path: " + rootedNode.getPath() + relativePath);
    }
  }
}
