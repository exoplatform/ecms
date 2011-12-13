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
package org.exoplatform.ecm.webui.component.admin.views;

import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2006
 * 2:09:18 PM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIViewContainer extends UIContainer {

  public UIViewContainer() throws Exception {
    addChild(UIViewList.class, null, null) ;
  }

  public void initPopup(String popupId) throws Exception {
    removeChildById(popupId) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setShowMask(true);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600,400) ;
    UIViewFormTabPane uiViewForm = createUIComponent(UIViewFormTabPane.class, null, null) ;
    uiPopup.setUIComponent(uiViewForm) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void update() throws Exception {
    UIViewList uiViewList = getChild(UIViewList.class);
    uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
  }

  public void initPopupPermission(String membership) throws Exception {
    removeChildById(UIViewFormTabPane.POPUP_PERMISSION) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, UIViewFormTabPane.POPUP_PERMISSION);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600, 300);
    UIPermissionSelector uiECMPermission =
      createUIComponent(UIPermissionSelector.class, null, null) ;
    uiECMPermission.setSelectedMembership(true);
    if(membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/") ;
      uiECMPermission.setCurrentPermission("/" + arrMember[1]) ;
    }
    uiPopup.setUIComponent(uiECMPermission);
    UIViewForm uiViewForm = findFirstComponentOfType(UIViewForm.class) ;    
    uiECMPermission.setSourceComponent(uiViewForm, new String[] {UIViewForm.FIELD_PERMISSION}) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
