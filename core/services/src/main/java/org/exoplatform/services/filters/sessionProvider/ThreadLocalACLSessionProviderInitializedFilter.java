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
package org.exoplatform.services.filters.sessionProvider;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.jcr.sessions.ACLSessionProviderService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * May 9, 2011  
 */
public class ThreadLocalACLSessionProviderInitializedFilter extends AbstractFilter {

  private ACLSessionProviderService aclService;
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                           ServletException {
    aclService = WCMCoreUtils.getService(ACLSessionProviderService.class);
    try {
      chain.doFilter(request, response);
    } finally {
      aclService.clearSessionProviders();
    }
  }

  /*
   * (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
  }
  
}
