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

import org.exoplatform.services.cms.link.NodeLinkAware;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Sep 29, 2006
 * 5:37:31 PM
 */
public class TreeNode {
  private static final Log LOG  = ExoLogger.getLogger(TreeNode.class.getName());
  
  //TODO Need use this class for BC TreeNode
  private boolean isExpanded_ ;
  private String path_;
  private String prefix;
  private NodeLocation node_ ;
  private NodeLinkAware node;
  private String name_;
  private List<TreeNode> children_ = new ArrayList<TreeNode>() ;

  private long childrenSize;

  public TreeNode(Node node) throws RepositoryException {
    this(node, node.getPath());
  }

  private TreeNode(Node node, String path) {
    if (node instanceof NodeLinkAware) {
      this.node = (NodeLinkAware)node;
      try {
        this.childrenSize = this.node.getNodesLazily().getSize();
      } catch (RepositoryException e) {
        this.childrenSize = 0;
      }
    } else {
      node_ = NodeLocation.getNodeLocationByNode(node);
      try {
        this.childrenSize = ((NodeImpl) node).getNodesLazily().getSize();
      } catch (RepositoryException e) {
        this.childrenSize = 0;
      }
    }

    name_ = getName(node);
    isExpanded_ = false ;
    path_ = path;
    prefix =  path_.equals("/") ? "" : path_;
  }

  public boolean isExpanded() { return isExpanded_; }
  public void setExpanded(boolean isExpanded) { isExpanded_ = isExpanded; }

  public String getName() throws RepositoryException {
    return name_;
  }

  private String getName(Node node) {
    StringBuilder buffer = new StringBuilder(128);
    try {
      buffer.append(node.getName());
      int index = node.getIndex();
      if (index > 1) {
        buffer.append('[');
        buffer.append(index);
        buffer.append(']');
      }
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return buffer.toString();
  }

  public String getPath() { return path_; }
  public String getNodePath() throws RepositoryException { 
    return node != null ? node.getPath() : node_.getPath(); 
  }

  public Node getNode() { 
    return node != null ? node : NodeLocation.getNodeByLocation(node_); 
  }
  public void setNode(Node node) {
    if (node instanceof NodeLinkAware) {
      this.node = (NodeLinkAware)node;
    } else {
      node_ = NodeLocation.getNodeLocationByNode(node);
    }
  }
  public String getNodePath4ID() {
    String tmp = Text.escape(path_);
    return tmp.replace('%', '_');
  }
  public List<TreeNode> getChildren() { return children_ ; }
  public int getChildrenSize() { 
    return (int) childrenSize;
    }

  public TreeNode getChildByName(String name) throws RepositoryException {
    for(TreeNode child : children_) {
      if(child.getName().equals(name)) return child ;
    }
    Node tempNode = this.getNode().getNode(name);
    if (tempNode == null) return null;
    TreeNode tempTreeNode = new TreeNode(tempNode, prefix + "/" + getName(tempNode));
    return tempTreeNode;
  }

  public void setChildren(List<Node> children) throws Exception {
    setExpanded(true) ;
    for(Node child : children) {
      children_.add(new TreeNode(child, prefix  + "/" + getName(child))) ;
    }
  }

}
