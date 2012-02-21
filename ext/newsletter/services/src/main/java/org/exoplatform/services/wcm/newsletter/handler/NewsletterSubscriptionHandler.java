/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.newsletter.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * May 21, 2009
 */
public class NewsletterSubscriptionHandler {

  /** The log. */
  private static Log log = ExoLogger.getLogger(NewsletterSubscriptionHandler.class);

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The workspace. */
  private String workspace;

  private boolean isRemove;

  public boolean isRemove() {
    return isRemove;
  }

  public void setRemove(boolean isRemove) {
    this.isRemove = isRemove;
  }

  /**
   * Update permission for category node.
   *
   * @param subscriptionNode    node which is will be updated
   * @param subscriptionConfig  Category Object
   * @param isAddNew        is <code>True</code> if is add new category node and <code>False</code> if only update
   * @throws Exception      The Exception
   */
  public List<String> updatePermissionForSubscriptionNode(Node subscriptionNode,
                                                          NewsletterSubscriptionConfig subscriptionConfig,
                                                          boolean isAddNew) throws Exception {
    ExtendedNode extendedSubscriptionNode = ExtendedNode.class.cast(subscriptionNode);
    List<String> afterRemovePermisions = new ArrayList<String>();
    if (extendedSubscriptionNode.canAddMixin("exo:privilegeable")
        || extendedSubscriptionNode.isNodeType("exo:privilegeable")) {
      if(extendedSubscriptionNode.canAddMixin("exo:privilegeable"))
        extendedSubscriptionNode.addMixin("exo:privilegeable");
      List<String> newRedactors = new ArrayList<String>();
      if(subscriptionConfig.getRedactor() != null && subscriptionConfig.getRedactor().trim().length() > 0)
        newRedactors.addAll(Arrays.asList(subscriptionConfig.getRedactor().split(",")));

      // get all administrator of newsletter and moderator of category which contain this subscription
      Node categoryNode = subscriptionNode.getParent();
      Node categoriesNode = categoryNode.getParent();
      List<String> listModerators = NewsletterConstant.getAllPermissionOfNode(categoryNode);
      List<String> listAddministrators = new ArrayList<String>();
      if(categoriesNode.hasProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR)) {
        Value[] values = categoriesNode.getProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR).getValues();
        listAddministrators = NewsletterConstant.convertValuesToArray(values);
      }
      listAddministrators.addAll(listModerators);

      // Set permission is all for Redactors
      String[] permissions = new String[]{PermissionType.REMOVE, PermissionType.ADD_NODE, PermissionType.SET_PROPERTY};
      ExtendedNode categoryExtend = ExtendedNode.class.cast(categoryNode);
      for(String redactor : newRedactors){
        // Set read permission in category which contain subscription for this redactor
        if(!listModerators.contains(redactor))categoryExtend.setPermission(redactor, permissions);
      }
      categoryExtend.getSession().save();

      for(String redactor : newRedactors)
        extendedSubscriptionNode.setPermission(redactor, PermissionType.ALL);

      // Set permission is addNode, remove and setProperty for administrators
      permissions = new String[] { PermissionType.READ, PermissionType.ADD_NODE,
          PermissionType.REMOVE, PermissionType.SET_PROPERTY };
      for(String admin : listAddministrators){
        if(newRedactors.contains(admin)) continue;
        extendedSubscriptionNode.setPermission(admin, permissions);
        newRedactors.add(admin);
      }

      permissions = new String[]{PermissionType.READ, PermissionType.SET_PROPERTY}; // permission for normal users
      // set permission for any user when add new
      if(isAddNew){
        extendedSubscriptionNode.setPermission("any", permissions);
      }

