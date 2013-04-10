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
package org.exoplatform.services.cms.link;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * NodeFinder is used to find a node with a given path.
 * If the path to the node contains sub-paths to exo:symlink nodes,
 * find the real link node.
 *
 * @LevelAPI Experimental
 */
public interface NodeFinder {

  /**
   * R Return the node at relPath related to the ancestor node.
   *
   * @param ancestorNode The ancestor of the node to retrieve from which we start.
   * @param relativePath The relative path of the node to retrieve.
   * @throws PathNotFoundException If no node exists at the specified path.
   * @throws RepositoryException If another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath) throws PathNotFoundException,
                                                             RepositoryException;

  /**
   * Return the node at relPath related to the ancestor node.
   * If the node is a link and giveTarget has been set to true,
   * the target node will be returned.
   *
   * @param ancestorNode The ancestor of the node to retrieve from which we start.
   * @param relativePath The relative path of the node to retrieve.
   * @param giveTarget Indicate if the target must be returned in case the item is a link.
   * @throws PathNotFoundException If no node exists at the specified path.
   * @throws RepositoryException If another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath, boolean giveTarget) throws PathNotFoundException,
                                                                                 RepositoryException;

  /**
   *  Return the item at the specified absolute path.
   *
   * @param workspace The workspace name.
   * @param absPath An absolute path.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItem(String workspace, String absPath) throws PathNotFoundException,
                                                                          RepositoryException;  
  
  /**
   * Return the item at the specified absolute path.
   *
   * @param workspace The workspace name.
   * @param absPath An absolute path.
   * @param system True to use the system session.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItemSys(String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                                             RepositoryException;  

  /**
   * Return the item at the specified absolute path.
   * If the item is a link and giveTarget has been
   * set to true, the target node will be returned.
   *
   * @param workspace The workspace name.
   * @param absPath An absolute path.
   * @param giveTarget Indicate if the target must be returned in case the item is a link.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItem(String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                                              RepositoryException;
  
  /**
   * Return the item at the specified absolute path.
   * If the item is a link and giveTarget has been set to true,
   * the target node will be returned.
   * 
   * @param workspace The workspace name.
   * @param absPath An absolute path.
   * @param giveTarget Indicate if the target must be returned in case the item is a link.
   * @param system The system provider.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItemGiveTargetSys(String workspace,
                                   String absPath,
                                   boolean giveTarget,
                                   boolean system) throws PathNotFoundException,
                                                  RepositoryException;

  /**
   * Return the item at the specified absolute path.
   *
   * @param session  The session is used to get the item.
   * @param absPath An absolute path.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItem(Session session, String absPath) throws PathNotFoundException,
                                                      RepositoryException;

  /**
   * Return the item at the specified absolute path.
   * If the item is a link and giveTarget has been set to true,
   * the target node will be returned.
   *
   * @param session The session is used to get the item.
   * @param absPath An absolute path.
   * @param giveTarget Indicate if the target must be returned in case the item is a link.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItem(Session session, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                          RepositoryException;

  /**
   * Return the item at the specified absolute path.
   * If the item is a link and giveTarget has been set to true,
   * the target node will be returned.
   *
   * @param session The session to use in order to get the item
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @param system The system provider.
   * @throws PathNotFoundException If the specified path cannot be found.
   * @throws RepositoryException If another error occurs.
   */
  public Item getItemTarget(Session session, String absPath, boolean giveTarget, boolean system) throws PathNotFoundException,
                                                                                                RepositoryException;

  /**
   * Return true if an item exists at absPath; otherwise returns false.
   * Also returns false if the specified absPath is malformed.
   *
   * @param session The session is used to get the item.
   * @param absPath An absolute path.
   * @return True if an item exists at absPath; otherwise return false.
   * @throws RepositoryException If an error occurs.
   */
  public boolean itemExists(Session session, String absPath) throws RepositoryException;
}
