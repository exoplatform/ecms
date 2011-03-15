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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
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
@ComponentConfig(events = {
    @EventConfig(
                 listeners = ExplorerActionComponent.ExplorerActionListener.class) })
public class ExplorerActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {});

  @UIExtensionFilters
  public static List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ExplorerActionListener extends UISideBarActionListener<ExplorerActionComponent> {

    @Override
    protected void processEvent(Event<ExplorerActionComponent> event) throws Exception {
      UISideBar uiSideBar = event.getSource().getAncestorOfType(UISideBar.class);
      UIJCRExplorer uiExplorer = uiSideBar.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.setSelectNode(uiExplorer.getCurrentPath());
      uiExplorer.setIsViewTag(false);
      uiSideBar.setCurrentComp(uiSideBar.getChild(UITreeExplorer.class).getId());
      uiSideBar.setSelectedComp(event.getSource().getUIExtensionName());
      uiExplorer.updateAjax(event);
    }

  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
