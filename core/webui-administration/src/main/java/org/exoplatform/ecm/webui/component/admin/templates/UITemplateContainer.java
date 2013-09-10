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
package org.exoplatform.ecm.webui.component.admin.templates;

import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2006
 * 2:09:18 PM
 */
@ComponentConfigs( {
  @ComponentConfig(lifecycle = UIContainerLifecycle.class)	
})

public class UITemplateContainer extends UIContainer {

  public UITemplateContainer() throws Exception {
    addChild(UITemplateList.class, null, null) ;
  }

  public void initPopup(UIComponent uiComponent, String popupId) throws Exception {
    removeChildById(popupId) ;    
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setShowMask(true);    
    uiPopup.setWindowSize(600,300) ;
    uiPopup.setUIComponent(uiComponent) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void update() throws Exception {
    UITemplateList uiTemplateList = getChild(UITemplateList.class);
    uiTemplateList.refresh(uiTemplateList.getUIPageIterator().getCurrentPage());
  }  

  public void initPopupPermission(String id, String membership) throws Exception {  	
    String popupId = id.concat(UITemplateContent.TEMPLATE_PERMISSION);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(560, 300);
    UIPermissionSelector uiECMPermission = createUIComponent(UIPermissionSelector.class, null, null);
    uiECMPermission.setSelectedMembership(true);
    if (membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/");
      uiECMPermission.setCurrentPermission("/" + arrMember[1]);
    }
    if (id.equals("AddNew")) {
      UITemplateForm uiForm = findFirstComponentOfType(UITemplateForm.class);
      uiECMPermission.setSourceComponent(uiForm, null);
    } else {
      UITemplateContent uiTemContent = findComponentById(id);
      uiECMPermission.setSourceComponent(uiTemContent, null);
    }
    uiPopup.setUIComponent(uiECMPermission);
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }
  public static class CloseActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UITemplatesManager uiManager = event.getSource().getAncestorOfType(UITemplatesManager.class) ;
      UITemplateContainer uiTemplateContainer = uiManager.getChildById(uiManager.getSelectedTabId());
      UIPopupWindow uiPopupWindow = uiTemplateContainer.getChild(UIPopupWindow.class) ;
      uiPopupWindow.setRendered(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
