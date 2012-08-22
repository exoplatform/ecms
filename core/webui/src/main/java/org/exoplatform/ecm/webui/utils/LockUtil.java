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
package org.exoplatform.ecm.webui.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 15, 2008 11:17:13 AM
 */
public class LockUtil {

  public static void keepLock(Lock lock) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String key = createLockKey(lock.getNode());
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(key,lock.getLockToken());
    lockService.putToLockHoding(userId,lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String keyRoot = createLockKey(lock.getNode(), userId);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lock.getLockToken());
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId, String lockToken) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String keyRoot = createLockKey(lock.getNode(), userId);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lockToken);
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  public static void removeLock(Node node) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) return;
    lockedNodesInfo.remove(key);
  }

  public static void changeLockToken(Node oldNode, Node newNode) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String newKey = createLockKey(newNode);
    String oldKey = createLockKey(oldNode);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.remove(oldKey) ;
    lockedNodesInfo.put(newKey,newNode.getLock().getLockToken());
    lockService.putToLockHoding(userId,lockedNodesInfo);
  }

  public static void changeLockToken(String srcPath, Node newNode) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String newKey = createLockKey(newNode);
    String oldKey = getOldLockKey(srcPath, newNode);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
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

  public static String getLockTokenOfUser(Node node) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if ((lockedNodesInfo != null) && (lockedNodesInfo.get(key) != null)) {
      return lockedNodesInfo.get(key);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static String getLockToken(Node node) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
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

  public static String getOldLockKey(String srcPath, Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(session.getUserID()).append(":/:")
          .append(srcPath);
    return buffer.toString();
  }

  public static String createLockKey(Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(userId).append(":/:")
          .append(node.getPath());
    return buffer.toString();
  }

  public static String createLockKey(Node node, String userId) throws Exception {
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

  public static boolean isLocked(Node node) throws Exception {
    if(!node.isLocked()) return false;
    String lockToken = LockUtil.getLockTokenOfUser(node);
    if(lockToken != null) {
      node.getSession().addLockToken(LockUtil.getLockToken(node));
      return false;
    }
    return true;
  }
  
  /**
   * update the lockCache by adding lockToken of all locked nodes for the given membership
   * @param membership
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public static void updateLockCache(String membership) throws Exception {
    ManageableRepository repo = WCMCoreUtils.getRepository();
    Session session = null;
    //get all locked nodes
    for (String ws : repo.getWorkspaceNames()) {
      session = WCMCoreUtils.getSystemSessionProvider().getSession(ws, repo);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("SELECT * FROM mix:lockable order by exo:dateCreated DESC", Query.SQL);
      QueryResult queryResult = query.execute();
      for(NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
        Node itemNode = iter.nextNode();
        //add lockToken of this locked node to the given membership
        if (!Utils.isInTrash(itemNode) && itemNode.isLocked()) {
          String lockToken = getLockToken(itemNode);
          keepLock(itemNode.getLock(), membership, lockToken);
          if (membership.startsWith("*")) {
            String lockTokenString = membership;
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            OrganizationService service = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
            List<MembershipType> memberships = (List<MembershipType>) service.getMembershipTypeHandler().findMembershipTypes();
            for (MembershipType m : memberships) {
              lockTokenString = membership.replace("*", m.getName());
              LockUtil.keepLock(itemNode.getLock(), lockTokenString, lockToken);
            }
          }
        }
      }
    }
  }  
}
