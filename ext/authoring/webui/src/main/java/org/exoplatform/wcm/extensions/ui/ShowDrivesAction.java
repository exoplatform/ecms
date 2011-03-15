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
package org.exoplatform.wcm.extensions.ui;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          benjamin.paillereau@exoplatform.com
 * 27 june 2010
 */
@ComponentConfig(
    events = {
        @EventConfig(listeners = ShowDrivesAction.ShowDrivesActionListener.class)
    }
)
public class ShowDrivesAction extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(
      new UIExtensionFilter[] {});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  public static class ShowDrivesActionListener extends UIActionBarActionListener<ShowDrivesAction> {

    @Override
    protected void processEvent(Event<ShowDrivesAction> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
        UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
        UIDrivesArea uiDriveArea = uiWorkingArea.getChild(UIDrivesArea.class);
        if (uiDriveArea.isRendered()) {
        uiDriveArea.setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
        } else {
        uiDriveArea.setRendered(true);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(false);
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea) ;
    }
  }
}
