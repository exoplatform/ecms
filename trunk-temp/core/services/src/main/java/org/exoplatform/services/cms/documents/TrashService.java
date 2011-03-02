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
 * 3:38:23 AM
 */

/**
 * This service used to move documents to trash foder or restore
 */

public interface TrashService {
  
  final static public String EXO_RESTORE_LOCATION = "exo:restoreLocation";
  final static public String RESTORE_PATH = "exo:restorePath";
  final static public String RESTORE_WORKSPACE = "exo:restoreWorkspace";
  
  /**
   * Move node to trash location
   * @param node Node will be moved to trash
   * @param trashPath The trash node path
   * @param trashWorkspace The trash workspace
   * @param repository The repository name
   * @param sessionProvider User session provider which will be used to get session
   * @throws Exception
   */
  public void moveToTrash(Node node, String trashPath, String trashWorkspace, String repository, 
      SessionProvider sessionProvider) throws Exception;
  
  /**
   * Restore node from trash
   * @param trashHomeNode trash home node
   * @param restorePath Restore path which will be used to restore
   * @param restoreWorkspace The workspace name of node which moved to trash
   * @param repository The repository name
   * @param sessionProvider User session provider which will be used to get session
   * @throws Exception
   */
  public void restoreFromTrash(Node trashHomeNode, String trashNodePath, 
      String repository, SessionProvider sessionProvider) throws Exception;
  
  /**
   * Get all nodes in trash location
   * @param trashWorkspace
   * @param repository
   * @param sessionProvider
   * @return List<Node> All nodes in trash
   * @throws Exception
   */
  public List<Node> getAllNodeInTrash(String trashWorkspace, String repository, 
      SessionProvider sessionProvider) throws Exception;
  
  /**
   * Get all nodes by user in trash location
   * @param trashWorkspace
   * @param repository
   * @param sessionProvider
   * @param userName
   * @return List<Node> all node in trash which moved by user
   * @throws Exception
   */
  public List<Node> getAllNodeInTrashByUser(String trashWorkspace, String repository, 
      SessionProvider sessionProvider, String userName) throws Exception;
  
  /**
   * Removes all 'relationable' property of nodes that have relation to this node
   * @param node 
   * @param sessionProvider
   * @param repository
   * @return 
   * @throws Exception
   */
  public void removeRelations(Node node, SessionProvider sessionProvider, 
  		String repository) throws Exception;
}
