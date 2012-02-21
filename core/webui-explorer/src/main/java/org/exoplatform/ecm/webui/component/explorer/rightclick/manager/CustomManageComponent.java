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
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
    events = {
      @EventConfig(listeners = CustomManageComponent.CustomActionListener.class)
    }
)
public class CustomManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                              new IsNotTrashHomeNodeFilter() });
  private final static Log       LOG  = ExoLogger.getLogger(CustomManageComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void customManage(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer,
      UIApplication uiApp) throws Exception {
    UIWorkingArea uicomp = event.getSource().getParent();
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    String actionName = event.getRequestContext().getRequestParameter("actionName");
    String repository = uiExplorer.getRepositoryName();
    String wsName = event.getRequestContext().getRequestParameter(UIWorkingArea.WS_NAME);
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    ActionServiceContainer actionService =
      uicomp.getApplicationComponent(ActionServiceContainer.class);
    try {
      Node node = uiExplorer.getNodeByPath(nodePath, session);
      String userId = event.getRequestContext().getRemoteUser();
      actionService.executeAction(userId, node, actionName, repository);
      Object[] arg = { actionName };
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.execute-successfully", arg));
      
      uiExplorer.updateAjax(event);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs while calling custom action on the node", e);
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static class CustomActionListener extends UIWorkingAreaActionListener<CustomManageComponent> {
    public void processEvent(Event<CustomManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
      customManage(event, uiExplorer, uiApp);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
