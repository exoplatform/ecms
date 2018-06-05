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

/**
 * Synchronization not supported for current Cloud Drive resource. This means
 * that the resource cannot be synchronized with remote cloud side what may
 * introduce inconsistency and the caller code should attempt to solve it by
 * special logic or raise an error to the user.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SyncNotSupportedException.java 00000 Oct 8, 2012 pnedonosko $
 */
public class SyncNotSupportedException extends CloudDriveException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -664526020486735361L;

  /**
   * Instantiates a new sync not supported exception.
   *
   * @param message the message
   */
  public SyncNotSupportedException(String message) {
    super(message);
  }

  /**
   * Instantiates a new sync not supported exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public SyncNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

}
