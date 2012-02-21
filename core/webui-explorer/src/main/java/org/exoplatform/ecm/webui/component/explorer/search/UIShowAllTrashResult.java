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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 21, 2009
 * 3:30:22 PM
 */

@ComponentConfig(template = "app:/groovy/webui/component/explorer/search/UIShowAllTrashResult.gtmpl",
                 events = {
    @EventConfig(listeners = UIShowAllTrashResult.DeleteActionListener.class),
    @EventConfig(listeners = UIShowAllTrashResult.RestoreActionListener.class),
    @EventConfig(listeners = UIShowAllTrashResult.SortASCActionListener.class),
    @EventConfig(listeners = UIShowAllTrashResult.SortDESCActionListener.class) })
public class UIShowAllTrashResult extends UIComponentDecorator {

  public static final int  SHOW_ALL_FROM_TRASH         = 0;

  public static final int  SHOW_ALL_FROM_TRASH_BY_USER = 1;

  private static final Log LOG                         = ExoLogger.getLogger("explorer.search.UIShowAllTrashResult");

  private static final int FILE_PER_PAGE               = 10;

  private static String    iconType                    = "BlueDownArrow";

  private static String    iconName                    = "";

  private long             searchTime_                 = 0;

  private List<Node>       trashNodes_                 = new ArrayList<Node>();

  private UIPageIterator   uiPageIterator_;

  private int              showNodeCase;

  private boolean          nodeListChange              = false;

