/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive.rest;


/**
 * Host management for Cloud Drive connections.<br>   
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveServiceLocator.java 00000 May 22, 2013 pnedonosko $
 * 
 */
public class DriveServiceLocator {

  /**
   * Base host name or <code>null</code> if base should be obtained from a request. 
   * 
   * @return {@link String}
   */
  public String getBaseHost() {
    return null;
  }
  
  /**
   * Compile service host name from request host, optionally  taking in account context. 
   * 
   * @param context {@link String}
   * @param requestHost {@link String}
   * @return {@link String}
   */
  public String getServiceHost(String context, String requestHost) {
    // ignore context 
    return requestHost;
  }

}
