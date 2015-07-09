/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.ecm.webui.component.explorer.sidebar.UIClipboard;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.action.ClipboardActionComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

/**
 * Override ECMS's {@link ClipboardActionComponent} to extend {@link UIClipboard} Paste-action to support
 * Cloud Drive file linking instead of copying or moving the file.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ClipboardActionComponent.java 00000 May 12, 2014 pnedonosko $
 * 
 */
@ComponentConfig(
                 events = { @EventConfig(
                                         listeners = CloudDriveClipboardActionComponent.ClipboardActionListener.class) })
public class CloudDriveClipboardActionComponent extends ClipboardActionComponent {

  public static class ClipboardActionListener extends ClipboardActionComponent.ClipboardActionListener {

    @Override
    protected void processEvent(Event<ClipboardActionComponent> event) throws Exception {
      // Replace UIClipboard listener in UISideBar

      UISideBar uiSideBar = event.getSource().getAncestorOfType(UISideBar.class);
      UIClipboard clipboard = uiSideBar.getChild(UIClipboard.class);
      // patch clipboard with Cloud Drive config
      clipboard.setComponentConfig(UICloudDriveClipboard.class, null);

      // let original code to continue
      super.processEvent(event);
    }
  }

  /**
   * 
   */
  public CloudDriveClipboardActionComponent() {
    super();
  }
}
