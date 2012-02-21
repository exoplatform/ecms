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
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.config.NewsletterUserConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * May 21, 2009
 */
public class NewsletterManageUserHandler {

  /** The log. */
  private static Log log = ExoLogger.getLogger(NewsletterManageUserHandler.class);

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The workspace. */
  private String workspace;

  /**
   * Instantiates a new newsletter manage user handler.
   *
   * @param repository the repository
   * @param workspace the workspace
   */
  @Deprecated
  public NewsletterManageUserHandler(String repository, String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    this.workspace = workspace;
  }
  
  /**
  * Instantiates a new newsletter manage user handler.
  *
  * @param workspace the workspace
  */
  public NewsletterManageUserHandler(String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    this.workspace = workspace;
  }

  /**
   * Gets the user from node.
   *
   * @param userNode the user node
   *
   * @return the user from node
   *
   * @throws Exception the exception
   */
  private NewsletterUserConfig getUserFromNode(Node userNode) throws Exception{
    NewsletterUserConfig user = new NewsletterUserConfig();
    user.setMail(userNode.getProperty(NewsletterConstant.USER_PROPERTY_MAIL).getString());
    user.setBanned(userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean());
    return user;
  }

  /**
   * Convert values to array.
   *
   * @param values the values
   *
   * @return the list< string>
   */
  private List<String> convertValuesToArray(Value[] values){
    List<String> listString = new ArrayList<String>();
    for(Value value : values){
      try {
        listString.add(value.getString());
      }catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Error when convert values to array: ", e);
        }
      }
    }
    return listString;
  }

  /**
   * Gets the all administrator.
   *
   * @param portalName the portal name
   *
   * @return the all administrator
   */
  public List<String> getAllAdministrator(SessionProvider sessionProvider, String portalName) {
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      Node categoriesNode = (Node) session.getItem(NewsletterConstant.generateCategoryPath(portalName));
      if (categoriesNode.hasProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR)) {
        return convertValuesToArray(categoriesNode.getProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR)
                                                  .getValues());
      }
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("getAllAdministrator() failed because of ", ex);
      }
    }
    return new ArrayList<String>();
  }

  /**
   * Adds the administrator.
   *
   * @param portalName the portal name
   * @param userId the user id
   *
   * @throws Exception the exception
   */
  public void addAdministrator(SessionProvider sessionProvider, String portalName, String userId) throws Exception{
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      Node categoriesNode = (Node) session.getItem(NewsletterConstant.generateCategoryPath(portalName));
      List<String> listUsers = new ArrayList<String>();
      if (categoriesNode.hasProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR))
        listUsers.addAll(convertValuesToArray(categoriesNode.getProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR)
                                                            .getValues()));
      if (listUsers.contains(userId)) {
        return;
      }
      listUsers.add(userId);
      categoriesNode.setProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR, listUsers.toArray(new String[]{}));
      Node categoryNode;
      ExtendedNode extendedCategoryNode ;
      String[] permissions = new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY, PermissionType.REMOVE};
      for(NodeIterator iterator = categoriesNode.getNodes(); iterator.hasNext(); ){
        categoryNode = iterator.nextNode();
        // Update permission for category node
        extendedCategoryNode = ExtendedNode.class.cast(categoryNode);
        if (extendedCategoryNode.canAddMixin("exo:privilegeable") || extendedCategoryNode.isNodeType("exo:privilegeable")) {
          if(extendedCategoryNode.canAddMixin("exo:privilegeable"))
            extendedCategoryNode.addMixin("exo:privilegeable");
          extendedCategoryNode.setPermission(userId, permissions);
        }

        // update permission for subscriptions node which are contained in this category
        NewsletterConstant.addPermissionsFromCateToSubs(categoryNode, new String[]{userId}, permissions);
      }
      session.save();
    }catch(Exception ex){
      if (log.isInfoEnabled()) {
        log.info("Add administrator for newsletter failed because of ", ex);
      }
    }
  }

  /**
   * Delete user addministrator.
   *
   * @param portalName the portal name
   * @param userId the user id
   *
   * @throws Exception the exception
   */
  public void deleteUserAddministrator(SessionProvider sessionProvider,
                                       String portalName,
                                       String userId) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    Node categoriesNode = (Node) session.getItem(NewsletterConstant.generateCategoryPath(portalName));
    List<String> listUsers = new ArrayList<String>();
    if (categoriesNode.hasProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR))
      listUsers.addAll(convertValuesToArray(categoriesNode.getProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR)
                                                          .getValues()));
    listUsers.remove(userId);
    categoriesNode.setProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR, listUsers.toArray(new String[]{}));
    session.save();
  }

  /**
   * Adds the.
   *
   * @param portalName the portal name
   * @param userMail the user mail
   * @param sessionProvider the session provider
   *
   * @return the node
   * @throws Exception
   */
  public Node add(SessionProvider sessionProvider, String portalName, String userMail) throws Exception {
    if (log.isInfoEnabled()) {
      log.info("Trying to add user " + userMail);
    }
    Node userNode = null;
    Session session = null;
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      session = sessionProvider.getSession(workspace, manageableRepository);
      String userPath = NewsletterConstant.generateUserPath(portalName);
      Node userFolderNode = (Node)session.getItem(userPath);
      userNode = userFolderNode.addNode(userMail, NewsletterConstant.USER_NODETYPE);
      userNode.setProperty(NewsletterConstant.USER_PROPERTY_MAIL, userMail);
      userNode.setProperty(NewsletterConstant.USER_PROPERTY_BANNED, false);
      userNode.setProperty(NewsletterConstant.USER_PROPERTY_VALIDATION_CODE, "PublicUser"
          + IdGenerator.generate());
      userNode.setProperty(NewsletterConstant.USER_PROPERTY_IS_CONFIRM, false);
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Add user " + userMail + " failed because of ", e);
      }
    }
    if(userNode == null){
      throw new Exception("Can not add new user");
    }
    return userNode;
  }

  /**
   * Gets the user node by email.
   *
   * @param portalName the portal name
   * @param userMail the user mail
   * @param session the session
   *
   * @return the user node by email
   *
   * @throws Exception the exception
   */
  private Node getUserNodeByEmail(SessionProvider sessionProvider,
                                  String portalName,
                                  String userMail) throws Exception {
    try{
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String userPath = NewsletterConstant.generateUserPath(portalName);
      Node userFolderNode = (Node)session.getItem(userPath);
      return userFolderNode.getNode(userMail);
    }catch(Exception ex){
      return null;
    }
  }

  /**
   * Change ban status.
   *
   * @param portalName the portal name
   * @param userMail the user mail
   * @param isBanClicked the is ban clicked
   */
  public void changeBanStatus(SessionProvider sessionProvider,
                              String portalName,
                              String userMail,
                              boolean isBanClicked) {
    if (log.isInfoEnabled()) {
      log.info("Trying to ban/unban user " + userMail);
    }
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      Node userNode = getUserNodeByEmail(sessionProvider, portalName, userMail);
      if (userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean() == isBanClicked) return;
      userNode.setProperty(NewsletterConstant.USER_PROPERTY_BANNED,
                           !userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean());
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Ban/UnBan user " + userMail + " failed because of ", e);
      }
    }
  }

  /**
   * Delete.
   *
   * @param portalName      the portal name
   * @param userMail        the user mail
   * @param SessionProvider the sessionprovider
   */
  public void delete(SessionProvider sessionProvider, String portalName, String userMail) {
    if (log.isInfoEnabled()) {
      log.info("Trying to delete user " + userMail);
    }
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String userPath = NewsletterConstant.generateUserPath(portalName);
      Node userFolderNode = (Node)session.getItem(userPath);
      Node userNode = userFolderNode.getNode(userMail);
      userNode.remove();

      QueryManager queryManager = session.getWorkspace().getQueryManager();
      String sqlQuery = "select * from " + NewsletterConstant.SUBSCRIPTION_NODETYPE + " where "
          + NewsletterConstant.SUBSCRIPTION_PROPERTY_USER + " like '%" + userMail + "%'";
      Query query = queryManager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator nodeIterator = queryResult.getNodes();
      for (;nodeIterator.hasNext();) {
        Node subscriptionNode = nodeIterator.nextNode();
        if (subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)) {
          Property subscribedUserProperty = subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER);
          List<Value> oldSubscribedUsers = Arrays.asList(subscribedUserProperty.getValues());
          List<Value> newSubscribedUsers = new ArrayList<Value>();
          for (Value value: oldSubscribedUsers) {
            String subscribedUserMail = value.getString();
            if (userMail.equals(subscribedUserMail)) {
              continue;
            }
            newSubscribedUsers.add(value);
          }
          subscribedUserProperty.setValue(newSubscribedUsers.toArray(new Value[newSubscribedUsers.size()]));
        }
      }
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Delete user " + userMail + " failed because of ", e);
      }
    }
  }

  /**
   * Gets the users.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   *
   * @return the users
   *
   * @throws Exception the exception
   */
  public List<NewsletterUserConfig> getUsers(SessionProvider sessionProvider,
                                             String portalName,
                                             String categoryName,
                                             String subscriptionName) throws Exception {
    List<NewsletterUserConfig> listUsers = new ArrayList<NewsletterUserConfig>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String userPath = NewsletterConstant.generateUserPath(portalName);
    Node userHomeNode = (Node)session.getItem(userPath);
    if(categoryName == null && subscriptionName == null){ // get all user email
      NodeIterator nodeIterator = userHomeNode.getNodes();
      while(nodeIterator.hasNext()){
        listUsers.add(getUserFromNode(nodeIterator.nextNode()));
      }
    } else{
      List<String> listEmail = new ArrayList<String>();
      if(categoryName != null && subscriptionName == null){ // get user of category
        Node categoryNode = (Node)session.getItem(NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName);
        NodeIterator nodeIterator = categoryNode.getNodes();
        Node subscriptionNode;
        Value subscribedUserValues[];
        while(nodeIterator.hasNext()){
          subscriptionNode = nodeIterator.nextNode();
          if(subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)){
            Property subscribedUserProperty = subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER);
            subscribedUserValues = subscribedUserProperty.getValues();
            for (Value value : subscribedUserValues) {
              if(!listEmail.contains(value.getString())) listEmail.add(value.getString());
            }
          }
        }
      }else{ // get user of subscription
        listEmail = getUsersBySubscription(sessionProvider, portalName, categoryName, subscriptionName);
      }
      // convert form email to userConfig
      for(String email : listEmail){
        listUsers.add(getUserFromNode((userHomeNode.getNode(email))));
      }
    }
    return listUsers;
  }

  /**
   * Gets the users by subscription.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   * @param session the session
   *
   * @return the users by subscription
   */
  private List<String> getUsersBySubscription(SessionProvider sessionProvider,
                                              String portalName,
                                              String categoryName,
                                              String subscriptionName) {
    if (log.isInfoEnabled()) {
      log.info("Trying to get list user by subscription " + portalName + "/" + categoryName + "/" + subscriptionName);
    }
    List<String> subscribedUsers = new ArrayList<String>();
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String subscriptionPath = NewsletterConstant.generateCategoryPath(portalName) + "/"
          + categoryName + "/" + subscriptionName;
      Node subscriptionNode = Node.class.cast(session.getItem(subscriptionPath));
      if (subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)) {
        Property subscribedUserProperty = subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER);
        Value subscribedUserValues[] = subscribedUserProperty.getValues();
        for (Value value : subscribedUserValues) {
          subscribedUsers.add(value.getString());
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Get list user by subscription " + portalName + "/" + categoryName + "/"
          + subscriptionName + " failed because of ", e);
      }
    }
    return subscribedUsers;
  }

  /**
   * Gets the quantity user by subscription.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   *
   * @return the quantity user by subscription
   */
  public int getQuantityUserBySubscription(SessionProvider sessionProvider,
                                           String portalName,
                                           String categoryName,
                                           String subscriptionName) {
    if (log.isInfoEnabled()) {
      log.info("Trying to get user's quantity by subscription " + portalName + "/" + categoryName + "/" + subscriptionName);
    }
    int countUser = 0;
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String subscriptionPath = NewsletterConstant.generateCategoryPath(portalName) + "/"
          + categoryName + "/" + subscriptionName;
      Node subscriptionNode = Node.class.cast(session.getItem(subscriptionPath));
      if (subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)) {
        Property subscribedUserProperty = subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER);
        countUser = subscribedUserProperty.getValues().length;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Get user's quantity by subscription " + portalName + "/" + categoryName + "/"
          + subscriptionName + " failed because of ", e);
      }
    }
    return countUser;
  }

  /**
   * Check existed email.
   *
   * @param portalName the portal name
   * @param email the email
   *
   * @return true, if successful
   */
  public boolean checkExistedEmail(SessionProvider sessionProvider, String portalName, String email) {
    try {
      Node userNode = getUserNodeByEmail(sessionProvider, portalName, email);
      if(userNode != null){
        return true;
      }
      return false;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("checkExistedEmail() failed because of ", e);
      }
    }
    return false;
  }







  /**
   * Check if user is an administrator or not
   *
   * @param userId the current user
   *
   * @return true if user is an administrator, otherwise return false
   *
   * @throws Exception the exception
   */
  public boolean isAdministrator(String portalName, String userId) {
    try {
      List<String> administrators = getAllAdministrator(WCMCoreUtils.getSystemSessionProvider(), portalName);
      String superuser = WCMCoreUtils.getService(UserACL.class).getSuperUser();
      if (!administrators.contains(superuser)) {
        administrators.add(superuser);
      }
      return WCMCoreUtils.hasPermission(userId, administrators, false);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("isAdministrator() failed because of ", e);
      }
    }
    return false;
  }

  /**
   * Check if user is a moderator of current category or not
   *
   * @param userId the current user
   * @param categoryName the current category's name
   *
   * @return true if user is an administrator, otherwise return false
   *
   * @throws Exception the exception
   */
  public boolean isModerator(String userId, NewsletterCategoryConfig config) {
    try {
      List<String> moderators = Arrays.asList(config.getModerator().split(","));
      return WCMCoreUtils.hasPermission(userId, moderators, false);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("isModerator() failed because of ", e);
      }
    }
    return false;
  }

}
