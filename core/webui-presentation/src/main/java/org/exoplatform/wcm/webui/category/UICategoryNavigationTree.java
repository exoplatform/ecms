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
package org.exoplatform.wcm.webui.category;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.category.config.UICategoryNavigationConfig;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 19, 2009
 */
@ComponentConfig(
    lifecycle = Lifecycle.class,
    template = "app:/groovy/CategoryNavigation/UICategoryNavigationTree.gtmpl",
    events = {
      @EventConfig(listeners = UICategoryNavigationTree.QuickEditActionListener.class),
      @EventConfig(listeners = UICategoryNavigationTree.ChangeNodeActionListener.class)
    }
)
public class UICategoryNavigationTree extends UIContainer {

  private static final Log         LOG             = ExoLogger.getLogger(UICategoryNavigationTree.class.getName());
  
  /** The allow publish. */
  private boolean            allowPublish        = false;

  /** The publication service_. */
  private PublicationService publicationService_ = null;

  /** The templates_. */
  private List<String>       templates_          = null;

  /** The accepted node types. */
  private String[]           acceptedNodeTypes   = {};

  /** The root tree node. */
  protected NodeLocation rootTreeNode;

  /** The current node. */
  protected NodeLocation currentNode;

  /**
   * Checks if is allow publish.
   *
   * @return true, if is allow publish
   */
  public boolean isAllowPublish() {
    return allowPublish;
  }

  /**
   * Sets the allow publish.
   *
   * @param allowPublish the allow publish
   * @param publicationService the publication service
   * @param templates the templates
   */
  public void setAllowPublish(boolean allowPublish,
                              PublicationService publicationService,
                              List<String> templates) {
    this.allowPublish = allowPublish;
    publicationService_ = publicationService;
    templates_ = templates;
  }

