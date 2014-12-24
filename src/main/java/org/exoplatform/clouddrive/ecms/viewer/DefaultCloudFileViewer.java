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
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.ecms.filters.CloudFileFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

/**
 * Default WebUI component for Cloud Drive files. It shows content of remote file by its URL in iframe on file
 * page in eXo Documents.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileViewerComponent.java 00000 Nov 1, 2012 pnedonosko $
 */
@ComponentConfig(template = "classpath:groovy/templates/DefaultCloudFileViewer.gtmpl")
public class DefaultCloudFileViewer extends AbstractFileViewer {

  protected static final Log                     LOG        = ExoLogger.getLogger(DefaultCloudFileViewer.class);

  public static final String                     EVENT_NAME = "ShowCloudFile";
  
  public static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2M

  protected static final List<UIExtensionFilter> FILTERS    = Arrays.asList(new UIExtensionFilter[] { new CloudFileFilter() });

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
      initContext();
      return "javascript:void(0);//objectId";
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isViewable() {
    boolean res = super.isViewable();
    if (res) {
      // accept only text/*
      return !file.getType().startsWith("text/");
    }
    return res;
  }
}
