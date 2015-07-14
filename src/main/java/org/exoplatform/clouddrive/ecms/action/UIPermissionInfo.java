/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.core.UIPermissionInfoGrid;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

@ComponentConfig(lifecycle = UIContainerLifecycle.class,
                 events = {
                     @EventConfig(listeners = UIPermissionInfo.DeleteActionListener.class,
                                  confirm = "UIPermissionInfo.msg.confirm-delete-permission"),
                     @EventConfig(listeners = UIPermissionInfo.EditActionListener.class) })
public class UIPermissionInfo extends org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionInfo {

  protected static String[]  PERMISSION_BEAN_FIELD = { "usersOrGroups" };

  protected static String[]  PERMISSION_ACTION     = { "Delete" };

  protected static final Log LOG                   = ExoLogger.getLogger(UIPermissionInfo.class);

  public UIPermissionInfo() throws Exception {
    super();
    // customize permissions grid
    UIPermissionInfoGrid uiGrid = getChild(UIPermissionInfoGrid.class);
    uiGrid.configure("usersOrGroups", PERMISSION_BEAN_FIELD, PERMISSION_ACTION);
  }

  static public class DeleteActionListener extends
                                           org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionInfo.DeleteActionListener {
    public void execute(Event<org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionInfo> event) throws Exception {
      UIPermissionInfo uiComp = (UIPermissionInfo) event.getSource();
      UIJCRExplorer uiJCRExplorer = uiComp.getAncestorOfType(UIJCRExplorer.class);
      UIPermissionManagerBase uiParent = uiComp.getParent();

      Node currentNode = uiComp.getCurrentNode();
      String userOrGroup = event.getRequestContext().getRequestParameter(OBJECTID);
      String[] groupIdentity = userOrGroup.split(":");
      String identity;
      if (groupIdentity.length == 2) {
        identity = groupIdentity[1];
      } else {
        identity = userOrGroup;
      }

      CloudDriveService cloudDrives = uiComp.getApplicationComponent(CloudDriveService.class);
      CloudDrive localDrive = cloudDrives.findDrive(currentNode);
      if (localDrive != null) {
        try {
          // TODO bad idea to use the original action logic: it does lot of bad work
          // DeleteActionListener.super.execute(event);

          CloudFileActionService actions = uiComp.getApplicationComponent(CloudFileActionService.class);
          // 1. remove all links to the cloud file for given user/group identity
          actions.removeLinks(currentNode, identity);//actions.getUserPublicNode(identity).getNodes()
          // 2. unshare the file
          actions.unshareCloudFile(currentNode, localDrive, userOrGroup);
        } catch (Exception e) {
          throw new CloudDriveException("Error invoking Delete action in Sharing info: " + e.getMessage(), e);
        }
      } else {
        LOG.warn("Cloud Drive cannot be found for " + currentNode.getPath());
      }
      
      try {
        currentNode.refresh(true);
        uiComp.updateGrid(uiComp.getChild(UIGrid.class).getUIPageIterator().getCurrentPage());
        uiJCRExplorer.setIsHidePopup(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
      } catch(InvalidItemStateException e) {
        uiJCRExplorer.setCurrentPath(uiJCRExplorer.getRootPath());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiJCRExplorer);        
      }      
    }
  }
}
