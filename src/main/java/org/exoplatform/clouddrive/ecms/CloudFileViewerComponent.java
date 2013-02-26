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
package org.exoplatform.clouddrive.ecms;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.clouddrive.ecms.filters.CloudFileFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;


/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileViewerComponent.java 00000 Nov 1, 2012 pnedonosko $
 */
@ComponentConfig(template = "classpath:groovy/templates/CloudFileViewer.gtmpl")
public class CloudFileViewerComponent extends UIComponent {

  protected static final Log                     LOG        = ExoLogger.getLogger(CloudFileViewerComponent.class);

  public static final String                     EVENT_NAME = "ShowCloudFile";

  protected static final List<UIExtensionFilter> FILTERS    = Arrays.asList(new UIExtensionFilter[] { new CloudFileFilter() });

  /**
   * 
   */
  public CloudFileViewerComponent() {
    CloudDriveContext.addScript(WebuiRequestContext.getCurrentInstance());
  }

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    if (EVENT_NAME.equals(name)) {
      return "javascript:cloudDrive.openFile(this, 'objectId');";
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }
}
