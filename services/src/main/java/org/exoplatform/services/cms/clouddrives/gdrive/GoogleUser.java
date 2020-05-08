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
package org.exoplatform.services.cms.clouddrives.gdrive;

import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.cms.clouddrives.CloudUser;

/**
 * User class for Google. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleUser.java 00000 Sep 13, 2012 pnedonosko $
 */
public class GoogleUser extends CloudUser {

  /** The api. */
  protected final GoogleDriveAPI api;

  /**
   * Google User in-memory bean.
   * 
   * @param id {@link String}
   * @param username {@link String}
   * @param email {@link String}
   * @param provider {@link CloudProvider}
   * @param api {@link GoogleDriveAPI}
   */
  public GoogleUser(String id, String username, String email, CloudProvider provider, GoogleDriveAPI api) {
    super(id, username, email, provider);
    this.api = api;
  }

  /**
   * Google Drive services API.
   * 
   * @return {@link GoogleDriveAPI} instance authenticated for this user.
   */
  GoogleDriveAPI api() {
    return api;
  }
}
