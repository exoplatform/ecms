/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class OneDriveProvider.
 */
public class OneDriveProvider extends CloudProvider {
  protected static final Log LOG = ExoLogger.getLogger(OneDriveProvider.class);
  private final String authUrl;
  private final String redirectUrl;

  /**
   * Instantiates a new cloud provider.
   *
   * @param id the id
   * @param name the name
   */
  protected OneDriveProvider(String id, String name, String authUrl, String redirectUrl) {
    super(id, name);
    this.authUrl = authUrl;
    this.redirectUrl = redirectUrl;
  }

  @Override
  public String getAuthURL() throws CloudDriveException {
    return this.authUrl;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  @Override
  public boolean retryOnProviderError() {
    return false;
  }
}
