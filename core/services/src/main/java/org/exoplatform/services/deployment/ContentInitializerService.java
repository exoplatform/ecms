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
package org.exoplatform.services.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class ContentInitializerService implements Startable{

  private List<DeploymentPlugin> listDeploymentPlugin = new ArrayList<DeploymentPlugin>();
  private RepositoryService repositoryService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private Log LOG = ExoLogger.getLogger(this.getClass());

  private static String CONTENT_INIT = "ContentInitializerService";
  private static String EXO_SERVICES = "eXoServices";

  public ContentInitializerService(RepositoryService repositoryService, NodeHierarchyCreator nodeHierarchyCreator) {
    this.repositoryService = repositoryService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
  }

  public void addPlugin(DeploymentPlugin deploymentPlugin) {
    listDeploymentPlugin.add(deploymentPlugin);
  }

  public void start() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      Node serviceFolder = (Node)session.getItem(nodeHierarchyCreator.getJcrPath(EXO_SERVICES));
      Node contentInitializerService = null;
      if (serviceFolder.hasNode(CONTENT_INIT)) {
        contentInitializerService = serviceFolder.getNode(CONTENT_INIT);
      } else {
        contentInitializerService = serviceFolder.addNode(CONTENT_INIT, "nt:unstructured");
      }
      if (!contentInitializerService.hasNode("ContentInitializerServiceLog")) {
        Date date = new Date();
        StringBuffer logData = new StringBuffer();
        for (DeploymentPlugin deploymentPlugin : listDeploymentPlugin) {
          try {
            deploymentPlugin.deploy(sessionProvider);
            logData.append("deploy " + deploymentPlugin.getName()
                + " deployment plugin succesful at " + date.toString() + "\n");
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("deploy " + deploymentPlugin.getName() + " deployment plugin failure at "
                + date.toString() + " by " + e.getMessage() + "\n");
            }
            logData.append("deploy " + deploymentPlugin.getName()
                + " deployment plugin failure at " + date.toString() + " by " + e.getMessage()
                + "\n");
          }
        }

        Node contentInitializerServiceLog = contentInitializerService.addNode("ContentInitializerServiceLog", "nt:file");
        Node contentInitializerServiceLogContent = contentInitializerServiceLog.addNode("jcr:content", "nt:resource");
        contentInitializerServiceLogContent.setProperty("jcr:encoding", "UTF-8");
        contentInitializerServiceLogContent.setProperty("jcr:mimeType", "text/plain");
        contentInitializerServiceLogContent.setProperty("jcr:data", logData.toString());
        contentInitializerServiceLogContent.setProperty("jcr:lastModified", date.getTime());
        session.save();
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected problem occurs when deploy contents", e);
      }
    } finally {
      sessionProvider.close();
    }
  }
  public void stop() {}

}
