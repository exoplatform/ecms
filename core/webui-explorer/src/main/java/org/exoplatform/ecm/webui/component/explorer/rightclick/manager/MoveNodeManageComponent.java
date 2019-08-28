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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
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
      @EventConfig(listeners = MoveNodeManageComponent.MoveNodeActionListener.class, confirm="UIWorkingArea.msg.confirm-move")
    }
)

public class MoveNodeManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{ new IsNotTrashHomeNodeFilter()} );

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private static final Log LOG = ExoLogger.getLogger(MoveNodeManageComponent.class.getName());

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
      // Reset the session to manage the links that potentially change of
      // workspace
      session = destNode.getSession();
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
          moveMultiNode(nodePath.split(";"), destNode, event);
      } else {
          moveNode(nodePath, null, destNode, event);
      }
      session.save();
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

  private static Node getNodeByPath(String srcPath, UIJCRExplorer uiExplorer) throws Exception {
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      srcPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + srcPath + "'");
    }
    Session srcSession = uiExplorer.getSessionByWorkspace(wsName);
    return uiExplorer.getNodeByPath(srcPath, srcSession, false);
  }

  private static void moveNode(String srcPath, Node selectedNode, Node destNode, Event<?> event) throws Exception {
    UIComponent uiComponent = (UIComponent)event.getSource();
    UIJCRExplorer uiExplorer = uiComponent.getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class);
    if (srcPath.indexOf(":/") > -1 || (selectedNode != null)) {
      String[] arrSrcPath = srcPath.split(":/");
      if ((srcPath.contains(":/") && ("/" + arrSrcPath[1]).equals(destNode.getPath()))
          || (selectedNode != null && selectedNode.equals(destNode))) {
        uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.can-not-move-to-itself",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
    }
    if(uiExplorer.isEditingDocument()) {
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.is-editing", null,
          ApplicationMessage.WARNING));
      return;
    }
    String messagePath = "";
    try {
      if (selectedNode == null) {
        selectedNode = getNodeByPath(srcPath, uiExplorer);
      }
      Session srcSession = selectedNode.getSession();
      if (!selectedNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null,
            ApplicationMessage.WARNING));

        return;
      }
      uiExplorer.addLockToken(selectedNode);
      String destPath = destNode.getPath();
      messagePath = destPath;
      // Make destination path without index on final name
      destPath = destPath.concat("/").concat(selectedNode.getName());
      Workspace srcWorkspace = srcSession.getWorkspace();
      Workspace destWorkspace = destNode.getSession().getWorkspace();
      if (srcPath.indexOf(":/") > -1)
        srcPath = srcPath.substring(srcPath.indexOf(":/") + 1);

      // Get thumbnail node if source node
      ThumbnailService thumbnailService = WCMCoreUtils.getService(ThumbnailService.class);
      Node srcThumbnailNode = thumbnailService.getThumbnailNode(selectedNode);

      ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
      ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
      Node desNode = null;
      if (srcWorkspace.equals(destWorkspace)) {
        srcWorkspace.move(srcPath, destPath);
        //delete EXO_RESTORE_LOCATION if source is in trash
        removeMixinEXO_RESTORE_LOCATION(srcSession, destPath);
        desNode = (Node)srcSession.getItem(destPath);
        LockUtil.changeLockToken(srcPath, desNode);
        if (activityService.isAcceptedNode(desNode) || desNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
          listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode, desNode.getPath());
        }
        srcSession.save();
      } else {
        destWorkspace.clone(srcWorkspace.getName(), srcPath, destPath, false);
        desNode =(Node) destWorkspace.getSession().getItem(destPath);
        if (activityService.isAcceptedNode(desNode) || desNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
          listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode, destPath);
        }
        //delete EXO_RESTORE_LOCATION if source is in trash
        removeMixinEXO_RESTORE_LOCATION(destWorkspace.getSession(), destPath);
        destWorkspace.getSession().save();
      }

      // Update thumbnail node for destination node
      thumbnailService.copyThumbnailNode(srcThumbnailNode, desNode);
    } catch (ItemExistsException iee) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.paste-node-same-name", null,
              ApplicationMessage.WARNING));
      return;
    } catch (Exception e) {
      Object[] args = { srcPath, messagePath };
      uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.move-problem", args,
          ApplicationMessage.WARNING));

      return;
    }
  }

  private static void removeMixinEXO_RESTORE_LOCATION(Session session, String restorePath) throws Exception {
    Node sameNameNode = ((Node) session.getItem(restorePath));
    Node parent = sameNameNode.getParent();
    String name = sameNameNode.getName();
    NodeIterator nodeIter = parent.getNodes(name);
    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (node.isNodeType(Utils.EXO_RESTORELOCATION))
        node.removeMixin(Utils.EXO_RESTORELOCATION);
    }
  }

  private static void moveMultiNode(String[] srcPaths, Node destNode, Event<? extends UIComponent> event) throws Exception {
    Map<String, Node> mapNode = new HashMap <String, Node>();
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    Node node;
    for (int i = 0; i < srcPaths.length; i++) {
      node = getNodeByPath(srcPaths[i], uiExplorer);
      mapNode.put(node.getPath(), node);
    }
    String path = null;
    Iterator<String> iterator = mapNode.keySet().iterator();
    while (iterator.hasNext()) {
      path = iterator.next();
      node = mapNode.get(path);
      node.refresh(true);
      moveNode(node.getPath(), node, destNode, event);
    }
  }

  public static class MoveNodeActionListener extends UIWorkingAreaActionListener<MoveNodeManageComponent> {
    public void processEvent(Event<MoveNodeManageComponent> event) throws Exception {
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      String destPath = event.getRequestContext().getRequestParameter("destInfo");

      if (isInTrash(nodePath) && isInTrash(destPath))
        return;
      else if (isInTrash(destPath))
        ((UIWorkingArea)event.getSource().getParent()).getChild(DeleteManageComponent.class).doDelete(nodePath, event);
      else
        MoveNodeManageComponent.processMultipleSelection(nodePath.trim(), destPath.trim(), event);
    }

    private static boolean isInTrash(String path) {
      PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
      PortletPreferences portletPref = pcontext.getRequest().getPreferences();
      String trashHomeNodePath = portletPref.getValue(Utils.TRASH_HOME_NODE_PATH, "");
      String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");

      return (path.startsWith(trashHomeNodePath) ||
          path.startsWith(trashWorkspace + ":" + trashHomeNodePath));
    }
  }
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
