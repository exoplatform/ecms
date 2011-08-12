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
package org.exoplatform.services.jcr.sessions.impl;

import java.util.List;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.sessions.ACLSessionProviderService;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Apr 25, 2011  
 */
public class ACLSessionProviderServiceThreadLocalImpl implements ACLSessionProviderService {
  
  private static ThreadLocal<SessionProvider> aclSessionProviderKeepers = new ThreadLocal<SessionProvider>();

  @Override
  public SessionProvider getACLSessionProvider(List<AccessControlEntry> accessList) {
    SessionProvider provider = aclSessionProviderKeepers.get();
    if (provider == null) {
      provider = SessionProvider.createProvider(accessList);
      aclSessionProviderKeepers.set(provider);
    }
    return provider;
  }

  @Override
  public void clearSessionProviders() {
    SessionProvider provider = aclSessionProviderKeepers.get();
    if (provider != null) {
      provider.close();
      aclSessionProviderKeepers.remove();
    }
  }
  
}
