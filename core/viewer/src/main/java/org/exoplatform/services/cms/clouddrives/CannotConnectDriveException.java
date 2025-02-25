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
 * Existing node (folder) cannot be connected as a Cloud Drive root. This error
 * means that this existing node was created by another user or for another
 * drive. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AnotherDriveException.java 00000 Dec 18, 2014 pnedonosko $
 */
public class CannotConnectDriveException extends CloudDriveException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 8207929347971677443L;

  /**
   * Instantiates a new cannot connect drive exception.
   *
   * @param message {@link String}
   */
  public CannotConnectDriveException(String message) {
    super(message);
  }

}
