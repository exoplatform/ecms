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
 * Finds a node with a given path.
 * If the node path contains sub-paths to exo:symlink nodes,
 * the real link node will be found.
 *
 * @LevelAPI Experimental
 */
public interface NodeFinder {

  /**
   * Gets a node at the relative path related to the ancestor node.
   *
   * @param ancestorNode The ancestor node.
   * @param relativePath The relative path.
   * @throws PathNotFoundException if no node exists at the specified path.
   * @throws RepositoryException if another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath) throws PathNotFoundException,
                                                             RepositoryException;

  /**
   * Gets a node at the relative path related to the ancestor node.
   *
   * @param ancestorNode The ancestor node.
   * @param relativePath The relative path.
   * @param giveTarget If the node is link and giveTarget has been set to "true",
   * the target node will be returned.
   * @throws PathNotFoundException if no node exists at the specified path.
   * @throws RepositoryException if another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath, boolean giveTarget) throws PathNotFoundException,
                                                                                 RepositoryException;

  /**
   * Gets an item at the specified absolute path.
   *
   * @param workspace The workspace name.
   * @param absPath The absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(String workspace, String absPath) throws PathNotFoundException,
                                                                          RepositoryException;  
  
  /**
   * Gets an item at the specified absolute path.
   *
   * @param workspace The workspace name.
   * @param absPath The absolute path.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemSys(String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                                             RepositoryException;  

  /**
   * Gets an item at the specified absolute path.
   *
   * @param workspace The workspace name.
   * @param absPath The absolute path.
   * @param giveTarget If the item is link and giveTarget has been
   * set to "true", the target node will be returned.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                                              RepositoryException;
  
  /**
   * Gets an item at the specified absolute path.
   * 
   * @param workspace The workspace name.
   * @param absPath The absolute path.
   * @param giveTarget If the item is link and giveTarget has been set to "true",
   * the target node will be returned.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemGiveTargetSys(String workspace,
                                   String absPath,
                                   boolean giveTarget,
                                   boolean system) throws PathNotFoundException,
                                                  RepositoryException;

  /**
   * Gets an item at the specified absolute path.
   *
   * @param session The session that is used to get the item.
   * @param absPath The absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(Session session, String absPath) throws PathNotFoundException,
                                                      RepositoryException;

  /**
   * Gets an item at the specified absolute path.
   *
   * @param session The session is used to get the item.
   * @param absPath The absolute path.
   * @param giveTarget If the item is link and giveTarget has been set to "true",
   * the target node will be returned.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(Session session, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                          RepositoryException;

  /**
   * Gets an item at the specified absolute path.
   *
   * @param session The session which is used to get the item.
   * @param absPath The absolute path.
   * @param giveTarget If the item is link and giveTarget has been set to "true",
   * the target node will be returned.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemTarget(Session session, String absPath, boolean giveTarget, boolean system) throws PathNotFoundException,
                                                                                                RepositoryException;

  /**
   * Checks if an item exists at the specified absolute path.
   *
   * @param session The session that is used to get the item.
   * @param absPath The absolute path.
   * @return "True" if the item exists at the absolute path.
   * "False" if the item does not exist or the specified absolute path is malformed.
   * @throws RepositoryException if an error occurs.
   */
  public boolean itemExists(Session session, String absPath) throws RepositoryException;
}
