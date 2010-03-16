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

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009  
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ShowJCRStructureActionComponent.ShowJCRStructureActionListener.class)
     }
 )
public class ShowJCRStructureActionComponent extends UIComponent {
  
  public static class ShowJCRStructureActionListener extends UIActionBarActionListener<ShowJCRStructureActionComponent> {
    public void processEvent(Event<ShowJCRStructureActionComponent> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      Preference pref = uiJCRExplorer.getPreference();
      if(uiJCRExplorer.getPreference().isJcrEnable()) pref.setJcrEnable(false);
      else pref.setJcrEnable(true);
      uiJCRExplorer.refreshExplorer();
    }
  }
}