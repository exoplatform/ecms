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
 * Synchronization not supported for current Cloud Drive resource. This means that the resource cannot be
 * synchronized with remote cloud side what may introduce inconsistency and the caller code should attempt to
 * solve it by special logic or raise an error to the user.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SyncNotSupportedException.java 00000 Oct 8, 2012 pnedonosko $
 * 
 */
public class SyncNotSupportedException extends CloudDriveException {

  /**
   * 
   */
  private static final long serialVersionUID = -664526020486735361L;

  /**
   * @param message
   */
  public SyncNotSupportedException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public SyncNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

}
