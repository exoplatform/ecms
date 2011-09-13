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
package org.exoplatform.ecm.webui.component.explorer.versions;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : trongtt
 *          trongtt@gmail.com
 * Oct 16, 2006
 * 14:07:15
 */

@ComponentConfig(
    type = UIActivateVersion.class,
    template = "app:/groovy/webui/component/explorer/versions/UIActivateVersion.gtmpl",
    events = {
        @EventConfig(listeners = UIActivateVersion.EnableVersionActionListener.class),
        @EventConfig(listeners = UIActivateVersion.CancelActionListener.class)
    }
)

public class UIActivateVersion extends UIContainer implements UIPopupComponent {

  public UIActivateVersion() throws Exception {}

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  static public class EnableVersionActionListener extends EventListener<UIActivateVersion> {
    public void execute(Event<UIActivateVersion> event) throws Exception {
      UIActivateVersion uiActivateVersion = event.getSource();
      UIJCRExplorer uiExplorer = uiActivateVersion.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      uiExplorer.addLockToken(currentNode);
      if(currentNode.isNodeType("rma:filePlan")){
        WebuiRequestContext contx = event.getRequestContext();
        UIPopupContainer popupAction = uiExplorer.getChild(UIPopupContainer.class) ;
        if(popupAction.isRendered()) {
          popupAction.deActivate() ;
          contx.addUIComponentToUpdateByAjax(popupAction) ;
        }
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.does-not-support-versioning",null,ApplicationMessage.WARNING)) ;
        
        return ;
      }
      try {
        currentNode.addMixin(Utils.MIX_VERSIONABLE);
        currentNode.save() ;
        currentNode.getSession().save();
        currentNode.getSession().refresh(true) ;
        uiExplorer.updateAjax(event) ;
      }
      catch (AccessDeniedException ex) {
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIActivateVersion.msg.access-denied",null,ApplicationMessage.WARNING)) ;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIActivateVersion> {
    public void execute(Event<UIActivateVersion> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }
}
