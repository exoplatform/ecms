/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.HasRemovePermissionFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UISelectRestorePath;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 14, 2009
 * 5:24:01 PM
 */

@ComponentConfig(
      events = {
        @EventConfig(listeners = RestoreFromTrashManageComponent.RestoreFromTrashActionListener.class)
      }
)
public class RestoreFromTrashManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
        = Arrays.asList(new UIExtensionFilter[] { new IsInTrashFilter(),
                                                   new IsNotLockedFilter(),
                                                   new IsCheckedOutFilter(),
                                                   new HasRemovePermissionFilter(),
                                                   new IsNotTrashHomeNodeFilter() });

  private final static Log                     LOG     = ExoLogger.getLogger(RestoreFromTrashManageComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private static void restoreFromTrash(String srcPath, Event<RestoreFromTrashManageComponent> event) throws Exception {
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);

    UIApplication uiApp = uiWorkingArea.getAncestorOfType(UIApplication.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
    String wsName = null;
    Node node = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      srcPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    try {
      // Use the method getNodeByPath because it is link aware
      node = uiExplorer.getNodeByPath(srcPath, session, false);
      // Reset the path to manage the links that potentially create virtual path
      srcPath = node.getPath();
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    }
    confirmToRestore(node, srcPath, event);
  }

  private static void confirmToRestore(Node node, String srcPath, Event<RestoreFromTrashManageComponent> event) throws Exception {
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);

    String restorePath = node.getProperty(Utils.EXO_RESTOREPATH).getString();
    String restoreWs = node.getProperty(Utils.EXO_RESTORE_WORKSPACE).getString();
    Session session = uiExplorer.getSessionByWorkspace(restoreWs);
    NodeFinder nodeFinder = uiExplorer.getApplicationComponent(NodeFinder.class);
    try {
      nodeFinder.getItem(session, restorePath);
    } catch (PathNotFoundException e) {
      doRestore(srcPath, node, event);
      return;
    }
    doRestore(srcPath, node, event);
  }

  public static void doRestore(String srcPath, Node node, Event<? extends UIComponent> event) throws Exception {
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    TrashService trashService = (TrashService)myContainer.getComponentInstanceOfType(TrashService.class);
    UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);

    String restorePath = node.getProperty(Utils.EXO_RESTOREPATH).getString();
    String restoreWs = node.getProperty(Utils.EXO_RESTORE_WORKSPACE).getString();

    try {
      uiExplorer.addLockToken(node);
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }

    try {
      PortletPreferences portletPrefs = uiExplorer.getPortletPreferences();
      String repository = uiExplorer.getRepositoryName();
      String trashWorkspace = portletPrefs.getValue(Utils.TRASH_WORKSPACE, "");
      String trashHomeNodePath = portletPrefs.getValue(Utils.TRASH_HOME_NODE_PATH, "");
      Session trashSession = uiExplorer.getSessionByWorkspace(trashWorkspace);
      Node trashHomeNode = (Node) trashSession.getItem(trashHomeNodePath);
      SessionProvider sessionProvider = uiExplorer.getSessionProvider();
      try {
        trashService.restoreFromTrash(srcPath, sessionProvider);
        uiExplorer.updateAjax(event);
      } catch(PathNotFoundException e) {
        UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
        UISelectRestorePath uiSelectRestorePath =
          uiWorkingArea.createUIComponent(UISelectRestorePath.class, null, null);

        uiSelectRestorePath.setTrashHomeNode(trashHomeNode);
        uiSelectRestorePath.setSrcPath(srcPath);
        uiSelectRestorePath.setRepository(repository);
        uiPopupContainer.activate(uiSelectRestorePath, 600, 300);

        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      }
      //restore categories for node
      try {
        Session session = uiExplorer.getSessionByWorkspace(restoreWs);
        NodeFinder nodeFinder = uiExplorer.getApplicationComponent(NodeFinder.class);
        Node restoredNode = (Node)nodeFinder.getItem(session, restorePath);
        WCMComposer wcmComposer = WCMCoreUtils.getService(WCMComposer.class);
        List<Node> categories = WCMCoreUtils.getService(TaxonomyService.class).getAllCategories(restoredNode);

        for(Node categoryNode : categories){
          wcmComposer.updateContents(categoryNode.getSession().getWorkspace().getName(),
                                     categoryNode.getPath(),
                                     new HashMap<String, String>());
        }

        String parentPath = restoredNode.getParent().getPath();
        String parentWSpace = restoredNode.getSession().getWorkspace().getName();

        wcmComposer.updateContents(parentWSpace, parentPath, new HashMap<String, String>());

      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    } catch (PathNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Path not found! Maybe, it was removed or path changed, can't restore node :" + node.getPath());
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    } catch (LockException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("node is locked, can't restore node :" + node.getPath());
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    } catch (VersionException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("node is checked in, can't restore node:" + node.getPath());
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    } catch (AccessDeniedException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("access denied, can't restore of node:" + node.getPath());
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    } catch (ConstraintViolationException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("access denied, can't restore of node:" + node.getPath());
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs", e);
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static class RestoreFromTrashActionListener extends UIWorkingAreaActionListener<RestoreFromTrashManageComponent> {
    public void restoreFromTrashManage(Event<RestoreFromTrashManageComponent> event) throws Exception {
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      if (srcPath.indexOf(';') > -1) {
        multiRestoreFromTrash(srcPath.split(";"), event);
      } else {
        restoreFromTrash(srcPath, event);
      }
    }

    private void multiRestoreFromTrash(String[] paths, Event<RestoreFromTrashManageComponent> event) throws Exception {
      for (String path : paths) {
        if (acceptForMultiNode(event, path))
        restoreFromTrash(path, event);
      }
    }

    public void processEvent(Event<RestoreFromTrashManageComponent> event) throws Exception {
      restoreFromTrashManage(event);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
