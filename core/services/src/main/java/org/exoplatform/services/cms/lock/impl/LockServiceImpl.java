/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
   * {@inheritDoc}
   */
  @Override
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
  @Override
  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    return settingLockList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (!settingLockList.contains(groupsOrUsers)) settingLockList.add(groupsOrUsers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (settingLockList.contains(groupsOrUsers)) settingLockList.remove(groupsOrUsers);
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
          Node node = (Node)session.getItem(nodePath);
          node.unlock();
          node.removeMixin("mix:lockable");
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
        if(lockedNode.isLocked()) {
          lockedNode.unlock();
        }  
        lockedNode.removeMixin("mix:lockable");
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
}
