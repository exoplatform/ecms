/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.tree;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.comparator.NodeTitleComparator;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */

@ComponentConfig(
    events = @EventConfig(listeners = UINodeTreeBuilder.ChangeNodeActionListener.class)
)
public class UINodeTreeBuilder extends UIContainer {
  private boolean allowPublish = false;
  private PublicationService publicationService_ = null;
  private List<String> templates_ = null;

  private String[] acceptedNodeTypes = {};

  private String[] defaultExceptedNodeTypes = {};

  /** The root tree node. */
  protected NodeLocation rootTreeNode;

  /** The current node. */
  protected NodeLocation currentNode;

  public boolean isAllowPublish() {
    return allowPublish;
  }

  public void setAllowPublish(boolean allowPublish, PublicationService publicationService, List<String> templates) {
    this.allowPublish = allowPublish;
    publicationService_ = publicationService;
    templates_ = templates;
  }

  /**
   * Instantiates a new uI node tree builder.
   *
   * @throws Exception the exception
   */
  public UINodeTreeBuilder() throws Exception {
    UITree tree = addChild(UINodeTree.class, null, UINodeTree.class.getSimpleName()+hashCode()) ;
    tree.setBeanLabelField("name") ;
    tree.setBeanIdField("path") ;
  }

  /**
   * Gets the root tree node.
   *
   * @return the root tree node
   */
  public Node getRootTreeNode() { 
    return NodeLocation.getNodeByLocation(rootTreeNode); 
  }

