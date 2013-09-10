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

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 25, 2006
 * 11:45:11 AM
 */

@ComponentConfig(template = "system:/groovy/webui/core/UITabPane_New.gtmpl", 
events = { @EventConfig(listeners = UIViewManager.SelectTabActionListener.class) })

public class UIViewManager extends UIAbstractManager {

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
    selectedTabId = getChild(index - 1).getId();
  }

  public UIViewManager() throws Exception{	
    addChild(UIViewContainer.class, null, null);
    UITemplateContainer uiECMTemp = addChild(UITemplateContainer.class, null, "ECMTemplate") ;
    uiECMTemp.addChild(UIECMTemplateList.class, null, null) ;
    setSelectedTab("UIViewContainer");
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    getChild(UIViewContainer.class).update() ;
    UIECMTemplateList uiECMTemplateList = ((UITemplateContainer)getChildById("ECMTemplate")).getChild(UIECMTemplateList.class);
    uiECMTemplateList.refresh(uiECMTemplateList.getUIPageIterator().getCurrentPage());
  }

  static public class SelectTabActionListener extends EventListener<UIViewManager>
  {
    public void execute(Event<UIViewManager> event) throws Exception
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

