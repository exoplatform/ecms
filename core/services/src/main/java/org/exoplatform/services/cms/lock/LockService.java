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
package org.exoplatform.services.cms.lock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : Chien Nguyen
 * chien.nguyen@exoplatform.com
 * Nov 17, 2009
 */

/**
 * This service used to manage all the stuff which related to the locking nodes.
 * @author minh_dang
 *
 */
public interface LockService {

  /**
   * Get all pre-setting lock list
   * @return List<String>
   * @throws Exception
   */
  public List<String> getPreSettingLockList() throws Exception;

  /**
   * Get all the identities which allowed to unlock nodes 
   * @return List<String>
   * @throws Exception
   */
  public List<String> getAllGroupsOrUsersForLock() throws Exception;

  /**
   * Allow to add user or group to the locking manager list which allowed to unlock all nodes
   * @param groupsOrUsers User id or group id
   * @throws Exception
   */
  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception;
  
  /**
   * Add new users or groups into lockGroupsOrUsersPlugin_
   * @param usersOrGroups
   */
  public void addLockGroupsOrUsersPlugin(ComponentPlugin plugin);

  /**
   * Remove user or group out of the locking manager list
   * @param groupsOrUsers
   * @throws Exception
   */
  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception;
  
  /**
   * Return a HashMap which keeping all locked nodes informations
   * @return HashMap<String, Map<String, String>
   */
  public HashMap<String, Map<String, String>> getLockHolding();
  
  /**
   * Put all informations of locked node such as locktoken to the Map
   * @param userId It is a key which will be used to get the locked information
   * @param lockedNodesInfo A Map which kept the locked node information
   */
  public void putToLockHoding(String userId, Map<String, String> lockedNodesInfo);
  
  /**
   * Return a Map which kept lock token of node which locked by user
   * @param userId 
   * @return
   */
  public Map<String, String> getLockInformation(String userId);
  
  /**
   * Remove all locked nodes user
   * @param userId User Identity which will be used to remove all locked node which belong to he/she
   */
  public void removeLocksOfUser(String userId);
  
  /**
   * Remove all locked nodes in the system. This function just used in the case server stop working
   */
  public void removeLocks();
}
