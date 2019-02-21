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
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
public class UIQueriesList extends UIPagingGridDecorator {
  
  private static final Log LOG  = ExoLogger.getLogger(UIQueriesList.class.getName());

  final static public String[] ACTIONS = {"AddQuery"} ;
  final static public String ST_ADD = "AddQueryForm" ;
  final static public String ST_EDIT = "EditQueryForm" ;

  public UIQueriesList() throws Exception {
    getUIPageIterator().setId("QueriesListIterator");
  }

  public String[] getActions() { return ACTIONS ; }

  public void refresh(int currentPage) throws Exception {
    ListAccess<Object> sharedQueryList = new ListAccessImpl<Object>(Object.class,
                                                                    NodeLocation.getLocationsByNodeList(getAllSharedQueries()));
    LazyPageList<Object> pageList = new LazyPageList<Object>(sharedQueryList,
                                                             getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public boolean canEditNode(Node node) {
    SessionProvider sProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      ManageableRepository manageableRepository = ((ManageableRepository)node.getSession().getRepository());
      Session session = sProvider.getSession(node.getSession().getWorkspace().getName(), manageableRepository);
      session.checkPermission(node.getPath(), PermissionType.SET_PROPERTY);
    }catch (Exception e){
      return false;
    }
    return true;
  }
  public boolean canRemoveNode(Node node) {
    SessionProvider sProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      ManageableRepository manageableRepository = ((ManageableRepository)node.getSession().getRepository());
      Session session = sProvider.getSession(node.getSession().getWorkspace().getName(), manageableRepository);
      session.checkPermission(node.getPath(), PermissionType.REMOVE);
    }catch (Exception e){
      return false;
    }
    return true;
  }
  public List getQueryList() throws Exception { 
    return NodeLocation.getNodeListByLocationList(getUIPageIterator().getCurrentPageData()); 
  }

  @SuppressWarnings("unchecked")
  public List<Node> getAllSharedQueries() throws Exception {
    QueryService queryService = getApplicationComponent(QueryService.class) ;
    List<Node> queries = queryService.getSharedQueries(WCMCoreUtils.getSystemSessionProvider()) ;
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
      UIApplication uiApp = uiQueriesList.getAncestorOfType(UIApplication.class);
      UIQueriesManager uiQueriesMan = event.getSource().getParent() ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      String queryName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      QueryService queryService = event.getSource().getApplicationComponent(QueryService.class) ;
      try {
        queryService.removeQuery(queryName, userName) ;
      } catch (PathNotFoundException pe) {
        uiApp.addMessage(new ApplicationMessage("UIQueriesList.msg.query-not-existed",
                                                null,ApplicationMessage.WARNING));
        return;
      } catch (Exception ex) {
        if (LOG.isErrorEnabled()) {
          LOG.error("cannot remove the query", ex);
        }
        uiApp.addMessage(new ApplicationMessage("UIQueriesList.msg.can-not-remove",
                                                null,ApplicationMessage.ERROR));
        return;
      }
      
      event.getSource().refresh(uiQueriesList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiQueriesMan) ;
    }
  }
}
