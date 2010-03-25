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
package org.exoplatform.services.deployment;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public abstract class DeploymentPlugin implements ComponentPlugin {
  
  private String name;  
  private String desc;      
  
  /**
   * This method used to deploy data from sourcePath to target that describe in 
   * <code>DeploymentDescriptor</code>.
   * 
   * @throws Exception the exception
   */
  public abstract void deploy(SessionProvider sessionProvider) throws Exception;
  
  /* (non-Javadoc)
   * @see org.exoplatform.container.component.ComponentPlugin#getName()
   */
  public String getName() { return name;  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.container.component.ComponentPlugin#setName(java.lang.String)
   */
  public void setName(String s) { this.name = s; }
  
  /* (non-Javadoc)
   * @see org.exoplatform.container.component.ComponentPlugin#getDescription()
   */
  public String getDescription() { return desc; }
  
  /* (non-Javadoc)
   * @see org.exoplatform.container.component.ComponentPlugin#setDescription(java.lang.String)
   */
  public void setDescription(String s) { this.desc = s; }  

}
