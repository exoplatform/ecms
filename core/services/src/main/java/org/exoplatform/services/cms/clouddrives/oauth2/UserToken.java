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
package org.exoplatform.services.cms.clouddrives.oauth2;

import java.util.LinkedHashSet;
import java.util.Set;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;

/**
 * OAuth2 token data (access and refresh tokens). Created by The eXo Platform
 * SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserToken.java 00000 Sep 2, 2013 pnedonosko $
 */
public abstract class UserToken {

  /** The access token. */
  private String                        accessToken;

  /** The refresh token. */
  private String                        refreshToken;

  /** The expiration time. */
  private Long                          expirationTime;

  /** The listeners. */
  private Set<UserTokenRefreshListener> listeners = new LinkedHashSet<UserTokenRefreshListener>();

  /**
   * Create empty store.
   */
  protected UserToken() {
  }

  /**
   * Adds the listener.
   *
   * @param listener the listener
   * @throws CloudDriveException the cloud drive exception
   */
  public void addListener(UserTokenRefreshListener listener) throws CloudDriveException {
    this.listeners.add(listener);
    listener.onUserTokenRefresh(this);
  }

  /**
   * Removes the listener.
   *
   * @param listener the listener
   */
  public void removeListener(UserTokenRefreshListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * Load OAuth2 token from given data.
   *
   * @param accessToken the access token
   * @param refreshToken the refresh token
   * @param expirationTime the expiration time
   * @throws CloudDriveException the cloud drive exception
   */
  public void load(String accessToken, String refreshToken, Long expirationTime) throws CloudDriveException {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expirationTime = expirationTime;
  }

  /**
   * Store new OAuth2 token data.
   *
   * @param accessToken the access token
   * @param refreshToken the refresh token
   * @param expirationTime the expiration time
   * @throws CloudDriveException the cloud drive exception
   */
  public void store(String accessToken, String refreshToken, Long expirationTime) throws CloudDriveException {
    load(accessToken, refreshToken, expirationTime);
    fireListenersRefresh();
  }

  /**
   * Clear current OAuth2 token data.
   *
   * @throws CloudDriveException the cloud drive exception
   */
  public void clear() throws CloudDriveException {
    load(null, null, null);
    fireListenersRemove();
  }

  /**
   * Import OAuth2 tokens from a new {@link UserToken} and unregister listeners
   * of that instance.
   *
   * @param newToken {@link UserToken}
   * @throws CloudDriveException the cloud drive exception
   */
  public void merge(UserToken newToken) throws CloudDriveException {
    newToken.removeListeners(); // May 4 2014, remove listeners on newToken (was on this instance)
    store(newToken.getAccessToken(), newToken.getRefreshToken(), newToken.getExpirationTime());
  }

  /**
   * Gets the access token.
   *
   * @return the access token, can be <code>null</code>
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Gets the refresh token.
   *
   * @return the refresh token, can be <code>null</code>
   */
  public String getRefreshToken() {
    return refreshToken;
  }

  /**
   * Gets the expiration time.
   *
   * @return the expiration time, can be <code>null</code>
   */
  public Long getExpirationTime() {
    return expirationTime;
  }

  /**
   * Unregister listeners.
   */
  void unregisterListeners() {
    listeners.clear();
  }

  /**
   * Removes the listeners.
   *
   * @throws CloudDriveException the cloud drive exception
   */
  void removeListeners() throws CloudDriveException {
    listeners.clear();
  }

  // internals

  /**
   * Fire listeners refresh event.
   *
   * @throws CloudDriveException the cloud drive exception
   */
  private void fireListenersRefresh() throws CloudDriveException {
    for (UserTokenRefreshListener listener : listeners) {
      listener.onUserTokenRefresh(this);
    }
  }

  /**
   * Fire listeners remove event.
   *
   * @throws CloudDriveException the cloud drive exception
   */
  private void fireListenersRemove() throws CloudDriveException {
    for (UserTokenRefreshListener listener : listeners) {
      listener.onUserTokenRemove();
    }
  }

}
