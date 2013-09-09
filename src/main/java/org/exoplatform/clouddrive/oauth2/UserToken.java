/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive.oauth2;

import org.exoplatform.clouddrive.CloudDriveException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * OAuth2 token data (access and refresh tokens).
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserToken.java 00000 Sep 2, 2013 pnedonosko $
 * 
 */
public abstract class UserToken {

  private String                        accessToken;

  private String                        refreshToken;

  private long                          expirationTime;

  private Set<UserTokenRefreshListener> listeners = new LinkedHashSet<UserTokenRefreshListener>();

  /**
   * Create empty store.
   * 
   * @param id
   */
  protected UserToken() {
  }

  public void addListener(UserTokenRefreshListener listener) throws CloudDriveException {
    this.listeners.add(listener);
    listener.onUserTokenRefresh(this);
  }

  public void removeListener(UserTokenRefreshListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * Load OAuth2 token from given data.
   * 
   * @param accessToken
   * @param refreshToken
   * @param expirationTime
   */
  public void load(String accessToken, String refreshToken, long expirationTime) throws CloudDriveException {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expirationTime = expirationTime;
  }
  
  /**
   * Store new OAuth2 token data.
   * 
   * @param accessToken
   * @param refreshToken
   * @param expirationTime
   */
  public void store(String accessToken, String refreshToken, long expirationTime) throws CloudDriveException {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expirationTime = expirationTime;
    fireListeners();
  }

  /**
   * Import OAuth2 tokens from a new {@link UserToken} and unregister listeners of that instance.
   * 
   * @param newToken {@link UserToken}
   * @throws CloudDriveException
   */
  public void merge(UserToken newToken) throws CloudDriveException {
    removeListeners();
    store(newToken.getAccessToken(), newToken.getRefreshToken(), newToken.getExpirationTime());
  }

  /**
   * @return the accessToken
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * @return the refreshToken
   */
  public String getRefreshToken() {
    return refreshToken;
  }

  /**
   * @return the expirationTime
   */
  public long getExpirationTime() {
    return expirationTime;
  }

  // internals

  void unregisterListeners() {
    listeners.clear();
  }

  private void fireListeners() throws CloudDriveException {
    for (UserTokenRefreshListener listener : listeners) {
      listener.onUserTokenRefresh(this);
    }
  }

  void removeListeners() throws CloudDriveException {
    listeners.clear();
  }

}
