/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.clouddrive.jcr;

import java.util.Collection;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * An abstraction for a {@link Node} lookup in JCR repository.<br>
 * We need this to decouple WCM's NodeFinder from dependencies of Cloud Drive
 * core. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NodeFinder.java 00000 Feb 26, 2013 pnedonosko $
 */
public interface NodeFinder {

  /**
   * Return JCR item by given path.
   *
   * @param userSession {@link Session}
   * @param path {@link String}
   * @param symlinkTarget boolean, if {@code true} and found item is a symlink
   *          node, return the symlink target node, if {@code false} just return
   *          the item obtained from the session
   * @return the item
   * @throws PathNotFoundException if item not found by given path
   * @throws RepositoryException if repository error eccurs
   */
  Item getItem(Session userSession, String path, boolean symlinkTarget) throws PathNotFoundException, RepositoryException;

  /**
   * Find JCR item by given path, if the path points to a symlink this method
   * return a target node of the link.
   *
   * @param userSession {@link Session}
   * @param path {@link String}
   * @return the item
   * @throws PathNotFoundException if item not found by given path
   * @throws RepositoryException if repository error eccurs
   */
  Item findItem(Session userSession, String path) throws PathNotFoundException, RepositoryException;

  /**
   * Find nodes linked to given node.
   *
   * @param session the session
   * @param uuid {@link String}
   * @return {@link Collection} of nodes
   * @throws PathNotFoundException the path not found exception
   * @throws RepositoryException the repository exception
   */
  Collection<Node> findLinked(Session session, String uuid) throws PathNotFoundException, RepositoryException;

  /**
   * Align given name to current repository conventions by removing or escaping
   * forbidden characters.
   * 
   * @param name {@link String}
   * @return {@link String} cleaned name
   */
  String cleanName(String name);

  /**
   * Return user home node of given user.
   *
   * @param userName String with user name in organization
   * @return {@link Node} user home node
   * @throws Exception the exception
   */
  Node getUserNode(String userName) throws Exception;

}
