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
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.comparator.NodeOwnerComparator;
import org.exoplatform.ecm.webui.comparator.NodeTitleComparator;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
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
                     @EventConfig(listeners = UILockNodeList.UnLockActionListener.class),
                     @EventConfig(listeners = UILockNodeList.SortActionListener.class)
                 }
    )
public class UILockNodeList extends UIPagingGridDecorator {
  final static public String[] ACTIONS = {};
  final static public String ST_EDIT = "EditUnLockForm";
  private Preference preferences_;

  private static final String LOCK_QUERY = "select * from mix:lockable where jcr:lockOwner IS NOT NULL " +
      "order by exo:dateCreated DESC";
  private String typeSort_ = NodetypeConstant.SORT_BY_NODETYPE;
  private String sortOrder_ = Preference.BLUE_UP_ARROW;
  private String order_ = Preference.ASCENDING_ORDER;

  public static final String SORT_BY_NODENAME = "Alphabetic" ;
  public static final String SORT_BY_NODEOWNER= "Owner" ;


  public String getTypeSort() { return typeSort_; }

  public void setTypeSort(String typeSort) {
    typeSort_ = typeSort;
  }

  public String getSortOrder() { return sortOrder_; }

  public void setSortOrder(String sortOrder) {
    sortOrder_ = sortOrder;
  }

  public String getOrder() { return order_; }

  public void setOrder(String order) {
    order_ = order;
  }

  public void setPreferences(Preference preference) {this.preferences_ = preference; }

  public UILockNodeList() throws Exception {
    getUIPageIterator().setId("LockNodeListIterator");

  }

  public String[] getActions() { return ACTIONS ; }

  public void refresh(int currentPage) throws Exception {
    if (!getUIPageIterator().isJustPaginated()) {
      PageList pageList = new UILockedNodePageList(LOCK_QUERY, getUIPageIterator().getItemsPerPage(), currentPage);
      getUIPageIterator().setPageList(pageList);
    }
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
    getUIPageIterator().setJustPaginated(false);
  }

  public List getLockedNodeList() throws Exception {
    return sort(NodeLocation.getNodeListByLocationList(getUIPageIterator().getCurrentPageData()));
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

  private List<Node> sort(List<Node> childrenList) {
    if (SORT_BY_NODENAME.equals(this.getTypeSort()))
      Collections.sort(childrenList, new NodeTitleComparator(this.getOrder())) ;
    else if(SORT_BY_NODEOWNER.equals(this.getTypeSort()))
      Collections.sort(childrenList, new NodeOwnerComparator(this.getOrder())) ;
    return childrenList;

  }

  static public class SortActionListener extends EventListener<UILockNodeList> {
    public void execute(Event<UILockNodeList> event) throws Exception {
      UILockNodeList uicomp = event.getSource();
      UIUnLockManager uiUnLockManager = event.getSource().getParent();
      UIApplication uiApp = uiUnLockManager.getAncestorOfType(UIApplication.class);


      try {
        String sortParam = event.getRequestContext().getRequestParameter(OBJECTID) ;
        String[] array = sortParam.split(";");
        String order = Preference.ASCENDING_ORDER.equals(array[0].trim()) || !array[1].trim().equals(uicomp.getTypeSort()) ?
                                                                                                                            Preference.BLUE_DOWN_ARROW : Preference.BLUE_UP_ARROW;

        String prefOrder = Preference.ASCENDING_ORDER.equals(array[0].trim()) || !array[1].trim().equals(uicomp.getTypeSort())?
                                                                                                                               Preference.ASCENDING_ORDER : Preference.DESCENDING_ORDER;
        uicomp.setSortOrder(order);
        uicomp.setTypeSort(array[1]);
        uicomp.setOrder(prefOrder);
        uiUnLockManager.refresh();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class UnLockActionListener extends EventListener<UILockNodeList> {
    private List<String> getCurrentUserMemberships() throws Exception {

      List<String> groupList = new ArrayList<String> ();
      
      Collection<MembershipEntry> memberships =
          ConversationState.getCurrent().getIdentity().getMemberships();
      for(MembershipEntry entry: memberships){
        groupList.add(entry.getMembershipType() + ":" + entry.getGroup());
      }
      
      return groupList;
    }

    public void execute(Event<UILockNodeList> event) throws Exception {
      WebuiRequestContext rContext = event.getRequestContext();
      UIUnLockManager uiUnLockManager = event.getSource().getParent();
      UIApplication uiApp = uiUnLockManager.getAncestorOfType(UIApplication.class);
      String nodePath = rContext.getRequestParameter(OBJECTID);
      RepositoryService repositoryService = uiUnLockManager.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageRepository = repositoryService.getCurrentRepository();
      Session session = null;
      Node lockedNode = null;
      UserACL userACLService = WCMCoreUtils.getService(UserACL.class);
      String remoteUser = rContext.getRemoteUser();
      boolean isAuthenticated = remoteUser.equals(userACLService.getSuperUser());
      if (!isAuthenticated) {
        LockService lockService = WCMCoreUtils.getService(LockService.class);
        List<String> authorizedMemberships = lockService.getAllGroupsOrUsersForLock();
        List<String> loginedUserMemberShips = getCurrentUserMemberships();
        Set<String> loginedUserGroups = ConversationState.getCurrent().getIdentity().getGroups();
        for (String authorizedMembership: authorizedMemberships) {
          if ((authorizedMembership.startsWith("*") &&
              loginedUserGroups.contains(StringUtils.substringAfter(authorizedMembership, ":")))
              || loginedUserMemberShips.contains(authorizedMembership)) {
            isAuthenticated=true;
            break;
          }
        }
      }

      RepositoryEntry repo = repositoryService.getCurrentRepository().getConfiguration();
      for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
        if (isAuthenticated) {
          session = WCMCoreUtils.getSystemSessionProvider().getSession(ws.getName(), manageRepository);
        }else {
          session = WCMCoreUtils.getSystemSessionProvider().getSession(ws.getName(), manageRepository);
        }
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

      if (isAuthenticated ) {
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
        uiApp.addMessage(new ApplicationMessage("UILockNodeList.msg.can-not-unlock-node", null, ApplicationMessage.WARNING));
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
