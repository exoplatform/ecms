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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.search.UIContentNameSearch;
import org.exoplatform.ecm.webui.component.explorer.search.UIECMSearch;
import org.exoplatform.ecm.webui.component.explorer.search.UISavedQuery;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.ecm.webui.component.explorer.search.UISimpleSearch;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 23, 2009
 * 4:01:53 AM
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/sidebar/UISavedSearches.gtmpl",
    events = {
        @EventConfig(listeners = UISavedSearches.ExecuteActionListener.class),
        @EventConfig(listeners = UISavedSearches.AdvanceSearchActionListener.class),
        @EventConfig(listeners = UISavedSearches.SavedQueriesActionListener.class)
    }
)
public class UISavedSearches extends UIComponent {

  public final static String ACTION_TAXONOMY = "exo:taxonomyAction";
  public final static String EXO_TARGETPATH = "exo:targetPath";
  public final static String EXO_TARGETWORKSPACE = "exo:targetWorkspace";

  private String queryPath;
  
  public UISavedSearches() throws Exception {
  }

  public List<Object> queryList() throws Exception {
    List<Object> objectList = new ArrayList<Object>();
    List<Node> sharedQueries = getSharedQueries();
    if(!sharedQueries.isEmpty()) {
      for(Node node : sharedQueries) {
        objectList.add(new NodeData(node));
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

  public String getCurrentUserId() { return Util.getPortalRequestContext().getRemoteUser();}

  public List<Query> getQueries() throws Exception { 
    QueryService queryService = getApplicationComponent(QueryService.class);
    try {
      return  queryService.getQueries(getCurrentUserId(), WCMCoreUtils.getUserSessionProvider());
    } catch(AccessDeniedException ace) {
      return new ArrayList<Query>();
    }
  }

  public List<Node> getSharedQueries() throws Exception { 
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    QueryService queryService = getApplicationComponent(QueryService.class);
    String userId = pcontext.getRemoteUser();
    return queryService.getSharedQueries(userId, WCMCoreUtils.getSystemSessionProvider());
  }

  public void setQueryPath(String queryPath) throws Exception {
    this.queryPath = queryPath;
  }

  public String getQueryPath() throws Exception {
    return this.queryPath;
  }

  static public class ExecuteActionListener extends EventListener<UISavedSearches> {
    public void execute(Event<UISavedSearches> event) throws Exception {
      UISavedSearches uiSavedSearches = event.getSource();
      UIJCRExplorer uiExplorer = uiSavedSearches.getAncestorOfType(UIJCRExplorer.class);
      String queryPath = event.getRequestContext().getRequestParameter(OBJECTID);
      uiSavedSearches.setQueryPath(queryPath);

      uiExplorer.setPathToAddressBar(Text.unescapeIllegalJcrChars(
                                                           uiExplorer.filterPath(queryPath)));
      String wsName = uiSavedSearches.getAncestorOfType(UIJCRExplorer.class).getCurrentWorkspace();
      UIApplication uiApp = uiSavedSearches.getAncestorOfType(UIApplication.class);
      QueryService queryService = uiSavedSearches.getApplicationComponent(QueryService.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIComponent uiSearch = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      UIDrivesArea uiDrivesArea = uiWorkingArea.getChild(UIDrivesArea.class);
      UISearchResult uiSearchResult = ((UIDocumentWorkspace)uiSearch).getChild(UISearchResult.class);
      Query query = null;
      try {
        query = queryService.getQuery(queryPath,
                                       wsName,
                                       WCMCoreUtils.getSystemSessionProvider(),
                                       uiSavedSearches.getCurrentUserId());
        query.execute();
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UISearchResult.msg.query-invalid", null,
                                                ApplicationMessage.WARNING));
      } finally {
        uiSearchResult.setQuery(query.getStatement(), wsName, query.getLanguage(), false, null);
        uiSearchResult.updateGrid();
      }   
      if (uiDrivesArea != null) uiDrivesArea.setRendered(false);
      uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);      
      ((UIDocumentWorkspace)uiSearch).setRenderedChild(UISearchResult.class);
    }
  }

  static public class AdvanceSearchActionListener extends EventListener<UISavedSearches> {
    public void execute(Event<UISavedSearches> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIECMSearch uiECMSearch = event.getSource().createUIComponent(UIECMSearch.class, null, null);
      UIContentNameSearch contentNameSearch = uiECMSearch.findFirstComponentOfType(UIContentNameSearch.class);
      String currentNodePath = uiJCRExplorer.getCurrentNode().getPath();
      contentNameSearch.setLocation(currentNodePath);
      UISimpleSearch uiSimpleSearch = uiECMSearch.findFirstComponentOfType(UISimpleSearch.class);
      uiSimpleSearch.getUIFormInputInfo(UISimpleSearch.NODE_PATH).setValue(currentNodePath);
      UIPopupContainer.activate(uiECMSearch, 700, 500, false);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }

  static public class SavedQueriesActionListener extends EventListener<UISavedSearches> {
    public void execute(Event<UISavedSearches> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UISavedQuery uiSavedQuery = event.getSource().createUIComponent(UISavedQuery.class, null, null);
      uiSavedQuery.setIsQuickSearch(true);
      uiSavedQuery.updateGrid(1);
      UIPopupContainer.activate(uiSavedQuery, 700, 400);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }

  public class QueryData {

    private String storedQueryPath_;
    
    public QueryData(Query query) {
      try {
        storedQueryPath_ = query.getStoredQueryPath();
      } catch (RepositoryException e) {
        storedQueryPath_ = "";
      }
    }

    public String getStoredQueryPath() {
      return storedQueryPath_;
    }

    public void setStoredQueryPath(String storedQueryPath) {
      storedQueryPath_ = storedQueryPath;
    }
  }
  
  public class NodeData {
    private String path_;
    private String name_;
    
    public NodeData(Node node) throws RepositoryException {
      this.path_ = node.getPath();
      this.name_ = node.getName();
    }

    public String getPath() {
      return path_;
    }

    public void setPath(String path) {
      path_ = path;
    }

    public String getName() {
      return name_;
    }

    public void setName(String name) {
      name_ = name;
    }
    
  }
}
