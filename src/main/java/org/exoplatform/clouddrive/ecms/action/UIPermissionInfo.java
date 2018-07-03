/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.core.UIPermissionInfoGrid;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.core.bean.PermissionBean;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;

/**
 * The Class UIPermissionInfo.
 */
@Deprecated // TODO not required
@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = UIPermissionInfo.DeleteActionListener.class, confirm = "UIPermissionInfo.msg.confirm-delete-permission"),
    @EventConfig(listeners = UIPermissionInfo.EditActionListener.class) })
public class UIPermissionInfo extends org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionInfo {

  /** The permission bean field. */
  protected static String[]  PERMISSION_BEAN_FIELD = { "usersOrGroups" };                        // PermissionType.READ,
                                                                                                 // PermissionType.REMOVE

  /**
   * The permission action.
   */
  protected static String[]  PERMISSION_ACTION     = { "Delete" };

  /** The Constant LOG. */
  protected static final Log LOG                   = ExoLogger.getLogger(UIPermissionInfo.class);

  /**
   * The listener interface for receiving deleteAction events. The class that is
   * interested in processing a deleteAction event implements this interface,
   * and the object created with that class is registered with a component using
   * the component's <code>addDeleteActionListener</code> method. When the
   * deleteAction event occurs, that object's appropriate method is invoked.
   */
  public static class DeleteActionListener extends
                                           org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionInfo.DeleteActionListener {

    /**
     * {@inheritDoc}
     */
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
          // FYI bad idea to use the original action logic: it does lot of bad
          // work DeleteActionListener.super.execute(event);

          CloudFileActionService actions = uiComp.getApplicationComponent(CloudFileActionService.class);
          // 1. remove all links to the cloud file for given user/group identity
          actions.removeLinks(currentNode, identity);
          // 2. unshare the file
          actions.unshareCloudFile(currentNode, localDrive, userOrGroup);
        } catch (Exception e) {
          throw new CloudDriveException("Error invoking Delete action in Sharing info: " + e.getMessage(), e);
        }
      } else {
        LOG.warn("Cloud Drive cannot be found for " + currentNode.getPath());
      }

      try {
        currentNode.getIndex(); // check if node valid here
        uiComp.updateGrid(uiComp.getChild(UIGrid.class).getUIPageIterator().getCurrentPage());
        uiJCRExplorer.setIsHidePopup(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
      } catch (InvalidItemStateException e) {
        uiJCRExplorer.setCurrentPath(uiJCRExplorer.getRootPath());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiJCRExplorer);
      }
    }
  }

  /**
   * Instantiates a new UI permission info.
   *
   * @throws Exception the exception
   */
  public UIPermissionInfo() throws Exception {
    super();
    // customize permissions grid
    UIPermissionInfoGrid uiGrid = getChild(UIPermissionInfoGrid.class);
    uiGrid.configure("usersOrGroups", PERMISSION_BEAN_FIELD, PERMISSION_ACTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateGrid(int currentPage) throws Exception {
    // let original code work
    super.updateGrid(currentPage);
    // then filter the permissions to remove managers of groups (if ones)
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class);
    PageList<PermissionBean> dataPageList = (PageList<PermissionBean>) uiGrid.getUIPageIterator().getPageList();
    List<PermissionBean> permBeans = new ArrayList<PermissionBean>();
    boolean filtered = false;
    next: for (Iterator<PermissionBean> iter = dataPageList.getAll().iterator(); iter.hasNext();) {
      PermissionBean permBean = iter.next();
      String permission = permBean.getUsersOrGroups();
      String[] ids = permission.split(":");
      if (ids.length == 2) {
        String membership = ids[0];
        String identity = ids[1];
        if (membership.equals("manager")) {
          // loop page list again to find other memberships of this group
          for (Iterator<PermissionBean> giter = dataPageList.getAll().iterator(); giter.hasNext();) {
            PermissionBean gpb = giter.next();
            String gpermission = gpb.getUsersOrGroups();
            String[] gids = gpermission.split(":");
            if (gids.length == 2) {
              String gmembership = gids[0];
              String gidentity = gids[1];
              if (identity.equals(gidentity)) {
                if (!membership.equals(gmembership)) {
                  // skip manager permission from first loop as we already have
                  // this group
                  // with another membership in gdp - will be added in the loop
                  filtered = true;
                  continue next;
                } // otherwise, it is only manager permission for this group -
                  // will be added below
              }
            }
          }
        }
      }
      permBeans.add(permBean);
    }
    // apply filtered permissions (same code as in
    // UIPermissionInfoBase.updateGrid())
    if (filtered) {
      ListAccess<PermissionBean> permList = new ListAccessImpl<PermissionBean>(PermissionBean.class, permBeans);
      dataPageList = new LazyPageList<PermissionBean>(permList, 10);
      uiGrid.getUIPageIterator().setPageList(dataPageList);
      if (currentPage > uiGrid.getUIPageIterator().getAvailablePage()) {
        uiGrid.getUIPageIterator().setCurrentPage(uiGrid.getUIPageIterator().getAvailablePage());
      } else {
        uiGrid.getUIPageIterator().setCurrentPage(currentPage);
      }
    }
  }

}
