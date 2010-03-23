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
package org.exoplatform.ecm.webui.component.admin.queries;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006  
 * 11:30:17 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/queries/UIQueriesList.gtmpl",
    events = {
        @EventConfig(listeners = UIQueriesList.AddQueryActionListener.class),
        @EventConfig(listeners = UIQueriesList.EditActionListener.class),
        @EventConfig(listeners = UIQueriesList.DeleteActionListener.class, confirm = "UIQueriesList.msg.confirm-delete")
    }
)
public class UIQueriesList extends UIComponentDecorator {

  final static public String[] ACTIONS = {"AddQuery"} ;
  final static public String ST_ADD = "AddQueryForm" ;
  final static public String ST_EDIT = "EditQueryForm" ;
  private UIPageIterator uiPageIterator_ ;
  
  public UIQueriesList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "QueriesListIterator");
    setUIComponent(uiPageIterator_) ;
  }

  public String[] getActions() { return ACTIONS ; }
  
  public void updateQueriesGrid(int currentPage) throws Exception {
    PageList pageList = new ObjectPageList(getAllSharedQueries(), 10) ;
    uiPageIterator_.setPageList(pageList) ;
    if(currentPage > getUIPageIterator().getAvailablePage())
      uiPageIterator_.setCurrentPage(currentPage-1);
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_ ; }
  
  public List getQueryList() throws Exception { return uiPageIterator_.getCurrentPageData() ; } 
  
  @SuppressWarnings("unchecked")
  public List<Node> getAllSharedQueries() throws Exception {
    QueryService queryService = getApplicationComponent(QueryService.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    List<Node> queries = queryService.getSharedQueries(repository, 
        SessionProviderFactory.createSystemProvider()) ;
    Collections.sort(queries, new QueryComparator()) ;
    return queries ;
  }
  
  static public class QueryComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        String name1 = ((Node) o1).getName() ;
        String name2 = ((Node) o2).getName() ;
        return name1.compareToIgnoreCase(name2) ;
      } catch(Exception e) {
        return 0;
      }
    }
  }
  
  static public class AddQueryActionListener extends EventListener<UIQueriesList> {
    public void execute(Event<UIQueriesList> event) throws Exception {
      UIQueriesManager uiQueriesMan = event.getSource().getParent() ;
      uiQueriesMan.removeChildById(UIQueriesList.ST_EDIT) ;
      uiQueriesMan.initFormPopup(UIQueriesList.ST_ADD) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiQueriesMan) ;
    }
  }
  
  static public class EditActionListener extends EventListener<UIQueriesList> {
    public void execute(Event<UIQueriesList> event) throws Exception {
      UIQueriesManager uiQueriesMan = event.getSource().getParent() ;
      uiQueriesMan.removeChildById(UIQueriesList.ST_ADD) ;
      uiQueriesMan.initFormPopup(UIQueriesList.ST_EDIT ) ;
      String queryPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIQueriesForm uiForm = uiQueriesMan.findFirstComponentOfType(UIQueriesForm.class) ;
      String queryName = queryPath.substring(queryPath.lastIndexOf("/") + 1) ;
      uiForm.update(queryName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiQueriesMan) ;
    }
  }
  
  static public class DeleteActionListener extends EventListener<UIQueriesList> {
    public void execute(Event<UIQueriesList> event) throws Exception {
      UIQueriesList uiQueriesList = event.getSource();
      UIQueriesManager uiQueriesMan = event.getSource().getParent() ;
      String repository = uiQueriesMan.getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      String queryName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      QueryService queryService = event.getSource().getApplicationComponent(QueryService.class) ;
      queryService.removeQuery(queryName, userName, repository) ;
      event.getSource().updateQueriesGrid(uiQueriesList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiQueriesMan) ;
    }
  }
}