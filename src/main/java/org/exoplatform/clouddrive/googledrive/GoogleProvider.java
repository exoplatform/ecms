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

import javax.jcr.RepositoryException;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.services.jcr.RepositoryService;


/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleProvider.java 00000 Oct 13, 2012 pnedonosko $
 */
public class GoogleProvider extends CloudProvider {
  
  protected final String            authUrl;

  protected final String            redirectUrl;

  protected final RepositoryService jcrService;

  /**
   * @param id
   * @param name
   */
  public GoogleProvider(String id,
                        String name,
                        String authUrl,
                        String redirectUrl,
                        RepositoryService jcrService) {
    super(id, name);
    this.authUrl = authUrl;
    this.redirectUrl = redirectUrl;
    this.jcrService = jcrService;
  }

  /**
   * Used in test.
   * 
   * @param id
   * @param name
   */
  @Deprecated
  GoogleProvider(String id, String name, String authUrl, String redirectUrl) {
    this(id, name, authUrl, redirectUrl, null);
  }

  /**
   * @inherritDoc
   */
  public String getAuthUrl() throws CloudDriveException {
    if (jcrService != null) {
      try {
        String currentRepo = jcrService.getCurrentRepository().getConfiguration().getName();
        return authUrl.replace(GoogleDriveAPI.NO_STATE, currentRepo);
      } catch (RepositoryException e) {
        throw new CloudDriveException(e);
      }
    } else {
      return authUrl;
    }
  }

  /**
   * @return the redirectUrl
   */
  public String getRedirectUrl() {
    return redirectUrl;
  }

  // ********* internals ***********

  @Override
  public boolean retryOnProviderError() {
    // TODO native retry support via Google Drive API should be studied deeper as it causes 401 Unauthorized
    return true;
  }

}
