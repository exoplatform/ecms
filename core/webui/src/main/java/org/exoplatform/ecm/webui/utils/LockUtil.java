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
package org.exoplatform.ecm.webui.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.Lock;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.lock.LockManagerImpl;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 15, 2008 11:17:13 AM
 */
public class LockUtil {

  public static ExoCache getLockCache() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    CacheService cacheService = (CacheService)container.getComponentInstanceOfType(CacheService.class);
    return cacheService.getCacheInstance(LockManagerImpl.class.getName());
  }

  @SuppressWarnings("unchecked")
  public static void keepLock(Lock lock) throws Exception {
    ExoCache lockcache = getLockCache();
    String key = createLockKey(lock.getNode());
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(key,lock.getLockToken());
    lockcache.put(userId,lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId) throws Exception {
    ExoCache lockcache = getLockCache();
    String keyRoot = createLockKey(lock.getNode(), userId);
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lock.getLockToken());
    lockcache.put(userId, lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId, String lockToken) throws Exception {
    ExoCache lockcache = getLockCache();
    String keyRoot = createLockKey(lock.getNode(), userId);
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lockToken);
    lockcache.put(userId, lockedNodesInfo);
  }

  @SuppressWarnings("unchecked")
  public static void removeLock(Node node) throws Exception {
    ExoCache lockcache = getLockCache();
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) return;
    lockedNodesInfo.remove(key);
  }

  @SuppressWarnings("unchecked")
  public static void changeLockToken(Node oldNode, Node newNode) throws Exception {
    ExoCache lockcache = getLockCache();
    String newKey = createLockKey(newNode);
    String oldKey = createLockKey(oldNode);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.remove(oldKey) ;
    lockedNodesInfo.put(newKey,newNode.getLock().getLockToken());
    lockcache.put(userId,lockedNodesInfo);
  }

  @SuppressWarnings("unchecked")
  public static void changeLockToken(String srcPath, Node newNode) throws Exception {
    ExoCache lockcache = getLockCache();
    String newKey = createLockKey(newNode);
    String oldKey = getOldLockKey(srcPath, newNode);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    if(lockedNodesInfo.containsKey(oldKey)) {
      lockedNodesInfo.put(newKey, lockedNodesInfo.get(oldKey));
      lockedNodesInfo.remove(oldKey);
    }
    lockcache.put(userId, lockedNodesInfo);
  }

  @SuppressWarnings("unchecked")
  public static String getLockTokenOfUser(Node node) throws Exception {
    ExoCache lockcache = getLockCache();
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
    if ((lockedNodesInfo != null) && (lockedNodesInfo.get(key) != null)) {
      return lockedNodesInfo.get(key);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static String getLockToken(Node node) throws Exception {
    ExoCache lockcache = getLockCache();
    String key = createLockKey(node);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = (Map<String,String>)lockcache.get(userId);
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
        lockedNodesInfo = (Map<String,String>)lockcache.get(permissionBuffer.toString());
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
}
