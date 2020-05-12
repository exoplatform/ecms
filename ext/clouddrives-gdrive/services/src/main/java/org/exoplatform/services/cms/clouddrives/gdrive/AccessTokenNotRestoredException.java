/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.gdrive;

import java.io.IOException;

/**
 * Indicates that Google client was unable to aquire or restore access token and need an user action to reconnect the drive.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AccessTokenNotRestoredException.java 00000 Nov 28, 2019 pnedonosko $
 */
public class AccessTokenNotRestoredException extends IOException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 639413195582762663L;

  /**
   * Instantiates a new access token not restored exception.
   *
   * @param message the message
   */
  public AccessTokenNotRestoredException(String message) {
    super(message);
  }

}
