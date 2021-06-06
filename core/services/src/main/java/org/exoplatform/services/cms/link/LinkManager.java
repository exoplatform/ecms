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

import javax.jcr.*;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Supplies API to work with the linked node or the link included in a node.
 *
 * @LevelAPI Platform
 */
public interface LinkManager {

  /**
   * Creates a new link with a given type.
   *
   * @param parent The parent node that contains the new link.
   * @param linkType The primary nodetype of the link that must be sub-type of
   *          "exo:symlink". Its default value is "exo:symlink".
   * @param target Target of the link.
   * @return The created link.
   * @throws RepositoryException if the link cannot be created for any reason.
   */
  public Node createLink(Node parent, String linkType, Node target) throws RepositoryException;

  /**
   * Creates a new link with "exo:symlink" type.
   *
   * @param parent The parent node that contains the new link.
   * @param target Target of the link.
   * @return The created link.
   * @throws RepositoryException if the link cannot be created for any reason.
   */
  public Node createLink(Node parent, Node target) throws RepositoryException;

  /**
   * Creates a new link with given type and name.
   *
   * @param parent The parent node that contains the new link.
   * @param linkType The primary nodetype of the link that must be sub-type of
   *          "exo:symlink". Its default value is "exo:symlink".
   * @param target Target of the link.
   * @param linkName Name of the link.
   * @return The created link.
   * @throws RepositoryException if the link cannot be created for any reason.
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName)
      throws RepositoryException;
  
  /**
   * Creates a new link with given type, name and title.
   *
   * @param parent The parent node that contains the new link.
   * @param linkType The primary nodetype of the link that must be sub-type of
   *          "exo:symlink". Its default value is "exo:symlink".
   * @param target Target of the link.
   * @param linkName Name of the link.
   * @param linkTitle Title of the link.
   * @return The created link.
   * @throws RepositoryException if the link cannot be created for any reason.
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName, String linkTitle)
      throws RepositoryException;

  /**
   * Updates a link node.
   *
   * @param link The link node to be updated.
   * @param target Target of the link node.
   * @return The updated link node.
   * @throws RepositoryException if the link cannot be updated for any reason.
   */
  public Node updateLink(Node link, Node target) throws RepositoryException;

  /**
   * Gets the target node of a given link.
   *
   * @param link The node of "exo:symlink" type.
   * @param system Indicates whether the target node must be retrieved using a
   *               system or user session. In case the target and the
   *               link are not in the same workspace, the system session will be used.
   * @return The target node.
   * @throws ItemNotFoundException if the target node cannot be found.
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node.
   */
  public Node getTarget(Node link, boolean system) throws ItemNotFoundException,
      RepositoryException;

  /**
   * Gets the target node of a given link using the user session.
   *
   * @param link The node of "exo:symlink" type.
   * @return The target node.
   * @throws ItemNotFoundException if the target node cannot be found.
   * @throws RepositoryException if an unexpected error occurs while retrieving
   *           the target node
   */
  public Node getTarget(Node link) throws ItemNotFoundException, RepositoryException;

  /**
   * Checks if the target node of a given link can be reached using the user session.
   *
   * @param link The node of "exo:symlink" type.
   * @return "True" if the target node is reachable. Otherwise, it returns "false".
   * @throws RepositoryException if an unexpected error occurs.
   */
  public boolean isTargetReachable(Node link) throws RepositoryException;

  /**
   * Checks if the target node of a given link can be reached using the user session.
   *
   * @param link The node of "exo:symlink" type.
   * @param system The boolean value which indicates if the system session is needed.
   * @return "True" if the target node is reachable. Otherwise, it returns "false".
   * @throws RepositoryException if an unexpected error occurs.
   */
  public boolean isTargetReachable(Node link, boolean system) throws RepositoryException;

  /**
   * Checks if a given item is link.
   *
   * @param item The item to be checked.
   * @return "True" if the given item is link. Otherwise, it returns "false".
   *
   * @throws RepositoryException if an unexpected error occurs
   */
  public boolean isLink(Item item) throws RepositoryException;

  /**
   * @param session
   * @param path
   * @return "True" if the given path is link for document or file. Otherwise, it
   *         returns "false".
   */
  default boolean isFileOrParentALink(Session session, String path) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the primary nodetype of a target node by a given link.
   *
   * @param link The given link.
   * @return The primary nodetype of the target node.
   * @throws RepositoryException if an unexpected error occurs.
   */
  public String getTargetPrimaryNodeType(Node link) throws RepositoryException;
  
  /**
   * Gets all links of a target node by a given link type.
   *
   * @param targetNode The target node.
   * @param linkType The given link type.
   * @return The list of links.
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType) throws Exception;
  
  /**
   * Gets all links of a target node by a given link type.
   *
   * @param targetNode The target node.
   * @param linkType The given link type.
   * @param sessionProvider The session provider.
   * @return The list of links.
   * @throws Exception
   */
  public List<Node> getAllLinks(Node targetNode, String linkType, SessionProvider sessionProvider) throws Exception;
  
  /**
   * Updates information for a symlink, including "exo:title", "exo:dateCreated", "exo:dateModified", "publication:liveDate" and "exo:index".
   *
   * @param link The link node.
   * @throws Exception
   */
  public void updateSymlink(Node link) throws Exception;

  /**
   * get the link of the document inside the folder
   *
   * @param contentUUID Jcr uuid of the file
   * @param folderPath path of the target folder
   * @param workspace target workspace
   */
  default List<Node> getNodeSymlinksUnderFolder(String contentUUID, String folderPath, String workspace) {
    throw new UnsupportedOperationException("Not implemented");
  }

}
