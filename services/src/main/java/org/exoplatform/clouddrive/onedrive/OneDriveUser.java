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

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;

/**
 * The Class OneDriveUser.
 */
public class OneDriveUser extends CloudUser {
  private final OneDriveAPI oneDriveAPI;

  /**
   * Instantiates a new cloud user.
   *
   * @param id the id
   * @param username {@link String}
   * @param email {@link String}
   * @param provider the provider
   */
  public OneDriveUser(String id, String username, String email, CloudProvider provider, OneDriveAPI oneDriveAPI) {
    super(id, username, email, provider);
    this.oneDriveAPI = oneDriveAPI;
  }

  public OneDriveAPI api() {
    return oneDriveAPI;
  }

}
