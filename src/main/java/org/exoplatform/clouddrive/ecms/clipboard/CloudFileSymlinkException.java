/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.clipboard;

import org.exoplatform.web.application.ApplicationMessage;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileSymlinkException.java 00000 May 19, 2014 pnedonosko $
 * 
 */
public class CloudFileSymlinkException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 24966321739068360L;
  
  protected final ApplicationMessage uiMessage;

  /**
   * 
   */
  public CloudFileSymlinkException(String message, ApplicationMessage uiMessage) {
    super(message);
    this.uiMessage = uiMessage;
  }

  /**
   * @return the uiMessage
   */
  public ApplicationMessage getUIMessage() {
    return uiMessage;
  }

}
