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

import org.exoplatform.services.jcr.ext.common.SessionProvider;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import java.util.List;

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
  final static public String TRASH_ID = "exo:trashId";
    
  /**
   * Move node to trash location
   * @param node Node will be moved to trash
   * @param sessionProvider User session provider which will be used to get session
   * @throws Exception
   * @return -1: move failed.
   *          trashId if moved succesfully
   */
  public String moveToTrash(Node node,
                          SessionProvider sessionProvider) throws Exception;

  /**
   * Move node to trash location with deep
   * @param node
   * @param sessionProvider
   * @param deep
   * @return -1: move failed.
   *          trashId if moved succesfully
   * @throws Exception
   */
  public String moveToTrash(Node node, SessionProvider sessionProvider, int deep) throws Exception;
  
  /**
   * Restore node from trash
   * 
   * @param trashNodePath The path.
   * @param sessionProvider The session provider.
   * @throws Exception
   */
  public void restoreFromTrash(String trashNodePath, SessionProvider sessionProvider) throws Exception;
  
      
  /**
   * Get all nodes in trash location
   * 
   * @param sessionProvider
   * @return All nodes in trash
   * @throws Exception
   */
  public List<Node> getAllNodeInTrash(SessionProvider sessionProvider) throws Exception;

   
  /**
   * Get all nodes by user in trash location
   * @param sessionProvider
   * @param userName
   * @return all node in trash which moved by user
   * @throws Exception
   */
  public List<Node> getAllNodeInTrashByUser(SessionProvider sessionProvider,
                                            String userName) throws Exception;  

 
  /**
   * Removes all 'relationable' property of nodes that have relation to this node
   * @param node
   * @param sessionProvider
   * @throws Exception
   */
  public void removeRelations(Node node, SessionProvider sessionProvider) throws Exception;  
  
  /**
   * Check whether a given node is in Trash or not
   * @param node a specify node
   * @return <code>true</code> if node is in Trash, <code>false</code> otherwise.
   * @throws RepositoryException
   */
  public boolean isInTrash(Node node) throws RepositoryException;
  
  /**
   * Get the trash hone's node
   * @return <code>Node</code> the node of trash home
   */
  public Node getTrashHomeNode();
  /**
   * Get <code>Node</code> in trash folder by trashId
   * @param trashId ID of node will return
   * @return <code>Node</code> in trash folder with thrashId, <code>null</code> if thrashId doesn't exist in trash folder
   * @throws InvalidQueryException 
   * @throws RepositoryException
   * */
  public Node getNodeByTrashId(String trashId) throws InvalidQueryException, RepositoryException;
}
