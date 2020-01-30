/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.ecms.application;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * Feb 20, 2012  
 */
public class PortalResourceLifecycle implements ApplicationLifecycle<PortalRequestContext>
{
  
  /** The PATH. */
  final private String PATH = "/{portalName}/javascript/live";
  
  @Override
  public void onInit(Application app) throws Exception {
    
  }

  @Override
  public void onStartRequest(Application app, PortalRequestContext context) throws Exception {

    if (SiteType.PORTAL == context.getSiteType()) {
      // add current site js data
      String javascriptPath = StringUtils.replaceOnce(PATH, "{portalName}", context.getSiteName());
      context.getJavascriptManager().addExtendedScriptURLs(context.getPortalContextPath()
                                                               + javascriptPath);

      // add shared JS data for current site
      javascriptPath = StringUtils.replaceOnce(PATH, "{portalName}", "shared");
      context.getJavascriptManager().addExtendedScriptURLs(context.getPortalContextPath()
                                                               + javascriptPath);
    }
    
  }

  @Override
  public void onFailRequest(Application app,
                            PortalRequestContext context,
                            RequestFailure failureType) {
    
  }

  @Override
  public void onEndRequest(Application app, PortalRequestContext context) throws Exception {
    
  }

  @Override
  public void onDestroy(Application app) throws Exception {
    
  }
  
}
