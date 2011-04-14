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

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * May 26, 2009
 */
public class NewsletterTemplateHandler {

  /** The log. */
  private static Log log = ExoLogger.getLogger(NewsletterTemplateHandler.class);

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The repository. */
  private String repository;

  /** The workspace. */
  private String workspace;

  /** The templates. */
  private List<Node> templates = null;

  /**
   * Instantiates a new newsletter template handler.
   *
   * @param repository the repository
   * @param workspace the workspace
   */
  public NewsletterTemplateHandler(String repository, String workspace) {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    this.repository = repository;
    this.workspace = workspace;
  }

  /**
   * Gets the templates.
   *
   * @param portalName the portal name
   * @param categoryConfig the category config
   *
   * @return the templates
   */
  public List<Node> getTemplates(
                                 SessionProvider sessionProvider,
                                 String portalName,
                                 NewsletterCategoryConfig categoryConfig) {
    log.info("Trying to get templates of category " + categoryConfig);
    try {
      List<Node> templates = new ArrayList<Node>();
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);

      Node defaultTemplateFolder = (Node)session.getItem(NewsletterConstant.generateDefaultTemplatePath(portalName));
      NodeIterator defaultTemplates = defaultTemplateFolder.getNodes();
      while(defaultTemplates.hasNext()) {
        templates.add(defaultTemplates.nextNode());
      }
      if (categoryConfig != null) {
        Node categoryTemplateFolder = (Node) session.getItem(NewsletterConstant.
                                                             generateCategoryTemplateBasePath(portalName,
                                                                                              categoryConfig.getName()));
        NodeIterator categoryTemplates = categoryTemplateFolder.getNodes();
        while(categoryTemplates.hasNext()) {
          templates.add(categoryTemplates.nextNode());
        }
      }
      this.templates = templates;
      return templates;
    } catch (Exception e) {
      log.error("Get templates of category " + categoryConfig + " failed because of ", e);
    }
    return null;
  }

  /**
   * Gets the template.
   *
   * @param portalName the portal name
   * @param categoryConfig the category config
   * @param templateName the template name
   *
   * @return the template
   */
  public Node getTemplate(SessionProvider sessionProvider,
                          String portalName,
                          NewsletterCategoryConfig categoryConfig,
                          String templateName) {
    log.info("Trying to get template " + templateName);
    try {
      if (templates == null || templates.size() == 0)
        templates = getTemplates(sessionProvider, portalName, categoryConfig);
      if (templateName == null && templates.size() > 0) return templates.get(0);
      for (Node template : templates) {
        if (templateName.equals(template.getName())) {
          return template;
        }
      }
    } catch (Exception e) {
      log.error("Get dialog " + templateName + " failed because of ", e);
    }
    return null;
  }

  /**
   * Convert as template.
   *
   * @param webcontentPath the webcontent path
   * @param portalName the portal name
   * @param categoryName the category name
   *
   * @return true, if successful
   * @throws Exception
   */
  public void convertAsTemplate(
                                SessionProvider sessionProvider,
                                String webcontentPath,
                                String portalName,
                                String categoryName) throws Exception {
    log.info("Trying to convert node " + webcontentPath + " to template at category " + categoryName);
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      Node categoryTemplateFolder = (Node) session.getItem(NewsletterConstant.generateCategoryTemplateBasePath(portalName,
                                                                                                               categoryName));
      String templateName = webcontentPath.substring(webcontentPath.lastIndexOf("/") + 1);
      if(!categoryTemplateFolder.hasNode(templateName)){
        session.getWorkspace().copy(webcontentPath, categoryTemplateFolder.getPath() + "/" + templateName);
        session.save();
      }else{
        throw new Exception("Same name");
      }
    } catch (Exception e) {
      log.error("Convert node " + webcontentPath + " to template at category " + categoryName + " failed because of ", e);
      throw e;
    }
  }
}
