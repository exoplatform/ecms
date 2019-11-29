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
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
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
  
  protected InitParams initParams;
  
  public DeploymentPlugin() {
  }
  
  public DeploymentPlugin(InitParams initParams) {
    this.initParams = initParams;
  }

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

  /**
   * indicates if this plugin will override old data every time server startups 
   */
  public boolean isOverride() {
    ValueParam overrideParam = initParams.getValueParam("override");
    return (overrideParam != null && Boolean.parseBoolean(overrideParam.getValue()));
  }
  
  /**
   * gets name of site in which data is deployed by this plugin
   * @return
   */
  public String getSiteName() {
    ValueParam siteName = initParams.getValueParam("siteName");
    return (siteName == null) ? null : siteName.getValue();
  }
}
