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
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Mar 14, 2009
 */
public interface NodeFinder {

  /**
   * Returns the node at relPath relative to ancestor node.
   *
   * @param ancestorNode The ancestor of the node to retrieve from which we
   *          start.
   * @param relativePath The relative path of the node to retrieve.
   * @throws PathNotFoundException If no node exists at the specified path.
   * @throws RepositoryException if another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath) throws PathNotFoundException,
                                                             RepositoryException;

  /**
   * Returns the node at relPath relative to ancestor node. If the node is a
   * link and giveTarget has been set to <code>true</code>, the target node will
   * be returned
   *
   * @param ancestorNode The ancestor of the node to retrieve from which we
   *          start.
   * @param relativePath The relative path of the node to retrieve.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @throws PathNotFoundException If no node exists at the specified path.
   * @throws RepositoryException if another error occurs.
   */
  public Node getNode(Node ancestorNode, String relativePath, boolean giveTarget) throws PathNotFoundException,
                                                                                 RepositoryException;

  /**
   * Returns the item at the specified absolute path.
   *
   * @param repository The name of repository
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  @Deprecated
  public Item getItem(String repository, String workspace, String absPath) throws PathNotFoundException,
                                                                          RepositoryException;
  
  /**
   * Returns the item at the specified absolute path.
   *
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(String workspace, String absPath) throws PathNotFoundException,
                                                                          RepositoryException;  

  /**
   * Returns the item at the specified absolute path.
   *
   * @param repository The name of repository
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  @Deprecated
  public Item getItemSys(String repository, String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                                             RepositoryException;
  
  /**
   * Returns the item at the specified absolute path.
   *
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemSys(String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                                             RepositoryException;  

  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   *
   * @param repository The name of repository
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  @Deprecated
  public Item getItem(String repository, String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                                              RepositoryException;
  
  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   *
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                                              RepositoryException;
  


  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   *
   * @param repository The name of repository
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @param system system provider
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  @Deprecated
  public Item getItemGiveTargetSys(String repository,
                                   String workspace,
                                   String absPath,
                                   boolean giveTarget,
                                   boolean system) throws PathNotFoundException,
                                                  RepositoryException;
  
  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   * 
   * @param workspace The name of workspace
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @param system system provider
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemGiveTargetSys(String workspace,
                                   String absPath,
                                   boolean giveTarget,
                                   boolean system) throws PathNotFoundException,
                                                  RepositoryException;

  /**
   * Returns the item at the specified absolute path.
   *
   * @param session The session to use in order to get the item
   * @param absPath An absolute path.
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(Session session, String absPath) throws PathNotFoundException,
                                                      RepositoryException;

  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   *
   * @param session The session to use in order to get the item
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItem(Session session, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                          RepositoryException;

  /**
   * Returns the item at the specified absolute path. If the item is a link and
   * giveTarget has been set to <code>true</code>, the target node will be
   * returned
   *
   * @param session The session to use in order to get the item
   * @param absPath An absolute path.
   * @param giveTarget Indicates if the target must be returned in case the item
   *          is a link
   * @param system system provider
   * @throws PathNotFoundException if the specified path cannot be found.
   * @throws RepositoryException if another error occurs.
   */
  public Item getItemTarget(Session session, String absPath, boolean giveTarget, boolean system) throws PathNotFoundException,
                                                                                                RepositoryException;

  /**
   * Returns <code>true</code> if an item exists at absPath; otherwise returns
   * <code>false</code>. Also returns <code>false</code> if the specified
   * absPath is malformed.
   *
   * @param session The session to use in order to get the item
   * @param absPath An absolute path.
   * @return <code>true</code> if an item exists at absPath; otherwise returns
   *         <code>false</code>.
   * @throws RepositoryException if an error occurs.
   */
  public boolean itemExists(Session session, String absPath) throws RepositoryException;
}
