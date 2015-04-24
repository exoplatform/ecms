/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import org.exoplatform.ecm.jcr.model.Preference;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.NodeSearchFilter;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.services.wcm.search.base.QueryData;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.paginator.UILazyPageIterator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Oct 2, 2006
 * 16:37:15
 *
 * Edited by : Dang Van Minh
 *             minh.dang@exoplatform.com
 * Jan 5, 2007
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/search/UISearchResult.gtmpl",
    events = {
        @EventConfig(listeners = UISearchResult.ViewActionListener.class),
        @EventConfig(listeners = UISearchResult.OpenFolderActionListener.class),
        @EventConfig(listeners = UISearchResult.SortASCActionListener.class),
        @EventConfig(listeners = UISearchResult.SortDESCActionListener.class)
    }
)
public class UISearchResult extends UIContainer {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UISearchResult.class.getName());

  private QueryData queryData_;
  private long searchTime_ = 0;
  private UIPageIterator uiPageIterator_;
  private static String iconType = "";
  private static String iconScore = "";
  static private int PAGE_SIZE = 10;
  private List<String> categoryPathList = new ArrayList<String>();
  private String constraintsCondition;
  private String workspaceName = null;
  private String currentPath = null;
  private String keyword ="";
  private AbstractPageList<RowData> pageList;

  public List<String> getCategoryPathList() { return categoryPathList; }
  public void setCategoryPathList(List<String> categoryPathListItem) {
    categoryPathList = categoryPathListItem;
  }

  public String getConstraintsCondition() { return constraintsCondition; }
  public void setConstraintsCondition(String constraintsConditionItem) {
    constraintsCondition = constraintsConditionItem;
  }

  public UISearchResult() throws Exception {
    uiPageIterator_ = addChild(UILazyPageIterator.class, null, "UISearchResultPageIterator");
  }

  public void setQuery(String queryStatement, String workspaceName, String language, boolean isSystemSession, String keyword) {
    queryData_ = new QueryData(queryStatement, workspaceName, language, isSystemSession);
    this.keyword = keyword;
  }

  public long getSearchTime() { return searchTime_; }
  public void setSearchTime(long time) { this.searchTime_ = time; }

  public List getCurrentList() throws Exception {
    return uiPageIterator_.getCurrentPageData();
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
  }

  public Session getSession() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getTargetSession();
  }

  public Date getDateCreated(Node node) throws Exception{
    if (node.hasProperty("exo:dateCreated")) {
      return node.getProperty("exo:dateCreated").getDate().getTime();
    }
    return new GregorianCalendar().getTime();
  }

  public Node getNodeByPath(String path) throws Exception {
    try {
      JCRPath nodePath = ((SessionImpl)getSession()).getLocationFactory().parseJCRPath(path);
      return (Node)getSession().getItem(nodePath.getAsString(false));
    } catch (Exception e) {
      return null;
    }
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public void updateGrid() throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    List<String> documentList = templateService.getDocumentTemplates();
     pageList =
      PageListFactory.createPageList(queryData_.getQueryStatement(),
                             queryData_.getWorkSpace(),
                             queryData_.getLanguage_(),
                             queryData_.isSystemSession(),
                             new NodeFilter(categoryPathList, keyword, documentList),
                             new RowDataCreator(),
                             PAGE_SIZE,
                             0);
    uiPageIterator_.setPageList(pageList);
  }

  private static class SearchComparator implements Comparator<RowData> {

    public static final String SORT_TYPE = "NODE_TYPE";
    public static final String SORT_SCORE = "JCR_SCORE";
    public static final String ASC = "ASC";
    public static final String DESC = "DECS";

    private String sortType;
    private String orderType;

    public void setSortType(String value) { sortType = value; }
    public void setOrderType(String value) { orderType = value; }

    public int compare(RowData row1, RowData row2) {
      try {
        if (SORT_TYPE.equals(sortType.trim())) {
          String s1 = row1.getJcrPrimaryType();
          String s2 = row2.getJcrPrimaryType();
          if (DESC.equals(orderType.trim())) { return s2.compareTo(s1); }
          return s1.compareTo(s2);
        } else if (SORT_SCORE.equals(sortType.trim())) {
          Long l1 = row1.getJcrScore();
          Long l2 = row2.getJcrScore();
          if (DESC.equals(orderType.trim())) { return l2.compareTo(l1); }
          return l1.compareTo(l2);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Cannot compare rows", e);
        }
      }
      return 0;
    }
  }

  public String StriptHTML(String s) {
    String[] targets = {"<div>", "</div>", "<span>", "</span>"};
    for (String target : targets) {
      s = s.replace(target, "");
    }
    return s;
  }

  static  public class ViewActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      UIJCRExplorer uiExplorer = uiSearchResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      UIApplication uiApp = uiSearchResult.getAncestorOfType(UIApplication.class);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      Item item = null;
      try {
        Session session = uiExplorer.getSessionByWorkspace(workspaceName);
        // Check if the path exists
        NodeFinder nodeFinder = uiSearchResult.getApplicationComponent(NodeFinder.class);
        item = nodeFinder.getItem(session, path);
      } catch(PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch(ItemNotFoundException inf) {
          uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
              ApplicationMessage.WARNING)) ;

          return ;
      } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null,
                  ApplicationMessage.WARNING)) ;

        return ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      if (isInTrash(item))
        return;

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      }

      uiExplorer.setSelectNode(workspaceName, path) ;

      uiDocumentWorkspace.getChild(UIDocumentContainer.class).setRendered(true);
      uiSearchResult.setRendered(false);
      uiExplorer.refreshExplorer((Node)item, true);
    }

    private boolean isInTrash(Item item) throws RepositoryException {
      return (item instanceof Node) && Utils.isInTrash((Node) item);
    }
  }

  static public class OpenFolderActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      UIJCRExplorer uiExplorer = uiSearchResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      String folderPath = LinkUtils.getParentPath(path);
      Node node = null;
      try {
        node = uiExplorer.getNodeByPath(folderPath, uiExplorer.getTargetSession());
      } catch(AccessDeniedException ace) {
        UIApplication uiApp = uiSearchResult.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISearchResult.msg.access-denied", null,
            ApplicationMessage.WARNING));

        return;
      } catch(PathNotFoundException ace) {
        UIApplication uiApp = uiSearchResult.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISearchResult.msg.access-denied", null,
            ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Cannot access the node at " + folderPath, e);
        }
      }

      uiExplorer.setSelectNode(node.getSession().getWorkspace().getName(), folderPath);
      uiExplorer.refreshExplorer(node, true);
    }
  }

  static public class SortASCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      SearchComparator comparator = new SearchComparator();
      if (objectId.equals("type")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_PRIMARYTYPE);
        comparator.setSortType(SearchComparator.SORT_TYPE);
        iconType = Preference.BLUE_DOWN_ARROW;
        iconScore = "";
      } else if (objectId.equals("score")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_SCORE);
        comparator.setSortType(SearchComparator.SORT_SCORE);
        iconScore = Preference.BLUE_DOWN_ARROW;
        iconType = "";
      }
      comparator.setOrderType(SearchComparator.ASC);
      uiSearchResult.pageList.setComparator(comparator);
      uiSearchResult.pageList.setOrder("ASC");
      uiSearchResult.pageList.sortData();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());
    }
  }

  static public class SortDESCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource() ;
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      SearchComparator comparator = new SearchComparator();
      if (objectId.equals("type")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_PRIMARYTYPE);
        comparator.setSortType(SearchComparator.SORT_TYPE);
        iconType = Preference.BLUE_UP_ARROW;
        iconScore = "";
      } else if (objectId.equals("score")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_SCORE);
        comparator.setSortType(SearchComparator.SORT_SCORE);
        iconScore = Preference.BLUE_UP_ARROW;
        iconType = "";
      }
      comparator.setOrderType(SearchComparator.DESC);
      uiSearchResult.pageList.setComparator(comparator);
      uiSearchResult.pageList.setOrder("DESC");
      uiSearchResult.pageList.sortData();
