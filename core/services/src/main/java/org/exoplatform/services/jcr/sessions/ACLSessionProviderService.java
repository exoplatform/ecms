/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.sessions;

import java.util.List;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Apr 25, 2011  
 */
public interface ACLSessionProviderService {
  
  /**
   * Gets the session provider by access control entry list
   * @param accessEntry
   * @return the SessionProvider
   */
  public SessionProvider getACLSessionProvider(List<AccessControlEntry> accessList);
  
  /**
   * Clears all created session providers
   */
  public void clearSessionProviders();
}
