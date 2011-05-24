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
package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.cms.documents.FavouriteService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 6, 2009
 * 3:39:40 AM
 */
@Deprecated
public class FavouriteServiceImpl implements FavouriteService {

  final static public String EXO_RESTORELOCATION = "exo:restoreLocation";
  private RepositoryService repositoryService;

  public FavouriteServiceImpl(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isFavouriter(String userName, Node node){
    if (userName == null)
      return false;
    try {
      if (node.isNodeType(EXO_FAVOURITE_NODE)) {
        Value[] favouriters = node.getProperty(EXO_FAVOURITER_PROPERTY).getValues();
        for (Value favouriter : favouriters) {
          if (userName.equals(favouriter.getString()))
            return true;
        }
      }
    } catch (Exception ex) {}
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void addFavourite(Node node, String userName) throws Exception {
    node.getSession().save();
    //add FAVOURITE mixin type if it does not exist in node
    if (!node.isNodeType(EXO_FAVOURITE_NODE)) {
      node.addMixin(EXO_FAVOURITE_NODE);
      node.setProperty(EXO_FAVOURITER_PROPERTY, new String[] {userName});
      node.getSession().save();
    }
    else {
      Property favouriteProperty = node.getProperty(EXO_FAVOURITER_PROPERTY);
      if (!foundValue(favouriteProperty, userName)) {
        Value[] values = favouriteProperty.getValues();
        Value[] newValues = new Value[values.length + 1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        ValueFactory valueFactory = node.getSession().getValueFactory();
        newValues[values.length] = valueFactory.createValue(userName);
        node.setProperty(EXO_FAVOURITER_PROPERTY, newValues);
        node.getSession().save();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeFavourite(Node node, String userName) throws Exception {
    node.getSession().save();
    if (node.isNodeType(EXO_FAVOURITE_NODE)) {
      Property favouriteProperty = node.getProperty(EXO_FAVOURITER_PROPERTY);
      if (foundValue(favouriteProperty, userName)) {
        Value[] values = favouriteProperty.getValues();

        if (values.length > 1) {
          Value[] newValues = new Value[values.length-1];
          int count = 0;
          for(Value value : values) {
            if (!userName.equals(value.getString()))
              newValues[count++] = value;
          }
          node.setProperty(EXO_FAVOURITER_PROPERTY, newValues);
        } else {
          node.removeMixin(EXO_FAVOURITE_NODE);
        }

        node.getSession().save();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllFavouriteNodes(String workspace,
                                         String repository,
                                         SessionProvider sessionProvider) throws Exception {

    StringBuilder query = new StringBuilder("SELECT * FROM ").
                    append(EXO_FAVOURITE_NODE).
                    append(" WHERE ").
                    append(EXO_FAVOURITER_PROPERTY).
                    append(" IS NOT NULL");

    return selectNodesByQueryString(workspace, sessionProvider, query.toString(), Query.SQL);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllFavouriteNodes(String workspace,
                                         SessionProvider sessionProvider) throws Exception {

    StringBuilder query = new StringBuilder("SELECT * FROM ").
                    append(EXO_FAVOURITE_NODE).
                    append(" WHERE ").
                    append(EXO_FAVOURITER_PROPERTY).
                    append(" IS NOT NULL");

    return selectNodesByQueryString(workspace, sessionProvider, query.toString(), Query.SQL);
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllFavouriteNodesByUser(String workspace,
                                               String repository,
                                               SessionProvider sessionProvider,
                                               String userName) throws Exception {

    StringBuilder query = new StringBuilder("SELECT * FROM ").append(EXO_FAVOURITE_NODE)
                                                             .append(" WHERE ")
                                                             .append(EXO_FAVOURITER_PROPERTY)
                                                             .append(" IS NOT NULL AND ")
                                                             .append(" CONTAINS (")
                                                             .append(EXO_FAVOURITER_PROPERTY)
                                                             .append(", '")
                                                             .append(userName)
                                                             .append("')");

    return selectNodesByQueryString(workspace,
                                    sessionProvider,
                                    query.toString(),
                                    Query.SQL);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllFavouriteNodesByUser(String workspace,
                                               SessionProvider sessionProvider,
                                               String userName) throws Exception {

    StringBuilder query = new StringBuilder("SELECT * FROM ").append(EXO_FAVOURITE_NODE)
                                                             .append(" WHERE ")
                                                             .append(EXO_FAVOURITER_PROPERTY)
                                                             .append(" IS NOT NULL AND ")
                                                             .append(" CONTAINS (")
                                                             .append(EXO_FAVOURITER_PROPERTY)
                                                             .append(", '")
                                                             .append(userName)
                                                             .append("')");

    return selectNodesByQueryString(workspace,
                                    sessionProvider,
                                    query.toString(),
                                    Query.SQL);
  }  


  /**
   * check if a value exists in the given property
   *
   * @throws RepositoryException
   */
  private boolean foundValue(Property property, String value) throws RepositoryException {
    if (property == null)
      return false;
    try {
      Value[] values = property.getValues();
      for (Value v : values)
        if (value.equals(v.getString()))
          return true;
      return false;
    } catch (ValueFormatException ex) {}
    try {
      Value v = property.getValue();
      if (value.equals(v.getString()))
        return true;
      return false;
    } catch (ValueFormatException ex) { return false; }
  }

  /**
   * Get all nodes by a query
   * @param workspace Get all favourite nodes from this workspace
   * @param sessionProvider The session provider which will be used to get session
   * @param queryString Query string
   * @param language Language SQL or XPath
   * @return List<Node> Get all favourite nodes
   * @throws Exception
   */
  private List<Node> selectNodesByQueryString(String workspace,
                                              SessionProvider sessionProvider,
                                              String queryString,
                                              String language) throws Exception {
    List<Node> ret = new ArrayList<Node>();

    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString, language);
    QueryResult queryResult = query.execute();

    NodeIterator nodeIter = queryResult.getNodes();
    while (nodeIter.hasNext()) {
      ret.add(nodeIter.nextNode());
    }
    return ret;
  }

}
