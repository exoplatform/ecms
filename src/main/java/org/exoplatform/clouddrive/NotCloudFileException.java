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
 * Indicates that some file (usually designated by path) lies in folder of but does not belong to a Cloud
 * Drive or cannot be treated as such.
 * 
 * <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NotCloudFileException.java 00000. 2012 peter $
 */
public class NotCloudFileException extends CloudDriveException {

  /**
   * 
   */
  private static final long serialVersionUID = 1895279671204623999L;

  /**
   * @param message
   */
  public NotCloudFileException(String message) {
    super(message);
  }

}
