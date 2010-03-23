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
import java.util.Collections;
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
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
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
  private static final Log LOG  = ExoLogger.getLogger("explorer.search.UISearchResult");
  
  private QueryResult queryResult_;
  private long searchTime_ = 0; 
  private boolean flag_ = false;
  private UIQueryResultPageIterator uiPageIterator_;
  private List<Node> currentListNodes_ = new ArrayList<Node>();
  private List<Row> currentListRows_ = new ArrayList<Row>();
  private int currentAvailablePage_ = 0;
  private boolean isEndOfIterator_ = false;
  private static String iconType = "";
  private static String iconScore = "";
  static private int PAGE_SIZE = 10;
  private List<String> categoryPathList = new ArrayList<String>();
  private String constraintsCondition;
  private static final String EXO_RESTORE_LOCATION = "exo:restoreLocation";  
  
  public List<String> getCategoryPathList() { return categoryPathList; }
  public void setCategoryPathList(List<String> categoryPathListItem) {
    categoryPathList = categoryPathListItem; 
  }
  
  public String getConstraintsCondition() { return constraintsCondition; }
  public void setConstraintsCondition(String constraintsConditionItem) {
    constraintsCondition = constraintsConditionItem; 
  }
  
  public UISearchResult() throws Exception {
    uiPageIterator_ = addChild(UIQueryResultPageIterator.class, null, null);
  }

  public void setQueryResults(QueryResult queryResult) throws Exception {
    queryResult_ = queryResult;         
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
  
  private void addNode(List<Node> listNodes, Node node, List<Row> listRows, Row r) throws Exception {
    List<Node> checkList = new ArrayList<Node>();
    if (flag_) checkList = currentListNodes_; 
    else checkList = listNodes;
    if (node.getName().equals(Utils.JCR_CONTENT)) {
      if (!checkList.contains(node.getParent())) {
        listNodes.add(node.getParent());
        listRows.add(r);
      }
    } else if (!checkList.contains(node)) {
      listNodes.add(node);
      listRows.add(r);
    }
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
  
  public List<Row> getResultList() throws Exception {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
    List<Node> listNodes = new ArrayList<Node>();
    List<Row> listRows = new ArrayList<Row>();
    Node resultNode = null;
    if (queryResult_ == null) return new ArrayList<Row>();
    long resultListSize = queryResult_.getNodes().getSize();
    if (!queryResult_.getRows().hasNext()) return currentListRows_;    
    if (resultListSize > 100) {
      for (RowIterator iter = queryResult_.getRows(); iter.hasNext();) {
        Row r = iter.nextRow();
        String path = r.getValue("jcr:path").getString();
        try {
          resultNode = getNodeByPath(path);
        } catch (Exception e) {
          LOG.warn("Can't get node by path " + path, e);
          continue;
        }
        if (resultNode != null) {
          if ((categoryPathList != null) && (categoryPathList.size() > 0)){
            for (String categoryPath : categoryPathList) {            
              int index = categoryPath.indexOf("/");
              List<String> pathCategoriesList = new ArrayList<String>();
              String searchCategory = rootTreePath + "/" + categoryPath;
              List<Node> listCategories = taxonomyService.getCategories(resultNode, categoryPath.substring(0, index));
              for (Node category : listCategories) {
                pathCategoriesList.add(category.getPath());
              }
              if (pathCategoriesList.contains(searchCategory)) addNode(listNodes, resultNode, listRows, r);
            }
          } else {
            addNode(listNodes, resultNode, listRows, r);
          }
        }
        if (!iter.hasNext()) isEndOfIterator_ = true;
        if (listNodes.size() == 100) {
          currentListNodes_.addAll(listNodes);
          currentListRows_.addAll(listRows); 
          break;
        }
        if (listNodes.size() < 100 && iter.hasNext()) {
          currentListNodes_.addAll(listNodes);
          currentListRows_.addAll(listRows);
          flag_ = true;
        }
      }
    } else {
      for (RowIterator iter = queryResult_.getRows(); iter.hasNext();) {
        Row r = iter.nextRow();
        if (!iter.hasNext()) isEndOfIterator_ = true;
        String path = r.getValue("jcr:path").getString();
        try {
          resultNode = getNodeByPath(path);
        } catch (Exception e) {
          LOG.warn("Can't get node by path " + path, e);
          continue;
        }
        if (resultNode != null) {
          if ((categoryPathList != null) && (categoryPathList.size() > 0)){
            for (String categoryPath : categoryPathList) {
              int index = categoryPath.indexOf("/");
              List<String> pathCategoriesList = new ArrayList<String>();
              String searchCategory = rootTreePath + "/" + categoryPath;
              List<Node> listCategories = taxonomyService.getCategories(resultNode, categoryPath.substring(0, index));
              for (Node category : listCategories) {
                pathCategoriesList.add(category.getPath());
              }
              if (pathCategoriesList.contains(searchCategory)) addNode(listNodes, resultNode, listRows, r);
            }
          } else {
            addNode(listNodes, resultNode, listRows, r);
          }
        }
      }
      currentListNodes_= listNodes;
      currentListRows_ = listRows;
    }
    return currentListRows_;
  }
  
  public void clearAll() {
    flag_ = false;
    isEndOfIterator_ = false;
    currentListNodes_.clear();
  }
  
  public UIQueryResultPageIterator getUIPageIterator() { return uiPageIterator_; }

  public void updateGrid(boolean flagCheck) throws Exception {
    SearchResultPageList pageList;
    if (flagCheck) {
      pageList = new SearchResultPageList(queryResult_, getResultList(), PAGE_SIZE, isEndOfIterator_);
    } else {
      pageList = new SearchResultPageList(queryResult_, currentListRows_, PAGE_SIZE, isEndOfIterator_);
    }
    currentAvailablePage_ = currentListNodes_.size()/PAGE_SIZE;
    uiPageIterator_.setSearchResultPageList(pageList);
    uiPageIterator_.setPageList(pageList);
  }
  
  public int getCurrentAvaiablePage() { return currentAvailablePage_; }
  
  private static class SearchComparator implements Comparator<Row> {
    public int compare(Row row1, Row row2) {
      try {
        if (iconType.equals("BlueUpArrow") || iconType.equals("BlueDownArrow")) {
          String s1 = row1.getValue("jcr:primaryType").getString();
          String s2 = row2.getValue("jcr:primaryType").getString();
          if (iconType.trim().equals("BlueUpArrow")) { return s2.compareTo(s1); }        
          return s1.compareTo(s2);
        } else if (iconScore.equals("BlueUpArrow") || iconScore.equals("BlueDownArrow")) {
          Long l1 = row1.getValue("jcr:score").getLong();
          Long l2 = row2.getValue("jcr:score").getLong();
          if (iconScore.trim().equals("BlueUpArrow")) { return l2.compareTo(l1); }        
          return l1.compareTo(l2);
        }
      } catch (Exception e) {
        LOG.error("Cannot compare rows", e);
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
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } catch(ItemNotFoundException inf) {
          uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null, 
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
      } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null, 
                  ApplicationMessage.WARNING)) ;
	      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
	      return ;    	  
      } catch(RepositoryException e) {
    		LOG.error("Repository cannot be found");      	
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null, 
            ApplicationMessage.WARNING)) ;
			  event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
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
      
      uiExplorer.updateAjax(event) ;      
    }
    
    private boolean isInTrash(Item item) throws RepositoryException {
    	return (item instanceof Node) &&
      			 ((Node) item).isNodeType(EXO_RESTORE_LOCATION);
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
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(Exception e) {
        LOG.error("Cannot access the node at " + folderPath, e);        
      }

    	PortletPreferences portletPrefs = uiExplorer.getPortletPreferences();
    	String trashHomeNodePath = portletPrefs.getValue(Utils.TRASH_HOME_NODE_PATH, "");

      if (!node.getPath().startsWith(trashHomeNodePath)) {
      	uiExplorer.setSelectNode(node.getSession().getWorkspace().getName(), folderPath);
	      uiExplorer.updateAjax(event);
      }
    }
  }
  
  static public class SortASCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();     
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueDownArrow";
        iconScore = "";
      } else if (objectId.equals("score")) {
        iconScore = "BlueDownArrow";
        iconType = "";
      }
      Collections.sort(uiSearchResult.currentListRows_, new SearchComparator());
      SearchResultPageList pageList = new SearchResultPageList(uiSearchResult.queryResult_, 
          uiSearchResult.currentListRows_, PAGE_SIZE, uiSearchResult.isEndOfIterator_);
      uiSearchResult.currentAvailablePage_ = uiSearchResult.currentListNodes_.size()/PAGE_SIZE;
      uiSearchResult.uiPageIterator_.setSearchResultPageList(pageList);
      uiSearchResult.uiPageIterator_.setPageList(pageList);      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());      
    }
  } 
  
  static public class SortDESCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource() ;     
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueUpArrow";
        iconScore = "";
      } else if (objectId.equals("score")) {
        iconScore = "BlueUpArrow";
        iconType = "";
      }
      Collections.sort(uiSearchResult.currentListRows_, new SearchComparator());
      SearchResultPageList pageList = new SearchResultPageList(uiSearchResult.queryResult_, 
          uiSearchResult.currentListRows_, PAGE_SIZE, uiSearchResult.isEndOfIterator_);
      uiSearchResult.currentAvailablePage_ = uiSearchResult.currentListNodes_.size()/PAGE_SIZE;
      uiSearchResult.uiPageIterator_.setSearchResultPageList(pageList);
      uiSearchResult.uiPageIterator_.setPageList(pageList);      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());      
    }
  } 
}