  /**
   * Sets the root tree node.
   *
   * @param node the new root tree node
   * @throws Exception the exception
   */
  public final void setRootTreeNode(Node node) throws Exception {
    this.rootTreeNode = NodeLocation.getNodeLocationByNode(node);
    this.currentNode = NodeLocation.getNodeLocationByNode(node);
    broadcastOnChange(node,null);
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   */
  public Node getCurrentNode() { 
    return NodeLocation.getNodeByLocation(currentNode); 
  }

  /**
   * Sets the current node.
   *
   * @param currentNode the new current node
   */
  public void setCurrentNode(Node currentNode) { 
    this.currentNode = NodeLocation.getNodeLocationByNode(currentNode); 
  }

  /**
   * Gets the accepted node types.
   *
   * @return the accepted node types
   */
  public String[] getAcceptedNodeTypes() { return acceptedNodeTypes; }

  /**
   * Sets the accepted node types.
   *
   * @param acceptedNodeTypes the new accepted node types
   */
  public void setAcceptedNodeTypes(String[] acceptedNodeTypes) {
    this.acceptedNodeTypes = acceptedNodeTypes;
  }

  /**
   * Gets the default excepted node types.
   *
   * @return the default excepted node types
   */
  public String[] getDefaultExceptedNodeTypes() { return defaultExceptedNodeTypes; }

  /**
   * Sets the default excepted node types.
   *
   * @param defaultExceptedNodeTypes the new excepted node types
   */
  public void setDefaultExceptedNodeTypes(String[] defaultExceptedNodeTypes) {
    this.defaultExceptedNodeTypes = defaultExceptedNodeTypes;
  }

  public boolean isExceptedNodeType(Node node) throws RepositoryException {
    if(defaultExceptedNodeTypes.length > 0) {
      for(String nodeType: defaultExceptedNodeTypes) {
        if(node.isNodeType(nodeType)) return true;
      }
    }
    return false;
  }

  /**
   * Builds the tree.
   *
   * @throws Exception the exception
   */

  public void buildTree() throws Exception {
    NodeIterator sibbling = null ;
    NodeIterator children = null ;
    UINodeTree tree = getChild(UINodeTree.class) ;
    Node selectedNode = getNodeByPathBreadcumbs();
    tree.setSelected(selectedNode);
    if (Utils.getNodeSymLink(selectedNode).getDepth() > 0) {
      tree.setParentSelected(selectedNode.getParent()) ;
      sibbling = Utils.getNodeSymLink(selectedNode).getNodes() ;
      children = Utils.getNodeSymLink(selectedNode).getNodes() ;
    } else {
      tree.setParentSelected(selectedNode) ;
      sibbling = Utils.getNodeSymLink(selectedNode).getNodes() ;
      children = null;
    }
    if (sibbling != null) {
      tree.setSibbling(filfer(sibbling));
    }
    if (children != null) {
      tree.setChildren(filfer(children));
    }
  }

  private Node getNodeByPathBreadcumbs() throws PathNotFoundException, RepositoryException {
    UIOneNodePathSelector uiOneNodePathSelector = (UIOneNodePathSelector) getParent();
    UIBreadcumbs uiBreadcumbs = uiOneNodePathSelector.getChildById("BreadcumbCategoriesOne");
    List<LocalPath> listLocalPath = uiBreadcumbs.getPath();
    StringBuilder buffer = new StringBuilder(1024);
    String rootPath = rootTreeNode.getPath();
    for (LocalPath iterLocalPath : listLocalPath) {
      buffer.append("/").append(iterLocalPath.getId());
    }
    String path = buffer.toString();
    if (path.startsWith("//"))
      path = path.substring(1);
    if (!path.startsWith(rootPath)) {
      StringBuffer sb = new StringBuffer();
      sb.append(rootPath).append(path);
      path = sb.toString();
    }
    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
    if (path.length() == 0) path = "/";
    if (buffer.length() == 0) return NodeLocation.getNodeByLocation(currentNode);
    NodeFinder nodeFinder_ = getApplicationComponent(NodeFinder.class);
    return (Node)nodeFinder_.getItem(uiOneNodePathSelector.getWorkspaceName(), path);
  }

  private void addNodePublish(List<Node> listNode, Node node, PublicationService publicationService) throws Exception {
    if (isAllowPublish()) {
      NodeType nt = node.getPrimaryNodeType();
      if (templates_.contains(nt.getName())) {
        Node nodecheck = publicationService.getNodePublish(node, null);
        if (nodecheck != null) {
          listNode.add(nodecheck);
        }
      } else {
        listNode.add(node);
      }
    } else {
      listNode.add(node);
    }
  }

  private List<Node> filfer(final NodeIterator iterator) throws Exception{
    List<Node> list = new ArrayList<Node>();
    if (acceptedNodeTypes.length > 0) {
      for(;iterator.hasNext();) {
        Node sibbling = iterator.nextNode();
        if(sibbling.isNodeType("exo:hiddenable")) continue;
        for(String nodetype: acceptedNodeTypes) {
          if(sibbling.isNodeType(nodetype)) {
            list.add(sibbling);
            break;
          }
        }
      }
      Collections.sort(list, new NodeTitleComparator(NodeTitleComparator.ASCENDING_ORDER));
      List<Node> listNodeCheck = new ArrayList<Node>();
      for (Node node : list) {
        addNodePublish(listNodeCheck, node, publicationService_);
      }
      return listNodeCheck;
    }
    for(;iterator.hasNext();) {
      Node sibbling = iterator.nextNode();
      if(sibbling.isNodeType("exo:hiddenable") || isExceptedNodeType(sibbling)) continue;
      list.add(sibbling);
    }
    Collections.sort(list, new NodeTitleComparator(NodeTitleComparator.ASCENDING_ORDER));
    List<Node> listNodeCheck = new ArrayList<Node>();
    for (Node node : list) addNodePublish(listNodeCheck, node, publicationService_);
    return listNodeCheck;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    Writer writer = context.getWriter() ;
    writer.write("<div class=\"explorerTree\">") ;
      buildTree() ;
      super.renderChildren() ;
    writer.write("</div>") ;
  }

  /**
   * When a node is change in tree. This method will be rerender the children and sibbling nodes of
   * current node and broadcast change node event to other uicomponent
   *
   * @param path the path
   * @param context the request context
   * @throws Exception the exception
   */
  public void changeNode(String path, Object context) throws Exception {
    NodeFinder nodeFinder_ = getApplicationComponent(NodeFinder.class);
    String rootPath = rootTreeNode.getPath();
    if(rootPath.equals(path) || !path.startsWith(rootPath)) {
      currentNode = rootTreeNode;
    }else {
      if (path.startsWith(rootPath)) path = path.substring(rootPath.length());
      if (path.startsWith("/")) path = path.substring(1);
      currentNode = NodeLocation.getNodeLocationByNode(nodeFinder_.getNode(
                                  NodeLocation.getNodeByLocation(rootTreeNode), path));
    }
    broadcastOnChange(NodeLocation.getNodeByLocation(currentNode),context);
  }

  /**
   * Broadcast on change.
   *
   * @param node the node
   * @param context the request context
   * @throws Exception the exception
   */
  public void broadcastOnChange(Node node, Object context) throws Exception {
    UIBaseNodeTreeSelector nodeTreeSelector = getAncestorOfType(UIBaseNodeTreeSelector.class);
    nodeTreeSelector.onChange(node, context);
  }

  /**
   * The listener interface for receiving changeNodeAction events. The class
   * that is interested in processing a changeNodeAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addChangeNodeActionListener</code> method. When
   * the changeNodeAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class ChangeNodeActionListener extends EventListener<UITree> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UITree> event) throws Exception {
      UINodeTreeBuilder builder = event.getSource().getParent();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID);
      builder.changeNode(uri,event.getRequestContext());
      UIBaseNodeTreeSelector nodeTreeSelector = builder.getAncestorOfType(UIBaseNodeTreeSelector.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(nodeTreeSelector);
    }
  }
}
