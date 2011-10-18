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
package org.exoplatform.services.cms.voting;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Jan 17, 2007
 */
public interface VotingService {

  /**
   * Voting the document is specified by the node by giving the rate, username, and language params
   * Any language belongs to this document can be voted.
   * This method uses variables to store values which are voted from user for all kind languages
   * of this document
   * @param document        The node document for voting
   * @param rate            The number rate for voting
   * @param userName        The username of current user is voting.
   *                        Can not be <code>null</code>
   * @param language        The language of this document for voting
   *                        Can not be <code>null</code>
   * @see                   Node
   * @throws Exception
   */
  public void vote(Node document, double rate, String userName, String language) throws Exception;

  /**
   * Gets total voting for all kind languages of this document is specified by node
   * @param node            The node document is specified to get total voting
   * @see                   Node
   * @return
   * @throws Exception
   */
  public long getVoteTotal(Node node) throws Exception;

  /**
   * Check if user had already voted on the given node or not
   * 
   * @param node the node that will be voted
   * @param userName the name of user had voted
   * @param language language of to-be-voted node
   * @return
   * @throws Exception
   */
  public boolean isVoted(Node node, String userName, String language) throws Exception;
  
  /**
   * returns user's vote value on the given node
   * 
   * @param node the node that will be voted
   * @param userName the name of user had voted
   * @param language language of to-be-voted node
   * @return
   * @throws Exception
   */  
  public double getVoteValueOfUser(Node node, String userName, String language) throws Exception; 

}
