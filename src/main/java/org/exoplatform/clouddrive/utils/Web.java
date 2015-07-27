
/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.clouddrive.utils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Web.java 00000 Jul 17, 2015 pnedonosko $
 * 
 */
public class Web {

  protected static final Log LOG = ExoLogger.getLogger(Web.class);

  /**
   * 
   */
  private Web() {
  }

  /**
   * Encode given string into application/x-www-form-urlencoded format using a UTF-8 encoding scheme.
   * 
   * @param str
   * @return encoded string or the same string if UTF-8 scheme not available (will be logged as warning)
   */
  public static String formEncode(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot encode " + str + ":" + e);
      return str;
    }
  }

  /**
   * Decode given string from application/x-www-form-urlencoded format using a UTF-8 encoding scheme.
   * 
   * @param str
   * @return decoded string or the same string if UTF-8 scheme not available (will be logged as warning)
   */
  public static String formDecode(String str) {
    try {
      return URLDecoder.decode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot decode " + str + ":" + e);
      return str;
    }
  }

  /**
   * Encode given string into URL friendly path format.
   * 
   * @param path
   * @return encoded string
   */
  public static String pathEncode(String path) {
    try {
      URI uri = new URI(null, null, path, null);
      return uri.getPath();
    } catch (URISyntaxException e) {
      LOG.warn("Cannot encode URL path " + path + ":" + e);
      return path;
    }
  }
  
  /**
   * Decode given string from URL path format to raw form.
   * 
   * @param path
   * @return decoded string
   */
  public static String pathDecode(String path) {
    try {
      URI uri = new URI(null, null, path, null);
      return uri.getRawPath();
    } catch (URISyntaxException e) {
      LOG.warn("Cannot decode URL path " + path + ":" + e);
      return path;
    }
  }

}
