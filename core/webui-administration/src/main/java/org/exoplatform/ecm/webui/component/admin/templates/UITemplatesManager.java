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

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.manager.UIAbstractManager;


/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane_New.gtmpl", 
events = { @EventConfig(listeners = UITemplatesManager.SelectTabActionListener.class) })

public class UITemplatesManager extends UIAbstractManager {
  final static public String EDIT_TEMPLATE = "EditTemplatePopup" ;
  final static public String NEW_TEMPLATE = "TemplatePopup" ;
  final static public String ACTIONS_TEMPLATE_ID = "UIActionsTemplateList";
  final static public String OTHERS_TEMPLATE_ID = "UIOthersTemplateList";
  
  
  private String selectedTabId = "";

  public String getSelectedTabId()
  {
     return selectedTabId;
  }

  public void setSelectedTab(String renderTabId)
  {
     selectedTabId = renderTabId;
  }

  public void setSelectedTab(int index)
  {
     selectedTabId = ((UIComponent)getChild(index - 1)).getId();
  }

  public UITemplatesManager() throws Exception {
    addChild(UITemplateList.class, null, null).setTemplateFilter(UITemplateList.DOCUMENTS_TEMPLATE_TYPE) ;
    addChild(UITemplateList.class, null, ACTIONS_TEMPLATE_ID);
    addChild(UITemplateList.class, null, OTHERS_TEMPLATE_ID);
    this.setSelectedTab(UITemplateList.DOCUMENTS_TEMPLATE_TYPE);
  }

  public boolean isEditingTemplate() {
  	UIECMAdminPortlet adminPortlet = this.getAncestorOfType(UIECMAdminPortlet.class);
  	UIPopupContainer popupContainer = adminPortlet.getChild(UIPopupContainer.class);
  	UIPopupWindow uiPopup = popupContainer.getChild(UIPopupWindow.class);
  	uiPopup.setId(EDIT_TEMPLATE);
    return (uiPopup != null && uiPopup.isShow() && uiPopup.isRendered());
  }

  public void initPopup(UIComponent uiComponent, String title) throws Exception {
    String popuId = title ;
    if (title == null ) popuId = uiComponent.getId() ;
    UIECMAdminPortlet adminPortlet = this.getAncestorOfType(UIECMAdminPortlet.class);
    UIPopupContainer popupContainer = adminPortlet.getChild(UIPopupContainer.class);
    UIPopupWindow uiPopup = popupContainer.getChild(UIPopupWindow.class);
    if(uiPopup == null) {
      uiPopup = popupContainer.addChild(UIPopupWindow.class, null, popuId) ;
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
    
    UITemplateList uiActionTemplate = ((UITemplateList)getChildById(ACTIONS_TEMPLATE_ID));
    uiActionTemplate.setTemplateFilter(UITemplateList.ACTIONS_TEMPLATE_TYPE);
    uiActionTemplate.refresh(1);
    
    UITemplateList uiOtherTemplate = ((UITemplateList)getChildById(OTHERS_TEMPLATE_ID));
    uiOtherTemplate.setTemplateFilter(UITemplateList.OTHERS_TEMPLATE_TYPE);
    uiOtherTemplate.refresh(1);
  }
  
  static public class SelectTabActionListener extends EventListener<UITemplatesManager>
  {
     public void execute(Event<UITemplatesManager> event) throws Exception
     {
        WebuiRequestContext context = event.getRequestContext();
        String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
        if (renderTab == null)
           return;
        event.getSource().setSelectedTab(renderTab);
        context.setResponseComplete(true);
        context.addUIComponentToUpdateByAjax(event.getSource().getParent());
     }
  }  
  
}
