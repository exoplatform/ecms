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
package org.exoplatform.services.handler;

import java.io.PrintWriter;

import javax.jcr.Node;

import org.exoplatform.ecm.utils.MessageDigester;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * Feb 20, 2012  
 */
public class SiteJavascriptHandler extends WebRequestHandler {
  private static final Log         LOG          = ExoLogger.getLogger(SiteJavascriptHandler.class.getName());

  private ExoCache<String, Object> jsCache_;

  private String                   siteName_;

  private LivePortalManagerService livePortalManagerService_;

  public static final String       CACHE_REGION = "ecms.site.javascript.cache";

  @Override
  public String getHandlerName() {
    return "javascript";
  }

  @Override
  public synchronized boolean execute(ControllerContext context) throws Exception {
    if (jsCache_ == null) {
      jsCache_ = WCMCoreUtils.getService(CacheService.class)
                             .getCacheInstance(SiteJavascriptHandler.CACHE_REGION);
    }
    siteName_ = context.getParameter(QualifiedName.create("gtn", "sitename"));

    String key = MessageDigester.getHash(siteName_);
    String jsData = (String) jsCache_.get(key);
    if (jsData == null) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      livePortalManagerService_ = WCMCoreUtils.getService(LivePortalManagerService.class);
      Node portalNode = null;
      try {
        if ("shared".equals(siteName_)) {
          portalNode = livePortalManagerService_.getLiveSharedPortal(sessionProvider);
        } else {
          portalNode = livePortalManagerService_.getLivePortal(sessionProvider, siteName_);  
        }
      } catch (Exception e) {
        LOG.warn("Can't find JCR portal node for site '{}'", siteName_);
      }
      if (portalNode == null) {
        return false;
      }
      jsData = WCMCoreUtils.getSiteGlobalActiveJs(portalNode, sessionProvider);
      jsCache_.put(key, jsData);
    }
    HttpServletResponse res = context.getResponse();
    res.setContentType("text/javascript");
    PrintWriter out = res.getWriter();
    out.println(jsData);
    return true;
  }
  
  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }
}
