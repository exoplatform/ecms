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
package org.exoplatform.services.cms.drives.impl;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2007 3:09:21 PM
 */
public class NewUserListener extends UserEventListener {

  private ManageDriveService driveService_ ;
  private InitParams initParams_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  private String userPath_ ;

  /**
   *
   * @param driveService
   * @param nodeHierarchyCreatorService
   * @param params
   * @throws Exception
   */
  public NewUserListener(ManageDriveService driveService,
      NodeHierarchyCreator nodeHierarchyCreatorService,
      InitParams params) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreatorService ;
    driveService_ = driveService ;
    initParams_ = params ;
    userPath_ = nodeHierarchyCreatorService.getJcrPath(BasePath.CMS_USERS_PATH) ;
  }

  /**
   *
   */
  @SuppressWarnings({"unused"})
  public void preSave(User user, boolean isNew) throws Exception {
    String workspace = initParams_.getValueParam("workspace").getValue();
    String permissions = initParams_.getValueParam("permissions").getValue();
    permissions = permissions.concat(","+ user.getUserName());
    //Set personal drive home path 
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, user.getUserName());
    String homePath = userNode.getPath();
    String views = initParams_.getValueParam("views").getValue();
    String icon = initParams_.getValueParam("icon").getValue();
    boolean viewPreferences = Boolean.parseBoolean(initParams_.getValueParam("viewPreferences").getValue());
    boolean viewNonDocument = Boolean.parseBoolean(initParams_.getValueParam("viewNonDocument").getValue());
    boolean viewSideBar = Boolean.parseBoolean(initParams_.getValueParam("viewSideBar").getValue());
    boolean showHiddenNode = Boolean.parseBoolean(initParams_.getValueParam("showHiddenNode").getValue());
    String allowCreateFolder = initParams_.getValueParam("allowCreateFolder").getValue();
    String allowNodeTypesOnTree = initParams_.getValueParam("allowNodeTypesOnTree").getValue();
    //Only user can access private drive
    String publicPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USER_PUBLIC_ALIAS) ;
    String privatePath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USER_PRIVATE_ALIAS) ;
    //Get user relPath
    String userRelPath = StringUtils.replaceOnce(userNode.getPath(), userPath_ + "/", "");
    //add drive with user relPath
    driveService_.addDrive(userRelPath + "|" + privatePath,
                           workspace,
                           user.getUserName(),
                           homePath + "/" + privatePath,
                           views,
                           icon,
                           viewPreferences,
                           viewNonDocument,
                           viewSideBar,
                           showHiddenNode,
                           allowCreateFolder,
                           allowNodeTypesOnTree);
    //User and everyone can see public drive for user
    driveService_.addDrive(userRelPath + "|" + publicPath,
                           workspace,
                           permissions,
                           homePath + "/" + publicPath,
                           views,
                           icon,
                           viewPreferences,
                           viewNonDocument,
                           viewSideBar,
                           showHiddenNode,
                           allowCreateFolder,
                           allowNodeTypesOnTree);
  }

  /**
   *
   */
  public void preDelete(User user) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, user.getUserName());
    String userRelPath = StringUtils.replaceOnce(userNode.getPath(), userPath_ + "/", "");
    //Remove private drive
    driveService_.removeDrive(userRelPath + "|" + nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USER_PRIVATE_ALIAS));
    
    //Remove public drive
    driveService_.removeDrive(userRelPath + "|" + nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USER_PUBLIC_ALIAS));
  }
}
