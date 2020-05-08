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
package org.exoplatform.services.cms.clouddrives.exodrive;

import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.cms.clouddrives.CloudUser;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveUser.java 00000 Oct 4, 2012 pnedonosko $
 */
public class ExoDriveUser extends CloudUser {

  /**
   * ExoDrive user constructor.
   * 
   * @param username {@link String}
   * @param email {@link String}
   */
  public ExoDriveUser(String username, String email, CloudProvider provider) {
    // using email as user id
    super(email, username, email, provider);
  }

  boolean isSystem() {
    return ExoDriveConnector.SYSTEM == username;
  };
}
