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
package org.exoplatform.clouddrive;

import javax.jcr.RepositoryException;

/**
 * General abstraction for an user of cloud drive. It provides integration points between cloud provider and
 * local cloud drive storage. <br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudUser.java 00000 Sep 7, 2012 pnedonosko $
 */
public abstract class CloudUser {

  /** The id. */
  protected final String        id;

  /** The username. */
  protected final String        username;

  /** The email. */
  protected final String        email;

  /** The provider. */
  protected final CloudProvider provider;

  /** The hash code. */
  protected final int           hashCode;

  /** The service name. */
  protected String              serviceName;

  /**
   * Instantiates a new cloud user.
   *
   * @param id the id
   * @param username {@link String}
   * @param email {@link String}
   * @param provider the provider
   */
  public CloudUser(String id, String username, String email, CloudProvider provider) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.provider = provider;

    int hc = 1;
    hc = hc * 31 + id.hashCode();
    hc = hc * 31 + username.hashCode();
    hc = hc * 31 + email.hashCode();
    hc = hc * 31 + provider.hashCode();
    this.hashCode = hc;
  }

  /**
   * User name on given cloud drive.
   * 
   * @return String
   */
  public String getUsername() {
    return username;
  }

  /**
   * User email in cloud drive.
   * 
   * @return String
   */
  public String getEmail() {
    return email;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public CloudProvider getProvider() {
    return provider;
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
   * Connected service name for human-readable uses. Service name is the same as the provider name by default,
   * but it also can be more precise description of the app the user connected to. For instance: the
   * same type of provider can connect to different instances of the app and the service name may
   * describe each one connected in runtime.<br>
   * 
   * @return String with the provider's service name (app instance name)
   */
  public String getServiceName() {
    return serviceName != null ? serviceName : provider.getName();
  }

  /**
   * Create a title for Cloud Drive root node. By default it is 'SERVICE_NAME - EMAIL', but implementation
   * may change it for more detailed.
   *
   * @return String with a text of root node for the user
   * @throws RepositoryException the repository exception
   * @throws DriveRemovedException the drive removed exception
   * @throws CloudDriveException the cloud drive exception
   */
  public String createDriveTitle() throws RepositoryException, DriveRemovedException, CloudDriveException {
    return getServiceName() + " - " + email;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CloudUser) {
      CloudUser other = (CloudUser) obj;
      return this.id.equals(other.id) && this.username.equals(other.username)
          && this.email.equals(other.email) && this.provider.equals(other.provider);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(username);
    s.append(' ');
    s.append('(');
    s.append(email);
    s.append(") at ");
    s.append(provider.getName());
    return s.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  // *********** internals ************

  /**
   * Set service name connected by this user.<br>
   *
   * @param serviceName the new service name
   * @see #getServiceName() for details
   */
  protected void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
