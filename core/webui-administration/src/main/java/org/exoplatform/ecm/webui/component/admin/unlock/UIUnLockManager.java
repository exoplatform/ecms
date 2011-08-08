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
package org.exoplatform.ecm.webui.component.admin.unlock;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006
 * 11:27:14 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")
public class UIUnLockManager extends UIAbstractManager {

  public UIUnLockManager() throws Exception {
    addChild(UILockNodeList.class, null, null).setRendered(true);
    addChild(UILockHolderContainer.class, null, null).setRendered(false);
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    UILockNodeList uiLockNodeList = getChild(UILockNodeList.class);
    uiLockNodeList.refresh(uiLockNodeList.getUIPageIterator().getCurrentPage());
    UILockHolderContainer uiHolderContainer = getChild(UILockHolderContainer.class);
    if (uiHolderContainer != null) {
      UILockHolderList uiLockHolderList = uiHolderContainer.getChild(UILockHolderList.class);
      uiLockHolderList.refresh(uiLockHolderList.getUIPageIterator().getCurrentPage());
    }
  }

  public void initFormPopup(String id) throws Exception {
    removeChildById(id);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, id);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600, 500);
    UIUnLockForm uiForm = createUIComponent(UIUnLockForm.class, null, null);
    uiPopup.setUIComponent(uiForm);
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  public void initPermissionPopup(String membership) throws Exception {
    removeChildById("PermissionPopup");
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "PermissionPopup");
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(560, 300);
    UIPermissionSelector uiECMPermission =
      createUIComponent(UIPermissionSelector.class, null, "GroupsOrUsersBrowse");
    uiECMPermission.setSelectedMembership(true);
    if(membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/");
      uiECMPermission.setCurrentPermission("/" + arrMember[1]);
    }
    uiPopup.setUIComponent(uiECMPermission);
    UIUnLockForm uiForm = findFirstComponentOfType(UIUnLockForm.class);
    uiECMPermission.setSourceComponent(uiForm, new String[] {UIUnLockForm.GROUPS_OR_USERS});
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }
}
