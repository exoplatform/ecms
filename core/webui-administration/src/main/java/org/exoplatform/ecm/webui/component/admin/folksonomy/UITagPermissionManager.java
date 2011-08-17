/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 11, 2009
 * 4:55:20 PM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)
public class UITagPermissionManager extends UIContainer {

  public UITagPermissionManager() throws Exception {
    addChild(UITagPermissionInfo.class, null, null);
    addChild(UITagPermissionForm.class, null, null);
  }

  public void initPopupPermission(UIComponent uiSelector) throws Exception {
    UIPopupWindow uiPopup = getChildById(UITagPermissionForm.POPUP_SELECT);
    if(uiPopup == null) {      
      uiPopup = addChild(UIPopupWindow.class, null, UITagPermissionForm.POPUP_SELECT);
      uiPopup.setWindowSize(560, 300);
      uiPopup.setShowMask(true);
    } else {
      uiPopup.setRendered(true);
    }
    uiPopup.setUIComponent(uiSelector);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  static  public class AddUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      UITagPermissionManager uiParent = uiForm.getAncestorOfType(UITagPermissionManager.class);
      UITagPermissionForm uiTagPermissionForm = uiParent.getChild(UITagPermissionForm.class);
      uiTagPermissionForm.doSelect(UITagPermissionInputSet.FIELD_USERORGROUP, uiForm.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.getChild(UIPopupWindow.class);
      uiPopup.setUIComponent(null);
      uiPopup.setShow(false);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }

}
