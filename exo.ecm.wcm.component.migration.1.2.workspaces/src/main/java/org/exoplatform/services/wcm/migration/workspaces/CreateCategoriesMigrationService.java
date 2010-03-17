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
package org.exoplatform.services.wcm.migration.workspaces;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 *          ngoc.tran@exoplatform.com
 * Dec 23, 2009  
 */
public class CreateCategoriesMigrationService implements Startable {

  private static final String PARENT_PATH = "/sites content/live";
  
  private static final Log log  = ExoLogger.getLogger("wcm:CreateCategoriesMigrationService"); 
  
  private RepositoryService repositoryService;
  
  public CreateCategoriesMigrationService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }
  
  public void start() {
    log.info("==========================================================");
    log.info("Start CreateCategoriesMigrationService...");
    log.info("==========================================================");
    try {
      createNode();
    } catch (Exception e) {
      log.error("There is an error occurs ", e);
    }
    log.info("==========================================================");
    log.info("End CreateCategoriesMigrationService...");
    log.info("==========================================================");
  }

  public void stop() {}

  /**
   * Create Categories node, User node for Newsletter and categories node.
   * @param parentNode
   * @throws Exception
   */
  private void createNode() throws Exception {
    Session session = repositoryService.getCurrentRepository().getSystemSession("collaboration");
    Node parentNode = (Node) session.getItem(PARENT_PATH);
    NodeIterator nodeIterator = parentNode.getNodes();
    while(nodeIterator.hasNext()) {
      Node childNode = nodeIterator.nextNode();
      Node applicationNode = null;
      Node newsletterAppNode = null;
      try {
        applicationNode = childNode.getNode("ApplicationData");
      } catch (Exception ex) {
        applicationNode = childNode.addNode("ApplicationData");
      }
      try {
        newsletterAppNode = applicationNode.getNode("NewsletterApplication");
      } catch (Exception ex) {
        newsletterAppNode = applicationNode.addNode("NewsletterApplication");
      }
      try {
        newsletterAppNode.getNode("Categories");
      } catch (Exception ex) {
        newsletterAppNode.addNode("Categories");
      }
      try {
        newsletterAppNode.getNode("Users");
      } catch (Exception ex) {
        newsletterAppNode.addNode("Users");
      }
      try {
        childNode.getNode("categories");
      } catch (Exception ex) {
        childNode.addNode("categories");
      }
      session.save();
      session.logout();
    }
  }
}
