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
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.ecms.filters.CloudFileFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

/**
 * Default WebUI component for Cloud Drive files. It shows content of remote file by its URL in iframe on file
 * page in eXo Documents.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DefaultFileViewer.java 00000 Nov 1, 2012 pnedonosko $
 */
@ComponentConfig(template = "classpath:groovy/templates/DefaultFileViewer.gtmpl")
public class DefaultFileViewer extends AbstractFileViewer {

  protected static final Log                     LOG        = ExoLogger.getLogger(DefaultFileViewer.class);

  public static final String                     EVENT_NAME = "ShowCloudFile";

  protected static final List<UIExtensionFilter> FILTERS    = Arrays.asList(new UIExtensionFilter[] {
      new CloudFileFilter() });

  public DefaultFileViewer() {
    super();
  }

  protected DefaultFileViewer(long viewableMaxSize) {
    super(viewableMaxSize);
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
      initContext();
      return "javascript:void(0);//objectId";
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }
}
