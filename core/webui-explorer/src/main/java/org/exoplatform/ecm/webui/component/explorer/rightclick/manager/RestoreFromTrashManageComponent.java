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

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.*;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UISelectRestorePath;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

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

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new IsInTrashFilter(),
      new IsNotLockedFilter(),
      new IsCheckedOutFilter(),
      new HasRemovePermissionFilter(),
      new IsAbleToRestoreFilter(),
      new IsNotTrashHomeNodeFilter());

  private final static Log                     LOG     = ExoLogger.getLogger(RestoreFromTrashManageComponent.class.getName());


  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class RestoreFromTrashActionListener extends UIWorkingAreaActionListener<RestoreFromTrashManageComponent> {
    private String itemName = "";

    private int numberItemsRestored = 0;

    public void restoreFromTrashManage(Event<RestoreFromTrashManageComponent> event) throws Exception {   
      numberItemsRestored = 0;
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      if (srcPath.indexOf(';') > -1) {
        multiRestoreFromTrash(srcPath.split(";"), event);
      } else {
        restoreFromTrash(srcPath, event);
      }
      RequestContext context = RequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String restoreNotice = "";
      if(!srcPath.contains(";") && numberItemsRestored == 1) {
        restoreNotice = "UIWorkingArea.msg.feedback-restore";
        restoreNotice = res.getString(restoreNotice);
        restoreNotice = restoreNotice.replace("{" + 0 + "}", itemName);
      } else if(srcPath.indexOf(';') > -1 && numberItemsRestored >= 1) {
        restoreNotice = "UIWorkingArea.msg.feedback-restore-multi";
        restoreNotice = res.getString(restoreNotice);
        restoreNotice = restoreNotice.replace("{" + 0 + "}", String.valueOf(numberItemsRestored));
      }      
      restoreNotice = restoreNotice.replace("\"", "'");
      restoreNotice = StringEscapeUtils.escapeHtml(restoreNotice);
      if(restoreNotice.length() > 0) {
        UIWorkingArea uiWorkingArea = event.getSource().getParent();
        uiWorkingArea.setWCMNotice(restoreNotice);
      }
    }

    private void multiRestoreFromTrash(String[] paths, Event<RestoreFromTrashManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      
      Session session;
      Matcher matcher;
      String  wsName;
      Node    node;
      String  origialPath;
      Arrays.sort(paths,Collections.reverseOrder());
      List<String> newPaths = new ArrayList<>();

      // In case multi checked items, check if a Symlink node is with its Target in Trash or not.
      for (int i =  0; i < paths.length ; i++) {
        String srcPath = paths[i];
        origialPath = srcPath;
        matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
        if (matcher.find()) {
          wsName  = matcher.group(1);
          srcPath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + srcPath + "'");
        }

        try {
          session = uiExplorer.getSessionByWorkspace(wsName);
          // Use the method getNodeByPath because it is link aware
          node = uiExplorer.getNodeByPath(srcPath, session, false);

          // Not allow to restore multi items if there is a symlink node, which have Target-In-Trash, in items list. 
          // program returns at once.
          if (Utils.targetNodeAndLinkInTrash(node)) {
            continue;
          }
          // Reset the path to manage the links that potentially create virtual
          newPaths.add(origialPath);
        } catch (PathNotFoundException path) {
          return;
        }
      }

      for (String path : newPaths) {
        if (acceptForMultiNode(event, path))
          restoreFromTrash(path, event);
      }
    }

    private void restoreFromTrash(String srcPath, Event<RestoreFromTrashManageComponent> event) throws Exception {
      UIWorkingArea uiWorkingArea = event.getSource().getParent();
      UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);

      UIApplication uiApp = uiWorkingArea.getAncestorOfType(UIApplication.class);
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
      String wsName;
      Node node;
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

        //return false if the target is already deleted
        if ( Utils.targetNodeAndLinkInTrash(node) ) {
          return;
        }
        // Reset the path to manage the links that potentially create virtual path
        srcPath = node.getPath();
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
                null,ApplicationMessage.WARNING));

        return;
      }
      confirmToRestore(node, srcPath, event);
    }

    private void confirmToRestore(Node node, String srcPath, Event<RestoreFromTrashManageComponent> event) throws Exception {
      UIWorkingArea uiWorkingArea = event.getSource().getParent();
      UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);
      itemName = Utils.getTitle(node);
      String restorePath = node.getProperty(Utils.EXO_RESTOREPATH).getString();
      String restoreWs = node.getProperty(Utils.EXO_RESTORE_WORKSPACE).getString();
      Session session = uiExplorer.getSessionByWorkspace(restoreWs);
      NodeFinder nodeFinder = uiExplorer.getApplicationComponent(NodeFinder.class);
      try {
        nodeFinder.getItem(session, restorePath);
      } catch (PathNotFoundException e) {
        doRestore(srcPath, node, event);
        numberItemsRestored++;
        return;
      }
      doRestore(srcPath, node, event);

      numberItemsRestored++;
    }

    public void doRestore(String srcPath, Node node, Event<? extends UIComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = event.getSource().getParent();
      TrashService trashService = WCMCoreUtils.getService(TrashService.class);
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);

      try {
        uiExplorer.addLockToken(node);
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      SessionProvider sessionProvider = null;
      try {
        PortletPreferences portletPrefs = uiExplorer.getPortletPreferences();
        String repository = uiExplorer.getRepositoryName();
        String trashWorkspace = portletPrefs.getValue(Utils.TRASH_WORKSPACE, "");
        String trashHomeNodePath = portletPrefs.getValue(Utils.TRASH_HOME_NODE_PATH, "");
        //Have to create session from System Provider to allow normal user to restore the content that deleted before
        sessionProvider = WCMCoreUtils.getSystemSessionProvider();
        RepositoryService repositoryService = uiExplorer.getApplicationComponent(RepositoryService.class);
        ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
        Session trashSession = sessionProvider.getSession(trashWorkspace, manageableRepository);

        Node trashHomeNode = (Node) trashSession.getItem(trashHomeNodePath);
        try {
          trashService.restoreFromTrash(srcPath, sessionProvider);

          // delete symlink after target node
          LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
          List<Node> symlinks = linkManager.getAllLinks(node, org.exoplatform.services.cms.impl.Utils.EXO_SYMLINK);
          for (Node symlink : symlinks) {
            String realPath = symlink.getPath();
            if(!trashHomeNode.getSession().itemExists(realPath)){
              realPath = trashHomeNodePath + "/" + symlink.getName();
            }
            trashService.restoreFromTrash(realPath, sessionProvider);
          }
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
      } catch (AccessDeniedException | ConstraintViolationException e) {
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

    public void processEvent(Event<RestoreFromTrashManageComponent> event) throws Exception {
      restoreFromTrashManage(event);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
