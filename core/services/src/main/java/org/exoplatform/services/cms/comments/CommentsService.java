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
package org.exoplatform.services.cms.comments;

import java.util.List;

import javax.jcr.Node;

/**
 * Manages comments related to the content.
 *
 * @LevelAPI Experimental
 */
public interface CommentsService {

  /**
   * Adds comment to a document.
   * This method uses variables to store values which are commented from the current user for all language types
   * of this document.
   * @param document The document node that is commented.
   * @param commentor Name of the current user.
   * If <code>null</code>, the commentor is considered as "anonymous".
   * @param email Email of the current user.
   * This param can be <code>null</code>.
   * @param site Site of the current user.
   * This param can be <code>null</code>.
   * @param comment Content of the comment.
   * @param language Language of the document that is commented.
   * @see Node
   * @throws Exception
   */
  public void addComment(Node document,
                         String commentor,
                         String email,
                         String site,
                         String comment,
                         String language) throws Exception;

  /**
   * Updates comment for a document.
   *
   * @param commentNode Node of the comment that will be updated.
   * @param newComment The new comment that is set for the node.
   * @throws Exception
   */
  public void updateComment(Node commentNode, String newComment) throws Exception;

  /**
   * Deletes comment of a document by a given comment node.
   *
   * @param commentNode The given comment node.
   * @throws Exception
   */
  public void deleteComment(Node commentNode) throws Exception;

  /**
   * Gets all comments of a specified document node.
   *
   * @param document The document node that is commented.
   * @param language Language of the document that is commented.
   * @see Node
   * @throws Exception
   */
  public List<Node> getComments(Node document, String language) throws Exception ;

}
