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
import org.exoplatform.ecm.webui.component.admin.views.UIECMTemplateList;
import org.exoplatform.ecm.webui.component.admin.templates.UITemplateContainer;
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
  final static public String ACTIONS_TEMPLATE_ID = "UIActionsTemplateContainer";
  final static public String OTHERS_TEMPLATE_ID = "UIOthersTemplateContainer";
  
  final static public String ACTIONS_TEMPLATE_LIST_ID = "UIActionsTemplateList";
  final static public String OTHERS_TEMPLATE_LIST_ID  = "UIOthersTemplateList";
  
  
  private String selectedTabId = "UITemplateContainer";

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
    UITemplateContainer uiTemp = addChild(UITemplateContainer.class, null, null) ;
    uiTemp.getChild(UITemplateList.class).setTemplateFilter(UITemplateList.DOCUMENTS_TEMPLATE_TYPE);
    
    UITemplateContainer uiActionsTemp = addChild(UITemplateContainer.class, null, ACTIONS_TEMPLATE_ID) ;
    uiActionsTemp.getChild(UITemplateList.class).setTemplateFilter(UITemplateList.ACTIONS_TEMPLATE_TYPE);
    uiActionsTemp.getChild(UITemplateList.class).setId(ACTIONS_TEMPLATE_LIST_ID);
    
    UITemplateContainer uiOthersTemp = addChild(UITemplateContainer.class, null, OTHERS_TEMPLATE_ID) ;
    uiOthersTemp.getChild(UITemplateList.class).setTemplateFilter(UITemplateList.OTHERS_TEMPLATE_TYPE);
    uiOthersTemp.getChild(UITemplateList.class).setId(OTHERS_TEMPLATE_LIST_ID);
    
    setSelectedTab("UITemplateContainer");
  }

  public boolean isEditingTemplate() {
  	UIECMAdminPortlet adminPortlet = this.getAncestorOfType(UIECMAdminPortlet.class);
  	UIPopupContainer popupContainer = adminPortlet.getChild(UIPopupContainer.class);
  	UIPopupWindow uiPopup = popupContainer.getChild(UIPopupWindow.class);
  	uiPopup.setId(EDIT_TEMPLATE);
    return (uiPopup != null && uiPopup.isShow() && uiPopup.isRendered());
  }  

  public void refresh() throws Exception {	
    UITemplateContainer templateContainer = ((UITemplateContainer)getChildById("UITemplateContainer"));
    templateContainer.update();
    templateContainer.getChild(UITemplateList.class).refresh(1);
    
    UITemplateContainer templateActionsContainer = ((UITemplateContainer)getChildById(ACTIONS_TEMPLATE_ID));
    templateActionsContainer.update();
    templateActionsContainer.getChild(UITemplateList.class).refresh(1);
    
    UITemplateContainer templateOthersContainer = ((UITemplateContainer)getChildById(OTHERS_TEMPLATE_ID));
    templateOthersContainer.update();
    templateOthersContainer.getChild(UITemplateList.class).refresh(1);   
    
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
       WebuiRequestContext parentContext = (WebuiRequestContext)context.getParentAppRequestContext();
       if (parentContext != null)
       {
          parentContext.setResponseComplete(true);
       }
       else
       {
          context.setResponseComplete(true);
       }
    }
  } 
  
}
