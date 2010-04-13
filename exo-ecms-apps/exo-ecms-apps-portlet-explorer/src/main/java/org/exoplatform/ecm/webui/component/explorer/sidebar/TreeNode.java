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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Sep 29, 2006
 * 5:37:31 PM 
 */
public class TreeNode {
  //TODO Need use this class for BC TreeNode
  private boolean isExpanded_ ;
  private String path_;
  private Node node_ ; 
  private List<TreeNode> children_ = new ArrayList<TreeNode>() ; 

  public TreeNode(Node node) throws RepositoryException {
    this(node, node.getPath());
  }
  
  private TreeNode(Node node, String path) {
    node_ = node ;
    isExpanded_ = false ;
    path_ = path;
  }

  public boolean isExpanded() { return isExpanded_; } 
  public void setExpanded(boolean isExpanded) { isExpanded_ = isExpanded; }

  public String getName() throws RepositoryException {
    return getName(node_); 
  }

  private String getName(Node node) throws RepositoryException {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append(node.getName());
    int index = node.getIndex();
    if (index > 1) {
      buffer.append('[');
      buffer.append(index);
      buffer.append(']');
    }
    return buffer.toString(); 	  
  }
  
  public String getPath() { return path_; }
  public String getNodePath() throws RepositoryException { return node_.getPath(); }

  public Node getNode() { return node_ ; }  
  public void setNode(Node node) { node_ = node ; }

  public List<TreeNode> getChildren() { return children_ ; }
  public int getChildrenSize() { return children_.size() ; }    
  
  public TreeNode getChildByName(String name) throws RepositoryException {
    for(TreeNode child : children_) {
      if(child.getName().equals(name)) return child ;
    }
    return null;
  }

  public void setChildren(List<Node> children) throws Exception {
    setExpanded(true) ;
    String prefix = path_.equals("/") ? "" : path_;
    for(Node child : children) {
      children_.add(new TreeNode(child, prefix  + "/" + getName(child))) ;
    } 
  }
  
}