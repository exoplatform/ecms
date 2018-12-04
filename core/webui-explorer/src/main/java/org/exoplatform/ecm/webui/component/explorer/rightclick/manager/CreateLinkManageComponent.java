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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
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
 * Aug 16, 2009
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = CreateLinkManageComponent.CreateLinkActionListener.class)
    }
)
public class CreateLinkManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
          = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                                  new IsNotTrashHomeNodeFilter()});

  private static final Log LOG = ExoLogger.getLogger(CreateLinkManageComponent.class.getName());
  private static final String EXO_TRASH_FOLDER = "exo:trashFolder";

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private static void createMultiLink(String[] srcPaths, Node destNode,
      Event<? extends UIComponent> event) throws Exception {
    for (int i = 0; i < srcPaths.length; i++) {
      createLink(srcPaths[i], destNode, event);
    }
  }

  private static void createLink(String srcPath, Node destNode, Event<? extends UIComponent> event)
      throws Exception {

    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      srcPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + srcPath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    Node selectedNode = uiExplorer.getNodeByPath(srcPath, session, false);
    if (selectedNode.isNodeType(EXO_TRASH_FOLDER)) return;
    UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
    LinkManager linkManager = event.getSource().getApplicationComponent(LinkManager.class);
    if (linkManager.isLink(destNode)) {
      Object[] args = { destNode.getPath() };
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.dest-node-is-link", args,
          ApplicationMessage.WARNING));
      
      return;
    }
    if (linkManager.isLink(selectedNode)) {
      Object[] args = { srcPath };
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.selected-is-link", args,
          ApplicationMessage.WARNING));
      
      return;
    }
    try {
      linkManager.createLink(destNode, Utils.EXO_SYMLINK, selectedNode, selectedNode.getName()
          + ".lnk");
    } catch (Exception e) {
      Object[] args = { Text.unescapeIllegalJcrChars(srcPath), Text.unescapeIllegalJcrChars(destNode.getPath()) };
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.create-link-problem", args,
          ApplicationMessage.WARNING));
      
      return;
    }
  }

  public static class CreateLinkActionListener extends UIWorkingAreaActionListener<CreateLinkManageComponent> {
    public void processEvent(Event<CreateLinkManageComponent> event) throws Exception {
      CreateLinkManageComponent.createLinkManager(event);
    }
  }

  private static void createLinkManager(Event<? extends UIComponent> event) throws Exception {
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    String destPath = event.getRequestContext().getRequestParameter("destInfo");
    processMultipleSelection(nodePath.trim(), destPath.trim(), event);
  }

  private static void processMultipleSelection(String nodePath, String destPath,
      Event<? extends UIComponent> event) throws Exception {
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      destPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
    if (destPath.startsWith(nodePath)) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.bound-move-exception", null,
          ApplicationMessage.WARNING));
      
      return;
    }
    Node destNode;
    try {
      // Use the method getNodeByPath because it is link aware
      destNode = uiExplorer.getNodeByPath(destPath, session);
      // Reset the path to manage the links that potentially create virtual path
      destPath = destNode.getPath();
    } catch (PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
          ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    if (!PermissionUtil.canAddNode(destNode)) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-move-node", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    }
    if (uiExplorer.nodeIsLocked(destNode)) {
      Object[] arg = { destPath };
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg,
          ApplicationMessage.WARNING));
      
      return;
    }
    if (!destNode.isCheckedOut()) {
      uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null));
      
      return;
    }
    try {
      if (nodePath.indexOf(";") > -1) {
        createMultiLink(nodePath.split(";"), destNode, event);
      } else {
        createLink(nodePath, destNode, event);
      }
      destNode.save();
      uiExplorer.updateAjax(event);
    } catch (AccessDeniedException ace) {
      Object[] arg = { destPath };
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.has-not-add-permission", arg,
          ApplicationMessage.WARNING));
      
      return;
    } catch (LockException lock) {
      Object[] arg = { nodePath };
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg,
          ApplicationMessage.WARNING));
      
      return;
    } catch (ConstraintViolationException constraint) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.move-constraint-exception", null,
          ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs while selecting the node", e);
      }
      JCRExceptionManager.process(uiApp, e);
      return;
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
