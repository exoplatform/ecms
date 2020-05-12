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
package org.exoplatform.services.cms.clouddrives;

/**
 * Indicates that an access to the drive has been revoked or changed by the
 * user. To handle this exception need obtain new access rights and retry the
 * underlying operation.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveAccessException.java 00000 20 Nov 2012 peter $
 */
public class CloudDriveAccessException extends CloudDriveException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 2607063560185842027L;

  /**
   * Instantiates a new cloud drive access exception.
   *
   * @param message the message
   */
  public CloudDriveAccessException(String message) {
    super(message);
  }

  /**
   * Instantiates a new cloud drive access exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public CloudDriveAccessException(String message, Throwable cause) {
    super(message, cause);
  }

}
