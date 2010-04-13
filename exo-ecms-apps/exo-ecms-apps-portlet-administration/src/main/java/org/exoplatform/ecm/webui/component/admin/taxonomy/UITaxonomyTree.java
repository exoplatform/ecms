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
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.ecm.webui.tree.UINodeTree;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 10, 2008 4:29:57 PM
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/admin/taxonomy/UITaxonomyTree.gtmpl",
    events = @EventConfig(listeners = UITaxonomyTree.ChangeNodeActionListener.class)
)
public class UITaxonomyTree extends UIContainer {

  private Node currentNode_;
  private Node rootNode_ = null;
  private String rootPath_;
  
  public UITaxonomyTree() throws Exception {
    UINodeTree tree = addChild(UINodeTree.class, null, "UITaxonomyTree");
    tree.setBeanLabelField("name");
    tree.setBeanIdField("path");
  }
  
  public void update() throws Exception {
    UITaxonomyManager uiManager = getParent();
    rootNode_ = uiManager.getRootNode();
    rootPath_ = rootNode_.getPath();
  }
  
  public Node getRootNode() throws Exception { return rootNode_;  }
  
  public void buildTree() throws Exception {
    Iterator sibbling = null;
    Iterator children = null;
    UITaxonomyManager uiTaxonomyManager = getParent();
    List<Node> taxonomyList = new ArrayList<Node>();
    if(rootNode_ == null ) {
      update();
      currentNode_ = rootNode_;
      children = rootNode_.getNodes();
      changeNode(rootNode_);
    }
    UINodeTree tree = getChildById("UITaxonomyTree");
    Node nodeSelected = getSelectedNode();
    if(nodeSelected.getPath().equals(rootPath_) || rootNode_.getParent().getPath().equals(currentNode_.getPath())) {
      nodeSelected = rootNode_;
      children = nodeSelected.getNodes();
    }
    tree.setSelected(nodeSelected);
    if(nodeSelected.getDepth() > 0) {
      tree.setParentSelected(nodeSelected.getParent());
      sibbling = nodeSelected.getParent().getNodes();
      children = nodeSelected.getNodes();
    } else {
      tree.setParentSelected(nodeSelected);
      sibbling = nodeSelected.getNodes();
    }
    List<Node> sibblingList = new ArrayList<Node>();
    List<Node> childrenList = new ArrayList<Node>();
    if(nodeSelected.getPath().equals(uiTaxonomyManager.getTaxonomyNode().getPath())) {
      sibbling = nodeSelected.getNodes();
    }
    while(sibbling.hasNext()) {
      Node sibblingNode = (Node)sibbling.next();
      if(PermissionUtil.canRead(sibblingNode) && !sibblingNode.isNodeType("exo:hiddenable")) {
        sibblingList.add(sibblingNode);      
      }
    }    
    if(nodeSelected.getPath().equals(rootPath_) || rootNode_.getParent().getPath().equals(currentNode_.getPath())) {
      taxonomyList.add(uiTaxonomyManager.getTaxonomyNode());
      children = taxonomyList.iterator();
    }
    
    if(children != null) {
      while(children.hasNext()) {
        Node childrenNode = (Node)children.next();
        if(PermissionUtil.canRead(childrenNode) && !childrenNode.isNodeType("exo:hiddenable")) {
          childrenList.add(childrenNode);        
        }
      }
    }
    if(nodeSelected.getPath().equals(rootPath_)) tree.setSibbling(childrenList); 
    else tree.setSibbling(sibblingList);
    tree.setChildren(childrenList);
  }
  
  public void renderChildren() throws Exception {
    buildTree();
    super.renderChildren();
  } 
  
  public String getRootPath() { return rootPath_; }
  
  public void setNodeSelect(String path) throws Exception {
    UITaxonomyManager uiManager = getParent();
    currentNode_ = uiManager.getNodeByPath(path);
    if(rootNode_.getParent().getPath().equals(path)) currentNode_ = rootNode_;
    uiManager.setSelectedPath(currentNode_.getPath());
    changeNode(currentNode_);
  }
  
  public void changeNode(Node nodeSelected) throws Exception {
    List<Node> nodes = new ArrayList<Node>();
    NodeIterator nodeIter = nodeSelected.getNodes();
    List<Node> rootTaxonomyList = new ArrayList<Node>();
    UITaxonomyManager uiTaxonomyManager = getParent();
    while(nodeIter.hasNext()) {
      nodes.add(nodeIter.nextNode());
    }
    if(nodeSelected.getPath().equals(rootPath_)) {
      rootTaxonomyList.add(uiTaxonomyManager.getTaxonomyNode());
      nodes = rootTaxonomyList;
    }
    UITaxonomyManager uiManager = getParent();
    UITaxonomyWorkingArea uiWorkingArea = uiManager.getChild(UITaxonomyWorkingArea.class);
    uiWorkingArea.setNodeList(nodes);
    uiWorkingArea.updateGrid();
  }
  
  public Node getSelectedNode() {
    if(currentNode_ == null) return rootNode_;
    return currentNode_; 
  }
  
  static public class ChangeNodeActionListener extends EventListener<UITaxonomyTree> {
    public void execute(Event<UITaxonomyTree> event) throws Exception {
      UITaxonomyTree uiTaxonomyTree = event.getSource();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiTaxonomyTree.setNodeSelect(uri);
      UITaxonomyManager uiTaxonomyManager = uiTaxonomyTree.getParent();
      uiTaxonomyManager.onChange(uiTaxonomyTree.getSelectedNode());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManager);
    }
  }
}
