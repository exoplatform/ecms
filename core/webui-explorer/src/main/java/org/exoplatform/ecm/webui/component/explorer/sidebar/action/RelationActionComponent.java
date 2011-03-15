/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.sidebar.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotSystemWorkspaceFilter;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UIViewRelationList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          ha.dangviet@exoplatform.com
 * Nov 22, 2010
 */
@ComponentConfig(events = { @EventConfig(listeners = RelationActionComponent.RelationActionListener.class) })
public class RelationActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS =
    Arrays.asList(new UIExtensionFilter[] {new IsNotSystemWorkspaceFilter()});

  @UIExtensionFilters
  public static List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class RelationActionListener extends UISideBarActionListener<RelationActionComponent> {
    protected void processEvent(Event<RelationActionComponent> event) throws Exception {
      UISideBar uiSideBar = event.getSource().getAncestorOfType(UISideBar.class);
      uiSideBar.setCurrentComp(uiSideBar.getChild(UIViewRelationList.class).getId());
      uiSideBar.setSelectedComp(event.getSource().getUIExtensionName());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());

    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
