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
package org.exoplatform.services.wcm.link;

import java.util.List;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong_phan@exoplatform.com
 * Aug 6, 2008
 */
public interface LiveLinkManagerService {

  /**
   * Get all link existed in all portal and update status for them (live or broken)
   *
   * @throws Exception the exception
   */
  public void updateLinks() throws Exception;

  /**
   * Get all link existed in specified portal and update status for them (live or broken)
   *
   * @param portalName the portal name
   *
   * @throws Exception the exception
   */
  public void updateLinks(String portalName) throws Exception;

  /**
   * Gets the broken links by portal.
   *
   * @param portalName the portal name
   *
   * @return the broken links
   *
   * @throws Exception the exception
   */
  public List<LinkBean> getBrokenLinks(String portalName) throws Exception;

  /**
   * Gets the broken links by web content.
   *
   * @param webContent the web content
   *
   * @return the broken links
   *
   * @throws Exception the exception
   */
  public List<String> getBrokenLinks(Node webContent) throws Exception ;

  /**
   * Extract all link (<code>a</code>, <code>iframe</code>, <code>frame</code>, <code>href</code>)
   * and all image (<code>img</code>) from html document
   *
   * @param htmlFile the node html file
   *
   * @return the list of link's URL
   *
   * @throws Exception the exception
   */
  public List<String> extractLinks(Node htmlFile) throws Exception ;

  /**
   * Add exo:links (multi value) property of exo:linkable node type to web content node, with pattern
   *
   * @param webContent the current web content node
   * @param newLinks the list of new links will be updated
   *
   * @throws Exception the exception
   */
  public void updateLinkDataForNode(Node webContent, List<String> newLinks) throws Exception ;

}
