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

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanEnableVersionFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotFolderFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotIgnoreVersionNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.ecm.webui.component.explorer.versions.UIActivateVersion;
import org.exoplatform.ecm.webui.component.explorer.versions.UIVersionInfo;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
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
       @EventConfig(listeners = ManageVersionsActionComponent.ManageVersionsActionListener.class)
     }
 )
public class ManageVersionsActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsNotRootNodeFilter("UIActionBar.msg.cannot-enable-version-rootnode"),
      new CanSetPropertyFilter(), new IsNotLockedFilter(), new CanEnableVersionFilter(),
      new IsNotEditingDocumentFilter(), new IsNotFolderFilter(), new IsNotIgnoreVersionNodeFilter()
      }
  );

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ManageVersionsActionListener extends UIActionBarActionListener<ManageVersionsActionComponent> {
    public void processEvent(Event<ManageVersionsActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.setIsHidePopup(false);
      if (currentNode.canAddMixin(Utils.MIX_VERSIONABLE)) {
        UIPopupContainer.activate(UIActivateVersion.class, 400);
        event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
      } else if (currentNode.isNodeType(Utils.MIX_VERSIONABLE)) {
        UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
        UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
        UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
        uiVersionInfo.setCurrentNode(currentNode);
        uiVersionInfo.activate();
        uiDocumentWorkspace.setRenderedChild(UIVersionInfo.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
      }
    }
  }
}
