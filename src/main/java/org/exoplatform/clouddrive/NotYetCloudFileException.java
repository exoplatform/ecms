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
 * Indicates that some file (usually designated by path) lies in Cloud Drive
 * folder but does not belong to it yet and in progress of creation (upload) to
 * the cloud site. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NotYetCloudFileException.java 00000. 2014 peter $
 */
public class NotYetCloudFileException extends NotCloudFileException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 4743059040181964693L;

  /**
   * Instantiates a new not yet cloud file exception.
   *
   * @param message the message
   */
  public NotYetCloudFileException(String message) {
    super(message);
  }
}
