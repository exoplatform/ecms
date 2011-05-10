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
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 16, 2009
 * 9:55:29 AM
 */
public interface FavoriteService {

  /**
   * Add favorite to node
   * 
   * @param node Add favorite to this node
   * @param userName The user added favorite
   * @throws Exception The exception will be raised if the node can not add
   *           mixin
   */
 public void addFavorite(Node node, String userName) throws Exception;


  /**
   * Remove favorite from node
   * 
   * @param node Remove favourite out of this node
   * @param userName Remove the name of current user out of property
   *          exo:favouriter
   * @throws Exception
   */
 public void removeFavorite(Node node, String userName) throws Exception;

  /**
   * Get all favourite nodes by user
   * 
   * @param workspace Get all favorite nodes from this workspace
   * @param repository Get all favorite nodes from this repository
   * @param userName User added favorite to the node
   * @return List<Node> All favorite node added by user
   * @throws Exception
   */
 public List<Node> getAllFavoriteNodesByUser(String workspace, String repository,
     String userName) throws Exception;

  /**
   * Check if user is in favourite list of node
   * 
   * @param userName The user to check
   * @param node Node to check
   */
 public boolean isFavoriter(String userName, Node node) throws Exception ;

}
