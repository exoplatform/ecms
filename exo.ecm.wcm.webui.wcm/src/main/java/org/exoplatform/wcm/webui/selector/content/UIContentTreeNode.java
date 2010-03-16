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
package org.exoplatform.wcm.webui.selector.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.wcm.core.NodeLocation;

/**
 * Created by The eXo Platform SARL
 * Author : Ha Mai Van
 * maivanha1610@gmail.com
 */
public class UIContentTreeNode {
  
  /** The tree path. */
  private String treePath;
  
  /** The deep. */
  private int deep;
  
  /** The name. */
  private String name;
  
  /** The node_. */
  private NodeLocation nodeLocation;
  
  /** The work space name. */
  private String workSpaceName;
  
  /**
   * Instantiates a new tree node.
   * 
   * @param path the path
   * @param workSpaceName the work space name
   * @param node the node
   * @param deep the deep
   */
  public UIContentTreeNode(String path, String workSpaceName, Node node, int deep) {
    this.name = null;
    nodeLocation = NodeLocation.make(node);
    this.deep = deep;
    this.workSpaceName = workSpaceName;
    try {
      this.treePath = path + "/" + getName();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Instantiates a new tree node.
   * 
   * @param name the name
   */
  public UIContentTreeNode(String name){
    this.name = name;
    this.treePath = "/" + name;
    nodeLocation = null;
    deep = 0;
  }
  
  /**
   * Instantiates a new tree node.
   * 
   * @param path the path
   * @param name the name
   * @param workSpaceName the work space name
   * @param node the node
   * @param deep the deep
   */
  public UIContentTreeNode(String path, String name, String workSpaceName, Node node, int deep){
    this.name = name;
    nodeLocation = NodeLocation.make(node);
    this.deep = deep;
    this.workSpaceName = workSpaceName;
    this.treePath = path + "/" + name;
  }

  /**
   * Gets the name.
   * 
   * @return the name
   * 
   * @throws RepositoryException the repository exception
   */
  public String getName() throws RepositoryException {
    StringBuilder buffer = new StringBuilder(128);
    if(name == null || name.trim().length() < 1){
      Node node = getNode();
      buffer.append(node.getName());
      int index = node.getIndex();
      if (index > 1) {
        buffer.append('[');
        buffer.append(index);
        buffer.append(']');
      }
    }else{
      buffer.append(this.name);
    }
    return buffer.toString();  
  }

  /**
   * Gets the node path.
   * 
   * @return the node path
   * 
   * @throws RepositoryException the repository exception
   */
  public String getNodePath() throws RepositoryException {
    Node node = getNode();
    if(node != null) return node.getPath();
    else return null;
    }

  /**
   * Gets the node.
   * 
   * @return the node
   */
  public Node getNode() { 
    return NodeLocation.getNodeByLocation(nodeLocation); 
  }  
  
  /**
   * Sets the node.
   * 
   * @param node the new node
   */
  public void setNode(Node node) { 
    nodeLocation = NodeLocation.make(node); 
  }

  /**
   * Gets the deep.
   * 
   * @return the deep
   */
  public int getDeep() {
    return deep;
  }

  /**
   * Sets the deep.
   * 
   * @param deep the new deep
   */
  public void setDeep(int deep) {
    this.deep = deep;
  }

  /**
   * Sets the name.
   * 
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the work space name.
   * 
   * @return the work space name
   */
  public String getWorkSpaceName() {
    return workSpaceName;
  }

  /**
   * Sets the work space name.
   * 
   * @param workSpaceName the new work space name
   */
  public void setWorkSpaceName(String workSpaceName) {
    this.workSpaceName = workSpaceName;
  }

  /**
   * Gets the tree path.
   * 
   * @return the tree path
   */
  public String getTreePath() {
    return treePath;
  }

  /**
   * Sets the tree path.
   * 
   * @param treePath the new tree path
   */
  public void setTreePath(String treePath) {
    this.treePath = treePath;
  }
}