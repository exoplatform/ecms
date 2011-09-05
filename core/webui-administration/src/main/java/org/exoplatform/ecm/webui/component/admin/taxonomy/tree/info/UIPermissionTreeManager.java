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
package org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info;

import javax.jcr.Node;

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
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 17, 2009
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIPermissionTreeManager extends UIContainer {

  public static final String POPUP_TAXONOMY_SELECT_USER = "TaxoPopupUserSelector";

  public UIPermissionTreeManager() throws Exception {
    addChild(UIPermissionTreeInfo.class, null, null);
    addChild(UIPermissionTreeForm.class, null, null);
  }

  public void initPopupPermission(UIComponent uiSelector) throws Exception {
    removeChildById(UIPermissionTreeForm.POPUP_SELECT);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, UIPermissionTreeForm.POPUP_SELECT);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(560, 300);
    uiPopup.setRendered(true);
    uiPopup.setUIComponent(uiSelector);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  public void initUserSelector() throws Exception {
    UIPopupWindow uiPopup = getChildById(POPUP_TAXONOMY_SELECT_USER);
    if (uiPopup == null) 
      uiPopup = addChild(UIPopupWindow.class, null, POPUP_TAXONOMY_SELECT_USER);    
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(790, 400);
    UIPermissionTreeSelectUser uiUserContainer = createUIComponent(UIPermissionTreeSelectUser.class, null, null);
    uiPopup.setUIComponent(uiUserContainer);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  public void update() throws Exception {
    getChild(UIPermissionTreeInfo.class).updateGrid();
  }

  public void checkPermissonInfo(Node node) throws Exception {
    if (node.isLocked()) {
      String lockToken = LockUtil.getLockToken(node);
      if (lockToken != null)
        node.getSession().addLockToken(lockToken);
      if (!Utils.isLockTokenHolder(node)) {
        getChild(UIPermissionTreeInfo.class).getChild(UIGrid.class).configure("usersOrGroups",
            UIPermissionTreeInfo.PERMISSION_BEAN_FIELD, new String[] {});
        getChild(UIPermissionTreeForm.class).setRendered(false);
      }
    } else {
      if (!PermissionUtil.canChangePermission(node)) {
        getChild(UIPermissionTreeInfo.class).getChild(UIGrid.class).configure("usersOrGroups",
            UIPermissionTreeInfo.PERMISSION_BEAN_FIELD, new String[] {});
        getChild(UIPermissionTreeForm.class).setRendered(false);
      }
    }
  }

  public static class AddUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      UIPermissionTreeManager uiParent = uiForm.getAncestorOfType(UIPermissionTreeManager.class);
      UIPermissionTreeForm uiPermissionForm = uiParent.getChild(UIPermissionTreeForm.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiForm.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.getChild(UIPopupWindow.class);
      uiPopup.setUIComponent(null);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
