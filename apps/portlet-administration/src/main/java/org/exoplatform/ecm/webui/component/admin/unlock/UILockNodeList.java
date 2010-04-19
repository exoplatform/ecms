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
package org.exoplatform.ecm.webui.component.admin.unlock;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
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
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006  
 * 11:30:17 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/unlock/UILockNodeList.gtmpl",
    events = {
        @EventConfig(listeners = UILockNodeList.UnLockActionListener.class)
    }
)
public class UILockNodeList extends UIComponentDecorator {
  final static public String[] ACTIONS = {};
  final static public String ST_EDIT = "EditUnLockForm";
  private UIPageIterator uiPageIterator_;
  
  private static final String LOCK_QUERY = "select * from nt:base where jcr:mixinTypes = 'mix:lockable' order by exo:dateCreated DESC";
  
  public UILockNodeList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "LockNodeListIterator");
    setUIComponent(uiPageIterator_);
  }

  public String[] getActions() { return ACTIONS ; }
  
  public void updateLockedNodesGrid(int currentPage) throws Exception {
    PageList pageList = new ObjectPageList(getAllLockedNodes(), 10);
    uiPageIterator_.setPageList(pageList) ;
    if(currentPage > getUIPageIterator().getAvailablePage())
      uiPageIterator_.setCurrentPage(currentPage-1);
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_ ; }
  
  public List getLockedNodeList() throws Exception { return uiPageIterator_.getCurrentPageData(); } 
  
  public List<Node> getAllLockedNodes() throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String repository = portletPref.getValue(Utils.REPOSITORY, "");
    RepositoryEntry repo = repositoryService.getConfig().getRepositoryConfiguration(repository);
    ManageableRepository manageRepository = repositoryService.getRepository(repository);
    
    List<Node> listLockedNodes = new ArrayList<Node>();
    QueryManager queryManager = null;
    Session session = null;
    String queryStatement = LOCK_QUERY;
    Query query = null;
    QueryResult queryResult = null;
    
    for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
      session = SessionProviderFactory.createSystemProvider().getSession(ws.getName(), manageRepository);
      queryManager = session.getWorkspace().getQueryManager();
      query = queryManager.createQuery(queryStatement, Query.SQL);
      queryResult = query.execute();    
      for(NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {          
        Node itemNode = iter.nextNode();
        if (!itemNode.isNodeType(Utils.EXO_RESTORELOCATION) && itemNode.isLocked()) {
          listLockedNodes.add(itemNode);
        }
      }
    }
        
    return listLockedNodes;
  }
  
  static public class UnLockActionListener extends EventListener<UILockNodeList> {
    public void execute(Event<UILockNodeList> event) throws Exception {
      UIUnLockManager uiUnLockManager = event.getSource().getParent();
      UIApplication uiApp = uiUnLockManager.getAncestorOfType(UIApplication.class);
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);      
      RepositoryService repositoryService = uiUnLockManager.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageRepository = repositoryService.getCurrentRepository();
      Session session = null;
      Node lockedNode = null;
      for(RepositoryEntry repo : repositoryService.getConfig().getRepositoryConfigurations() ) {
        for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
          session = SessionProviderFactory.createSessionProvider().getSession(ws.getName(), manageRepository);
          try {
            lockedNode = (Node) session.getItem(nodePath);
            if ((lockedNode != null) && !lockedNode.isNodeType(Utils.EXO_RESTORELOCATION)) break;
          } catch (PathNotFoundException e) {
            continue;
          } catch (AccessDeniedException accessDeniedException) {
            continue;
          }
        }
      }
      if (lockedNode == null) {
        Object[] args = {nodePath};
        uiApp.addMessage(new ApplicationMessage("UILockNodeList.msg.access-denied-exception", args, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        uiUnLockManager.refresh();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;
      }
      try {
        if(lockedNode.holdsLock()) {
          String lockToken = LockUtil.getLockToken(lockedNode);
          if(lockToken != null) {
            session.addLockToken(lockToken);
          }
          lockedNode.unlock();   
          lockedNode.removeMixin(Utils.MIX_LOCKABLE);
          lockedNode.getSession().save();
          LockUtil.removeLock(lockedNode);
        }
      } catch(LockException le) {
        Object[] args = {lockedNode.getName()};
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-unlock-node", args, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;
      } catch(VersionException versionException) {
        Object[] args = {lockedNode.getName()};
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-unlock-node-is-checked-in", args, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;  
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      }
      uiUnLockManager.refresh();
      uiUnLockManager.getChild(UILockNodeList.class).setRendered(true);
      uiUnLockManager.getChild(UILockHolderContainer.class).setRendered(false);
    }
  }  
}