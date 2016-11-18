
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
 * Cloud provider doesn't accept creation, modification or removal if the cloud drive resource.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NotAcceptableException.java 00000 Jul 21, 2015 pnedonosko $
 * 
 */
public class NotAcceptableException extends ConstraintException {

  /**
   * Instantiates a new not acceptable exception.
   *
   * @param message the message
   */
  public NotAcceptableException(String message) {
    super(message);
  }

  /**
   * Instantiates a new not acceptable exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public NotAcceptableException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new not acceptable exception.
   *
   * @param cause the cause
   */
  public NotAcceptableException(Throwable cause) {
    super(cause);
  }

}
