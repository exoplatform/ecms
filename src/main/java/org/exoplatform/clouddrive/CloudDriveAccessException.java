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
 * Indicates that an access to the drive has been revoked or changed by the user. To handle this
 * exception need obtain new access rights and retry the underlying operation.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveAccessException.java 00000 20 Nov 2012 peter $
 */
public class CloudDriveAccessException extends CloudDriveException {

  /**
   * 
   */
  private static final long serialVersionUID = 2607063560185842027L;

  /**
   * @param message
   */
  public CloudDriveAccessException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public CloudDriveAccessException(String message, Throwable cause) {
    super(message, cause);
  }

}
