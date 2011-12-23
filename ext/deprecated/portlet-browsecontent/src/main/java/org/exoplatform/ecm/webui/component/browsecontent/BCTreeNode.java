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
package org.exoplatform.ecm.webui.component.browsecontent;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Jan 3, 2007 3:24:37 PM
 */
public class BCTreeNode {
  private boolean isExpanded_ ;
  private Node node_ ;
  private List<BCTreeNode> children_ = new ArrayList<BCTreeNode>() ;

  public BCTreeNode(Node node, List<Node> children) {
    node_ = node ;
    isExpanded_ = true;
    setChildren(children) ;
  }

  public BCTreeNode(Node node) {
    node_ = node ;
    isExpanded_ = false ;
  }

  public int getLevel() throws RepositoryException { return node_.getDepth(); }

  public boolean isExpanded() { return isExpanded_; }
  public void setExpanded(boolean isExpanded) { isExpanded_ = isExpanded; }

  public String getName() throws RepositoryException { return node_.getName(); }
  public String getPath() throws RepositoryException { return node_.getPath(); }

  public Node getNode() { return node_ ; }
  public void setNode(Node node) { node_ = node ; }

  public List<BCTreeNode> getChildren() { return children_ ; }
  public int getChildrenSize() { return children_.size() ; }

  public BCTreeNode getChild(String relPath) throws RepositoryException {
    for(BCTreeNode child : children_) {
      String path = child.getPath() ;
      String nodeName = path.substring(path.lastIndexOf("/") + 1) ;
      if(nodeName.equals(relPath)) return child ;
    }
    return null;
  }

  public void setChildren(List<Node> children) {
    setExpanded(true) ;
    for(Node child : children) {
      children_.add(new BCTreeNode(child)) ;
    }
  }
}
