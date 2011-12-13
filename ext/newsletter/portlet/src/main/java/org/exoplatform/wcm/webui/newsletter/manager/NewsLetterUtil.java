/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.newsletter.manager;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;

/**
 * The Class NewsLetterUtil.
 */
public class NewsLetterUtil {

  /**
   * Gets the portal name.
   *
   * @return the portal name
   */
  public static String getPortalName() {
    return Util.getPortalRequestContext().getPortalOwner();
  }

  /**
   * Generate link.
   *
   * @param url the url
   *
   * @return the string
   */
  public static String generateLink(String url) throws Exception {
    String link = url.replaceFirst("Subcribe", "ConfirmUserCode")
                      .replaceFirst("UINewsletterViewerForm", "UINewsletterViewerPortlet")
                      .replaceAll("&amp;", "&");
    String selectedNode = Util.getUIPortal().getSelectedUserNode().getURI();
    String portalName = "/" + getPortalName() ;
    if(link.indexOf(portalName) > 0) {
      if(link.indexOf(portalName + "/" + selectedNode) < 0){
        link = link.replaceFirst(portalName, portalName + "/" + selectedNode) ;
      }
    }
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    url = portalContext.getRequest().getRequestURL().toString();
    url = url.replaceFirst("http://", "") ;
    url = url.substring(0, url.indexOf("/")) ;
    StringBuffer buf = new StringBuffer();
    buf.append("http://").append(url).append(link);
    link = buf.toString();
    return link.replaceFirst("private", "public");
  }

  /**
   * Get current user
   * @return
   * @throws Exception
   */
  static public String getCurrentUser() throws Exception {
    return Util.getPortalRequestContext().getRemoteUser();
  }

}
