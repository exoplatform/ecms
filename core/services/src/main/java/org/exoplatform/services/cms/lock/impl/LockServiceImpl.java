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
package org.exoplatform.services.cms.lock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Chien Nguyen
 * chien.nguyen@exoplatform.com
 * Nov 17, 2009
 */

public class LockServiceImpl implements LockService, Startable {

  private List<String> settingLockList = new ArrayList<String>();
  private List<String> preSettingLockList = new ArrayList<String>();
  private List<LockGroupsOrUsersPlugin> lockGroupsOrUsersPlugin_ = new ArrayList<LockGroupsOrUsersPlugin>();
  private static final Log LOG = ExoLogger.getLogger(LockServiceImpl.class.getName());
  private HashMap<String, Map<String, String>> lockHolding = new HashMap<String, Map<String, String>>();

  /**
   * Constructor method
   * @param params
   * @throws Exception
   */
  public LockServiceImpl(InitParams params) throws Exception {
    //group_ = params.getValueParam("group").getValue();
  }

  /**
   * Add new users or groups into lockGroupsOrUsersPlugin_
   * @param plugin
   */
  public void addLockGroupsOrUsersPlugin(ComponentPlugin plugin) {
    if (plugin instanceof LockGroupsOrUsersPlugin)
      lockGroupsOrUsersPlugin_.add((LockGroupsOrUsersPlugin)plugin);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getPreSettingLockList(){
    return preSettingLockList;
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    return settingLockList;
  }

  /**
   * {@inheritDoc}
   */
  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (!settingLockList.contains(groupsOrUsers)) settingLockList.add(groupsOrUsers);
  }

  /**
   * {@inheritDoc}
   */
  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (settingLockList.contains(groupsOrUsers)) settingLockList.remove(groupsOrUsers);
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    lockHolding.clear();
    settingLockList.clear();
    preSettingLockList.clear();
    removeLocks();
    for(LockGroupsOrUsersPlugin plugin : lockGroupsOrUsersPlugin_) {
      try{
        settingLockList.addAll(plugin.initGroupsOrUsers());
        preSettingLockList.addAll(plugin.initGroupsOrUsers());
      }catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can not init lock groups or users: ", e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
    lockHolding.clear();
    settingLockList.clear();
    preSettingLockList.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HashMap<String, Map<String, String>> getLockHolding() {
    return lockHolding;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putToLockHoding(String userId, Map<String, String> lockedNodesInfo) {
    lockHolding.put(userId, lockedNodesInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getLockInformation(String userId) {
    return lockHolding.get(userId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeLocksOfUser(String userId) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    if (LOG.isInfoEnabled()) {
      LOG.info("Removing all locked nodes of user " + userId);
    }
    Map<String,String> lockedNodes = lockHolding.get(userId);
    if(lockedNodes == null || lockedNodes.values().isEmpty()) return;
    try {
      for(Iterator<String> iter = lockedNodes.keySet().iterator(); iter.hasNext();) {
        try {
          //The key structure is built in org.exoplatform.ecm.webui.utils.LockUtil.createLockKey() method
          String key = iter.next();
          String[] temp = key.split(":/:");
          String nodePath = temp[1];
          String[] location = temp[0].split("/::/");
          String workspaceName = location[1] ;
          Session session = sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());
          String lockToken = lockedNodes.get(key);
          session.addLockToken(lockToken);
          Node node =null;
          try {
            node = (Node)session.getItem(nodePath);
          }catch (PathNotFoundException e) {
            if (LOG.isInfoEnabled()) {
              LOG.info("Node " + nodePath + " has been already removed before");
            }
            continue;
          }
          if (!node.isCheckedOut() && node.isNodeType(NodetypeConstant.MIX_VERSIONABLE)) {
            node.checkout();
          }
          if (node.isLocked()) {
            node.unlock();
          }
          if (node.isNodeType("mix:lockable") && node.isCheckedOut()) {
            node.removeMixin("mix:lockable");
          }
          node.save();
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Error while unlocking the locked nodes",e);
          }
        }
      }
      lockedNodes.clear();
    } finally {
      sessionProvider.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeLocks() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Clean all locked nodes in the system");
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    try {
      String wsName = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(wsName, repositoryService.getCurrentRepository());
      String lockQueryStatement = "SELECT * from mix:lockable ORDER BY exo:dateCreated";
      QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(lockQueryStatement, Query.SQL).execute();
      NodeIterator nodeIter = queryResult.getNodes();
      while(nodeIter.hasNext()) {
        Node lockedNode = nodeIter.nextNode();
        //Check to avoid contains some corrupted data in the system which still contains mix:lockable but not locked.
        if (!lockedNode.isCheckedOut() && lockedNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)) {
          lockedNode.checkout();
        }
        if(lockedNode.isLocked()) {
          lockedNode.unlock();
        }
        if (lockedNode.isNodeType("mix:lockable") && lockedNode.isCheckedOut()) {
          lockedNode.removeMixin("mix:lockable");
        }
        lockedNode.save();
      }
    } catch(RepositoryException re) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error while unlocking the locked nodes", re);
      }
    } finally {
      sessionProvider.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLockTokenOfUser(Node node) throws Exception {
    String key = createLockKey(node);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if ((lockedNodesInfo != null) && (lockedNodesInfo.get(key) != null)) {
      return lockedNodesInfo.get(key);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createLockKey(Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(userId).append(":/:")
          .append(node.getPath());
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String createLockKey(Node node, String userId) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    if(userId == null) userId = IdentityConstants.ANONIM;
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(userId).append(":/:")
          .append(node.getPath());
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public String getLockToken(Node node) throws Exception {
    String key = createLockKey(node);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if ((lockedNodesInfo != null) && (lockedNodesInfo.get(key) != null)) {
      return lockedNodesInfo.get(key);
    }
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    OrganizationService service = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    Collection<org.exoplatform.services.organization.Membership>
                        collection = service.getMembershipHandler().findMembershipsByUser(userId);
    String keyPermission;
    for(org.exoplatform.services.organization.Membership membership : collection) {
      StringBuffer permissionBuffer = new StringBuffer();
      permissionBuffer.append(membership.getMembershipType()).append(":").append(membership.getGroupId());
      if ((permissionBuffer != null) && (permissionBuffer.toString().length() > 0)) {
        keyPermission = createLockKey(node, permissionBuffer.toString());
        lockedNodesInfo = lockService.getLockInformation(permissionBuffer.toString());
        if ((lockedNodesInfo != null) && (lockedNodesInfo.get(keyPermission) != null)) {
          return lockedNodesInfo.get(keyPermission);
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeLockToken(String srcPath, Node newNode) throws Exception {
    String newKey = createLockKey(newNode);
    String oldKey = getOldLockKey(srcPath, newNode);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    if(lockedNodesInfo.containsKey(oldKey)) {
      lockedNodesInfo.put(newKey, lockedNodesInfo.get(oldKey));
      lockedNodesInfo.remove(oldKey);
    }
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeLockToken(Node oldNode, Node newNode) throws Exception {
    String newKey = createLockKey(newNode);
    String oldKey = createLockKey(oldNode);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.remove(oldKey) ;
    lockedNodesInfo.put(newKey,newNode.getLock().getLockToken());
    lockService.putToLockHoding(userId,lockedNodesInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOldLockKey(String srcPath, Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(session.getUserID()).append(":/:")
          .append(srcPath);
    return buffer.toString();
  }
}
