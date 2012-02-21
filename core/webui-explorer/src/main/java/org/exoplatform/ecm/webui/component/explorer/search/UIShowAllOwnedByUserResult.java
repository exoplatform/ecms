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
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
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
 * Oct 23, 2009
 * 9:44:59 AM
 */
@ComponentConfig(template = "app:/groovy/webui/component/explorer/search/UIShowAllOwnedByUserResult.gtmpl",
                 events = {
    @EventConfig(listeners = UIShowAllOwnedByUserResult.ViewActionListener.class),
    @EventConfig(listeners = UIShowAllOwnedByUserResult.SortASCActionListener.class),
    @EventConfig(listeners = UIShowAllOwnedByUserResult.SortDESCActionListener.class) })
public class UIShowAllOwnedByUserResult extends UIComponentDecorator {

  private static final Log LOG  = ExoLogger.getLogger("explorer.search.UIShowAllOwnedByUserResult");
  private static final int FILE_PER_PAGE = 10;
  private static String iconType = "BlueDownArrow";
  private static String iconName = "";
  private long searchTime_ = 0;
  private List<Node> ownedByUserNodes_ = new ArrayList<Node>();
  private UIPageIterator uiPageIterator_ ;

  public UIShowAllOwnedByUserResult() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "UIShowAllOwnedByUserIterator");
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
  public List<Node> getAllOwnedByUserNodes() throws Exception {
    //return OwnedByUserNodes_;
    return (List<Node>)uiPageIterator_.getCurrentPageData();
  }

  protected List<Node> getNewOwnedByUserNodeList() throws Exception {
    List<Node> ret = new ArrayList<Node>();

    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    Session session = uiExplorer.getSession();
    String userName = session.getUserID();
    StringBuilder queryString = new StringBuilder("SELECT * FROM ").append(Utils.NT_BASE);
    queryString.append(" WHERE CONTAINS(")
               .append(Utils.EXO_OWNER)
               .append(",'")
               .append(userName)
               .append("')");
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString.toString(), Query.SQL);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    Node node = null;
    Set<Node> set = new TreeSet<Node>(new NodeComparator());
    while (iter.hasNext()) {
      node = iter.nextNode();
      if (node.getName().equals(Utils.JCR_CONTENT)) continue;
      if (node.isNodeType(Utils.NT_RESOURCE))
        node = node.getParent();
      if (!set.contains(node)) {
        ret.add(node);
        set.add(node);
      }
    }

    return ret;
  }

  public void updateList() throws Exception {
    List<Node> nodeList = getNewOwnedByUserNodeList();
    ListAccess<Node> ownedByUserNodeLiist = new ListAccessImpl<Node>(Node.class, nodeList);
    LazyPageList<Node> pageList = new LazyPageList<Node>(ownedByUserNodeLiist, FILE_PER_PAGE);
    uiPageIterator_.setPageList(pageList);
    ownedByUserNodes_ = nodeList;
  }

  private void updateTable(Event<UIShowAllOwnedByUserResult> event) throws Exception {
    int currentPage = uiPageIterator_.getCurrentPage();

    Collections.sort(ownedByUserNodes_, new SearchComparator());
    ListAccess<Node> ownedByUserNodeLiist = new ListAccessImpl<Node>(Node.class, ownedByUserNodes_);
    LazyPageList<Node> pageList = new LazyPageList<Node>(ownedByUserNodeLiist, FILE_PER_PAGE);

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
      EventListener<UIShowAllOwnedByUserResult> {
    @Override
    public void execute(Event<UIShowAllOwnedByUserResult> event) throws Exception {
      UIShowAllOwnedByUserResult uiShowAllOwnedByUserResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueDownArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueDownArrow";
      }
      uiShowAllOwnedByUserResult.updateTable(event);
    }
  }

  public static class SortDESCActionListener extends
      EventListener<UIShowAllOwnedByUserResult> {
    @Override
    public void execute(Event<UIShowAllOwnedByUserResult> event) throws Exception {
      UIShowAllOwnedByUserResult uiShowAllOwnedByUserResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueUpArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueUpArrow";
      }

      uiShowAllOwnedByUserResult.updateTable(event);
    }
  }

  static  public class ViewActionListener extends EventListener<UIShowAllOwnedByUserResult> {
      public void execute(Event<UIShowAllOwnedByUserResult> event) throws Exception {
        UIShowAllOwnedByUserResult uiShowAllOwnedByUserResult = event.getSource();
        UIJCRExplorer uiExplorer = uiShowAllOwnedByUserResult.getAncestorOfType(UIJCRExplorer.class);
        String path = event.getRequestContext().getRequestParameter(OBJECTID);
        UIApplication uiApp = uiShowAllOwnedByUserResult.getAncestorOfType(UIApplication.class);
        Node node;
        try {
          node = uiExplorer.getNodeByPath(path, uiExplorer.getTargetSession());
        } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIShowAllOwnedByUserResult.msg.access-denied", null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        TemplateService templateService = uiShowAllOwnedByUserResult.getApplicationComponent(TemplateService.class);
        if (!templateService.isManagedNodeType(node.getPrimaryNodeType().getName())) {
          uiApp.addMessage(new ApplicationMessage("UIShowAllOwnedByUserResult.msg.not-support", null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        UIPopupWindow uiPopup = uiExplorer.getChildById("ViewSearch");
        if (uiPopup == null) {
          uiPopup = uiExplorer.addChild(UIPopupWindow.class, null, "ViewSearch");
        }
        uiPopup.setResizable(true);
        uiPopup.setShowMask(true);
        UIViewSearchResult uiViewSearch = uiPopup.createUIComponent(UIViewSearchResult.class, null, null);
        uiViewSearch.setNode(node);

        uiPopup.setWindowSize(600,750);
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

  private static class NodeComparator implements Comparator<Node> {

    public int compare(Node o1, Node o2) {
      try {
        if (o1.isSame(o2))
          return 0;
        return o1.getName().compareTo(o2.getName());
      } catch (RepositoryException e) {
        return 1;
      }
    }

  }

}
