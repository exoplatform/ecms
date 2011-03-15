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
import java.util.Iterator;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 14, 2009
 * 3:01:45 PM
 */
public class LockGroupsOrUsersPlugin extends BaseComponentPlugin {

  private InitParams params_;

  public LockGroupsOrUsersPlugin(InitParams params) {
    params_ = params;
  }

  /**
   * Init tag permission in repository.
   */
  @SuppressWarnings("unchecked")
  public List<String> initGroupsOrUsers() throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator();
    LockGroupsOrUsersConfig lockGroupsOrUsersConfig;
    while(it.hasNext()) {
      lockGroupsOrUsersConfig = (LockGroupsOrUsersConfig)it.next().getObject();
      return lockGroupsOrUsersConfig.getSettingLockList();
    }
    return new ArrayList<String>();
  }

}
