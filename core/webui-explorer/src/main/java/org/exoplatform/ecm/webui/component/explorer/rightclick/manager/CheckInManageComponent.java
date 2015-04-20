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
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableOrAncestorFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
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
      @EventConfig(listeners = CheckInManageComponent.CheckInActionListener.class)
    }
)

public class CheckInManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
        = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                                new CanSetPropertyFilter(),
                                                new IsNotLockedFilter(),
                                                new IsVersionableOrAncestorFilter(),
                                                new IsCheckedOutFilter(),
                                                new IsVersionableFilter(),
                                                new IsNotTrashHomeNodeFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void checkInManage(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer,
      UIApplication uiApp) throws Exception {
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    if (nodePath == null) {
      nodePath = uiExplorer.getCurrentWorkspace() + ':' + uiExplorer.getCurrentPath();
    }
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      nodePath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ nodePath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    // Use the method getNodeByPath because it is link aware
    Node node = uiExplorer.getNodeByPath(nodePath, session);
    // Reset the path to manage the links that potentially create virtual path
    nodePath = node.getPath();
    // Reset the session to manage the links that potentially change of workspace
    session = node.getSession();
    // Reset the workspace name to manage the links that potentially change of workspace
    wsName = session.getWorkspace().getName();

    try {
      Node parentNode = node.getParent();
      uiExplorer.addLockToken(parentNode);
      node.checkin();
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    } catch (AccessDeniedException adEx) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-checkin-node", null, ApplicationMessage.WARNING));
      uiExplorer.updateAjax(event);
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }

  }

  public static class CheckInActionListener extends UIWorkingAreaActionListener<CheckInManageComponent> {
    public void processEvent(Event<CheckInManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
      checkInManage(event, uiExplorer, uiApp);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
