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
package org.exoplatform.ecm.webui.component.admin.manager;

import javax.jcr.AccessDeniedException;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminWorkingArea;
import org.exoplatform.ecm.webui.component.admin.listener.UIECMAdminControlPanelActionListener;
import org.exoplatform.ecm.webui.component.admin.templates.clv.UICLVTemplatesManager;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 22, 2013
 * 9:48:15 AM  
 */
@ComponentConfig(
        events = {
          @EventConfig(listeners = UICLVTemplatesManagerComponent.UICLVTemplatesManagerActionListener.class)
        }
    )
public class UICLVTemplatesManagerComponent extends UIAbstractManagerComponent {

  public static class UICLVTemplatesManagerActionListener extends UIECMAdminControlPanelActionListener<UICLVTemplatesManagerComponent> {
    public void processEvent(Event<UICLVTemplatesManagerComponent> event) throws Exception {
      UIECMAdminPortlet portlet = event.getSource().getAncestorOfType(UIECMAdminPortlet.class);
      UIECMAdminWorkingArea uiWorkingArea = portlet.getChild(UIECMAdminWorkingArea.class);
      try {
        uiWorkingArea.getChild(UICLVTemplatesManager.class).refresh() ;
        uiWorkingArea.setChild(UICLVTemplatesManager.class) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea) ;
      } catch(AccessDeniedException ace) {
        throw new MessageException(new ApplicationMessage("UIECMAdminControlPanel.msg.access-denied",
                                                          null, ApplicationMessage.WARNING)) ;
      }
    }
  }
  
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UICLVTemplatesManager.class;
  }

}
