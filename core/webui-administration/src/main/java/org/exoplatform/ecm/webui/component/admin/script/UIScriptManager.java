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
package org.exoplatform.ecm.webui.component.admin.script;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 27, 2006
 * 09:13:15 AM
 */
@ComponentConfig(template = "app:/groovy/webui/component/admin/script/UIScriptManager.gtmpl",
events = { @EventConfig(listeners = UIScriptManager.SelectTabActionListener.class) })

public class UIScriptManager extends UIAbstractManager {

  private String selectedTabId = "UIScriptContainer";

  final static public String POPUP_TEMPLATE_ID = "ScriptContainerPopup";
  final static public String ACTION_TEMPLATE_ID = "UIActionScriptContainer";
  final static public String INTERCEPTOR_TEMPLATE_ID = "UIInterceptorScriptContainer";
  final static public String WIDGET_TEMPLATE_ID = "UIWidgetScriptContainer";

  final static public String ACTION_TEMPLATE_LIST_ID = "UIActionTemplateList";
  final static public String INTERCEPTOR_TEMPLATE_LIST_ID  = "UIInterceptorTemplateList";
  final static public String WIDGET_TEMPLATE_LIST_ID  = "UIWidgetTemplateList";

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
    selectedTabId = getChild(index - 1).getId();
  }

  public UIScriptManager() throws Exception {
    UIScriptContainer uiActionTemp = addChild(UIScriptContainer.class, null, ACTION_TEMPLATE_ID) ;
    uiActionTemp.getChild(UIScriptList.class).setTemplateFilter(UIScriptList.ACTION_SCRIPT_TYPE);

    UIScriptContainer uiInterceptorTemp = addChild(UIScriptContainer.class, null, INTERCEPTOR_TEMPLATE_ID) ;
    uiInterceptorTemp.getChild(UIScriptList.class).setTemplateFilter(UIScriptList.INTERCEPTOR_SCRIPT_TYPE);

    UIScriptContainer uiWidgetTemp = addChild(UIScriptContainer.class, null, WIDGET_TEMPLATE_ID) ;
    uiWidgetTemp.getChild(UIScriptList.class).setTemplateFilter(UIScriptList.WIDGET_SCRIPT_TYPE);

    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, POPUP_TEMPLATE_ID) ;    
    setSelectedTab(ACTION_TEMPLATE_ID);
  }

  public void initPopup(UIComponent uiComponent) throws Exception {
    UIPopupWindow uiPopup = getChildById(POPUP_TEMPLATE_ID);
    uiPopup.setRendered(true);
    uiPopup.setShowMask(true);    
    uiPopup.setWindowSize(600,260) ;
    uiPopup.setUIComponent(uiComponent) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void refresh()throws Exception {
    UIScriptContainer actionContainer = ((UIScriptContainer)getChildById(ACTION_TEMPLATE_ID));
    actionContainer.update();
    actionContainer.getChild(UIScriptList.class).refresh(UIScriptList.ACTION_SCRIPT_TYPE, 1);

    UIScriptContainer interceptorContainer = ((UIScriptContainer)getChildById(INTERCEPTOR_TEMPLATE_ID));
    interceptorContainer.update();
    interceptorContainer.getChild(UIScriptList.class).refresh(UIScriptList.INTERCEPTOR_SCRIPT_TYPE, 1);

    UIScriptContainer widgetContainer = ((UIScriptContainer)getChildById(WIDGET_TEMPLATE_ID));
    widgetContainer.update();
    widgetContainer.getChild(UIScriptList.class).refresh(UIScriptList.WIDGET_SCRIPT_TYPE, 1);  	
  }  

  static public class SelectTabActionListener extends EventListener<UIScriptManager>
  {
    public void execute(Event<UIScriptManager> event) throws Exception
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
