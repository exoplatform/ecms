/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.control.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UIViewRelationList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ViewRelationsActionComponent.ViewRelationsActionListener.class)
     }
 )
public class ViewRelationsActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{new IsNotRootNodeFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ViewRelationsActionListener extends UIActionBarActionListener<ViewRelationsActionComponent> {
    public void processEvent(Event<ViewRelationsActionComponent> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
      UISideBar uiSideBar = uiWorkingArea.getChild(UISideBar.class);
      if(uiJCRExplorer.getPreference().isShowSideBar()) {
        if(uiSideBar.getCurrentComp().equals(uiSideBar.getChild(UIViewRelationList.class).getId())) {
          UITreeExplorer treeExplorer = uiSideBar.getChild(UITreeExplorer.class);
          treeExplorer.buildTree();
          uiSideBar.setRenderedChild(UITreeExplorer.class);
          uiSideBar.setCurrentComp(uiSideBar.getChild(UITreeExplorer.class).getId());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
        } else {
          uiSideBar.setCurrentComp(uiSideBar.getChild(UIViewRelationList.class).getId());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
        }
      } else {
        uiJCRExplorer.getPreference().setShowSideBar(true);
        uiSideBar.setRenderedChild(UIViewRelationList.class);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    }
  }
}
