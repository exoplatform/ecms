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
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NotConnectedException.java 00000 Sep 11, 2012 pnedonosko $
 */
public class NotConnectedException extends CloudDriveException {

  /**
   * 
   */
  private static final long serialVersionUID = -5167284657750510695L;

  /**
   * Cloud drive not connected due to cause exception.
   * 
   * @param message
   * @param cause
   */
  public NotConnectedException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Cloud drive not connected.
   * 
   * @param message
   */
  public NotConnectedException(String message) {
    super(message);
  }

}
