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
package org.exoplatform.clouddrive.utils;

import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityHelper.java 00000 May 15, 2014 pnedonosko $
 * 
 */
public class IdentityHelper {

  public static final String ROOT_USER_ID   = "root";

  public static final String SYSTEM_USER_ID = IdentityConstants.SYSTEM;

  /**
   * 
   */
  private IdentityHelper() {
  }

  /**
   * Compare user names and return <code>true</code> if they match. Also will be <code>true</code> if one of
   * user names is system account id or root.
   * 
   * @param user1 {@link String}
   * @param user2 {@link String}
   * @return boolean
   */
  public static boolean isUserMatch(String user1, String user2) {
    return user1.equals(user2)
        || (ROOT_USER_ID.equals(user1) || SYSTEM_USER_ID.equals(user1) || ROOT_USER_ID.equals(user2) || SYSTEM_USER_ID.equals(user2));
  }
}
