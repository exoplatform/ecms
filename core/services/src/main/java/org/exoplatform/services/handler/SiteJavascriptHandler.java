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
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.ecm.utils.MessageDigester;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * Feb 20, 2012  
 */
public class SiteJavascriptHandler extends WebRequestHandler {
  
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

    String username = IdentityConstants.ANONIM;
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null && conversationState.getIdentity() != null) {
      username = conversationState.getIdentity().getUserId();
      if (StringUtils.isBlank(username)) {
        username = IdentityConstants.ANONIM;
      }
    }
    String key = MessageDigester.getHash(siteName_) + MessageDigester.getHash(username);
    String jsData = (String) jsCache_.get(key);
    if (jsData == null || jsData.trim().length() == 0) {
      SessionProvider sessionProvider = IdentityConstants.ANONIM.equals(username) ? WCMCoreUtils.createAnonimProvider()
                                                                                  : WCMCoreUtils.getUserSessionProvider();
      livePortalManagerService_ = WCMCoreUtils.getService(LivePortalManagerService.class);
      Node portalNode = null;
      if ("shared".equals(siteName_)) {
        portalNode = livePortalManagerService_.getLiveSharedPortal(sessionProvider);
      } else {
        portalNode = livePortalManagerService_.getLivePortal(sessionProvider, siteName_);  
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
