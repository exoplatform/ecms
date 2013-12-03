/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.services.wcm.friendly.impl;

import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.filter.Filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 2, 2013
 * Converts friendly url to unfriendly url.
 * Wraps request by new one containing unfriendly url. 
 */
public class FriendlyFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                           ServletException {
    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    if (request instanceof HttpServletRequest) {
      String uri = ((HttpServletRequest)request).getRequestURI();
      String unfriendlyUri = friendlyService.getUnfriendlyUri(uri);
      if (uri != null && !uri.equals(unfriendlyUri)) {
        String portalName = "/" + WCMCoreUtils.getPortalName();
        if (unfriendlyUri.startsWith(portalName)) {
          unfriendlyUri = unfriendlyUri.substring(portalName.length());
        }
        ((HttpServletRequest)request).getRequestDispatcher(unfriendlyUri).forward(request, response);
      } else {
        chain.doFilter(request, response);
      }
    } else {
      chain.doFilter(request, response);
    }
  }
}
