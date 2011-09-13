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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * 18-07-2007
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/browse/UICategoryTree.gtmpl",
    events = {
        @EventConfig(listeners = UICategoryTree.SelectActionListener.class)
    }
)
public class UICategoryTree extends UIComponent {

  private BCTreeNode treeRoot_ ;
  public UICategoryTree() { }

  public BCTreeNode getTreeRoot() { return this.treeRoot_ ;}
  public void setTreeRoot(Node node) throws Exception {
    treeRoot_ = new BCTreeNode(node) ;
  }

  public Node getRootNode() throws Exception {return getAncestorOfType(UIBrowseContainer.class).getRootNode() ;}

  public String getIcons(Node node, String type) throws Exception {
    return Utils.getNodeTypeIcon(node, type) ;
  }

  protected boolean isCategories(NodeType nodeType) {
    for(String type : Utils.CATEGORY_NODE_TYPES) {
      if(nodeType.getName().equals(type)) return true ;
    }
    return false ;
  }

  protected boolean isCategories(Node node) throws RepositoryException {
    NodeType nodeType = node.getPrimaryNodeType();
    String primaryTypeName = nodeType.getName();
    if(node.isNodeType(Utils.EXO_SYMLINK)) {
      primaryTypeName = node.getProperty(Utils.EXO_PRIMARYTYPE).getString();
    }
    for(String type : Utils.CATEGORY_NODE_TYPES) {
      if(primaryTypeName.equals(type)) return true;
    }
    return false;
  }

  public List<Node> getCategoryList(Node node) throws Exception{
    List<Node> nodes = new ArrayList<Node>() ;
    node = getTargetNode(node);
    NodeIterator item = node.getNodes() ;
    while(item.hasNext()) {
      Node child = item.nextNode() ;
      if(isCategories(child)) nodes.add(child) ;
    }
    return nodes ;
  }

  public void buildTree(String path) throws Exception {
    getTreeRoot().getChildren().clear() ;
    String[] arr = path.replaceFirst(getTreeRoot().getPath(), "").split("/") ;
    BCTreeNode temp = getTreeRoot() ;
    for(String nodeName : arr) {
      if(nodeName.length() == 0) continue ;
      temp.setChildren(getCategoryList(temp.getNode())) ;
      temp = temp.getChild(nodeName) ;
      if(temp == null) return ;
    }
    temp.setChildren(getCategoryList(temp.getNode())) ;
  }

  public boolean isSymLink(Node node) throws RepositoryException {
    return Utils.isSymLink(node);
  }

  public Node getTargetNode(Node node) throws ItemNotFoundException, RepositoryException {
    if(Utils.isSymLink(node)) {
      LinkManager linkManager = getApplicationComponent(LinkManager.class);
      return linkManager.getTarget(node);
    }
    return node;
  }

  static public class SelectActionListener extends EventListener<UICategoryTree> {
    public void execute(Event<UICategoryTree> event) throws Exception {
      UICategoryTree cateTree = event.getSource() ;
      UIBrowseContainer uiContainer = cateTree.getAncestorOfType(UIBrowseContainer.class) ;
      String path = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node node = uiContainer.getNodeByPath(path) ;
      if(node == null) {
        UIApplication app = uiContainer.getAncestorOfType(UIApplication.class) ;
        app.addMessage(new ApplicationMessage("UICategoryTree.msg.invalid-node", null)) ;
        
        return ;
      }
      uiContainer.setShowDocumentDetail(false)  ;
      uiContainer.setShowDocumentByTag(false)  ;
      uiContainer.setShowAllChildren(false) ;
      uiContainer.setSelectedTabPath(path) ;
      uiContainer.setCurrentNodePath(path) ;
      cateTree.buildTree(path) ;
      uiContainer.setPageIterator(uiContainer.getSubDocumentList(uiContainer.getCurrentNode()));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }
}
