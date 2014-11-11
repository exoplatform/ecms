/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.rest;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;

/**
 * Provider representation that will be returned to clients. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ProviderInfo.java 00000 Nov 9, 2014 pnedonosko $
 * 
 */
public class ProviderInfo {

  protected final String id;

  protected final String name;

  protected final String authURL;

  protected final String serviceName;

  public ProviderInfo(String id, String name, String authURL, String serviceName) {
    super();
    this.id = id;
    this.name = name;
    this.authURL = authURL;
    this.serviceName = serviceName;
  }

  public ProviderInfo(String id, String name, String authURL) {
    this(id, name, authURL, name);
  }

  public ProviderInfo(CloudUser user) throws CloudDriveException {
    this(user.getProvider().getId(),
         user.getProvider().getName(),
         user.getProvider().getAuthURL(),
         user.getServiceName());
  }

  public ProviderInfo(CloudProvider provider) throws CloudDriveException {
    this(provider.getId(), provider.getName(), provider.getAuthURL());
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the authURL
   */
  public String getAuthURL() {
    return authURL;
  }

  /**
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

}
