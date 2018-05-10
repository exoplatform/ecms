/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
 * Cloud user not authorized to create, modify or remove the cloud drive resource.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UnauthorizedException.java 00000 Feb 15, 2015 pnedonosko $
 * 
 */
public class UnauthorizedException extends ConstraintException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 3047097748189249585L;

  /**
   * Instantiates a new unauthorized exception.
   *
   * @param message the message
   */
  public UnauthorizedException(String message) {
    super(message);
  }

  /**
   * Instantiates a new unauthorized exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new unauthorized exception.
   *
   * @param cause the cause
   */
  public UnauthorizedException(Throwable cause) {
    super(cause);
  }

}
