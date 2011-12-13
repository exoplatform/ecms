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
import java.util.Collection;
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

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
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
    template = "app:/groovy/webui/component/admin/unlock/UILockNodeList.gtmpl",
    events = {
        @EventConfig(listeners = UILockNodeList.UnLockActionListener.class)
    }
)
public class UILockNodeList extends UIPagingGridDecorator {
  final static public String[] ACTIONS = {};
  final static public String ST_EDIT = "EditUnLockForm";

  private static final String  LOCK_QUERY = "select * from nt:base "
                                              + "where jcr:mixinTypes = 'mix:lockable' "
                                              + "order by exo:dateCreated DESC";

  public UILockNodeList() throws Exception {
    getUIPageIterator().setId("LockNodeListIterator");
    
  }

  public String[] getActions() { return ACTIONS ; }

  public void refresh(int currentPage) throws Exception {
    ListAccess<Object> lockedNodeList = new ListAccessImpl<Object>(Object.class,
                                                                   NodeLocation.getLocationsByNodeList(getAllLockedNodes()));
    LazyPageList<Object> pageList = new LazyPageList<Object>(lockedNodeList,
                                                             getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public List getLockedNodeList() throws Exception {
    return NodeLocation.getNodeListByLocationList(getUIPageIterator().getCurrentPageData());
  }

  public List<Node> getAllLockedNodes() throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);

    ManageableRepository manageRepository = repositoryService.getCurrentRepository();
    RepositoryEntry repo = manageRepository.getConfiguration();

    List<Node> listLockedNodes = new ArrayList<Node>();
    QueryManager queryManager = null;
    Session session = null;
    String queryStatement = LOCK_QUERY;
    Query query = null;
    QueryResult queryResult = null;

    for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
      session = WCMCoreUtils.getSystemSessionProvider().getSession(ws.getName(), manageRepository);
      queryManager = session.getWorkspace().getQueryManager();
      query = queryManager.createQuery(queryStatement, Query.SQL);
      queryResult = query.execute();
      for(NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
        Node itemNode = iter.nextNode();
        if (!Utils.isInTrash(itemNode) && itemNode.isLocked()) {
          listLockedNodes.add(itemNode);
        }
      }
    }

    return listLockedNodes;
  }

  static public class UnLockActionListener extends EventListener<UILockNodeList> {
    private List<String> getGroups(String userId) throws Exception {
      PortalContainer  manager = PortalContainer.getInstance() ;
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
    ((ComponentRequestLifecycle) organizationService).startRequest(manager);
    List<String> groupList = new ArrayList<String> ();
    //Collection<?> groups = organizationService.getGroupHandler().findGroupsOfUser(userId);
    Collection<?> gMembership = organizationService.getMembershipHandler().findMembershipsByUser(userId);
    Object[] objects = gMembership.toArray();
    for(int i = 0; i < objects.length; i ++ ){
      Membership member = (Membership)objects[i];
      groupList.add(member.getMembershipType()+":"+member.getGroupId());
    }
    return groupList;
  }
    public void execute(Event<UILockNodeList> event) throws Exception {
      UIUnLockManager uiUnLockManager = event.getSource().getParent();
      UIApplication uiApp = uiUnLockManager.getAncestorOfType(UIApplication.class);
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      RepositoryService repositoryService = uiUnLockManager.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageRepository = repositoryService.getCurrentRepository();
      Session session = null;
      Node lockedNode = null;
      RepositoryEntry repo = repositoryService.getCurrentRepository().getConfiguration();
      for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
        session = WCMCoreUtils.getUserSessionProvider().getSession(ws.getName(), manageRepository);
        try {
          lockedNode = (Node) session.getItem(nodePath);
          if ((lockedNode != null) && !Utils.isInTrash(lockedNode)) break;
        } catch (PathNotFoundException e) {
          continue;
        } catch (AccessDeniedException accessDeniedException) {
          continue;
        }
      }

      if (lockedNode == null) {
        Object[] args = { nodePath };
        ApplicationMessage msg = new ApplicationMessage("UILockNodeList.msg.access-denied-exception",
                                                        args,
                                                        ApplicationMessage.WARNING);
        msg.setArgsLocalized(false);
        uiApp.addMessage(msg);
        uiUnLockManager.refresh();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;
      }
      LockService lockService = WCMCoreUtils.getService(LockService.class);
      String remoteUser = lockedNode.getSession().getUserID();
      List<String> authenticatedGroups = lockService.getAllGroupsOrUsersForLock();
      List<String> memberShips = getGroups(remoteUser);
      boolean isAuthenticated =false;
      for (String group: authenticatedGroups) {
        if (memberShips.contains(group)){
        isAuthenticated=true;
        break;
        }
      }
      UserACL userACLService = WCMCoreUtils.getService(UserACL.class);
      if (isAuthenticated || remoteUser.equals(userACLService.getSuperUser())) {
        session = WCMCoreUtils.getSystemSessionProvider()
                              .getSession(lockedNode.getSession().getWorkspace().getName(),
                                          (ManageableRepository) lockedNode.getSession()
                                                                           .getRepository());
        lockedNode = (Node)session.getItem(lockedNode.getPath());
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
        
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;
      } catch(VersionException versionException) {
        Object[] args = {lockedNode.getName()};
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-unlock-node-is-checked-in", args,
            ApplicationMessage.WARNING));
        
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
        return;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        
      }
      uiUnLockManager.refresh();
      uiUnLockManager.getChild(UILockNodeList.class).setRendered(true);
      uiUnLockManager.getChild(UILockHolderContainer.class).setRendered(false);
    }
  }
}
