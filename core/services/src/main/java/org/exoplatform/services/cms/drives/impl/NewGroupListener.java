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

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2007 3:09:45 PM
 */
public class NewGroupListener extends GroupEventListener {

  private ManageDriveService driveService_ ;
  private RepositoryService jcrService_;
  private InitParams initParams_ ;
  private String groupsPath_ ;

  final static private String GROUPS_PATH = "groupsPath";

  /**
   *
   * @param jcrService
   * @param driveService
   * @param nodeHierarchyCreatorService
   * @param params
   * @throws Exception
   */
  public NewGroupListener(RepositoryService jcrService,
      ManageDriveService driveService,
      NodeHierarchyCreator nodeHierarchyCreatorService,
      InitParams params) throws Exception {
    jcrService_ = jcrService ;
    driveService_ = driveService ;
    initParams_ = params ;
    groupsPath_ = nodeHierarchyCreatorService.getJcrPath(GROUPS_PATH) ;
  }

  @SuppressWarnings({"unused", "hiding", "null"})
  public void preSave(Group group, boolean isNew) throws Exception {
    String  groupId = group.getId() ;
    String name = group.getId().replace("/", ".");
    String workspace = initParams_.getValueParam("workspace").getValue();
    String permissions = "*:".concat(groupId);
    String extpermissions = initParams_.getValueParam("permissions").getValue();
    if(StringUtils.isNotBlank(extpermissions)) {
      permissions.concat(",").concat(extpermissions);
    }
    String homePath = groupsPath_ + groupId ;
    String views = initParams_.getValueParam("views").getValue();
    String icon = initParams_.getValueParam("icon").getValue();
    boolean viewPreferences = Boolean.parseBoolean(initParams_.getValueParam("viewPreferences").getValue());
    boolean viewNonDocument = Boolean.parseBoolean(initParams_.getValueParam("viewNonDocument").getValue());
    boolean viewSideBar = Boolean.parseBoolean(initParams_.getValueParam("viewSideBar").getValue());
    boolean showHiddenNode = Boolean.parseBoolean(initParams_.getValueParam("showHiddenNode").getValue());
    String allowCreateFolder = initParams_.getValueParam("allowCreateFolder").getValue();
    String allowNodeTypesOnTree = initParams_.getValueParam("allowNodeTypesOnTree").getValue();
    driveService_.addDrive(name, workspace, permissions, homePath, views, icon, viewPreferences,
        viewNonDocument, viewSideBar, showHiddenNode, allowCreateFolder, allowNodeTypesOnTree);
  }

  public void preDelete(Group group) throws Exception {
    driveService_.removeDrive(group.getId().replace("/", "."));
  }
}
