/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.popup.service;

import javax.jcr.Session;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.*;
/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 6, 2012  
 */
public class DumpThreadLocalSessionProviderService extends ThreadLocalSessionProviderService {

  private DumpSessionProvider userSessionProvider;

  public DumpThreadLocalSessionProviderService(){
    super();
  }

  public SessionProvider getSessionProvider(Object key) {
    return userSessionProvider;
  }

  /**
   * Apply an user session
   * @param userSession
   */
  public void applyUserSession(Session userSession) {
    // create user session provider
    if (userSession != null) {
      userSessionProvider = new DumpSessionProvider(new ConversationState(new Identity(userSession.getUserID())));
    } else {
      userSessionProvider = new DumpSessionProvider(new ConversationState(new Identity(IdentityConstants.ANONIM)));
    }
    userSessionProvider.setSession(userSession);
  }

  public static class DumpSessionProvider extends SessionProvider {

    private Session session = null;

    public DumpSessionProvider(ConversationState userState) {
      super(userState);
    }

    public void setSession(Session value) {
      session = value;
    }

    public Session getSession(String workspace, ManageableRepository repo) {
      return session;
    }

  }
}
