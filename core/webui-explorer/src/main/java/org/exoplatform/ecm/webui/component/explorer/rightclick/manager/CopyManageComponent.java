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
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
 * Aug 5, 2009
 */

@ComponentConfig(
    events = {
      @EventConfig(listeners = CopyManageComponent.CopyActionListener.class)
    }
)

public class CopyManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                              new IsNotTrashHomeNodeFilter()});

  private final static Log       LOG  = ExoLogger.getLogger(CopyManageComponent.class.getName());

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void multipleCopy(String[] srcPaths, Event<UIComponent> event) throws Exception {
    for(int i=0; i< srcPaths.length; i++) {
      processCopy(srcPaths[i], event, true);
    }
  }

  public static void processCopy(String srcPath, Event<?> event, boolean isMultiSelect) throws Exception {
    UIWorkingArea uiWorkingArea = ((UIComponent)event.getSource()).getParent();
    UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiWorkingArea.getAncestorOfType(UIApplication.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      srcPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    try {
      // Use the method getNodeByPath because it is link aware
      Node node = uiExplorer.getNodeByPath(srcPath, session, false);
      // Reset the path to manage the links that potentially create virtual path
      srcPath = node.getPath();
      // Reset the session to manage the links that potentially change of workspace
      session = node.getSession();
      // Reset the workspace name to manage the links that potentially change of workspace
      wsName = session.getWorkspace().getName();
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    }
    try {
      ClipboardCommand clipboard = new ClipboardCommand(ClipboardCommand.COPY, srcPath, wsName);
      ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      clipboardService.addClipboardCommand(userId, clipboard, false);
      if (isMultiSelect) {
        clipboardService.addClipboardCommand(userId, clipboard, true);
      }
      uiExplorer.getSession().save();
    } catch(ConstraintViolationException cons) {
      uiExplorer.getSession().refresh(false);
      uiExplorer.refreshExplorer();
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.constraintviolation-exception",
          null,ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs", e);
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static void copyManage(Event<UIComponent> event) throws Exception {
    String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
    
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    clipboardService.clearClipboardList(userId, true);
    
    if(srcPath.indexOf(";") > -1) {      
      multipleCopy(Utils.removeChildNodes(srcPath), event);
    } else {
      processCopy(srcPath, event, false);
    }
  }

  public static class CopyActionListener extends UIWorkingAreaActionListener<CopyManageComponent> {
    public void processEvent(Event<CopyManageComponent> event) throws Exception {
      Event<UIComponent> event_ = new Event<UIComponent>( event.getSource(), event.getName(),event.getRequestContext());
      copyManage(event_);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
