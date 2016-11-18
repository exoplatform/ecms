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

package org.exoplatform.clouddrive.webui;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.filter.Filter;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContextFilter.java 00000 Nov 17, 2016 pnedonosko $
 * 
 */
public class CloudDriveContextFilter implements Filter {

  protected static final Logger LOG = LoggerFactory.getLogger(CloudDriveContextFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {

    PortalContainer container = PortalContainer.getInstance();
    WebAppController controller = (WebAppController) container.getComponentInstanceOfType(WebAppController.class);
    PortalApplication app = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);

    final CloudDriveLifecycle lifecycle = new CloudDriveLifecycle();
    try {
      app.getApplicationLifecycle().add(lifecycle);
      chain.doFilter(request, response);
    } finally {
      app.getApplicationLifecycle().remove(lifecycle);
    }
  }

}
