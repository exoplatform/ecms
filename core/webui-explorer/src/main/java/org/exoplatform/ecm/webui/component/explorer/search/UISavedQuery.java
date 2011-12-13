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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 4, 2006
 * 16:37:15
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/search/UISavedQuery.gtmpl",
    events = {
        @EventConfig(listeners = UISavedQuery.ExecuteActionListener.class),
        @EventConfig(listeners = UISavedQuery.DeleteActionListener.class, confirm = "UISavedQuery.msg.confirm-delete-query"),
        @EventConfig(listeners = UISavedQuery.EditActionListener.class)
    }
)

public class UISavedQuery extends UIContainer implements UIPopupComponent {

  final static public String EDIT_FORM = "EditSavedQueryForm";

  private UIPageIterator uiPageIterator_;

  private boolean isQuickSearch_ = false;

  public UISavedQuery() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "SavedQueryIterator");
  }

  public void updateGrid(int currentPage) throws Exception {
    ListAccess<Object> queryList = new ListAccessImpl<Object>(Object.class,
                                                              NodeLocation.getLocationsByNodeList(queryList()));
    LazyPageList<Object> pageList = new LazyPageList<Object>(queryList, 10);
    uiPageIterator_.setPageList(pageList);
    if (currentPage > uiPageIterator_.getAvailablePage())
      uiPageIterator_.setCurrentPage(uiPageIterator_.getAvailablePage());
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }

  public List<Object> queryList() throws Exception {
    List<Object> objectList = new ArrayList<Object>();
    List<Node> sharedQueries = getSharedQueries();
    if(!sharedQueries.isEmpty()) {
      for(Node node : sharedQueries) {
        objectList.add(node);
      }
    }
    List<Query> queries = getQueries();
    if(!queries.isEmpty()) {
      for(Query query : queries) {
        objectList.add(new QueryData(query));
      }
    }
    return objectList;
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getQueryList() throws Exception {
    return NodeLocation.getNodeListByLocationList(uiPageIterator_.getCurrentPageData());
  }

  public void initPopupEditForm(Query query) throws Exception {
    removeChildById(EDIT_FORM);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, EDIT_FORM);
    uiPopup.setWindowSize(500,0);
    UIJCRAdvancedSearch uiJAdvancedSearch =
      createUIComponent(UIJCRAdvancedSearch.class, null, "EditQueryForm");
    uiJAdvancedSearch.setActions(new String[] {"Save", "Cancel"});
    uiPopup.setUIComponent(uiJAdvancedSearch);
    uiPopup.setRendered(true);
    uiJAdvancedSearch.setIsEdit(true);
    uiJAdvancedSearch.setQuery(query);
    uiJAdvancedSearch.update(query);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }

  public List<Query> getQueries() throws Exception {
    QueryService queryService = getApplicationComponent(QueryService.class);
    try {
      return queryService.getQueries(getCurrentUserId(), WCMCoreUtils.getUserSessionProvider());
    } catch(AccessDeniedException ace) {
      return new ArrayList<Query>();
    }
  }

  public String getCurrentUserId() { return Util.getPortalRequestContext().getRemoteUser();}

  public List<Node> getSharedQueries() throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    QueryService queryService = getApplicationComponent(QueryService.class);
    String userId = pcontext.getRemoteUser();
    return queryService.getSharedQueries(userId, WCMCoreUtils.getSystemSessionProvider());
  }

  //public List<Node> getSharedQueries() { return sharedQueries_; }

  public void activate() throws Exception { }

  public void deActivate() throws Exception { }

  public void setIsQuickSearch(boolean isQuickSearch) { isQuickSearch_ = isQuickSearch; }

  static public class ExecuteActionListener extends EventListener<UISavedQuery> {
    public void execute(Event<UISavedQuery> event) throws Exception {
      UISavedQuery uiQuery = event.getSource();
      UIJCRExplorer uiExplorer = uiQuery.getAncestorOfType(UIJCRExplorer.class);
      String wsName = uiQuery.getAncestorOfType(UIJCRExplorer.class).getCurrentWorkspace();
      UIApplication uiApp = uiQuery.getAncestorOfType(UIApplication.class);
      QueryService queryService = uiQuery.getApplicationComponent(QueryService.class);
      String queryPath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIComponent uiSearch = null;
      UISearchResult uiSearchResult = null;
      if(uiQuery.isQuickSearch_) {
        uiSearch = uiExplorer.getChild(UIWorkingArea.class).getChild(UIDocumentWorkspace.class);
        uiSearchResult = ((UIDocumentWorkspace)uiSearch).getChild(UISearchResult.class);
      } else {
        uiSearch = uiQuery.getParent();
        uiSearchResult = ((UIECMSearch)uiSearch).getChild(UISearchResult.class);
        ((UIECMSearch)uiSearch).setSelectedTab(uiSearchResult.getId());
      }
      Query query = null;
      QueryResult queryResult = null;
      try {
        query = queryService.getQuery(queryPath,
                                       wsName,
                                       WCMCoreUtils.getSystemSessionProvider(),
                                       uiQuery.getCurrentUserId());
        queryResult = query.execute();
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UISearchResult.msg.query-invalid", null,
                                                ApplicationMessage.WARNING));
        if(!uiQuery.isQuickSearch_) ((UIECMSearch)uiSearch).setSelectedTab(uiQuery.getId());
        return;
      } finally {
        if(queryResult == null || queryResult.getNodes().getSize() ==0) {
          uiApp.addMessage(new ApplicationMessage("UISavedQuery.msg.not-result-found", null));
          
          if(!uiQuery.isQuickSearch_) ((UIECMSearch)uiSearch).setSelectedTab(uiQuery.getId());
          return;
        }
        uiSearchResult.setQuery(query.getStatement(), wsName, query.getLanguage(), true, null);
        uiSearchResult.updateGrid();
      }
      if(uiQuery.isQuickSearch_) {
        ((UIDocumentWorkspace)uiSearch).setRenderedChild(UISearchResult.class);
        UIPopupContainer uiPopup = uiExplorer.getChild(UIPopupContainer.class);
        uiPopup.deActivate();
      }
    }
  }

  static public class EditActionListener extends EventListener<UISavedQuery> {
    public void execute(Event<UISavedQuery> event) throws Exception {
      UISavedQuery uiQuery = event.getSource();
      String userName = Util.getPortalRequestContext().getRemoteUser();
      QueryService queryService = uiQuery.getApplicationComponent(QueryService.class);
      String queryPath = event.getRequestContext().getRequestParameter(OBJECTID);
      Query query = queryService.getQueryByPath(queryPath,
                                                userName,
                                                WCMCoreUtils.getSystemSessionProvider());
      uiQuery.initPopupEditForm(query);
      if(!uiQuery.isQuickSearch_) {
        UIECMSearch uiECSearch = uiQuery.getParent();
        uiECSearch.setSelectedTab(uiQuery.getId());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiECSearch);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiQuery.getParent());
      }
    }
  }

  static public class DeleteActionListener extends EventListener<UISavedQuery> {
    public void execute(Event<UISavedQuery> event) throws Exception {
      UISavedQuery uiQuery = event.getSource();
      String userName = Util.getPortalRequestContext().getRemoteUser();
      QueryService queryService = uiQuery.getApplicationComponent(QueryService.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      queryService.removeQuery(path, userName);
      uiQuery.updateGrid(uiQuery.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiQuery);
    }
  }
  
  public class QueryData {

    private String language_;
    private String statement_;
    private String storedQueryPath_;
    
    public QueryData(Query query) {
      language_ = query.getLanguage();
      statement_ = query.getStatement();
      try {
        storedQueryPath_ = query.getStoredQueryPath();
      } catch (RepositoryException e) {
        storedQueryPath_ = "";
      }
    }

    public String getLanguage() {
      return language_;
    }

    public void setLanguage(String language) {
      language_ = language;
    }

    public String getStatement() {
      return statement_;
    }

    public void setStatement(String statement) {
      statement_ = statement;
    }

    public String getStoredQueryPath() {
      return storedQueryPath_;
    }

    public void setStoredQueryPath(String storedQueryPath) {
      storedQueryPath_ = storedQueryPath;
    }
  }
}
