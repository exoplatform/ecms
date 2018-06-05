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
package org.exoplatform.clouddrive.oauth2;

import org.exoplatform.clouddrive.CloudDriveException;

/**
 * Listen for user token updates caused by provider service. Created by The eXo
 * Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserTokenRefreshListener.java 00000 Sep 3, 2013 pnedonosko $
 */
public interface UserTokenRefreshListener {

  /**
   * Action on token refresh from provider.
   *
   * @param token {@link UserToken}
   * @throws CloudDriveException the cloud drive exception
   */
  void onUserTokenRefresh(UserToken token) throws CloudDriveException;

  /**
   * Action on token removal by provider.
   *
   * @throws CloudDriveException the cloud drive exception
   */
  void onUserTokenRemove() throws CloudDriveException;

}
