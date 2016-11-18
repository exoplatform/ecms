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
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.ProviderNotAvailableException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class BaseConnectActionComponent.
 */
public abstract class BaseConnectActionComponent extends BaseCloudDriveManagerComponent implements CloudDriveUIMenuAction {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(BaseConnectActionComponent.class);

  /**
   * Cloud Drive provider id of this connect component.
   * 
   * @return String
   */
  protected abstract String getProviderId();

  /**
   * Render event URL.
   *
   * @param ajax the ajax
   * @param name the name
   * @param beanId the bean id
   * @param params the params
   * @return the string
   * @throws Exception the exception
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    // this action hasn't a template, so we initialize request context on rendering phase
    CloudDriveService drivesService = WCMCoreUtils.getService(CloudDriveService.class);
    if (drivesService != null) {
      try {
        CloudProvider provider = drivesService.getProvider(getProviderId());
        initContext();

        // XXX do workaround here, need point an id of the provider for this Connect component
        // this could be better to do by HTML attribute, but we cannot do this for the moment
        return "javascript:void(0);//" + provider.getId() + "//objectId";
      } catch (ProviderNotAvailableException e) {
        // if no such provider, cannot do anything - default link
        LOG.error("Error rendering Connect to " + getProviderId() + " component: " + e.getMessage());
        return super.renderEventURL(ajax, name, beanId, params);
      }
    } else {
      LOG.error("CloudDriveService not registred in the container.");
      return super.renderEventURL(ajax, name, beanId, params);
    }
  }

  /**
   * Gets the name.
   *
   * @return the name
   * 
   */
  @Override
  public String getName() {
    // Name used in UI
    String connectYour = WebuiRequestContext.getCurrentInstance()
                                            .getApplicationResourceBundle()
                                            .getString("UIPopupWindow.title.ConnectYour");
    if (connectYour == null || connectYour.length() == 0) {
      connectYour = "Connect your";
    }

    CloudDriveService drivesService = WCMCoreUtils.getService(CloudDriveService.class);
    if (drivesService != null) {
      try {
        CloudProvider provider = drivesService.getProvider(getProviderId());
        return connectYour + " " + provider.getName();
      } catch (ProviderNotAvailableException e) {
        // if no such provider, cannot do anything - default name will be
        LOG.error("Error rendering Connect to " + getProviderId() + " component: " + e.getMessage());
      }
    } else {
      LOG.error("CloudDriveService not registred in the container.");
    }
    return connectYour + " " + getProviderId();
  }
}
