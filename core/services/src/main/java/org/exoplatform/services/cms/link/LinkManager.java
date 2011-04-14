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

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com Mar 13, 2009
 */
public interface LinkManager {

  /**
   * Creates a new link, add it to the parent node and returns the link
   *
   * @param parent The parent node of the link
   * @param linkType The primary node type of the link must be a sub-type of
   *          exo:symlink, the default value is "exo:symlink"
   * @param target The target of the link
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, String linkType, Node target) throws RepositoryException;

  /**
   * Creates a new node of type exo:symlink, add it to the parent node and
   * returns the link node
   *
   * @param parent The parent node of the link to create
   * @param target The target of the link
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, Node target) throws RepositoryException;

  /**
   * Creates a new link, add it to the parent node and returns the link
   *
   * @param parent The parent node of the link
   * @param linkType The primary node type of the link must be a sub-type of
   *          exo:symlink, the default value is "exo:symlink"
   * @param target The target of the link
   * @param linkName The name of the link
   * @return
   * @throws RepositoryException if the link cannot be created for any reason
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName)
      throws RepositoryException;

  /**
   * Updates the target node of the given link
   *
   * @param link The link node to update
   * @param target The new target of the link
   * @throws RepositoryException if the link cannot be updated for any reason
   */
  public Node updateLink(Node link, Node target) throws RepositoryException;

  /**
   * Gets the target node of the given link
   *
   * @param link The node of type exo:symlink
   * @param system Indicates whether the target node must be retrieved using a
   *          session system or user session in case we cannot use the same
   *          session as the node link because the target and the link are not
   *          in the same workspace
   * @throws ItemNotFoundException if the target node cannot be found
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node
   */
  public Node getTarget(Node link, boolean system) throws ItemNotFoundException,
      RepositoryException;

  /**
   * Gets the target node of the given link using the user session
   *
   * @param link The node of type exo:symlink
   * @throws ItemNotFoundException if the target node cannot be found
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node
   */
  public Node getTarget(Node link) throws ItemNotFoundException, RepositoryException;

  /**
   * Checks if the target node of the given link can be reached using the user
   * session
   *
   * @param link The node of type exo:symlink
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isTargetReachable(Node link) throws RepositoryException;

  /**
   * Checks if the target node of the given link can be reached using the user
   * session
   *
   * @param link The node of type exo:symlink
   * @param system
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isTargetReachable(Node link, boolean system) throws RepositoryException;

  /**
   * Indicates whether the given item is a link
   *
   * @param item the item to test
   * @return <code>true</code> if the node is a link, <code>false</code>
   *         otherwise
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isLink(Item item) throws RepositoryException;

  /**
   * Gives the primary node type of the target
   *
   * @param link The node of type exo:symlink
   * @return the primary node type of the target
   * @throws RepositoryException if an unexpected error occurs
   */
  public String getTargetPrimaryNodeType(Node link) throws RepositoryException;

  /**
   * Gives all links of the given node
   *
   * @param targetNode The target node to get links
   * @param linkType The type of link to get
   * @param repoName Name of the repository
   * @return the list of link of the target node with given type
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType, String repoName) throws Exception;
  
  /**
   * Gives all links of the given node
   *
   * @param targetNode The target node to get links
   * @param linkType The type of link to get
   * @return the list of link of the target node with given type
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType) throws Exception;  
}