  public UIShowAllTrashResult() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "UIShowAllTrashIterator");
    setUIComponent(uiPageIterator_);
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
  }

  public void setShowNodeCase(int value) {
    this.showNodeCase = value;
  }

  public long getSearchTime() { return searchTime_; }
  public void setSearchTime(long time) { this.searchTime_ = time; }
  public UIPageIterator  getUIPageIterator() {  return uiPageIterator_ ; }

  public Date getDateCreated(Node node) throws Exception{
      if (node.hasProperty("exo:dateCreated")) {
        return node.getProperty("exo:dateCreated").getDate().getTime();
      }
      return new GregorianCalendar().getTime();
  }

  @SuppressWarnings("unchecked")
  public List<Node> getAllTrashNodes() throws Exception {
    return (List<Node>)uiPageIterator_.getCurrentPageData();
  }

  protected List<Node> getNewTrashNodeList() throws Exception {
    List<Node> ret = new ArrayList<Node>();

    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    TrashService trashService =
      (TrashService) myContainer
      .getComponentInstanceOfType(TrashService.class);
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
      PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");
    SessionProvider sessionProvider = uiExplorer.getSessionProvider();
    boolean byUser = uiExplorer.getPreference().isShowItemsByUser();
    if (!byUser) {
      ret = trashService.getAllNodeInTrash(
          trashWorkspace,
          sessionProvider);
    } else {
      ret = trashService.getAllNodeInTrashByUser(
          trashWorkspace,
          sessionProvider,
          uiExplorer.getSession().getUserID());
    }
    return ret;
  }

  public void updateList() throws Exception {
    List<Node> nodeList = getNewTrashNodeList();
    ListAccess<Node> newTrashNodeList = new ListAccessImpl<Node>(Node.class, nodeList);
    LazyPageList<Node> pageList = new LazyPageList<Node>(newTrashNodeList, FILE_PER_PAGE);
    uiPageIterator_.setPageList(pageList);
    trashNodes_ = nodeList;
  }

  private void updateTable(Event<UIShowAllTrashResult> event) throws Exception {
    int currentPage = uiPageIterator_.getCurrentPage();
    if (nodeListChange) {
      updateList();
      nodeListChange = false;
    }
    Collections.sort(trashNodes_, new SearchComparator());
    ListAccess<Node> trashNodeList = new ListAccessImpl<Node>(Node.class, trashNodes_);
    LazyPageList<Node> pageList = new LazyPageList<Node>(trashNodeList, FILE_PER_PAGE);

    UIPageIterator uiPageIterator = uiPageIterator_;
    uiPageIterator.setPageList(pageList);
    uiPageIterator.setPageList(pageList);
    if (uiPageIterator.getAvailablePage() >= currentPage)
      uiPageIterator.setCurrentPage(currentPage);
    else {
      uiPageIterator.setCurrentPage(uiPageIterator.getAvailablePage());
    }
    event.getRequestContext().addUIComponentToUpdateByAjax(this.getParent());
  }

  public static class SortASCActionListener extends
      EventListener<UIShowAllTrashResult> {
    @Override
    public void execute(Event<UIShowAllTrashResult> event) throws Exception {
      UIShowAllTrashResult uiShowAllTrashResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueDownArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueDownArrow";
      }
      uiShowAllTrashResult.updateTable(event);
    }
  }

  public static class SortDESCActionListener extends
      EventListener<UIShowAllTrashResult> {
    @Override
    public void execute(Event<UIShowAllTrashResult> event) throws Exception {
      UIShowAllTrashResult uiShowAllTrashResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueUpArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueUpArrow";
      }

      uiShowAllTrashResult.updateTable(event);
    }
  }

  static  public class DeleteActionListener extends EventListener<UIShowAllTrashResult> {
      public void execute(Event<UIShowAllTrashResult> event) throws Exception {
          String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
          UIShowAllTrashResult uiShowAllTrashResult = event.getSource();
          UIJCRExplorer uiExplorer = uiShowAllTrashResult.getAncestorOfType(UIJCRExplorer.class);
        UIApplication uiApp = uiShowAllTrashResult.getAncestorOfType(UIApplication.class);

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
          //srcPath = node.getPath();
          // Reset the session to manage the links that potentially change of workspace
          session = node.getSession();
          // Reset the workspace name to manage the links that potentially change of workspace
          //wsName = session.getWorkspace().getName();
        } catch(PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
              null,ApplicationMessage.WARNING));
          
          return;
        }

        try {
          uiExplorer.addLockToken(node);
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          return;
        }
        Node parentNode = node.getParent();
        uiExplorer.addLockToken(parentNode);
        try {
          if (PermissionUtil.canRemoveNode(node)) {
            if (node.isNodeType(Utils.RMA_RECORD))
              removeMixins(node);
            ThumbnailService thumbnailService = uiShowAllTrashResult.getApplicationComponent(ThumbnailService.class);
            thumbnailService.processRemoveThumbnail(node);
            node.remove();
            parentNode.save();
            uiShowAllTrashResult.nodeListChange = true;
          uiShowAllTrashResult.updateTable(event);
          } else {
            throw new AccessDeniedException();
          }
        } catch (AccessDeniedException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Access denied! No permission for deleting node: " + node.getPath());
          }
          uiApp.addMessage(new ApplicationMessage("UIShowAllTrashResult.msg.accessDenied", null, ApplicationMessage.WARNING));
          
          } catch (VersionException ve) {
            uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.remove-verion-exception", null,
                ApplicationMessage.WARNING));
            
            uiExplorer.updateAjax(event);
            return;
          } catch (ReferentialIntegrityException ref) {
            session.refresh(false);
            uiExplorer.refreshExplorer();
            uiApp
                .addMessage(new ApplicationMessage(
                    "UIPopupMenu.msg.remove-referentialIntegrityException", null,
                    ApplicationMessage.WARNING));
            
            uiExplorer.updateAjax(event);
            return;
          } catch (ConstraintViolationException cons) {
            session.refresh(false);
            uiExplorer.refreshExplorer();
            uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.constraintviolation-exception",
                null, ApplicationMessage.WARNING));
            
            uiExplorer.updateAjax(event);
            return;
          } catch (LockException lockException) {
            uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-other-person", null,
                ApplicationMessage.WARNING));
            
            uiExplorer.updateAjax(event);
            return;
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("an unexpected error occurs while removing the node", e);
            }
            JCRExceptionManager.process(uiApp, e);
            
            return;
          }

      }

      private void removeMixins(Node node) throws Exception {
          NodeType[] mixins = node.getMixinNodeTypes();
          for (NodeType nodeType : mixins) {
            node.removeMixin(nodeType.getName());
          }
        }

  }

  static public class RestoreActionListener extends EventListener<UIShowAllTrashResult> {
      public void execute(Event<UIShowAllTrashResult> event) throws Exception {
          String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
          UIShowAllTrashResult uiShowAllTrashResult = event.getSource();
          UIJCRExplorer uiExplorer = uiShowAllTrashResult.getAncestorOfType(UIJCRExplorer.class);
        UIApplication uiApp = uiShowAllTrashResult.getAncestorOfType(UIApplication.class);

        ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
        TrashService trashService = (TrashService)myContainer.getComponentInstanceOfType(TrashService.class);

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
          // Reset the session to manage the links that potentially change of workspace
          //session = node.getSession();
          // Reset the workspace name to manage the links that potentially change of workspace
          //wsName = session.getWorkspace().getName();
        } catch(PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
              null,ApplicationMessage.WARNING));
          
          return;
        }

        try {
          uiExplorer.addLockToken(node);
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          return;
        }

        try {
          if (PermissionUtil.canRemoveNode(node)) {
            PortletPreferences portletPrefs = uiExplorer.getPortletPreferences();
            String trashWorkspace = portletPrefs.getValue(Utils.TRASH_WORKSPACE, "");
            String trashHomeNodePath = portletPrefs.getValue(Utils.TRASH_HOME_NODE_PATH, "");
            Session trashSession = uiExplorer.getSessionByWorkspace(trashWorkspace);
            Node trashHomeNode = (Node) trashSession.getItem(trashHomeNodePath);

            SessionProvider sessionProvider = uiExplorer.getSessionProvider();

            trashService.restoreFromTrash(trashHomeNode,
                            srcPath,
                            sessionProvider);
            uiShowAllTrashResult.nodeListChange = true;
            uiShowAllTrashResult.updateTable(event);
          } else {
            throw new AccessDeniedException();
          }
        } catch (LockException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("node is locked, can't restore node :" + node.getPath());
          }
          JCRExceptionManager.process(uiApp, e);
          
          uiExplorer.updateAjax(event);
        } catch (AccessDeniedException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Access denied! No permission for restoring node: " + node.getPath());
          }
          uiApp.addMessage(new ApplicationMessage("UIShowAllTrashResult.msg.accessDenied", null, ApplicationMessage.WARNING));
          
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("an unexpected error occurs", e);
          }
          JCRExceptionManager.process(uiApp, e);
          
          uiExplorer.updateAjax(event);
        }

      }
  }

  private static class SearchComparator implements Comparator<Node> {
      public int compare(Node node1, Node node2) {
        try {
          if (iconType.equals("BlueUpArrow") || iconType.equals("BlueDownArrow")) {
            String s1 = node1.getProperty("jcr:primaryType").getString();
            String s2 = node2.getProperty("jcr:primaryType").getString();
            if (iconType.trim().equals("BlueUpArrow")) { return s2.compareTo(s1); }
            return s1.compareTo(s2);
          } else if (iconName.equals("BlueUpArrow") || iconName.equals("BlueDownArrow")) {
            String name1 = node1.getName();
            String name2 = node2.getName();
            if (iconName.trim().equals("BlueUpArrow")) { return name2.compareTo(name1); }
            return name1.compareTo(name2);
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Cannot compare nodes", e);
          }
        }
        return 0;
      }
  }

}
