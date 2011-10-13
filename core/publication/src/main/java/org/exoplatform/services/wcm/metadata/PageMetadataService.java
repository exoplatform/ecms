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
package org.exoplatform.services.wcm.metadata;

import java.util.HashMap;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Nov 4, 2008
 */
public interface PageMetadataService {

  /** The Constant HTTP META TAG KEYWORDS. */
  public final static String KEYWORDS = "keywords";

  /** The Constant HTTP META TAG ROBOTS. */
  public final static String ROBOTS = "robots";

  /** The Constant HTTP META TAG DESCRIPTION. */
  public final static String DESCRIPTION = "description";

  /** The Constant eXo  META TAG SITE_TITLE. */
  public final static String SITE_TITLE = "siteTitle";

  /** The Constant eXO Metatag PAGE_TITLE. */
  public final static String PAGE_TITLE = "pageTitle";

  /**
   * Extract metadata information from node.
   *
   * @param node the node
   *
   * @return the hash map< string, string>
   *
   * @throws Exception the exception
   */
  public HashMap<String, String> extractMetadata(Node node) throws Exception;

  /**
   * Retrieves  the portal metadata information for each request uri.
   *
   * @param uri the uri
   * @param sessionProvider the session provider
   *
   * @return the portal metadata
   *
   * @throws Exception the exception
   */
  public HashMap<String,String> getPortalMetadata(SessionProvider sessionProvider, String uri) throws Exception;
}
