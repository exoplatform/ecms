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

import java.util.Collection;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * May 28, 2008
 */
public interface WebSchemaConfigService {

  /**
   * Adds the web schema handler.
   *
   * @param plugin the plugin
   *
   * @throws Exception the exception
   */
  public void addWebSchemaHandler(ComponentPlugin plugin) throws Exception;

  /**
   * Gets the all web schema handler.
   *
   * @return the all web schema handler
   *
   * @throws Exception the exception
   */
  public Collection<WebSchemaHandler> getAllWebSchemaHandler() throws Exception;

  /**
   * Gets the web schema handler by type.
   *
   * @param clazz the clazz
   *
   * @return the web schema handler by type
   */
  public <T extends WebSchemaHandler> T getWebSchemaHandlerByType(Class<T> clazz);

  /**
   * Call this method when a node is created in observed tree.
   *
   * @param node the node
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void createSchema(SessionProvider sessionProvider, final Node node) throws Exception;

  /**
   * Update schema when a node is modified.
   *
   * @param node the node
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void updateSchemaOnModify(SessionProvider sessionProvider, final Node node) throws Exception;

  /**
   * Update schema on when a node is removed.
   *
   * @param node the node
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void updateSchemaOnRemove(SessionProvider sessionProvider, final Node node) throws Exception;
}
