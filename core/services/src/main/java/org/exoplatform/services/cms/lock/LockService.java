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
package org.exoplatform.services.cms.lock;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Chien Nguyen
 * chien.nguyen@exoplatform.com
 * Nov 17, 2009
 */

public interface LockService {

  public List<String> getPreSettingLockList() throws Exception;

  public List<String> getAllGroupsOrUsersForLock() throws Exception;

  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception;

  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception;
}
