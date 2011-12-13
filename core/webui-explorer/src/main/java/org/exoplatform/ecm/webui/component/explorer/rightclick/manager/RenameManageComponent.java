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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIRenameForm;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
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
      @EventConfig(listeners = RenameManageComponent.RenameActionListener.class)
    }
)

public class RenameManageComponent extends UIAbstractManagerComponent {

 private static final List<UIExtensionFilter> FILTERS
         = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                                 new IsNotInTrashFilter(),
                                                 new CanSetPropertyFilter(),
                                                 new IsNotLockedFilter(),
                                                 new IsCheckedOutFilter(),
                                                 new IsNotTrashHomeNodeFilter(),
                                                 new IsNotEditingDocumentFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void renameManage(Event<RenameManageComponent> event, UIJCRExplorer uiExplorer)
      throws Exception {
    UIWorkingArea uicomp = event.getSource().getParent();
    String renameNodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(renameNodePath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      renameNodePath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ renameNodePath + "'");
    }
    uiExplorer.setIsHidePopup(false);
    UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    Node renameNode = null;
    try {
      // Use the method getNodeByPath because it is link aware
      renameNode = uiExplorer.getNodeByPath(renameNodePath, session, false);
      // Reset the session to manage the links that potentially change of workspace
      session = renameNode.getSession();
      // Reset the workspace name to manage the links that potentially change of workspace
      wsName = renameNode.getSession().getWorkspace().getName();
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    try {
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      UIRenameForm uiRenameForm = uiActionBar.createUIComponent(UIRenameForm.class, null, null);
      uiRenameForm.update(renameNode);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(uiRenameForm, 700, 0);
      UIPopupContainer.setRendered(true);
    } catch(Exception e) {
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static class RenameActionListener extends UIWorkingAreaActionListener<RenameManageComponent> {
    public void processEvent(Event<RenameManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      renameManage(event, uiExplorer);
    }
  }


  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
