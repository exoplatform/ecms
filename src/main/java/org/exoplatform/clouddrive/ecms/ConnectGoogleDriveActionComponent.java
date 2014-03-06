/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.ProviderNotAvailableException;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

@ComponentConfig(
                 events = { @EventConfig(
                                         listeners = ConnectGoogleDriveActionComponent.ConnectGoogleDriveActionListener.class) })
public class ConnectGoogleDriveActionComponent extends BaseCloudDriveManagerComponent implements
    CloudDriveUIMenuAction {

  protected static final Log LOG = ExoLogger.getLogger(ConnectGoogleDriveActionComponent.class);

  public static class ConnectGoogleDriveActionListener
                                                      extends
                                                      UIActionBarActionListener<ConnectGoogleDriveActionComponent> {

    public void processEvent(Event<ConnectGoogleDriveActionComponent> event) throws Exception {
    }
  }

  /**
   * @inherritDoc
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    // this action hasn't a template, so we initialize request context on rendering phase
    CloudDriveService drivesService = WCMCoreUtils.getService(CloudDriveService.class);
    if (drivesService != null) {
      try {
        // XXX gdrive - Google Drive id from configuration
        initContext(drivesService.getProvider("gdrive"));

        // XXX do workaround here, need point an id of the provider for this Connect component
        // this could be better to do by HTML attribute, but we cannot do this for the moment
        return "javascript:void(0);//objectId";
      } catch (ProviderNotAvailableException e) {
        // if no such provider, cannot do anything - default link
        LOG.error("Error rendering Connect to Google Drive component: " + e.getMessage());
        return super.renderEventURL(ajax, name, beanId, params);
      }
    } else {
      LOG.error("CloudDriveService not registred in the container.");
      return super.renderEventURL(ajax, name, beanId, params);
    }
  }

  /**
   * @inherritDoc
   */
  @Override
  public String getName() {
    return "Connect your Google Drive";
  }
}
