
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
package org.exoplatform.clouddrive;

import org.exoplatform.clouddrive.features.CloudDriveFeatures;

/**
 * Cannot create a Cloud Drive due to false response from {@link CloudDriveFeatures}.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CannotCreateDriveException.java 00000 Feb 10, 2014 pnedonosko $
 * 
 */
public class CannotCreateDriveException extends CloudDriveException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 8325706808760255291L;

  /**
   * Instantiates a new cannot create drive exception.
   *
   * @param message {@link String}
   */
  public CannotCreateDriveException(String message) {
    super(message);
  }

  /**
   * Instantiates a new cannot create drive exception.
   *
   * @param message {@link String}
   * @param cause {@link Throwable}
   */
  public CannotCreateDriveException(String message, Throwable cause) {
    super(message, cause);
  }

}
