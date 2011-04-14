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

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 6, 2009
 * 3:38:36 AM
 */
/**
 * This class used to process the actions with favourite nodes
 */
@Deprecated
public interface FavouriteService {

  final static public String EXO_FAVOURITE_NODE = "exo:favourite";
  final static public String EXO_FAVOURITER_PROPERTY = "exo:favouriter";

  /**
   * Check if user is in favourite list of node
   * @param node Node to check
   * @param userName The user to check
   */
  public boolean isFavouriter(String userName, Node node);

  /**
   * Add favourite to node
   * @param node Add favourite to this node
   * @param userName The user added favourite
   * @throws Exception The exception will be raised if the node can not add mixin
   */
  public void addFavourite(Node node, String userName) throws Exception;


  /**
   * Remove favourite from node
   * @param node Remove favourite out of this node
   * @param userName Remove the name of current user out of property exo:favouriter
   * @throws Exception
   */
  public void removeFavourite(Node node, String userName) throws Exception;

  /**
   * Get all favourite nodes
   * @param workspace Get all favourite nodes from this workspace
   * @param repository Get all favourite nodes from this repository
   * @param sessionProvider The session provider which will be used to get session
   * @return List<Node> Get all favourite nodes
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllFavouriteNodes(String workspace,
                                         String repository,
                                         SessionProvider sessionProvider) throws Exception;
  
  /**
   * Get all favourite nodes
   * @param workspace Get all favourite nodes from this workspace
   * @param sessionProvider The session provider which will be used to get session
   * @return List<Node> Get all favourite nodes
   * @throws Exception
   */
  public List<Node> getAllFavouriteNodes(String workspace, SessionProvider sessionProvider) throws Exception;

  /**
   * Get all favourite nodes by user
   * @param workspace Get all favourite nodes from this workspace
   * @param repository Get all favourite nodes from this repository
   * @param sessionProvider The session provider which will be used to get session
   * @param userName User added favourite to the node
   * @return List<Node> All favourite node added by user
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllFavouriteNodesByUser(String workspace, String repository,
      SessionProvider sessionProvider, String userName) throws Exception;
  
  /**
   * Get all favourite nodes by user
   * @param workspace Get all favourite nodes from this workspace
   * @param sessionProvider The session provider which will be used to get session
   * @param userName User added favourite to the node
   * @return List<Node> All favourite node added by user
   * @throws Exception
   */
  public List<Node> getAllFavouriteNodesByUser(String workspace,
                                               SessionProvider sessionProvider,
                                               String userName) throws Exception;
}
