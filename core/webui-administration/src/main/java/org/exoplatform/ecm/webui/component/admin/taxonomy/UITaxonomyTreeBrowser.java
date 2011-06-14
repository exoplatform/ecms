/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.ecm.webui.tree.UINodeTree;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 7, 2009
 */

@ComponentConfig(
    template =  "app:/groovy/webui/component/admin/taxonomy/UITaxonomyTree.gtmpl",
    events = @EventConfig(listeners = UITaxonomyTreeBrowser.ChangeNodeActionListener.class)
)
public class UITaxonomyTreeBrowser extends UIContainer {

  private NodeLocation currentNode_;
  private NodeLocation rootNode_ = null;
  private String rootPath_;
  private String[] acceptedNodeTypes = {};

  public UITaxonomyTreeBrowser() throws Exception {
    UINodeTree tree = addChild(UINodeTree.class, null, "UITaxonomyTreeBrowser");
    tree.setBeanLabelField("name");
    tree.setBeanIdField("path");
  }

  public void update() throws Exception {
    UITaxonomyTreeCreateChild uiManager = getParent();
    rootNode_ = NodeLocation.getNodeLocationByNode(uiManager.getRootNode());
    rootPath_ = rootNode_.getPath();
  }

  public Node getRootNode() { 
    return NodeLocation.getNodeByLocation(rootNode_);  
  }

  public String[] getAcceptedNodeTypes() {
    return acceptedNodeTypes;
  }

  public void setAcceptedNodeTypes(String[] acceptedNodeTypes) {
    this.acceptedNodeTypes = acceptedNodeTypes;
  }

  public boolean matchNodeType(Node node) throws Exception {
    if(acceptedNodeTypes == null || acceptedNodeTypes.length == 0) return true;
    for(String nodeType: acceptedNodeTypes) {
      if(node.isNodeType(nodeType)) return true;
    }
    return false;
  }

  public void buildTree() throws Exception {
    Iterator sibbling = null;
    Iterator children = null;
    UITaxonomyTreeCreateChild uiManager = getParent();
    List<Node> taxonomyList = new ArrayList<Node>();
    if(rootNode_ == null ) {
      update();
      currentNode_ = rootNode_;
      children = getRootNode().getNodes();
      changeNode(getRootNode());
    }
    UINodeTree tree = getChildById("UITaxonomyTreeBrowser");
    Node nodeSelected = getSelectedNode();
    if(nodeSelected.getPath().equals(rootPath_)) {
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
    if(nodeSelected.getPath().equals(uiManager.getTaxonomyTreeNode().getPath())) {
      sibbling = nodeSelected.getNodes();
    }
    while(sibbling.hasNext()) {
      Node sibblingNode = (Node)sibbling.next();
      if(!matchNodeType(sibblingNode)) continue;
      if(PermissionUtil.canRead(sibblingNode) && !sibblingNode.isNodeType("exo:hiddenable")) {
        sibblingList.add(sibblingNode);
      }
    }
    if(nodeSelected.getPath().equals(rootPath_)) {
      taxonomyList.add(uiManager.getTaxonomyTreeNode());
      children = taxonomyList.iterator();
    }

    if(children != null) {
      while(children.hasNext()) {
        Node childrenNode = (Node)children.next();
        if(!matchNodeType(childrenNode)) continue;
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
    UITaxonomyTreeCreateChild uiManager = getParent();
    currentNode_ = NodeLocation.getNodeLocationByNode(uiManager.getNodeByPath(path));
    if (!rootNode_.getPath().equals("/"))
      if (getRootNode().getParent().getPath().equals(path))
        currentNode_ = rootNode_;
    uiManager.setSelectedPath(currentNode_.getPath());
    changeNode(NodeLocation.getNodeByLocation(currentNode_));
  }

  public void changeNode(Node nodeSelected) throws Exception {
    List<Node> nodes = new ArrayList<Node>();
    NodeIterator nodeIter = nodeSelected.getNodes();
    List<Node> rootTaxonomyList = new ArrayList<Node>();
    UITaxonomyTreeCreateChild uiTaxonomyManager = getParent();
    while(nodeIter.hasNext()) {
      nodes.add(nodeIter.nextNode());
    }
    if(nodeSelected.getPath().equals(rootPath_)) {
      rootTaxonomyList.add(uiTaxonomyManager.getTaxonomyTreeNode());
      nodes = rootTaxonomyList;
    }

    UITaxonomyTreeCreateChild uiManager = getParent();
    UITaxonomyTreeWorkingArea uiTreeWorkingArea = uiManager.getChild(UITaxonomyTreeWorkingArea.class);
    List<Node> lstNode = new ArrayList<Node>();
    for (Node node : nodes) {
      if (uiTreeWorkingArea.matchNodeType(node))
        lstNode.add(node);
    }
    uiTreeWorkingArea.setNodeList(lstNode);
    uiTreeWorkingArea.updateGrid();
  }

  public Node getSelectedNode() {
    if(currentNode_ == null) return getRootNode();
    return NodeLocation.getNodeByLocation(currentNode_);
  }

  public static class ChangeNodeActionListener extends EventListener<UITaxonomyTreeBrowser> {
    public void execute(Event<UITaxonomyTreeBrowser> event) throws Exception {
      UITaxonomyTreeBrowser uiTaxonomyTreeBrowser = event.getSource();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiTaxonomyTreeBrowser.setNodeSelect(uri);
      UITaxonomyTreeCreateChild uiTaxonomyTreeCreateChild = uiTaxonomyTreeBrowser.getParent();
      uiTaxonomyTreeCreateChild.onChange(uiTaxonomyTreeBrowser.getSelectedNode());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyTreeCreateChild);
    }
  }
}

