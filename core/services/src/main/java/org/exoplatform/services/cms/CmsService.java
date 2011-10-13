
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
 * @author benjaminmestrallet
 */
public interface CmsService {


  public final static String POST_CREATE_CONTENT_EVENT = "CmsService.event.postCreate";
  public final static String POST_EDIT_CONTENT_EVENT = "CmsService.event.postEdit";
  public final static String PRE_CREATE_CONTENT_EVENT = "CmsService.event.preCreate";
  public final static String PRE_EDIT_CONTENT_EVENT = "CmsService.event.preEdit";

  /**
   * Constant string to refer property of node in Map
   * For getting properties of specific node in Map,
   * use key = NODE + propertyName
   */
  public static final String NODE = "/node";

  /**
   * Store node in given workspace and repository with given properties
   * @param workspace       name of workspace
   * @param nodetypeName    NodeType's name
   * @param storePath       Path to store node
   * @param inputProperties Map of node's property including (property name, value)
   * @throws Exception      Throwing exception
   * @return path to saved node
   * @see #storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew) throws Exception
   */
  public String storeNode(String workspace,
                          String nodetypeName,
                          String storePath,
                          Map inputProperties) throws Exception;
  
  /**
   * Store node in given workspace and repository with given properties
   * @param workspace       name of workspace
   * @param nodetypeName    NodeType's name
   * @param storePath       Path to store node
   * @param inputProperties Map of node's property including (property name, value)
   * @param repository      Repository's name
   * @throws Exception      Throwing exception
   * @return path to saved node
   * @see #storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew) throws Exception
   */
  @Deprecated
  public String storeNode(String workspace,
                          String nodetypeName,
                          String storePath,
                          Map inputProperties,
                          String repository) throws Exception;

  /**
   * Store node in given repository with given properties
   * @param nodetypeName    NodeType's name
   * @param storeHomeNode   Parent node, where node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @return                return path to saved node
   * @throws Exception
   */
  public String storeNode(String nodetypeName,
                          Node storeHomeNode,
                          Map inputProperties,
                          boolean isAddNew) throws Exception;
  
  /**
   * Store node in given repository with given properties
   * @param nodetypeName    NodeType's name
   * @param storeHomeNode   Parent node, where node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @param repository      Repository's name
   * @return                return path to saved node
   * @throws Exception
   */
  @Deprecated
  public String storeNode(String nodetypeName,
                          Node storeHomeNode,
                          Map inputProperties,
                          boolean isAddNew,
                          String repository) throws Exception;

  /**
   * Store edited node in given repository with given properties
   * used in case that user only has permission to access storeNode but
   * can't access parent of storeNode (storeHomeNode)
   * @param nodetypeName    NodeType's name
   * @param storeNode       Node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @return                return path to saved node
   * @throws Exception
   */
  public String storeEditedNode(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew) throws Exception;
  
  /**
   * Store edited node in given repository with given properties
   * used in case that user only has permission to access storeNode but
   * can't access parent of storeNode (storeHomeNode)
   * @param nodetypeName    NodeType's name
   * @param storeNode       Node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @param repository      Repository's name
   * @return                return path to saved node
   * @throws Exception
   */
  @Deprecated
  public String storeEditedNode(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew,
                                String repository) throws Exception;

  /**
   * Store node in given repository with given properties and return UUID of saved node
   * @param nodetypeName    NodeType's name
   * @param storeNode       Node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @return                return UUID of saved node
   * @throws Exception
   * @see #storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew) throws Exception
   * @see #storeNode(String workspace, String nodetypeName, String storePath, Map inputProperties) throws Exception
   */
  public String storeNodeByUUID(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew) throws Exception;
  
  /**
   * Store node in given repository with given properties and return UUID of saved node
   * @param nodetypeName    NodeType's name
   * @param storeNode       Node is stored
   * @param inputProperties Map of node's property including (property name, value)
   * @param isAddNew        flag to decide whether this situation is adding node or updating node
   * @param repository      Repository's name
   * @return                return UUID of saved node
   * @throws Exception
   * @see #storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew) throws Exception
   * @see #storeNode(String workspace, String nodetypeName, String storePath, Map inputProperties) throws Exception
   */
  @Deprecated
  public String storeNodeByUUID(String nodetypeName,
                                Node storeNode,
                                Map inputProperties,
                                boolean isAddNew,
                                String repository) throws Exception;

  /**
   * Move node from one workspace to the other, with the same repository
   * @param nodePath      Path to node in source workspace
   * @param srcWorkspace  Source workspace name
   * @param destWorkspace Destination of workspace name
   * @param destPath      Destination of node path
   */
  public void moveNode(String nodePath, String srcWorkspace, String destWorkspace, String destPath);
  
  /**
   * Move node from one workspace to the other, with the same repository
   * @param nodePath      Path to node in source workspace
   * @param srcWorkspace  Source workspace name
   * @param destWorkspace Destination of workspace name
   * @param destPath      Destination of node path
   * @param repository      Repository's name
   */
  @Deprecated
  public void moveNode(String nodePath, String srcWorkspace, String destWorkspace, String destPath, String repository);  
}
