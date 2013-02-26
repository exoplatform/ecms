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
package org.exoplatform.clouddrive.googledrive;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;

/**
 * User class for Google. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleUser.java 00000 Sep 13, 2012 pnedonosko $
 */
public class GoogleUser extends CloudUser {

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
  
  // TODO cleanup
  @Deprecated
  String getLargestChangeId() {
    throw new IllegalArgumentException();
  }
  
  @Deprecated
  void setLargestChangeId(String ch) {
    throw new IllegalArgumentException(); 
  }
}