//      Collections.sort(uiSearchResult.currentListRows_, new SearchComparator());
//      SearchResultPageList pageList = new SearchResultPageList(uiSearchResult.queryResult_,
//          uiSearchResult.currentListRows_, PAGE_SIZE, uiSearchResult.isEndOfIterator_);
//      uiSearchResult.currentAvailablePage_ = uiSearchResult.currentListNodes_.size()/PAGE_SIZE;
//      uiSearchResult.uiPageIterator_.setSearchResultPageList(pageList);
//      uiSearchResult.uiPageIterator_.setPageList(pageList);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());
    }
  }

  public static class NodeFilter implements NodeSearchFilter {

    private List<String> categoryPathList;
    private TaxonomyService taxonomyService;
    private NodeHierarchyCreator nodeHierarchyCreator;
    private String rootTreePath;
    private LinkManager linkManager = null;
    private String keyword ="";
    private List<String> documentTypes;

    final static private String  CHECK_LINK_MATCH_QUERY1 = "select * from nt:base "
                                                             + "where jcr:path = '$0' and ( contains(*, '$1') "
                                                             + "or lower(exo:name) like '%$2%' "
                                                             + "or lower(exo:title) like '%$2%')";

    final static private String  CHECK_LINK_MATCH_QUERY2 = "select * from nt:base where jcr:path like '$0/%' "
                                                             + "and ( contains(*, '$1') or lower(exo:name) like '%$2%' "
                                                             + "or lower(exo:title) like '%$2%')";

    public NodeFilter(List<String> categories, String keyword, List<String> documentTypes) {
      taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
      nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      linkManager = WCMCoreUtils.getService(LinkManager.class);
      rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
      categoryPathList = categories;
      this.keyword = keyword;
      this.documentTypes = documentTypes;
    }
    public Node filterNodeToDisplay(Node node) {
      try {
        if (node == null || node.getPath().contains("/jcr:system/")) return null;
        if (node != null) {
          if ((categoryPathList != null) && (categoryPathList.size() > 0)){
            for (String categoryPath : categoryPathList) {
              int index = categoryPath.indexOf("/");
              String taxonomyName = categoryPath;
              String postFixTaxonomy = "";
              if (index > 0) {
                taxonomyName = categoryPath.substring(0, index);
                postFixTaxonomy = categoryPath.substring(index + 1);
              }

              List<String> pathCategoriesList = new ArrayList<String>();
              String searchCategory = taxonomyService.getTaxonomyTree(taxonomyName).getPath() +
                                      ("".equals(postFixTaxonomy) ? "" : "/" + postFixTaxonomy);
              Node targetNode = node.isNodeType(Utils.EXO_SYMLINK) ?
                                        linkManager.getTarget(node) : node;
              List<Node> listCategories = taxonomyService.getCategories(targetNode, taxonomyName);
              for (Node category : listCategories) {
                pathCategoriesList.add(category.getPath());
              }
              if (pathCategoriesList.contains(searchCategory))
              {
                if (node.isNodeType(Utils.EXO_SYMLINK)) {
                  if (checkTargetMatch(node, keyword)) return node;
                }else {
                  return node;
                }
              }
            }
            return null;
          } else {
            if (node.isNodeType(Utils.EXO_SYMLINK)) {
              if (checkTargetMatch(node, keyword)) return node;
            } else if (node.isNodeType(Utils.NT_RESOURCE)) {
              return node.getParent();
            } else if (node.isNodeType(Utils.EXO_COMMENTS)) {
              return node.getParent().getParent();
            } else {
              return node;
            }
          }
        }
      } catch (RepositoryException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      return null;
    }
    /**
     * Check a symlink/taxonomylink if its target matches with keyword for searching ...link
     * @param linkPath
     * @param keyword
     * @return
     */
    protected boolean checkTargetMatch(Node symlinkNode, String keyword) {
      String queryStatement = CHECK_LINK_MATCH_QUERY1;
      Session targetSession;
      Node target=null;
      if (keyword ==null || keyword.length()==0 ) return true;
      if (linkManager==null) {
        linkManager = WCMCoreUtils.getService(LinkManager.class);
      }
      try {
        if (!linkManager.isLink(symlinkNode)) return true;
        target = linkManager.getTarget(symlinkNode);
        if (target == null) return false;
        targetSession = target.getSession();
        queryStatement = StringUtils.replace(queryStatement,"$0", target.getPath());
        queryStatement = StringUtils.replace(queryStatement,"$1", keyword.replaceAll("'", "''"));
        queryStatement = StringUtils.replace(queryStatement,"$2", keyword.replaceAll("'", "''").toLowerCase());
        QueryManager queryManager = targetSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryStatement, Query.SQL);
        QueryResult queryResult = query.execute();
        if ( queryResult.getNodes().getSize()>0 ) return true;
        if (isFodlderDocument(target)||target.hasNode("jcr:content") ) {
          queryStatement = CHECK_LINK_MATCH_QUERY2;
          queryStatement = StringUtils.replace(queryStatement,"$0", target.getPath());
          queryStatement = StringUtils.replace(queryStatement,"$1", keyword.replaceAll("'", "''"));
          queryStatement = StringUtils.replace(queryStatement,"$2", keyword.replaceAll("'", "''").toLowerCase());
          query = queryManager.createQuery(queryStatement, Query.SQL);
          queryResult = query.execute();
          return queryResult.getNodes().getSize()>0;
        }else {
          return false;
        }
      } catch (RepositoryException e) {
        return false;
      }
    }
    private boolean isFodlderDocument(Node node) throws RepositoryException{
      if (!node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)) return false;
      for (String documentType : documentTypes) {
        if (node.getPrimaryNodeType().isNodeType(documentType))
          return true;
      }
      return false;
    }
  }

  public static class RowDataCreator implements SearchDataCreator<RowData> {

    public RowData createData(Node node, Row row) {
      return new RowData(row, node);
    }

  }

  public static class RowData {
    private String jcrPath = "";
    private String repExcerpt = "";
    private long jcrScore = 0;
    private String jcrPrimaryType = "";

    public RowData(Row row) {
      this(row, null);
    }
    
    public RowData(Row row, Node node) {
      try {
        jcrPath = node != null ? node.getPath() : row.getValue("jcr:path").getString();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      try {
        repExcerpt = row.getValue("rep:excerpt(.)").getString();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      try {
        jcrScore = row.getValue("jcr:score").getLong();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      try {
        jcrPrimaryType = row.getValue("jcr:primaryType").getString();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }

    public String getJcrPath() {
      return jcrPath;
    }

    public void setJcrPath(String jcrPath) {
      this.jcrPath = jcrPath;
    }

    public String getRepExcerpt() {
      return repExcerpt;
    }

    public void setRepExcerpt(String repExcerpt) {
      this.repExcerpt = repExcerpt;
    }

    public long getJcrScore() {
      return jcrScore;
    }

    public void setJcrScore(long jcrScore) {
      this.jcrScore = jcrScore;
    }

    public String getJcrPrimaryType() {
      return jcrPrimaryType;
    }

    public void setJcrPrimaryType(String value) {
      jcrPrimaryType = value;
    }

    public int hashCode() {
      return (jcrPath == null ? 0 : jcrPath.hashCode());
    }

    public boolean equals(Object o) {
      if (o == null) return false;
      if (! (o instanceof RowData)) return false;
      RowData data = (RowData) o;
      return (jcrPath == null && data.jcrPath == null || jcrPath.equals(data.jcrPath));
    }
  }
}
