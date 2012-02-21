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
package org.exoplatform.services.wcm.core;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 21, 2009
 */
public class NodetypeUtils {

  private static final Log LOG = ExoLogger.getLogger(NodetypeUtils.class);

  /**
   * Display all nodes and their properties inside a workspace.
   *
   * @param workspaceName the workspace name
   * @param repositoryName the repository name
   *
   * @throws Exception the exception
   */
  @Deprecated
  public static void displayAllNode(String workspaceName, String repositoryName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    RepositoryService repositoryService = (RepositoryService) ExoContainerContext.getCurrentContainer().
        getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspaceName, repository);
    Node root = session.getRootNode();
    displayAllChildNode(root);
  }
  
  /**
   * Display all nodes and their properties inside a workspace.
   *
   * @param workspaceName the workspace name
   *
   * @throws Exception the exception
   */
  public static void displayAllNode(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    RepositoryService repositoryService = (RepositoryService) ExoContainerContext.getCurrentContainer().
        getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspaceName, repository);
    Node root = session.getRootNode();
    displayAllChildNode(root);
  }  

  /**
   * Display the child node and its properties.
   *
   * @param node the current node
   *
   * @throws Exception the exception
   */
  public static void displayAllChildNode(Node node) throws Exception {
    NodeIterator nodeIterator = node.getNodes();
    while (nodeIterator.hasNext()) {
      Node childNode = nodeIterator.nextNode();
      displayOneNode(childNode);
      if (LOG.isInfoEnabled()) {
        LOG.info("\n------------------\n");
      }
      displayAllChildNode(childNode);
    }
  }

  /**
   * Display one node. and its properties
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  public static void displayOneNode(Node node) throws Exception {
    if (LOG.isInfoEnabled()) {
      LOG.info("Node name: " + node.getName());
      LOG.info("Node path: " + node.getPath());
    }
    PropertyIterator propertyIterator = node.getProperties();
    while (propertyIterator.hasNext()) {
      Property property = propertyIterator.nextProperty();
      try {
        if (LOG.isInfoEnabled()) {
          LOG.info("\t" + property.getName() + ": " + property.getString());
        }
      } catch (Exception e) {
        for (Value value : property.getValues()) {
          if (LOG.isInfoEnabled()) {
            LOG.info("\t" + property.getName() + ": " + value.getString());
          }
        }
      }
    }
  }
}
