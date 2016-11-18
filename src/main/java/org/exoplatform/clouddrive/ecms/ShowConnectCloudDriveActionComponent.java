/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

@ComponentConfig(
                 events = { @EventConfig(
                                         listeners = ShowConnectCloudDriveActionComponent.ShowConnectCloudDriveActionListener.class) })
public class ShowConnectCloudDriveActionComponent extends BaseCloudDriveManagerComponent implements
    CloudDriveUIMenuAction {

  public static class ShowConnectCloudDriveActionListener
                                                         extends
                                                         UIActionBarActionListener<ShowConnectCloudDriveActionComponent> {

    public void processEvent(Event<ShowConnectCloudDriveActionComponent> event) throws Exception {

      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      connectToDrive(event, uiExplorer);
    }

    private void connectToDrive(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer) throws Exception {
      UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      // this form will initialize request context from its template
      uiPopupContainer.activate(ConnectCloudDriveForm.class, 400);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
