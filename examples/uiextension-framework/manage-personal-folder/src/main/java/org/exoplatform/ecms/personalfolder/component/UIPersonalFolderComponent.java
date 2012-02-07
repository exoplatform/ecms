/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecms.personalfolder.component;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminWorkingArea;
import org.exoplatform.ecm.webui.component.admin.listener.UIECMAdminControlPanelActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 9, 2012
 * 3:44:19 PM  
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = UIPersonalFolderComponent.ManagePersonalFolderActionListener.class)
    }
)
public class UIPersonalFolderComponent extends UIAbstractManagerComponent{

  public static class ManagePersonalFolderActionListener extends UIECMAdminControlPanelActionListener<UIPersonalFolderComponent> {
    public void processEvent(Event<UIPersonalFolderComponent> event) throws Exception {
      UIECMAdminPortlet portlet = event.getSource().getAncestorOfType(UIECMAdminPortlet.class);
      UIECMAdminWorkingArea uiWorkingArea = portlet.getChild(UIECMAdminWorkingArea.class);
      uiWorkingArea.setChild(UIPersonalFolderManager.class) ;
      UIPersonalFolderManager personalFolder = uiWorkingArea.getChild(UIPersonalFolderManager.class);
      personalFolder.refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    }
  }

  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIPersonalFolderManager.class;
  }

}
