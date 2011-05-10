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
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Jan 22, 2007
 */
public interface CommentsService {

  /**
   * Comment the document is specified by the node by giving the commentor, email, site, comment and
   * language params
   * Any language belongs to this document can be commented.
   * This method uses variables to store values which are commented from user for all kind languages
   * of this document
   * @param document        The node document is commented
   * @param commentor       The name of current user
   *                        Can be <code>null</code>
   * @param email           The email of current user
   *                        Can be <code>null</code>
   * @param site            The site of current user
   *                        Can be <code>null</code>
   * @param comment         The comment's content
   * @param language        The language of this document is commented
   * @see                   Node
   * @throws Exception
   */
  public void addComment(Node document,
                         String commentor,
                         String email,
                         String site,
                         String comment,
                         String language) throws Exception;

  /**
   * Update comment for document: set new comment for node
   * @param commentNode the node that need to update comment
   * @param newComment the new comment is set for node
   * @throws Exception
   */
  public void updateComment(Node commentNode, String newComment) throws Exception;

  /**
   * Delete comment of document by given comment node
   * @param commentNode given comment node
   * @throws Exception
   */
  public void deleteComment(Node commentNode) throws Exception;
  /**
   * Gets all comments from the specified node
   * @param document        The node document is commented
   * @param language        The language of this document is commented
   * @see                   Node
   * @throws Exception
   */
  public List<Node> getComments(Node document, String language) throws Exception ;

}
