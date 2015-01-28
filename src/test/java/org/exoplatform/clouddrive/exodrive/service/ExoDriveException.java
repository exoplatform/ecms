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
package org.exoplatform.clouddrive.exodrive.service;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveConfigurationException.java 00000 Oct 5, 2012 pnedonosko $
 * 
 */
public class ExoDriveException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -5131278306818138644L;

  /**
   * @param message
   */
  public ExoDriveException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public ExoDriveException(String message, Throwable cause) {
    super(message, cause);
  }

}
