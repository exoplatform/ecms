/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

  /** The id. */
  protected final String id;

  /** The name. */
  protected final String name;

  /** The auth URL. */
  protected final String authURL;

  /** The service name. */
  protected final String serviceName;

  /**
   * Instantiates a new provider info.
   *
   * @param id the id
   * @param name the name
   * @param authURL the auth URL
   * @param serviceName the service name
   */
  public ProviderInfo(String id, String name, String authURL, String serviceName) {
    super();
    this.id = id;
    this.name = name;
    this.authURL = authURL;
    this.serviceName = serviceName;
  }

  /**
   * Instantiates a new provider info.
   *
   * @param id the id
   * @param name the name
   * @param authURL the auth URL
   */
  public ProviderInfo(String id, String name, String authURL) {
    this(id, name, authURL, name);
  }

  /**
   * Instantiates a new provider info.
   *
   * @param user the user
   * @throws CloudDriveException the cloud drive exception
   */
  public ProviderInfo(CloudUser user) throws CloudDriveException {
    this(user.getProvider().getId(),
         user.getProvider().getName(),
         user.getProvider().getAuthURL(),
         user.getServiceName());
  }

  /**
   * Instantiates a new provider info.
   *
   * @param provider the provider
   * @throws CloudDriveException the cloud drive exception
   */
  public ProviderInfo(CloudProvider provider) throws CloudDriveException {
    this(provider.getId(), provider.getName(), provider.getAuthURL());
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the auth URL.
   *
   * @return the authURL
   */
  public String getAuthURL() {
    return authURL;
  }

  /**
   * Gets the service name.
   *
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

}
