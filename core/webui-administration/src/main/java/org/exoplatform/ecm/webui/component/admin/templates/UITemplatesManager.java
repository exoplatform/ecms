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
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;


/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UITemplatesManager extends UIAbstractManager {
  final static public String EDIT_TEMPLATE = "EditTemplatePopup" ;
  final static public String NEW_TEMPLATE = "TemplatePopup" ;

  public UITemplatesManager() throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();  	
    addChild(UITemplateList.class, null, "ugb_" + UITemplateList.class.getSimpleName() + pContext.getWindowId()) ;
  }

  public boolean isEditingTemplate() {
    UIPopupWindow uiPopup = getChildById(EDIT_TEMPLATE);
    return (uiPopup != null && uiPopup.isShow() && uiPopup.isRendered());
  }

  public void initPopup(UIComponent uiComponent, String title) throws Exception {
    String popuId = title ;
    if (title == null ) popuId = uiComponent.getId() ;
    UIPopupWindow uiPopup = getChildById(popuId) ;
    if(uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, popuId) ;
      uiPopup.setWindowSize(700, 500) ;
      uiPopup.setShowMask(true);
    } else {
      uiPopup.setRendered(true) ;
    }
    uiPopup.setUIComponent(uiComponent) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
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
    return;
  }

  public void refresh() throws Exception {
    getChild(UITemplateList.class).refresh(1);
  }
}
