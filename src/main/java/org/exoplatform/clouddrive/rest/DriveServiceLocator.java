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
package org.exoplatform.clouddrive.rest;

/**
 * Host management for Cloud Drive connections.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveServiceLocator.java 00000 May 22, 2013 pnedonosko $
 */
public class DriveServiceLocator {

  /**
   * Base host name or <code>null</code> if base should be obtained from a
   * request. This method can be overridden for context depended locators.
   * 
   * @return {@link String}
   */
  public String getBaseHost() {
    return null;
  }

  /**
   * Compile service host name from given request's host, optionally taking in
   * account context. This method can be overridden for context depended
   * locators. See also {@link #isRedirect(String)}.
   * 
   * @param context {@link String}
   * @param requestURI {@link String}
   * @return {@link String}
   */
  public String getServiceLink(String context, String requestURI) {
    // ignore context
    return requestURI;
  }

  /**
   * Answers whether the request (to given host) should be redirected to its
   * contextual link. See also {@link #getServiceLink(String, String)}.
   *
   * @param requestHost the request host
   * @return boolean, <code>true</code> if request should be redirected,
   *         <code>false</code> otherwise.
   */
  public final boolean isRedirect(String requestHost) {
    String baseHost = getBaseHost();
    if (baseHost != null) {
      return baseHost.equals(requestHost);
    } else {
      return false;
    }
  }

  /**
   * Return host name for given request URI.
   * 
   * @param requestHost {@link String}
   * @return String with the host's domain name or empty string if it's
   *         <code>localhost</code>
   */
  public final String getServiceHost(String requestHost) {
    String host = getBaseHost();
    if (host == null) {
      host = requestHost;
    }
    if (host.equalsIgnoreCase("localhost")) {
      // empty host for localhost domain, see
      // http://curl.haxx.se/rfc/cookie_spec.html
      host = "";
    }
    return host;
  }

}
