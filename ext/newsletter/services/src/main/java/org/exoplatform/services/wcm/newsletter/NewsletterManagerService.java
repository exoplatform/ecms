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
package org.exoplatform.services.wcm.newsletter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterEntryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterManageUserHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterPublicUserHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterTemplateHandler;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com May 21, 2009
 */
public class NewsletterManagerService {

  /** The category handler. */
  private NewsletterCategoryHandler categoryHandler;

  /** The subscription handler. */
  private NewsletterSubscriptionHandler subscriptionHandler;

  /** The entry handler. */
  private NewsletterEntryHandler entryHandler;

  /** The template handler. */
  private NewsletterTemplateHandler templateHandler;

  /** The manage user handler. */
  private NewsletterManageUserHandler manageUserHandler;

  /** The public user handler. */
  private NewsletterPublicUserHandler publicUserHandler;

  private RepositoryService repositoryService_;

  private MailService mailService_;

  /** The repository name. */
  private String repositoryName;

  /** The workspace name. */
  private String workspaceName;

  /** The log. */
  private static Log log = ExoLogger.getLogger(NewsletterManagerService.class);

  /**
   * Instantiates a new newsletter manager service.
   *
   * @param initParams the init params
   * @param dmsConfiguration the dms configuration
   */
  public NewsletterManagerService(InitParams initParams,
                                  DMSConfiguration dmsConfiguration,
                                  RepositoryService repositoryService,
                                  MailService mailService) {
    if (log.isInfoEnabled()) {
      log.info("Starting NewsletterManagerService ... ");
    }
    workspaceName = initParams.getValueParam("workspace").getValue();
    categoryHandler = new NewsletterCategoryHandler(workspaceName);
    subscriptionHandler = new NewsletterSubscriptionHandler(workspaceName);
    entryHandler = new NewsletterEntryHandler(workspaceName);
    manageUserHandler = new NewsletterManageUserHandler(workspaceName);
    publicUserHandler = new NewsletterPublicUserHandler(workspaceName);
    templateHandler = new NewsletterTemplateHandler(workspaceName);
    repositoryService_ = repositoryService;
    mailService_ = mailService;
  }

  /**
   * Gets the category handler.
   *
   * @return the category handler
   */
  public NewsletterCategoryHandler getCategoryHandler() {
    return categoryHandler;
  }

  /**
   * Gets the subscription handler.
   *
   * @return the subscription handler
   */
  public NewsletterSubscriptionHandler getSubscriptionHandler() {
    return subscriptionHandler;
  }

  /**
   * Gets the entry handler.
   *
   * @return the entry handler
   */
  public NewsletterEntryHandler getEntryHandler() {
    return entryHandler;
  }

  /**
   * Gets the template handler.
   *
   * @return the template handler
   */
  public NewsletterTemplateHandler getTemplateHandler() {
    return templateHandler;
  }

  public List<String> getAllBannedUser() throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Session session = sessionProvider.getSession(workspaceName, manageableRepository);
    List<String> listEmails = new ArrayList<String>();
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      String sqlQuery = "select * from " + NewsletterConstant.USER_NODETYPE + " where "
          + NewsletterConstant.USER_PROPERTY_BANNED + "='true' or "
          + NewsletterConstant.USER_PROPERTY_IS_CONFIRM + "='false'";
      Query query = queryManager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator nodeIterator = queryResult.getNodes();
      while (nodeIterator.hasNext()) {
        listEmails.add(nodeIterator.nextNode()
                                   .getProperty(NewsletterConstant.USER_PROPERTY_MAIL)
                                   .getString());
      }
    } catch (RepositoryException repositoryException) {
      if (log.isInfoEnabled()) {
        log.info("User node is not created!");
      }
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("Error when get all users who can't get newsletter: ", ex);
      }
    } finally {
      if (session != null)
        session.logout();
    }
    return listEmails;
  }

  /**
   * Gets the manage user handler.
   *
   * @return the manage user handler
   */
  public NewsletterManageUserHandler getManageUserHandler() {
    return manageUserHandler;
  }

  /**
   * Gets the public user handler.
   *
   * @return the public user handler
   */
  public NewsletterPublicUserHandler getPublicUserHandler() {
    return publicUserHandler;
  }

  /**
   * Send newsletter.
   *
   * @throws Exception the exception
   */
  public void sendNewsletter() throws Exception {
    List<String> listBannedEmail = this.getAllBannedUser();

    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Session session = sessionProvider.getSession(workspaceName, manageableRepository);

    Message message = null;
    QueryManager queryManager = session.getWorkspace().getQueryManager();

    String sqlQuery =
    "SELECT * FROM exo:newsletterEntry WHERE exo:newsletterEntryStatus LIKE '%" +
    NewsletterConstant.STATUS_AWAITING +
    "%' AND exo:newsletterEntryDate <= TIMESTAMP '" +
    ISO8601.format(Calendar.getInstance()) + "'";

    Query query = queryManager.createQuery(sqlQuery, Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator nodeIterator = queryResult.getNodes();

    while (nodeIterator.hasNext()) {
      Node newsletterEntry = nodeIterator.nextNode();
      Node subscriptionNode = newsletterEntry.getParent();

      List<String> listEmailAddress = null;
      if(subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)){
        listEmailAddress = convertValuesToArray(subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)
                                                                .getValues(),
                                                listBannedEmail);
      }

      if(listEmailAddress != null && listEmailAddress.size() > 0) {
        message = new Message();
        message.setTo(listEmailAddress.get(0));
        StringBuffer receiver = new StringBuffer();
        for (int i = 1; i < listEmailAddress.size(); i++) {
          receiver.append(listEmailAddress.get(i)).append(",");
        }
        message.setBCC(receiver.toString());
        message.setSubject(newsletterEntry.getProperty("exo:title").getString());
        message.setBody(entryHandler.getContent(sessionProvider, newsletterEntry));
        message.setMimeType("text/html");

        try {
          mailService_.sendMessage(message);
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Error when send newsletter: ", e);
          }
        }
      }
      newsletterEntry.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_SENT);
    }
    session.save();
    session.logout();
  }

  /**
   * Send verification mail.
   *
   * @param email the email
   *
   * @throws RepositoryException the repository exception
   * @throws RepositoryConfigurationException the repository configuration exception
   */
  public void sendVerificationMail(String email) throws RepositoryException, RepositoryConfigurationException {
  }

  /**
   * Convert values to array.
   *
   * @param values the values
   *
   * @return the list< string>
   */
  private List<String> convertValuesToArray(Value[] values, List<String> listBannedUser) {
    List<String> listString = new ArrayList<String>();
    String email="";
    for (Value value : values) {
      try {
        email = value.getString();
        if(!listBannedUser.contains(email)) listString.add(email);
      } catch(Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Error when convert values to array: ", e);
        }
      }
    }
    return listString;
  }

  /**
   * Gets the repository name.
   *
   * @return the repository name
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /**
   * Sets the repository name.
   *
   * @param repositoryName the new repository name
   */
  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  /**
   * Gets the workspace name.
   *
   * @return the workspace name
   */
  public String getWorkspaceName() {
    return workspaceName;
  }

  /**
   * Sets the workspace name.
   *
   * @param workspaceName the new workspace name
   */
  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

}