  /**
   * Instantiates a new uI node tree builder.
   *
   * @throws Exception the exception
   */
  public UICategoryNavigationTree() throws Exception {

    PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
    String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    Node rootTreeNode = null;
    try {
      rootTreeNode = taxonomyService.getTaxonomyTree(preferenceTreeName);
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    setRootTreeNode(rootTreeNode);
    setAcceptedNodeTypes(new String[] {"nt:folder", "nt:unstructured", "nt:file", "exo:taxonomy"});

    UITree tree = addChild(UICategoryNavigationTreeBase.class, null, null);
    tree.setBeanLabelField("name");
    tree.setBeanIdField("path");
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
   *
   * @throws Exception the exception
   */
  public final void setRootTreeNode(Node node) throws Exception {
    this.rootTreeNode = NodeLocation.getNodeLocationByNode(node);
    this.currentNode = NodeLocation.getNodeLocationByNode(node);
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
  public String[] getAcceptedNodeTypes() {
    return acceptedNodeTypes;
  }

  /**
   * Sets the accepted node types.
   *
   * @param acceptedNodeTypes the new accepted node types
   */
  public void setAcceptedNodeTypes(String[] acceptedNodeTypes) {
    this.acceptedNodeTypes = acceptedNodeTypes;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    String parameters = null;
    try {
      parameters = URLDecoder.decode(StringUtils.substringAfter(Util.getPortalRequestContext()
                                                                    .getNodePath(),
                                                                Util.getUIPortal()
                                                                    .getSelectedUserNode()
                                                                    .getURI()
                                                                    + "/"), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      org.exoplatform.wcm.webui.Utils.createPopupMessage(this,
                                                         "UICategoryNavigationConfig.msg.not-support-encoding",
                                                         null,
                                                         ApplicationMessage.ERROR);
    }
    PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
    String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    Node treeNode = null;
    try {
      treeNode = taxonomyService.getTaxonomyTree(preferenceTreeName);
    } catch (RepositoryException e) {
      currentNode = null;
      super.processRender(context);
      return;
    }


    String categoryPath = parameters.substring(parameters.indexOf("/") + 1);
    if (preferenceTreeName.equals(categoryPath)) categoryPath = "";
    currentNode = NodeLocation.getNodeLocationByNode(treeNode.getNode(categoryPath));

    super.processRender(context);
  }

  /**
   * Builds the tree.
   *
   * @throws Exception the exception
   */
  public void buildTree() throws Exception {
    NodeIterator sibbling = null;
    NodeIterator children = null;
    UICategoryNavigationTreeBase tree = getChild(UICategoryNavigationTreeBase.class);
    Node selectedNode = NodeLocation.getNodeByLocation(currentNode);
    tree.setSelected(selectedNode);
    if (selectedNode == null) {
      return;
    }
    if (Utils.getNodeSymLink(selectedNode).getDepth() > 0) {
      tree.setParentSelected(selectedNode.getParent());
      sibbling = Utils.getNodeSymLink(selectedNode).getNodes();
      children = Utils.getNodeSymLink(selectedNode).getNodes();
    } else {
      tree.setParentSelected(selectedNode);
      sibbling = Utils.getNodeSymLink(selectedNode).getNodes();
      children = null;
    }
    if (sibbling != null) {
      tree.setSibbling(filter(sibbling));
    }
    if (children != null) {
      tree.setChildren(filter(children));
    }
  }

  /**
   * Adds the node publish.
   *
   * @param listNode the list node
   * @param node the node
   * @param publicationService the publication service
   *
   * @throws Exception the exception
   */
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

  /**
   * Filter.
   *
   * @param iterator the iterator
   *
   * @return the list< node>
   *
   * @throws Exception the exception
   */
  private List<Node> filter(final NodeIterator iterator) throws Exception {
    List<Node> list = new ArrayList<Node>();
    if (acceptedNodeTypes.length > 0) {
      for (; iterator.hasNext();) {
        Node sibbling = iterator.nextNode();
        if (sibbling.isNodeType("exo:hiddenable"))
          continue;
        for (String nodetype : acceptedNodeTypes) {
          if (sibbling.isNodeType(nodetype)) {
            list.add(sibbling);
            break;
          }
        }
      }
      List<Node> listNodeCheck = new ArrayList<Node>();
      for (Node node : list) {
        addNodePublish(listNodeCheck, node, publicationService_);
      }
      return listNodeCheck;
    }
    for (; iterator.hasNext();) {
      Node sibbling = iterator.nextNode();
      if (sibbling.isNodeType("exo:hiddenable"))
        continue;
      list.add(sibbling);
    }
    List<Node> listNodeCheck = new ArrayList<Node>();
    for (Node node : list)
      addNodePublish(listNodeCheck, node, publicationService_);
    return listNodeCheck;
  }

  /**
   * When a node is change in tree. This method will be rerender the children and sibbling nodes of
   * current node and broadcast change node event to other uicomponent
   *
   * @param path the path
   * @param context the context
   *
   * @throws Exception the exception
   */
  public void changeNode(String path, Object context) throws Exception {
    NodeFinder nodeFinder_ = getApplicationComponent(NodeFinder.class);
    String rootPath = rootTreeNode.getPath();
    if (rootPath.equals(path) || !path.startsWith(rootPath)) {
      currentNode = rootTreeNode;
    } else {
      if (path.startsWith(rootPath))
        path = path.substring(rootPath.length());
      if (path.startsWith("/"))
        path = path.substring(1);
      currentNode = NodeLocation.getNodeLocationByNode(nodeFinder_.getNode(NodeLocation.getNodeByLocation(rootTreeNode), path));
    }
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
      UICategoryNavigationTree categoryNavigationTree = event.getSource().getParent();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID);
      categoryNavigationTree.changeNode(uri, event.getRequestContext());
      event.getRequestContext().addUIComponentToUpdateByAjax(categoryNavigationTree.getParent());
    }
  }

  /**
   * The listener interface for receiving quickEditAction events. The class
   * that is interested in processing a quickEditAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addQuickEditActionListener</code> method. When
   * the quickEditAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class QuickEditActionListener extends EventListener<UICategoryNavigationTree> {
    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryNavigationTree> event) throws Exception {
      UICategoryNavigationTree uiContainer = event.getSource();
      UICategoryNavigationConfig configForm = uiContainer.createUIComponent(UICategoryNavigationConfig.class,
                                                                            null,
                                                                            null);
      org.exoplatform.wcm.webui.Utils.createPopupWindow(uiContainer,
                                                        configForm,
                                                        UICategoryNavigationPortlet.CONFIG_POPUP_WINDOW,
                                                        600);
    }
  }

}
