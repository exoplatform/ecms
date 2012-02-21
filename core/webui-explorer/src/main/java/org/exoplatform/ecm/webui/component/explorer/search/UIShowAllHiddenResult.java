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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 22, 2009
 * 2:44:09 PM
 */
@ComponentConfig(template = "app:/groovy/webui/component/explorer/search/UIShowAllHiddenResult.gtmpl",
                 events = {
    @EventConfig(listeners = UIShowAllHiddenResult.ViewActionListener.class),
    @EventConfig(listeners = UIShowAllHiddenResult.RemoveHiddenAttributeActionListener.class),
    @EventConfig(listeners = UIShowAllHiddenResult.SortASCActionListener.class),
    @EventConfig(listeners = UIShowAllHiddenResult.SortDESCActionListener.class) })
public class UIShowAllHiddenResult extends UIComponentDecorator {

  private static final Log LOG  = ExoLogger.getLogger("explorer.search.UIShowAllHiddenResult");
  private static final int FILE_PER_PAGE = 10;
  private static String iconType = "BlueDownArrow";
  private static String iconName = "";
  private long searchTime_ = 0;
  private List<Node> hiddenNodes_ = new ArrayList<Node>();
  private UIPageIterator uiPageIterator_ ;
  private boolean nodeListChange = false;

