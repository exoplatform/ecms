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

/**
 * Cloud Drive provider entity.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudProvider.java 00000 Oct 4, 2012 pnedonosko $
 */
public abstract class CloudProvider {

  public static final String CONNECT_URL_BASE = "/portal/rest/clouddrive/connect/";

  public static final String AUTH_NOSTATE     = "__no_state_set__";

  protected final String     id;

  protected final String     name;

  protected final int        hashCode;

  protected CloudProvider(String id, String name) {
    // intern strings for fast comparison
    this.id = id.intern();
    this.name = name.intern();

    int hc = 1;
    hc = hc * 31 + id.hashCode();
    hc = hc * 31 + name.hashCode();
    this.hashCode = hc;
  }

  /**
   * String with authentication URL.
   * 
   * @return String with valid authentication URL.
   * @throws CloudDriveException when cannot build the auth url (e.g. cannot obtain current repository name)
   */
  public abstract String getAuthURL() throws CloudDriveException;

  /**
   * CloudProvider id used in URLs.
   * 
   * @return String with provider id
   */
  public String getId() {
    return id;
  }

  /**
   * Provider name for human-readable uses.
   * 
   * @return String with the name
   */
  public String getName() {
    return name;
  }

  /**
   * Provider specific message for given JVM exception (Throwable). By default this method uses
   * {@link Throwable#getMessage()}.
   * 
   * @return String with the error message
   */
  public String getErrorMessage(Throwable error) {
    return error.getMessage();
  }

  /**
   * Provider specific message for given error string and description. By default this method returns
   * concatenated error and description string.
   * 
   * @return String with the error message
   */
  public String getErrorMessage(String error, String errorDescription) {
    StringBuilder msg = new StringBuilder();
    msg.append(error);
    if (errorDescription != null && errorDescription.length() > 0) {
      msg.append(". ");
      msg.append(errorDescription);
    }
    return msg.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CloudProvider) {
      CloudProvider other = (CloudProvider) obj;
      return id == other.id && name == other.name;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return getId() + " " + getName();
  }

  /**
   * Tells if some operation against the provider service should be retried on failure. <br>
   * This method for internal use.
   * 
   * @return boolean, {@code true} if need retry on operation failure.
   * @see CloudProviderException
   * @see CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS
   * @see CloudDriveConnector#PROVIDER_REQUEST_ATTEMPT_TIMEOUT
   */
  public abstract boolean retryOnProviderError();

}
