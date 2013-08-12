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
package org.exoplatform.services.cms.link;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Supply API to work with the linked node or the link included in a node.
 *
 * @LevelAPI Platform
 */
public interface LinkManager {

  /**
   * Create a new link that is added to the parent node and return the link.
   *
   * @param parent The parent node of the link
   * @param linkType The primary node type of the link must be a sub-type of
   *          exo:symlink, the default value is "exo:symlink"
   * @param target The target of the link
   * @return Node
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, String linkType, Node target) throws RepositoryException;

  /**
   * Creates a new node of type exo:symlink, then add it to the parent node and
   * return the link node.
   *
   * @param parent The parent node of the link to create
   * @param target The target of the link
   * @return Node
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, Node target) throws RepositoryException;

  /**
   * Create a new link that is added to the parent node and return the link.
   *
   * @param parent The parent node of the link
   * @param linkType The primary node type of the link must be a sub-type of
   *          exo:symlink, the default value is "exo:symlink"
   * @param target The target of the link
   * @param linkName The name of the link
   * @return Node
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName)
      throws RepositoryException;
  
  
  /**
   * Create a new link that is added to the parent node and return the link.
   *
   * @param parent The parent node of the link
   * @param linkType The primary node type of the link must be a sub-type of
   *          exo:symlink, the default value is "exo:symlink"
   * @param target The target of the link
   * @param linkName The name of the link
   * @param linkTitle The title of the link
   * @return Node
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName, String linkTitle)
      throws RepositoryException;

  /**
   * Update the target node of the given link.
   *
   * @param link The link node to update
   * @param target The new target of the link
   * @return Node
   * @throws RepositoryException if the link cannot be updated for any reason
   */
  public Node updateLink(Node link, Node target) throws RepositoryException;

  /**
   * Get the target node of the given link.
   *
   * @param link The node of type exo:symlink
   * @param system Indicate whether the target node must be retrieved using a
   *               session system or user session in case we cannot use the
   *               same session as the node link because the target and the
   *               link are not in the same workspace.
   * @return Node
   * @throws ItemNotFoundException if the target node cannot be found
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node
   */
  public Node getTarget(Node link, boolean system) throws ItemNotFoundException,
      RepositoryException;

  /**
   * Get the target node of the given link using the user session.
   *
   * @param link The node of type exo:symlink
   * @return Node
   * @throws ItemNotFoundException if the target node cannot be found
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node
   */
  public Node getTarget(Node link) throws ItemNotFoundException, RepositoryException;

  /**
   * Check if the target node of the given link can be reached using the user session.
   *
   * @param link The node of type exo:symlink
   * @return True if the Target is reachable or False if not
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isTargetReachable(Node link) throws RepositoryException;

  /**
   * Check if the target node of the given link can be reached using the user session.
   *
   * @param link The node of type exo:symlink
   * @param system
   * @return True if the Target is reachable or False if not
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isTargetReachable(Node link, boolean system) throws RepositoryException;

  /**
   * Indicates whether the given item is a link.
   *
   * @param item The item to test
   * @return True if the node is a link or False otherwise
   *
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isLink(Item item) throws RepositoryException;

  /**
   * Return the primary node type of the target.
   *
   * @param link The node of type exo:symlink
   * @return the primary node type of the target
   * @throws RepositoryException if an unexpected error occurs
   */
  public String getTargetPrimaryNodeType(Node link) throws RepositoryException;
  
  /**
   * Return all links of the given node.
   *
   * @param targetNode The target node to get links
   * @param linkType The type of link to get
   * @return the list of link of the target node with given type
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType) throws Exception;
  
  /**
   * Return all links of the given node
   *
   * @param targetNode The target node to get links
   * @param linkType The type of link to get
   * @param sessionProvider The session provider
   * @return the list of link of the target node with given type
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType, SessionProvider sessionProvider) throws Exception;
  
  /**
   * clones some data from target to link: exo:title, exo:dateCreated, exo:dateModified, publication:liveDate, exo:index
   * @param link the link
   * @throws Exception
   */
  public void updateSymlink(Node link) throws Exception;
  
}