  public UIShowAllHiddenResult() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "UIShowAllHiddenIterator");
    setUIComponent(uiPageIterator_);
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
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
  public List<Node> getAllHiddenNodes() throws Exception {
    //return HiddenNodes_;
    return (List<Node>)uiPageIterator_.getCurrentPageData();
  }

  protected List<Node> getNewHiddenNodeList() throws Exception {
    List<Node> ret = new ArrayList<Node>();
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    boolean byUser = uiExplorer.getPreference().isShowItemsByUser();

    StringBuilder queryString = new StringBuilder("SELECT * FROM ").append(Utils.EXO_HIDDENABLE);
    if (byUser) {
      queryString.append(" WHERE CONTAINS(")
                 .append(Utils.EXO_OWNER)
                 .append(",'")
                 .append(uiExplorer.getSession().getUserID())
                 .append("')");
    }
    Session session = uiExplorer.getSession();
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString.toString(), Query.SQL);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    while (iter.hasNext()) {
      ret.add(iter.nextNode());
    }

    return ret;
  }

  public void updateList() throws Exception {
    List<Node> nodeList = getNewHiddenNodeList();
    ListAccess<Node> hiddenNodeList = new ListAccessImpl<Node>(Node.class, nodeList);
    LazyPageList<Node> pageList = new LazyPageList<Node>(hiddenNodeList, FILE_PER_PAGE);
    uiPageIterator_.setPageList(pageList);
    hiddenNodes_ = nodeList;
  }

  private void updateTable(Event<UIShowAllHiddenResult> event) throws Exception {
    int currentPage = uiPageIterator_.getCurrentPage();
    if (nodeListChange) {
      updateList();
      nodeListChange = false;
    }
    Collections.sort(hiddenNodes_, new SearchComparator());
    ListAccess<Node> hiddenNodeList = new ListAccessImpl<Node>(Node.class, hiddenNodes_);
    LazyPageList<Node> pageList = new LazyPageList<Node>(hiddenNodeList, FILE_PER_PAGE);

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
      EventListener<UIShowAllHiddenResult> {
    @Override
    public void execute(Event<UIShowAllHiddenResult> event) throws Exception {
      UIShowAllHiddenResult uiShowAllHiddenResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueDownArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueDownArrow";
      }
      uiShowAllHiddenResult.updateTable(event);
    }
  }

  public static class SortDESCActionListener extends EventListener<UIShowAllHiddenResult> {
    @Override
    public void execute(Event<UIShowAllHiddenResult> event) throws Exception {
      UIShowAllHiddenResult uiShowAllHiddenResult = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueUpArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueUpArrow";
      }

      uiShowAllHiddenResult.updateTable(event);
    }
  }

  static public class RemoveHiddenAttributeActionListener extends
                                                         EventListener<UIShowAllHiddenResult> {
    public void execute(Event<UIShowAllHiddenResult> event) throws Exception {
      // final String virtualNodePath = nodePath;
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIShowAllHiddenResult uiShowAllHiddenResult = event.getSource();
      UIJCRExplorer uiExplorer = uiShowAllHiddenResult.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiShowAllHiddenResult.getAncestorOfType(UIApplication.class);

      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
      String wsName = null;
      Node node = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        srcPath = matcher.group(2);
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '" + srcPath + "'");
      }

      Session session = uiExplorer.getSessionByWorkspace(wsName);
      try {
        // Use the method getNodeByPath because it is link aware
        node = uiExplorer.getNodeByPath(srcPath, session, false);
        // Reset the path to manage the links that potentially create virtual
        // path
        // srcPath = node.getPath();
        // Reset the session to manage the links that potentially change of
        // workspace
        session = node.getSession();
        // Reset the workspace name to manage the links that potentially change
        // of workspace
        // wsName = session.getWorkspace().getName();
      } catch (PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
                                                null,
                                                ApplicationMessage.WARNING));
        
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
          uiShowAllHiddenResult.nodeListChange = true;
          node.removeMixin(Utils.EXO_HIDDENABLE);
          node.save();
          uiShowAllHiddenResult.updateTable(event);
        } else {
          throw new AccessDeniedException();
        }
      } catch (LockException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("node is locked, can't remove hidden property of node :" + node.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
        uiExplorer.updateAjax(event);
      } catch (AccessDeniedException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Access denied! No permission for modifying property " + Utils.EXO_HIDDENABLE
            + " of node: " + node.getPath());
        }
        uiApp.addMessage(new ApplicationMessage("UIShowAllHiddenResult.msg.accessDenied",
                                                null,
                                                ApplicationMessage.WARNING));
        
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs while removing the node", e);
        }
        JCRExceptionManager.process(uiApp, e);
        
        return;
      }
    }
  }

  static public class ViewActionListener extends EventListener<UIShowAllHiddenResult> {
    public void execute(Event<UIShowAllHiddenResult> event) throws Exception {
      UIShowAllHiddenResult uiShowAllHiddenResult = event.getSource();
      UIJCRExplorer uiExplorer = uiShowAllHiddenResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      UIApplication uiApp = uiShowAllHiddenResult.getAncestorOfType(UIApplication.class);
      Node node;
      try {
        node = uiExplorer.getNodeByPath(path, uiExplorer.getTargetSession());
      } catch (AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIShowAllHiddenResult.msg.access-denied",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      TemplateService templateService = uiShowAllHiddenResult.getApplicationComponent(TemplateService.class);
      if (!templateService.isManagedNodeType(node.getPrimaryNodeType().getName())) {
        uiApp.addMessage(new ApplicationMessage("UIShowAllHiddenResult.msg.not-support",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      UIPopupWindow uiPopup = uiExplorer.getChildById("ViewSearch");
      if (uiPopup == null) {
        uiPopup = uiExplorer.addChild(UIPopupWindow.class, null, "ViewSearch");
      }
      uiPopup.setResizable(true);
      uiPopup.setShowMask(true);
      UIViewSearchResult uiViewSearch = uiPopup.createUIComponent(UIViewSearchResult.class,
                                                                  null,
                                                                  null);
      uiViewSearch.setNode(node);

      uiPopup.setWindowSize(600, 750);
      uiPopup.setUIComponent(uiViewSearch);
      uiPopup.setRendered(true);
      uiPopup.setShow(true);
      return;
    }
  }

  private static class SearchComparator implements Comparator<Node> {
    public int compare(Node node1, Node node2) {
      try {
        if (iconType.equals("BlueUpArrow") || iconType.equals("BlueDownArrow")) {
          String s1 = node1.getProperty("jcr:primaryType").getString();
          String s2 = node2.getProperty("jcr:primaryType").getString();
          if (iconType.trim().equals("BlueUpArrow")) {
            return s2.compareTo(s1);
          }
          return s1.compareTo(s2);
        } else if (iconName.equals("BlueUpArrow") || iconName.equals("BlueDownArrow")) {
          String name1 = node1.getName();
          String name2 = node2.getName();
          if (iconName.trim().equals("BlueUpArrow")) {
            return name2.compareTo(name1);
          }
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
