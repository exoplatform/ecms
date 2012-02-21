/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.javascript;

import java.io.Reader;
import java.io.StringReader;

import javax.jcr.Node;

import org.exoplatform.portal.resource.Resource;
import org.exoplatform.portal.resource.ResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 *          vuna@exoplatform.com
 * Nov 15, 2011  
 */
public class WCMJavascriptResourceResolver  implements ResourceResolver {
  
  private static Log              log                  = ExoLogger.getLogger("wcm:WCMJavascriptResourceResolver");  
  
  private LivePortalManagerService livePortalManagerService_;
  private JavascriptConfigService javascriptConfigService_;
  
  public WCMJavascriptResourceResolver(LivePortalManagerService livePortalManagerService,
                                       JavascriptConfigService javascriptConfigService) {
    this.livePortalManagerService_ = livePortalManagerService;
    this.javascriptConfigService_ = javascriptConfigService;
  }

  @Override
  public Resource resolve(String path) throws NullPointerException {
    if(!path.matches(XJavascriptService.JS_PATH_REGEXP)) return null;
    String[] elements = path.split("/");
    String portalName = elements[4];
    try {
      Node portalNode = livePortalManagerService_.getLivePortal(WCMCoreUtils.getSystemSessionProvider(), portalName);  
      final String jsData = WCMCoreUtils.getSiteGlobalActiveJs(portalNode);
      if(jsData == null)
        return null;
      return new Resource(path) {
        public Reader read() {
          return new StringReader(jsData);
        }
      };
    } catch(Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Unexpected error happens", e);
      }
    }
    return null;
  }

}
