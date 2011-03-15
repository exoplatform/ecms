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
package org.exoplatform.services.wcm.core;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 3, 2008
 */
public interface WebSchemaHandler {
  /**
   * Match handler to process the schema
   *
   * @param node the node
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean matchHandler(SessionProvider sessionProvider, final Node node) throws Exception;

  /**
   * Process schema when crete node
   *
   * @param node the node
   * @throws Exception the exception
   */
  public void onCreateNode(SessionProvider sessionProvider, final Node node) throws Exception;

  /**
   * Update schema when modify node
   *
   * @param node the node
   * @throws Exception the exception
   */
  public void onModifyNode(SessionProvider sessionProvider, final Node node) throws Exception;

  /**
   * Update schema before a node is removed
   *
   * @param node the node
   * @throws Exception the exception
   */
  public void onRemoveNode(SessionProvider sessionProvider, final Node node) throws Exception;
}
