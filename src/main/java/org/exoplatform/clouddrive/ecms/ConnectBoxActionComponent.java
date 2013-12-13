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
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.ProviderNotAvailableException;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

@ComponentConfig(
                 events = { @EventConfig(listeners = ConnectBoxActionComponent.ConnectBoxActionListener.class) })
public class ConnectBoxActionComponent extends BaseCloudDriveManagerComponent {

  protected static final Log LOG = ExoLogger.getLogger(ConnectBoxActionComponent.class);

  public static class ConnectBoxActionListener extends UIActionBarActionListener<ConnectBoxActionComponent> {

    public void processEvent(Event<ConnectBoxActionComponent> event) throws Exception {
    }
  }

  /**
   * @inherritDoc
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    CloudDriveService drivesService = WCMCoreUtils.getService(CloudDriveService.class);
    if (drivesService != null) {
      try {
        // XXX box - Box id from configuration
        CloudProvider provider = drivesService.getProvider("box");

        initContext();

        // add provider's default params
        JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
        js.getRequireJS().addScripts("\ncloudDrive.initProvider('" + provider.getId() + "', '"
            + provider.getAuthUrl() + "');\n");

        // XXX do workaround here, need point an id of the provider for this Connect component
        // this could be better to do by HTML attribute, but we cannot do this for the moment
        return "javascript:void(0);//" + provider.getId() + "//objectId";
      } catch (ProviderNotAvailableException e) {
        // if no such provider, cannot do anything - default link
        LOG.error("Error rendering Connect to Box component: " + e.getMessage());
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
    return "Connect your Box";
  }
}
