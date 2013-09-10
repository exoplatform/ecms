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

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane_New.gtmpl", 
events = { @EventConfig(listeners = UIViewTemplate.SelectTabActionListener.class) })

public class UIViewTemplate extends UIContainer {
  private String nodeTypeName_ ;

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

  public UIViewTemplate() throws Exception {
    addChild(UITemplateEditForm.class, null, null) ;
    setSelectedTab("UITemplateEditForm");
    addChild(UIDialogTab.class, null, null);
    addChild(UIViewTab.class, null, null);
    addChild(UISkinTab.class, null, null);
  }

  public void refresh() throws Exception {
    getChild(UIDialogTab.class).updateGrid(nodeTypeName_);
    getChild(UIViewTab.class).updateGrid(nodeTypeName_);
    getChild(UISkinTab.class).updateGrid(nodeTypeName_);
  }
  public void setNodeTypeName(String nodeType) { nodeTypeName_ = nodeType ; }

  public String getNodeTypeName() { return nodeTypeName_ ; }

  static public class SelectTabActionListener extends EventListener<UIViewTemplate>
  {
    public void execute(Event<UIViewTemplate> event) throws Exception
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
