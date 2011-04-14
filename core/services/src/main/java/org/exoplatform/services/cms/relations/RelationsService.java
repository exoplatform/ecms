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
package org.exoplatform.services.cms.relations;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
/**
 * @author monica franceschini
 */
public interface RelationsService {

  /**
   * Returns true is the given node has relation
   * @param node              Specify the node wants to check relation
   * @see                     Node
   * @throws Exception
   */
  public boolean hasRelations(Node node) throws Exception;

  /**
   * Gets all node that has relation to the given node
   * @param node              Specify the node wants to get all node relative to it
   * @param repository        The name of repository
   * @param provider          The SessionProvider object is used to managed Sessions
   * @see                     Node
   * @see                     SessionProvider
   * @throws Exception
   */
  @Deprecated
  public List<Node> getRelations(Node node, String repository, SessionProvider provider) throws Exception;
  
  /**
   * Gets all node that has relation to the given node
   * @param node              Specify the node wants to get all node relative to it
   * @param provider          The SessionProvider object is used to managed Sessions
   * @see                     Node
   * @see                     SessionProvider
   * @throws Exception
   */
  public List<Node> getRelations(Node node, SessionProvider provider) throws Exception;  

  /**
   * Removes the relation to the given node by specified the relationPath params
   * @param node              Specify the node wants to remove a relation
   * @param relationPath      The path of relation
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
  @Deprecated
  public void removeRelation(Node node, String relationPath, String repository) throws Exception;
  
  /**
   * Removes the relation to the given node by specified the relationPath params
   * @param node              Specify the node wants to remove a relation
   * @param relationPath      The path of relation
   * @see                     Node
   * @throws Exception
   */
  public void removeRelation(Node node, String relationPath) throws Exception;  

  /**
   * Inserts a new relation is specified by relationPath params to the given node
   * @param node              Specify the node wants to insert a new relation
   * @param relationPath      The path of relation
   * @param workspaceName     The name of workspace
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
  @Deprecated
  public void addRelation(Node node, String relationPath, String workspaceName, String repository) throws Exception;
  
  /**
   * Inserts a new relation is specified by relationPath params to the given node
   * @param node              Specify the node wants to insert a new relation
   * @param relationPath      The path of relation
   * @param workspaceName     The name of workspace
   * @see                     Node
   * @throws Exception
   */
  public void addRelation(Node node, String relationPath, String workspaceName) throws Exception;  

  /**
   * Initial the root of relation node and its sub node
   * @param repository        The name of repository
   * @throws Exception
   */
  @Deprecated
  public void init(String repository) throws Exception ;
  
  /**
   * Initial the root of relation node and its sub node
   * @throws Exception
   */
  public void init() throws Exception ;  
}
