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
package org.exoplatform.services.wcm.migration.data;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 *          ngoc.tran@exoplatform.com
 * Dec 21, 2009  
 */
public class PortletDataMigrationService implements Startable {

  private static final Log log  = ExoLogger.getLogger("wcm:PortletDataMigrationService"); 
  
  private RepositoryService repositoryService;
  private static final String APLICATION_PATH = "/exo:registry/exo:applications/MainPortalData/";

  public PortletDataMigrationService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public void start() {
    log.info("==========================================================");
    log.info("Start PortletDataMigrationService...");
    log.info("==========================================================");
    Session session = null;
    try {
      WorkspaceEntry workspaceEntry = getSystemWorkspace();
      session = repositoryService.getCurrentRepository().getSystemSession(workspaceEntry.getName());
      Node rootNode = (Node) session.getItem(APLICATION_PATH);
      if(rootNode != null) {
        NodeIterator portalNodes = rootNode.getNodes();
        getJcrXmlTextProperty(portalNodes);
        session.save();
      }
    } catch (Exception e) {
      log.error("There is an error occurs", e);
    } finally {
      if (session != null) session.logout();
    }
    log.info("==========================================================");
    log.info("End PortletDataMigrationService");
    log.info("==========================================================");
  }

  /**
   * Change value of jcr:xmlcharacters property.
   * @param portalNodes
   */
  private void getJcrXmlTextProperty(NodeIterator portalNodes) {
    log.info("==========================================================");
    log.info("Starting get property");
    log.info("Proccessing...");
    try {
      while(portalNodes.hasNext()) {
        Node portalNode = portalNodes.nextNode();
        NodeIterator childNodes = getChildPortalNode(portalNode);
        while(childNodes.hasNext()) {
          Node child = childNodes.nextNode();
          if(child.getName().equals("pages")) {
            NodeIterator pagesNodeIterator = child.getNodes();
            while(pagesNodeIterator.hasNext()) {
              Node propertyNode = pagesNodeIterator.nextNode().getNode("data/jcr:xmltext");
              String jcrProperty = propertyNode.getProperty("jcr:xmlcharacters").getString();
              jcrProperty = jcrProperty.replaceAll("web-presentation", "presentation");
              propertyNode.setProperty("jcr:xmlcharacters", jcrProperty);
            }
          } else if(child.getName().equals("portletPreferences")){
            changNameAndValue(child);
          } else {
            Node propertyNode = child.getNode("data/jcr:xmltext");
            String jcrProperty = propertyNode.getProperty("jcr:xmlcharacters").getString();
            jcrProperty = jcrProperty.replaceAll("web-presentation", "presentation");
            propertyNode.setProperty("jcr:xmlcharacters", jcrProperty);
          }
        }
      }
    } catch (Exception e) {
      log.error("There is an error occurs", e);
    }
  }
  
  /**
   * Change name of childnode in portletPreferneces folder
   * and value of jcr:xmlcharacters property.
   * @param parentNode
   * @throws Exception
   */
  private void changNameAndValue(Node parentNode) throws Exception {
    log.info("==========================================================");
    log.info("Start change name and value for child node");
    log.info("==========================================================");
    Node newNode = null;
    NodeIterator nodes = parentNode.getNodes();
    while(nodes.hasNext()) {
      Node node = nodes.nextNode();
      Session session = node.getSession();;
      String destPath = "";
      if(node.getName().indexOf("web-presentation") > 0) {
        String newName = node.getName().replaceAll("web-presentation", "presentation");
        destPath = parentNode.getPath() + "/" + newName;
        session.getWorkspace().move(node.getPath(), destPath);
        newNode = (Node) session.getItem(destPath);
      } else {
        newNode = (Node) session.getItem(node.getPath());
      }
      Node propertyNode = newNode.getNode("data/jcr:xmltext");
      String jcrProperty = propertyNode.getProperty("jcr:xmlcharacters").getString();
      jcrProperty = jcrProperty.replaceAll("web-presentation", "presentation");
      if(jcrProperty.indexOf("/jcr:system/exo:ecm") > 0) {
        jcrProperty = jcrProperty.replaceAll("/jcr:system/exo:ecm", "/exo:ecm");
      }
      propertyNode.setProperty("jcr:xmlcharacters", jcrProperty);
    }
    log.info("==========================================================");
    log.info("End change name and value for child node");
    log.info("==========================================================");
  }

  /**
   * get All portal.
   * @param parentNode
   * @throws Exception
   * @return NodeIterator
   */
  private NodeIterator getChildPortalNode(Node parentNode) throws Exception {
    return  (NodeIterator) parentNode.getNodes();
  }
  
  public void stop() {
  }
  
  /**
   * Get system workspace to get information which configured in current repository
   * @return WorkspaceEntry
   * @throws RepositoryException
   */
  private WorkspaceEntry getSystemWorkspace() throws RepositoryException {
    List<WorkspaceEntry> workspaces = 
      repositoryService.getCurrentRepository().getConfiguration().getWorkspaceEntries();
    String systemWsName = 
      repositoryService.getCurrentRepository().getConfiguration().getSystemWorkspaceName();
    for(WorkspaceEntry wsEntry : workspaces) {
      if(wsEntry.getName().equals(systemWsName)) {
        return wsEntry;
      }
    }
    return null;
  }
}
