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
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
  private static final Log LOG = ExoLogger.getLogger(LockService.class);

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
   * @param usersOrGroups
   */
  public void addLockGroupsOrUsersPlugin(ComponentPlugin plugin) {
    if (plugin instanceof LockGroupsOrUsersPlugin)
      lockGroupsOrUsersPlugin_.add((LockGroupsOrUsersPlugin)plugin);
  }

  public List<String> getPreSettingLockList(){
    return preSettingLockList;
  }

  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    return settingLockList;
  }

  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (!settingLockList.contains(groupsOrUsers)) settingLockList.add(groupsOrUsers);
  }

  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    if (settingLockList.contains(groupsOrUsers)) settingLockList.remove(groupsOrUsers);
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    try {
      settingLockList.clear();
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
    }catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("===>>>>Exception when init LockService", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }
}
