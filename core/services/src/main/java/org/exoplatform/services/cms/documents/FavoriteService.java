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
package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;

/**
 * Manages favorites of users.
 *
 * @LevelAPI Experimental
 */
public interface FavoriteService {

  /**
   * Adds favorite to a node.
   * 
   * @param node The node to which favorite is added.
   * @param userName The user who added favorite to the node.
   * @throws Exception The exception will be raised if the node can not add
   *           mixin.
   */
 public void addFavorite(Node node, String userName) throws Exception;


  /**
   * Removes favorite from a node.
   * 
   * @param node The node from which favorite is removed.
   * @param userName The user who removed favorite from the node.
   * @throws Exception
   */
 public void removeFavorite(Node node, String userName) throws Exception;

  /**
   * Gets all favorite nodes by a given user.
   * 
   * @param workspace The workspace from which the favorite nodes are got.
   * @param repository The repository from which the favorite nodes are got.
   * @param userName The user who added favorite.
   * @return The favorite node added by the user.
   * @throws Exception
   */
 public List<Node> getAllFavoriteNodesByUser(String workspace, String repository,
     String userName) throws Exception;

  /**
   * Checks if a node is in favorite list of a given user.
   * 
   * @param userName The given user.
   * @param node The node to be checked.
   */
 public boolean isFavoriter(String userName, Node node) throws Exception ;

}
