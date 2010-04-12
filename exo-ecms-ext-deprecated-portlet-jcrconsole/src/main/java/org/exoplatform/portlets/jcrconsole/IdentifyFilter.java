/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portlets.jcrconsole;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;

/**
 * Created by The eXo Platform SARL .
 * 
 * @author <a href="mailto:uy7c@yahoo.com">Max Shaposhnik</a>
 * @version $Id$
 */

public class IdentifyFilter implements Filter {
  
//  private AuthenticationService authenticationService;

 protected SessionProvider         sessionProvide;
  
  public void init(FilterConfig config) {
  }
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();

//    authenticationService = (AuthenticationService) container.getComponentInstanceOfType(AuthenticationService.class);
//    if (((HttpServletRequest) request).getRemoteUser() != null) {
//      if (authenticationService.getCurrentIdentity() == null) {
//        Identity identity = null;
//        try {
//          identity = authenticationService.getIdentityBySessionId(((HttpServletRequest) request).getRemoteUser());
//        } catch (Exception e) { }
//        authenticationService.setCurrentIdentity(identity);
//      }
//    }


    IdentityRegistry identityRegistry = (IdentityRegistry)container.getComponentInstanceOfType(IdentityRegistry.class);
    sessionProvide = new SessionProvider(new ConversationState(identityRegistry.getIdentity("admin")));


    chain.doFilter(request, response);
  }
  public void destroy() {
  }

}
