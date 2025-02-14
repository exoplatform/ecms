
/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms;

import java.util.Map;

import javax.jcr.Node;

/**
 * Stores and moves nodes based on different criteria.
 *
 * @LevelAPI Experimental
 */
public interface CmsService {


  public final static String POST_CREATE_CONTENT_EVENT = "CmsService.event.postCreate";
  public final static String POST_EDIT_CONTENT_EVENT = "CmsService.event.postEdit";
  public final static String PRE_CREATE_CONTENT_EVENT = "CmsService.event.preCreate";
  public final static String PRE_EDIT_CONTENT_EVENT = "CmsService.event.preEdit";

  /**
   * Constant string to refer to the property of node in Map.
   * For getting properties of a specific node in Map,
   * use key = NODE + propertyName.
   */
  public static final String NODE = "/node";

  /**
   * Stores a node in a given repository with given properties.
   * @param nodetypeName Name of the nodetype.
   * @param storeHomeNode The parent node where the node is stored.
   * @param inputProperties Map of node's properties, including property name and value.
   * @param isAddNew If "true", the new node is added. If "false", the node is updated.
   * @return Path to the saved node.
   * @throws Exception The exception
   */
  public String storeNode(String nodetypeName,
                          Node storeHomeNode,
                          Map inputProperties,
                          boolean isAddNew) throws Exception;

  /**
   * Gets all properties of a node.
   *
   * @return Map of properties.
   */
  public Map<String, Object> getPreProperties();  
  
}
