
/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.portlet;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.services.cms.clouddrives.webui.CloudDriveContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDrivePortlet.java 00000 Jul 6, 2018 pnedonosko $
 */
public class CloudDrivePortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(CloudDrivePortlet.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) throws PortletException, IOException {
    final String remoteUser = request.getRemoteUser();
    try {
      // We init core script w/o context workspace and path, this will init
      // global UI enhancements (for Search etc.)
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance());
    } catch (Exception e) {
      LOG.error("Error processing Cloud Drive portlet for user " + remoteUser, e);
    }
  }

}
