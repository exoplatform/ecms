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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import javax.jcr.Node;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SARL
 * Author : TrongTT
 *          TrongTT@exoplatform.com
 * Sep 13, 2006
 * Editor : TuanP
 *        phamtuanchip@yahoo.de
 * Oct 13, 2006
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIPermissionManager extends UIContainer implements UIPopupComponent{
  public UIPermissionManager() throws Exception {
    addChild(UIPermissionInfo.class, null, null);
    addChild(UIPermissionForm.class, null, null);
  }
  public void initPopupPermission(UIComponent uiSelector) throws Exception {
    UIPopupWindow uiPopup = getChildById(UIPermissionForm.POPUP_SELECT);
    if(uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, UIPermissionForm.POPUP_SELECT);
      uiPopup.setWindowSize(560, 300);
      uiPopup.setShowMask(true);
    } else {
      uiPopup.setShowMask(true);
      uiPopup.setRendered(true);
    }
    uiPopup.setUIComponent(uiSelector);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  public void initUserSelector() throws Exception {
    UIPopupWindow uiPopup = getChildById("PopupUserSelector");
    if(uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, "PopupUserSelector");
    }
    uiPopup.setWindowSize(790, 400);
    UIUserContainer uiUserContainer = createUIComponent(UIUserContainer.class, null, null);
    uiPopup.setUIComponent(uiUserContainer);
    uiPopup.setShow(true);
    uiPopup.setShowMask(true);
    uiPopup.setResizable(true);
  }

  public void activate() throws Exception {
    getChild(UIPermissionInfo.class).updateGrid(1);
  }

  public void checkPermissonInfo(Node node) throws Exception {
    if (node.isLocked()) {
      String lockToken = LockUtil.getLockToken(node);
      if (lockToken != null)
        node.getSession().addLockToken(lockToken);
      if (!Utils.isLockTokenHolder(node)) {
        getChild(UIPermissionInfo.class).getChild(UIGrid.class)
                                        .configure("usersOrGroups",
                                                   UIPermissionInfo.PERMISSION_BEAN_FIELD,
                                                   new String[] {});
        getChild(UIPermissionForm.class).setRendered(false);
      }
    } else {
      if (!PermissionUtil.canChangePermission(node)) {
        getChild(UIPermissionInfo.class).getChild(UIGrid.class)
                                        .configure("usersOrGroups",
                                                   UIPermissionInfo.PERMISSION_BEAN_FIELD,
                                                   new String[] {});
        getChild(UIPermissionForm.class).setRendered(false);
      }
    }
  }

  public void deActivate() throws Exception {
  }

  static public class AddUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      UIPermissionManager uiParent = uiForm.getAncestorOfType(UIPermissionManager.class);
      UIPermissionForm uiPermissionForm = uiParent.getChild(UIPermissionForm.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiForm.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.getChild(UIPopupWindow.class);
      uiPopup.setUIComponent(null);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
