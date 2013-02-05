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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(template = "classpath:groovy/wcm/webui/core/UIPermissionManager.gtmpl", 
events = { @EventConfig(listeners = UIPermissionManager.CloseActionListener.class) })
public class UIPermissionManager extends UIPermissionManagerBase {
  public UIPermissionManager() throws Exception {
    this.addChild(UIPermissionInfo.class, null, null);
    this.addChild(UIPermissionForm.class, null, null);
  }
  
  static public class CloseActionListener extends EventListener<UIPermissionManager> {
    public void execute(Event<UIPermissionManager> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }
}
