
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
package org.exoplatform.clouddrive.box;

import org.exoplatform.clouddrive.CloudDriveAccessException;

/**
 * Error to authenticate to Box service (access and refresh tokens may be expired). Need user re-authentication.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AuthTokenException.java 00000 Dec 18, 2013 pnedonosko $
 * 
 */
public class AuthTokenException extends CloudDriveAccessException {

  /**
   * @param message
   */
  public AuthTokenException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public AuthTokenException(String message, Throwable cause) {
    super(message, cause);
  }

}
