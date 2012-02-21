/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.skin;

import java.io.Reader;
import java.io.StringReader;

import javax.jcr.Node;

import org.exoplatform.portal.resource.Resource;
import org.exoplatform.portal.resource.ResourceResolver;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.phamvu@exoplatform.com
 * Nov 25, 2008
 */
public class WCMSkinResourceResolver implements ResourceResolver {
  private SkinService skinService;
  private LivePortalManagerService livePortalService;
  
  /** The log. */
  private static Log              log                  = ExoLogger.getLogger("wcm:WCMSkinResourceResolver");
  
  public WCMSkinResourceResolver(SkinService skinService, LivePortalManagerService livePortalService) {
    this.skinService = skinService;
    this.livePortalService = livePortalService;
  }

  public Resource resolve(String path) {
    if(!path.matches(XSkinService.SKIN_PATH_REGEXP)) return null;
    String[] elements = path.split("/");
    String portalName = elements[4];
    String skinName = elements[5];
    String skinModule = portalName;
    String cssPath = null;
    SkinConfig portalSkinConfig = skinService.getSkin(portalName,skinName);
    if(portalSkinConfig != null) {
      cssPath = portalSkinConfig.getCSSPath();
    }
    //get css for shared portal if the portalName is shared Portal
    if(cssPath == null) {
      for(SkinConfig skinConfig: skinService.getPortalSkins(skinName)) {
        if(skinConfig.getModule().equals(skinModule)) {
          cssPath = skinConfig.getCSSPath();
          break;
        }
      }
    }
    try {
      Node portalNode = livePortalService.getLivePortal(WCMCoreUtils.getSystemSessionProvider(), portalName);  
      final String cssData = WCMCoreUtils.getSiteGlobalActiveStylesheet(portalNode);
      if(cssData == null)
        return null;
      return new Resource(cssPath) {
        public Reader read() {
          return new StringReader(cssData);
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
