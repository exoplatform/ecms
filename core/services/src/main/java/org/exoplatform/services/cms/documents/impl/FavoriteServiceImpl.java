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
package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 16, 2009
 * 10:02:04 AM
 */
public class FavoriteServiceImpl implements FavoriteService {

  private static final String EXO_PRIVILEGEABLE = "exo:privilegeable";
  private static final String EXO_FAVORITEFOLDER = "exo:favoriteFolder";
  private static final String NT_UNSTRUCTURED = "nt:unstructured";
  private static final String FAVORITE_ALIAS = "userPrivateFavorites";

  private NodeHierarchyCreator nodeHierarchyCreator;
  private LinkManager linkManager;
  private SessionProviderService sessionProviderService;

  public FavoriteServiceImpl(NodeHierarchyCreator nodeHierarchyCreator, LinkManager linkManager,
      SessionProviderService sessionProviderService, OrganizationService organizationService) {
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.linkManager = linkManager;
    this.sessionProviderService = sessionProviderService;
  }

  /**
   * {@inheritDoc}
   */
  public void addFavorite(Node node, String userName) throws Exception {
    // check if node is symlink
    if (linkManager.isLink(node)) return;

    // check if node has already been favorite node of current user
    Node userFavoriteNode = null;
    try {
      userFavoriteNode = getUserFavoriteFolder(userName);
      if (userFavoriteNode == null) {
        return;
      }
    } catch (PathNotFoundException e) {
      userFavoriteNode = createFavoriteFolder(userName);
    }

    NodeIterator nodeIter = userFavoriteNode.getNodes();
    while (nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      if (linkManager.isLink(childNode)) {
        Node targetNode = getTargetNode(childNode);
        if (node.isSame(targetNode)) return;
      }
    }
    // add favorite symlink
    linkManager.createLink(userFavoriteNode, NodetypeConstant.EXO_SYMLINK, node, node.getName() + ".lnk");
    userFavoriteNode.getSession().save();
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllFavoriteNodesByUser(String workspace, String repository, String userName) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    Node userFavoriteNode = getUserFavoriteFolder(userName);
    if (userFavoriteNode != null) {
      NodeIterator nodeIter = userFavoriteNode.getNodes();
      while (nodeIter.hasNext()) {
        Node childNode = nodeIter.nextNode();
        if (linkManager.isLink(childNode)) {
          Node targetNode = getTargetNode(childNode);
          if (targetNode != null)
            ret.add(targetNode);
        }
      }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  public void removeFavorite(Node node, String userName) throws Exception {
    Node targetNode = null;

    // check if node is symlink
    if (linkManager.isLink(node)) {
      targetNode = getTargetNode(node);
      if (targetNode != null) removeFavorite(targetNode, userName);
    } else {
      // remove favorite
      Node userFavoriteNode = getUserFavoriteFolder(userName);
      if(userFavoriteNode != null) { // to avoid NPE
        NodeIterator nodeIter = userFavoriteNode.getNodes();
        while (nodeIter.hasNext()) {
          Node childNode = nodeIter.nextNode();
          if (linkManager.isLink(childNode)) {
           targetNode = getTargetNode(childNode);
            if (node.isSame(targetNode)) {
              childNode.remove();
              userFavoriteNode.getSession().save();
              return;
            }
          }
        }
      }
    }
  }

  public boolean isFavoriter(String userName, Node node) throws Exception {
    LinkManager lnkManager = WCMCoreUtils.getService(LinkManager.class);

    if (lnkManager.isLink(node) && lnkManager.isTargetReachable(node)) {
      node = lnkManager.getTarget(node);
    }

    Node userFavoriteNode = null;
    try {
      userFavoriteNode = getUserFavoriteFolder(userName);
      if(userFavoriteNode == null) {
        return false;
      }
    } catch (PathNotFoundException e) {
      return false;
    }

    NodeIterator nodeIter = userFavoriteNode.getNodes();
    while (nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      if (linkManager.isLink(childNode)) {
        Node targetNode = getTargetNode(childNode);
        if (node.isSame(targetNode)) {
          return true;
        }
      }
    }
    return false;
  }

  private Node getUserFavoriteFolder(String userName) throws Exception {
    if (userName == null) {
      return null;
    }
    User user = null;
    user = WCMCoreUtils.getUserObject(userName);
    if (user == null) {
      return null;
    }
    Node userNode =
      nodeHierarchyCreator.getUserNode(sessionProviderService.getSessionProvider(null), userName);
    String favoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);
    return userNode.getNode(favoritePath);
  }

  private Node createFavoriteFolder(String userName) throws Exception {
    // Get default favorite path
    Node userNode =
      nodeHierarchyCreator.getUserNode(sessionProviderService.getSessionProvider(null), userName);
    String userFavoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);

    // Create favorite path
    Node userFavoriteNode = userNode.addNode(userFavoritePath, NT_UNSTRUCTURED);

    // Add Mixin types
    userFavoriteNode.addMixin(EXO_PRIVILEGEABLE);
    userFavoriteNode.addMixin(EXO_FAVORITEFOLDER);

    // Add permission
    Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
    permissionsMap.put(userName, PermissionType.ALL);
    ((ExtendedNode)userFavoriteNode).setPermissions(permissionsMap);

    return userFavoriteNode;
  }

  /**
   * Get Target Node
   * @param linkNode Link Node
   * @return Real Node
   */
  private Node getTargetNode(Node linkNode) {
    Node targetNode = null;
    try {
      targetNode = linkManager.getTarget(linkNode);
    } catch (ItemNotFoundException e) {
      targetNode = null;
    } catch (RepositoryException e) {
      targetNode = null;
    }
    return targetNode;
  }
}
