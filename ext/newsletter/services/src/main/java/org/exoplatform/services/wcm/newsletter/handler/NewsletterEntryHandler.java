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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.config.NewsletterManagerConfig;
import org.exoplatform.services.wcm.skin.XSkinService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * May 21, 2009
 */
public class NewsletterEntryHandler {

  /** The log. */
  private static Log log = ExoLogger.getLogger(NewsletterEntryHandler.class);

  /** The repository service. */
  private RepositoryService repositoryService;

  private XSkinService xSkinService;

  /** The workspace. */
  private String workspace;

  /**
   * Instantiates a new newsletter entry handler.
   *
   * @param repository the repository
   * @param workspace the workspace
   */
  @Deprecated
  public NewsletterEntryHandler(String repository, String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    xSkinService = WCMCoreUtils.getService(XSkinService.class);
    this.workspace = workspace;
  }
  
  /**
   * Instantiates a new newsletter entry handler.
   *
   * @param workspace the workspace
   */
  public NewsletterEntryHandler(String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    xSkinService = WCMCoreUtils.getService(XSkinService.class);
    this.workspace = workspace;
  }

  /**
   * Gets the entry from node.
   *
   * @param entryNode the entry node
   *
   * @return the entry from node
   *
   * @throws Exception the exception
   */
  private NewsletterManagerConfig getEntryFromNode(Node entryNode) throws Exception{
    NewsletterManagerConfig newsletterEntryConfig = new NewsletterManagerConfig();
    newsletterEntryConfig.setNewsletterName(entryNode.getName());
    if (entryNode.hasProperty("exo:title"))
      newsletterEntryConfig.setNewsletterTitle(entryNode.getProperty("exo:title").getString());
    newsletterEntryConfig.setNewsletterSentDate(entryNode.getProperty(NewsletterConstant.ENTRY_PROPERTY_DATE)
                                                         .getDate()
                                                         .getTime());
    newsletterEntryConfig.setStatus(entryNode.getProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS).getString());
    newsletterEntryConfig.setSubcriptionName(entryNode.getParent().getName());
    newsletterEntryConfig.setCategoryName(entryNode.getParent().getParent().getName());
    return newsletterEntryConfig;
  }

  /**
   * Delete.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   * @param listIds the list ids
   */
  public void delete(SessionProvider sessionProvider,
                     String portalName,
                     String categoryName,
                     String subscriptionName,
                     List<String> listIds) {
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      String path = NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName + "/" + subscriptionName;
      Node subscriptionNode = (Node)session.getItem(path);
      Node newsletterNode = null;
      for (String id : listIds) {
        newsletterNode = subscriptionNode.getNode(id);
        newsletterNode.remove();
      }
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Delete newsletter entry failed because of ", e);
      }
    }
  }

  /**
   * Gets the newsletter entries by subscription.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   *
   * @return the newsletter entries by subscription
   *
   * @throws Exception the exception
   */
  public List<NewsletterManagerConfig> getNewsletterEntriesBySubscription(SessionProvider sessionProvider,
                                                                          String portalName,
                                                                          String categoryName,
                                                                          String subscriptionName)
    throws Exception{
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String path = NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName + "/" + subscriptionName;
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    String sqlQuery = "select * from " + NewsletterConstant.ENTRY_NODETYPE
        + " where jcr:path LIKE '" + path + "[%]/%'";
    Query query = queryManager.createQuery(sqlQuery, Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator nodeIterator = queryResult.getNodes();
    List<NewsletterManagerConfig> listNewsletterEntry = new ArrayList<NewsletterManagerConfig>();
    while(nodeIterator.hasNext()){
      try{
        listNewsletterEntry.add(getEntryFromNode(nodeIterator.nextNode()));
      }catch(Exception ex){
        if (log.isErrorEnabled()) {
          log.error("Get getNewsletterEntriesBySubscription() failed because of ", ex);
        }
        continue;
      }
    }
    return listNewsletterEntry;
  }

  /**
   * Gets the newsletter entry.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   * @param newsletterName the newsletter name
   *
   * @return the newsletter entry
   *
   * @throws Exception the exception
   */
  public NewsletterManagerConfig getNewsletterEntry(SessionProvider sessionProvider,
                                                    String portalName,
                                                    String categoryName,
                                                    String subscriptionName,
                                                    String newsletterName) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String path = NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName + "/"
        + subscriptionName + "/" + newsletterName;
    NewsletterManagerConfig newsletterManagerConfig = getEntryFromNode((Node)session.getItem(path));
    return newsletterManagerConfig;
  }

  /**
   * Gets the newsletter entry by path.
   *
   * @param path the path
   *
   * @return the newsletter entry by path
   *
   * @throws Exception the exception
   */
  public NewsletterManagerConfig getNewsletterEntryByPath(SessionProvider sessionProvider,
                                                          String path) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    NewsletterManagerConfig newsletterManagerConfig = getEntryFromNode((Node)session.getItem(path));
    return newsletterManagerConfig;
  }

  /**
   * Gets the content.
   *
   * @param portalName the portal name
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   * @param newsletterName the newsletter name
   *
   * @return the content
   *
   * @throws Exception the exception
   */
  public String getContent(SessionProvider sessionProvider,
                           String portalName,
                           String categoryName,
                           String subscriptionName,
                           String newsletterName) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    String path = NewsletterConstant.generateCategoryPath(portalName) + "/" + categoryName + "/"
        + subscriptionName + "/" + newsletterName;
    Node newsletterNode = (Node)session.getItem(path);
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("<style type=\"text/css\">");
      sb.append(removeEncodedCharacter(xSkinService.getActiveStylesheet(newsletterNode)));
      sb.append("</style>");
      sb.append(newsletterNode.getNode("default.html")
                              .getNode("jcr:content")
                              .getProperty("jcr:data")
                              .getString());
      return sb.toString();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when get content: ", e);
      }
    }
    return null;
  }

  /**
   * Gets the content.
   *
   * @param webContent the web content
   *
   * @return the content
   *
   * @throws Exception the exception
   */
  public String getContent(SessionProvider sessionProvider, Node webContent) throws Exception{
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  ");
      sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
      sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\" dir=\"ltr\">");
      sb.append("<head>");
      sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
      sb.append("<style type=\"text/css\">");
      sb.append(removeEncodedCharacter(xSkinService.getActiveStylesheet(webContent)));
      sb.append("</style>");
      sb.append("</head>");
      sb.append("<body>");
      sb.append(webContent.getNode("default.html")
                          .getNode("jcr:content")
                          .getProperty("jcr:data")
                          .getString());
      sb.append("</body>");
      sb.append("</html>");

      return sb.toString();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when get content: ", e);
      }
    }
    return null;
  }

  /**
   * Removes the encoded character.
   *
   * @param rawString the raw string
   *
   * @return the string
   */
  private String removeEncodedCharacter(String rawString){
    return  rawString.replaceAll("\t", " ").replaceAll("\n", " ");
  }
}
