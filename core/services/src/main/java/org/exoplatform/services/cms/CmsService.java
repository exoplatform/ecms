
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
   * Stores a node in a given workspace and repository with given properties.
   *
   * @param workspace Name of the workspace.
   * @param nodetypeName Name of the nodetype.
   * @param storePath Path of the store node.
   * @param inputProperties Map of node's properties, including property name and value.
   * @throws Exception The exception
   * @return Path of the saved node.
   */
  public String storeNode(String workspace,
                          String nodetypeName,
                          String storePath,
                          Map inputProperties) throws Exception;
  
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
   * Stores the edited node in a given repository with given properties
   * used in case that user only has permission to access storeNode but
   * cannot access parent of storeNode (storeHomeNode).
   * @param nodetypeName Name of the nodetype.
   * @param storeNode The store node.
   * @param inputProperties Map of node's properties, including property name and value.
   * @param isAddNew If "true", the new node is added. If "false", the node is updated.
   * @return Path of the saved node.
   * @throws Exception The exception
   */
  public String storeEditedNode(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew) throws Exception;
  

  /**
   * Stores a node in a repository with given properties.
   *
   * @param nodetypeName Name of the nodetype.
   * @param storeNode The store node.
   * @param inputProperties Map of node's properties, including property name and value.
   * @param isAddNew If "true", the new node is added. If "false", the node is updated.
   * @return UUID of the saved node.
   * @throws Exception The exception
   */
  public String storeNodeByUUID(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew) throws Exception;
  
  /**
   * Moves a node from one workspace to the other, with the same repository.
   *
   * @param nodePath Path to the node in the source workspace.
   * @param srcWorkspace Name of the source workspace.
   * @param destWorkspace Name of the destination workspace.
   * @param destPath Path of the destination node.
   */
  public void moveNode(String nodePath, String srcWorkspace, String destWorkspace, String destPath);

  /**
   * Gets all properties of a node.
   *
   * @return Map of properties.
   */
  public Map<String, Object> getPreProperties();  
  
}