      // set only read permission for normal users who are not administrator ,moderator or redactor.
      List<String> allPermissions = NewsletterConstant.getAllPermissionOfNode(subscriptionNode);
      if(allPermissions != null && allPermissions.size() > 0){
        for(String oldPer : allPermissions){
          if(!newRedactors.contains(oldPer)){
            extendedSubscriptionNode.removePermission(oldPer, PermissionType.ADD_NODE);
            extendedSubscriptionNode.removePermission(oldPer, PermissionType.REMOVE);
            extendedSubscriptionNode.removePermission(oldPer, PermissionType.CHANGE_PERMISSION);
            extendedSubscriptionNode.setPermission(oldPer, permissions);
            afterRemovePermisions.add(oldPer);
          }
        }
      }
    }
    extendedSubscriptionNode.save();
    return afterRemovePermisions;
  }

  /**
   * Instantiates a new newsletter subscription handler.
   *
   * @param repository the repository
   * @param workspace the workspace
   */
  @Deprecated
  public NewsletterSubscriptionHandler(String repository, String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    this.workspace = workspace;
  }
  
  /**
   * Instantiates a new newsletter subscription handler.
   *
   * @param workspace the workspace
   */
  public NewsletterSubscriptionHandler(String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    this.workspace = workspace;
  }

  /**
   * Gets the subscription form node.
   *
   * @param subscriptionNode the subscription node
   *
   * @return the subscription form node
   *
   * @throws Exception the exception
   */
  private NewsletterSubscriptionConfig getSubscriptionFormNode(Node subscriptionNode) throws Exception{
    NewsletterSubscriptionConfig subscriptionConfig = new NewsletterSubscriptionConfig();
    subscriptionConfig.setName(subscriptionNode.getName());
    subscriptionConfig.setTitle(subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_TITLE)
                                                .getString());
    if (subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION))
      subscriptionConfig.setDescription(subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION)
                                                        .getString());
    subscriptionConfig.setCategoryName(subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_CATEGORY_NAME)
                                                       .getString());
    // get permission for this category
    StringBuffer permission = new StringBuffer();
    for(String per : NewsletterConstant.getAllPermissionOfNode(subscriptionNode)){
      if(permission.length() > 0) permission.append(",");
      permission.append(per);
    }
    subscriptionConfig.setRedactor(permission.toString());
    return subscriptionConfig;
  }

  /**
   * Adds the.
   *
   * @param sessionProvider the session provider
   * @param portalName the portal name
   * @param subscription the subscription
   *
   * @throws Exception the exception
   */
  public void add(SessionProvider sessionProvider,
                  String portalName,
                  NewsletterSubscriptionConfig subscription) throws Exception {
    if (log.isInfoEnabled()) {
      log.info("Trying to add subcription " + subscription.getName());
    }
    Session session = null;
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      session = sessionProvider.getSession(workspace, manageableRepository);
      String path = NewsletterConstant.generateCategoryPath(portalName);
      Node categoryNode = ((Node)session.getItem(path)).getNode(subscription.getCategoryName());
      Node subscriptionNode = categoryNode.addNode(subscription.getName(), NewsletterConstant.SUBSCRIPTION_NODETYPE);
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_TITLE, subscription.getTitle());
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION, subscription.getDescription());
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_CATEGORY_NAME, subscription.getCategoryName());

      this.updatePermissionForSubscriptionNode(subscriptionNode, subscription, true);
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Add subcription " + subscription.getName() + " failed because of ", e);
      }
      throw e;
    }
  }

  /**
   * Edits the.
   *
   * @param portalName the portal name
   * @param subscription the subscription
   * @param sessionProvider the session provider
   */
  public void edit(SessionProvider sessionProvider, String portalName, NewsletterSubscriptionConfig subscription) {
    if (log.isInfoEnabled()) {
      log.info("Trying to edit subcription " + subscription.getName());
    }
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String path = NewsletterConstant.generateCategoryPath(portalName);
      Node categoryNode = ((Node)session.getItem(path)).getNode(subscription.getCategoryName());
      Node subscriptionNode = categoryNode.getNode(subscription.getName());
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_TITLE, subscription.getTitle());
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION, subscription.getDescription());
      subscriptionNode.setProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_CATEGORY_NAME, subscription.getCategoryName());
      List<String> candicateRemove = this.updatePermissionForSubscriptionNode(subscriptionNode, subscription, false);
      if(isRemove) {
        List<String> ableToRemove = NewsletterConstant.removePermission(subscriptionNode,
                                                                        null,
                                                                        candicateRemove,
                                                                        false,
                                                                        portalName,
                                                                        session);
        String [] removePer = new String [ableToRemove.size()];
        NewsletterConstant.removeAccessPermission(ableToRemove.toArray(removePer));
      }
      categoryNode.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Edit subcription " + subscription.getName() + " failed because of ", e);
      }
    }
  }

  /**
   * Delete.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscription the subscription
   * @param sessionProvider the session provider
   */
  public void delete(SessionProvider sessionProvider, String portalName,
                     String categoryName, NewsletterSubscriptionConfig subscription) {

    if (log.isInfoEnabled()) {
      log.info("Trying to delete subcription " + subscription.getName());
    }
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String path = NewsletterConstant.generateCategoryPath(portalName);
      Node categoryNode = ((Node)session.getItem(path)).getNode(categoryName);
      Node subscriptionNode = categoryNode.getNode(subscription.getName());
      List<String> candicateRemove = NewsletterConstant.getAllRedactor(portalName, session);
      if(isRemove) {
        List<String> ableToRemove = NewsletterConstant.removePermission(subscriptionNode,
                                                                        null,
                                                                        candicateRemove,
                                                                        false,
                                                                        portalName,
                                                                        session);
        String [] removePer = new String [ableToRemove.size()];
        NewsletterConstant.removeAccessPermission(ableToRemove.toArray(removePer));
      }
      subscriptionNode.remove();
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Delete subcription " + subscription.getName() + " failed because of ", e);
      }
    }
  }

  /**
   * Gets the subscriptions by category.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param sessionProvider the session provider
   *
   * @return the subscriptions by category
   *
   * @throws Exception the exception
   */
  public List<NewsletterSubscriptionConfig> getSubscriptionsByCategory(SessionProvider sessionProvider,
                                                                       String portalName,
                                                                       String categoryName) throws Exception {

    List<NewsletterSubscriptionConfig> listSubscriptions = new ArrayList<NewsletterSubscriptionConfig>();

    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String path = NewsletterConstant.generateCategoryPath(portalName);
    try {
      Node categoryNode = ((Node)session.getItem(path)).getNode(categoryName);
      NodeIterator nodeIterator = categoryNode.getNodes();
      while(nodeIterator.hasNext()){
        try{
          Node childNode = nodeIterator.nextNode();
          if(!childNode.isNodeType(NewsletterConstant.SUBSCRIPTION_NODETYPE)) continue;
          listSubscriptions.add(getSubscriptionFormNode(childNode));
        }catch(Exception ex){
          if (log.isErrorEnabled()) {
            log.error("Error when get subcriptions by category " + categoryName + " failed because of ", ex);
          }
        }
      }  
    } catch(RepositoryException repo) {
      return new ArrayList<NewsletterSubscriptionConfig>();
    }
    
    return listSubscriptions;
  }

  public List<NewsletterSubscriptionConfig> getSubscriptionByRedactor(String portalName,
                                                                      String categoryName,
                                                                      String userName,
                                                                      SessionProvider sessionProvider) throws Exception {
    List<NewsletterSubscriptionConfig> listSubs = new ArrayList<NewsletterSubscriptionConfig>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String categoriesPath = NewsletterConstant.generateCategoryPath(portalName);
    String categoryPath = categoriesPath + "/" + categoryName;
    Node categoryNode = (Node)session.getItem(categoryPath);
    Node subscriptionNode;
    NodeIterator subscriptionIterator = categoryNode.getNodes();
    while (subscriptionIterator.hasNext()) {
      try {
        subscriptionNode = subscriptionIterator.nextNode();
        if (!subscriptionNode.isNodeType(NewsletterConstant.SUBSCRIPTION_NODETYPE))
          continue;
        if (NewsletterConstant.hasPermission(userName, subscriptionNode))
          listSubs.add(getSubscriptionFormNode(subscriptionNode));
      } catch (Exception ex) {
        if (log.isErrorEnabled()) {
          log.error("Error when get subcriptions by category " + categoryName + " failed because of ",
                  ex);
        }
      }
    }
    return listSubs;
  }

  /**
   * Gets the subscription ids by public user.
   *
   * @param portalName the portal name
   * @param userEmail the user email
   * @param sessionProvider the session provider
   *
   * @return the subscription ids by public user
   *
   * @throws Exception the exception
   */
  public List<NewsletterSubscriptionConfig> getSubscriptionIdsByPublicUser(SessionProvider sessionProvider,
                                                                           String portalName,
                                                                           String userEmail) throws Exception {
    List<NewsletterSubscriptionConfig> listSubscriptions = new ArrayList<NewsletterSubscriptionConfig>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    String sqlQuery = "select * from " + NewsletterConstant.SUBSCRIPTION_NODETYPE +
                      " where " + NewsletterConstant.SUBSCRIPTION_PROPERTY_USER + " = '" + userEmail + "'";
    Query query = queryManager.createQuery(sqlQuery, Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator nodeIterator = queryResult.getNodes();
    while(nodeIterator.hasNext()){
      try{
        listSubscriptions.add(getSubscriptionFormNode(nodeIterator.nextNode()));
      } catch(Exception ex) {
        if (log.isErrorEnabled()) {
          log.error("getSubscriptionIdsByPublicUser() failed because of ", ex);
        }
      }
    }
    return listSubscriptions;
  }

  /**
   * Gets the subscriptions by name.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subCriptionName the sub cription name
   * @param sessionProvider the session provider
   *
   * @return the subscriptions by name
   *
   * @throws Exception the exception
   */
  public NewsletterSubscriptionConfig getSubscriptionsByName(SessionProvider sessionProvider,
                                                             String portalName,
                                                             String categoryName,
                                                             String subCriptionName) throws Exception {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String path = NewsletterConstant.generateCategoryPath(portalName);
      Node categoryNode = ((Node)session.getItem(path)).getNode(categoryName);
      try {
        Node subNode = categoryNode.getNode(subCriptionName);
        return getSubscriptionFormNode(subNode);
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Node name is not found: " + subCriptionName);
        }
        return null;
      }
  }

  /**
   * Gets the number of newsletters waiting.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subScriptionName the sub scription name
   * @param sessionProvider the session provider
   *
   * @return the number of newsletters waiting
   *
   * @throws Exception the exception
   */
  public long getNumberOfNewslettersWaiting(SessionProvider sessionProvider,
                                            String portalName,
                                            String categoryName,
                                            String subScriptionName) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String path = NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName + "/" + subScriptionName;
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    String sqlQuery = "select * from " + NewsletterConstant.ENTRY_NODETYPE +
                      " where jcr:path LIKE '" + path + "[%]/%' and " + NewsletterConstant.ENTRY_PROPERTY_STATUS +
                      " = '" + NewsletterConstant.STATUS_AWAITING + "'";
    Query query = queryManager.createQuery(sqlQuery, Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator nodeIterator = queryResult.getNodes();
    return nodeIterator.getSize();
  }
}
