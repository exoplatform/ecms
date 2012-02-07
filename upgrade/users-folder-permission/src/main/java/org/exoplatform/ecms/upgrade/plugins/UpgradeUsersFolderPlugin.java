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
package org.exoplatform.ecms.upgrade.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Feb 3, 2012
 * 3:03:41 PM  
 * 
 * This class will be used to do 2 operations
 * 1. Allow super user has full permission on Users folder
 * 2. Remove "Remove" right permission of folder Private & Public under user folder
 */
public class UpgradeUsersFolderPlugin extends UpgradeProductPlugin {

  private RepositoryService repoService_;
  private Log LOG = ExoLogger.getLogger(this.getClass());  
  private NodeHierarchyCreator nodeHCreator_;
  private OrganizationService orgService_;

  public UpgradeUsersFolderPlugin(OrganizationService orgService, NodeHierarchyCreator nodeHCreator, 
      RepositoryService repoService, InitParams initParams) {
    super(initParams); 
    repoService_ = repoService;
    nodeHCreator_ = nodeHCreator;
    orgService_ = orgService;
  }
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if(LOG.isInfoEnabled()) LOG.info("Starting upgrade permisson for Users folder");

    //Upgrade permission for Users folder. Super user will grand full permission to Users folder
    upgradePermissionForUserFolder();

    //Update permission for all folder under User folder (Private & Public)
    try {
      upgradePermissionForChildren();
    } catch (Exception e) {
      LOG.error("Upgrade FAILED! Unexpected problem happens", e);
    }

    if(LOG.isInfoEnabled()) LOG.info("Finished upgrade permission for Users Folder");
  }


  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

  /**
   * Upgrade permission for Users folder. Super user will grand full permission to Users folder
   */
  private void upgradePermissionForUserFolder() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    String superUser = WCMCoreUtils.getService(UserACL.class).getSuperUser();
    try {
      String defaultWSName = repoService_.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      //Get User folder by Alias usersPath
      Node usersFolder = (Node)sessionProvider.getSession(
          defaultWSName, repoService_.getCurrentRepository()).getItem(nodeHCreator_.getJcrPath("usersPath"));

      //Prepare permission list to update permission for User folder
      List<String> persList = new ArrayList<String>(4);
      persList.add(PermissionType.READ);
      persList.add(PermissionType.ADD_NODE);
      persList.add(PermissionType.SET_PROPERTY);
      persList.add(PermissionType.REMOVE);

      //Grand permission for super user
      ((ExtendedNode)usersFolder).setPermission(superUser, persList.toArray(new String[persList.size()]));
      sessionProvider.getSession(defaultWSName, repoService_.getCurrentRepository()).save();
    } catch (PathNotFoundException e) {
      LOG.error("Upgrade FAILED! Users folder can not be found", e);
    } catch (LoginException e) {
      LOG.error("Upgrade failed! Cannot login into system", e);
    } catch (NoSuchWorkspaceException e) {
      LOG.error("Upgrade FAILED! The default workspace cannot be found", e);
    } catch (RepositoryException e) {
      LOG.error("Upgrade FAILED! Unexpected problem happens", e);
    } finally {
      if(sessionProvider != null) sessionProvider.close();
    }
  }

  /**
   * Remove "Remove" right permission of children folder of User folder (Private & Public) 
   * @throws Exception 
   */
  private void upgradePermissionForChildren() throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    String defaultWSName = repoService_.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    try {
      //Get all users
      WCMCoreUtils.startRequest(orgService_);
      ListAccess<User> userListAccess = orgService_.getUserHandler().findAllUsers();
      List<User> userList = WCMCoreUtils.getAllElementsOfListAccess(userListAccess);
      for(User user : userList) {
        String userName = user.getUserName();
        Node userNode = nodeHCreator_.getUserNode(sessionProvider, userName);
        NodeIterator nodeIter = userNode.getNodes();
        //Remove "Remove" right permission of all nodes under user folder
        while (nodeIter.hasNext()) {
          NodeImpl nodeImpl = (NodeImpl) nodeIter.next();
          nodeImpl.removePermission(userName, PermissionType.REMOVE);
        }
      }
      sessionProvider.getSession(defaultWSName, repoService_.getCurrentRepository()).save();
    } catch (Exception e) {
      //Rollback the change in the case exception occurs
      sessionProvider.getSession(defaultWSName, repoService_.getCurrentRepository()).refresh(false);
      LOG.error("MIGRATION DIDN'T SUCCESSED", e);
    } finally {
      WCMCoreUtils.endRequest(orgService_);
      if(sessionProvider != null) sessionProvider.close();
    }
  }

}
