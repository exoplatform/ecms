/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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

  protected final String        id;

  protected final String        username;

  protected final String        email;

  protected final CloudProvider provider;

  protected final int           hashCode;

  protected String              serviceName;

  /**
   * {@link CloudUser} constructor.
   * 
   * @param username {@link String}
   * @param email {@link String}
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
   * @return the provider
   */
  public CloudProvider getProvider() {
    return provider;
  }

  /**
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
   * @throws RepositoryException
   * @throws DriveRemovedException
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
   * Set servide name connected by this user.<br>
   * 
   * @see {@link #getServiceName()} for details
   * 
   * @param serviceName
   */
  protected void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
