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

import java.util.HashMap;
import java.util.Map;

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
  
  private Map<String, DMSRepositoryConfiguration> dmsConfigMap_ = 
    new HashMap<String, DMSRepositoryConfiguration>();
  
  /**
   * Get DMS configuration with specific repository. 
   * @param repository repository name.
   * @return
   */
  public DMSRepositoryConfiguration getConfig(String repository) {
    DMSRepositoryConfiguration dmsRepositoryConfiguration= dmsConfigMap_.get(repository);
    if (dmsRepositoryConfiguration != null) {
      return dmsRepositoryConfiguration;
    }
//    DMSConfiguration rootConfig = (DMSConfiguration)RootContainer.getInstance().getComponentInstanceOfType(DMSConfiguration.class);
//    if (rootConfig.getConfig(repository) != null) {
    dmsConfigMap_.putAll(getDmsConfigMap());
//    }
    return dmsConfigMap_.get(repository);
  }
  
  /**
   * This method will add more plugin.
   * @param plugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    if(plugin instanceof DMSRepositoryConfiguration) {
      dmsConfigMap_.put(((DMSRepositoryConfiguration)plugin).getRepositoryName(), 
          (DMSRepositoryConfiguration)plugin);
    }
  }
  
  /**
   * This method will create new repository 
   * @param repository          repository name
   * @param plugin              plugin name
   */
  public void initNewRepo(String repository, DMSRepositoryConfiguration plugin) {
    dmsConfigMap_.put(repository, plugin);
  }

  public void start() {
    // TODO Auto-generated method stub
    
  }

  public void stop() {
    // TODO Auto-generated method stub
    
  }

  public Map<String, DMSRepositoryConfiguration> getDmsConfigMap() {
    return dmsConfigMap_;
  }
}
