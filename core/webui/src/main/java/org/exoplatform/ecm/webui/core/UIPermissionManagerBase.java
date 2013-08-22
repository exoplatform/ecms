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
package org.exoplatform.ecm.webui.core;

import javax.jcr.Node;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

public abstract class UIPermissionManagerBase extends UIContainer implements UIPopupComponent{
  private static final Log LOG  = ExoLogger.getLogger(UIPermissionManagerBase.class.getName());

  public void initPopupPermission(UIComponent uiSelector) throws Exception {
    UIPopupWindow uiPopup = getChildById(UIPermissionFormBase.POPUP_SELECT);
    if(uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, UIPermissionFormBase.POPUP_SELECT);
      uiPopup.setWindowSize(560, 345);
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
    UIUserContainer uiUserContainer = this.createUIComponent(UIUserContainer.class, null, null);
    uiPopup.setUIComponent(uiUserContainer);
    uiPopup.setShow(true);
    uiPopup.setShowMask(true);
    uiPopup.setResizable(true);
  }

  public void activate() {
    try {
      getChild(UIPermissionInfoBase.class).updateGrid(1);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  public void checkPermissonInfo(Node node) throws Exception {
    if (node.isLocked()) {
      String lockToken = LockUtil.getLockToken(node);
      if (lockToken != null)
        node.getSession().addLockToken(lockToken);
      if (!Utils.isLockTokenHolder(node)) {
        getChild(UIPermissionInfoBase.class).getChild(UIGrid.class)
                                        .configure("usersOrGroups",
                                                   UIPermissionInfoBase.PERMISSION_BEAN_FIELD,
                                                   new String[] {});
        getChild(UIPermissionFormBase.class).setRendered(false);
      }
    } else {
      if (!PermissionUtil.canChangePermission(node)) {
        getChild(UIPermissionInfoBase.class).getChild(UIGrid.class)
                                        .configure("usersOrGroups",
                                                   UIPermissionInfoBase.PERMISSION_BEAN_FIELD,
                                                   new String[] {});
        getChild(UIPermissionFormBase.class).setRendered(false);
      }
    }
  }
  
  public void deActivate() {
  }

  static public class AddUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      UIPermissionManagerBase uiParent = uiForm.getAncestorOfType(UIPermissionManagerBase.class);
      UIPermissionFormBase uiPermissionForm = uiParent.getChild(UIPermissionFormBase.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiForm.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.getChild(UIPopupWindow.class);
      uiPopup.setUIComponent(null);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
