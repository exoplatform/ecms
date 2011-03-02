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
package org.exoplatform.services.cms.impl;

import org.exoplatform.container.component.ComponentPlugin;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 27, 2009  
 * 9:14:45 AM
 */
public class DMSConfiguration implements Startable {

  private DMSRepositoryConfiguration dmsConfig;

  /**
   * Get DMS configuration with specific repository. 
   * @return
   */
  public DMSRepositoryConfiguration getConfig() {
    return dmsConfig;

  }

  /**
   * This method will add more plugin.
   * @param plugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    if (plugin instanceof DMSRepositoryConfiguration) {
      dmsConfig = (DMSRepositoryConfiguration)plugin;
    }
  }

  /**
   * This method will create new repository 
   * @param plugin              plugin name
   */
  public void initNewRepo(DMSRepositoryConfiguration plugin) {
    dmsConfig = plugin;
  }

  public void start() {
    // TODO Auto-generated method stub
  }

  public void stop() {
    // TODO Auto-generated method stub
  }

}
