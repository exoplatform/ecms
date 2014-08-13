/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.extensions.deployment;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;


import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.deployment.Utils;
import org.exoplatform.services.deployment.DeploymentUtils;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tanhq@exoplatform.com
 * Oct 3, 2013  
 */
public class WCMPublicationDeploymentPlugin extends DeploymentPlugin{
  
  /** The configuration manager. */
  private ConfigurationManager configurationManager;

  /** The repository service. */
  private RepositoryService repositoryService;
  
  /** The publication service */
  private PublicationService publicationService;
  
  /** The publication service */
  private WCMPublicationService wcmPublicationService;
  
  private TrashService trashService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(WCMPublicationDeploymentPlugin.class.getName());
  
  private static final String CLEAN_PUBLICATION             = "clean-publication";
  private static final String PUBLISH_FIRST_PUBLICATION     = "publish-first-publication";
  private static final String KEEP_PUBLICATION              = "keep-publication";
  
  /**
   * Instantiates a new xML deployment plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param publicationService the publication service
   */
  public WCMPublicationDeploymentPlugin(InitParams initParams,
                             ConfigurationManager configurationManager,
                             RepositoryService repositoryService,
                             PublicationService publicationService,
                             WCMPublicationService wcmPublicationService,
                             TrashService trashService) {
    super(initParams);
    this.configurationManager = configurationManager;
    this.repositoryService = repositoryService;
    this.publicationService = publicationService;
    this.wcmPublicationService = wcmPublicationService;
    this.trashService = trashService;
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform
   * .services.jcr.ext.common.SessionProvider)
   */
  public void deploy(SessionProvider sessionProvider) throws Exception {
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Iterator iterator = initParams.getObjectParamIterator();
    WCMPublicationDeploymentDescriptor deploymentDescriptor = null;
    while (iterator.hasNext()) {
      try {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        deploymentDescriptor = (WCMPublicationDeploymentDescriptor) objectParameter.getObject();
        String sourcePath = deploymentDescriptor.getSourcePath();
        // sourcePath should start with: war:/, jar:/, classpath:/, file:/
        String versionHistoryPath = deploymentDescriptor.getVersionHistoryPath();
        String cleanupPublicationType = deploymentDescriptor.getCleanupPublicationType();

        InputStream inputStream = configurationManager.getInputStream(sourcePath);
        Session session = sessionProvider.getSession(deploymentDescriptor.getTarget().getWorkspace(), repository);       
        
        String nodeName = DeploymentUtils.getNodeName(configurationManager.getInputStream(sourcePath));
        //remove old resources
        if (this.isOverride()) {
          Node parent = (Node)session.getItem(deploymentDescriptor.getTarget().getNodePath());
          if (parent.hasNode(nodeName)) {
            trashService.moveToTrash(parent.getNode(nodeName), sessionProvider);
          }
        }
        
        session.importXML(deploymentDescriptor.getTarget().getNodePath(),
                          inputStream,
                          ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        if (CLEAN_PUBLICATION.equalsIgnoreCase(cleanupPublicationType) || 
            PUBLISH_FIRST_PUBLICATION.equalsIgnoreCase(cleanupPublicationType)) {
          /**
           * This code allows to cleanup the publication lifecycle and publish the first version in the target
           * folder after importing the data. By using this, the publication
           * live revision property will be re-initialized and the content will
           * be set as published directly. Thus, the content will be visible in
           * front side.
           */    
          
          Node parent = (Node)session.getItem(deploymentDescriptor.getTarget().getNodePath() + "/" + nodeName);
          cleanPublication(parent, cleanupPublicationType, true);
        }

        if (versionHistoryPath != null && versionHistoryPath.length() > 0) {
          // process import version history
          Node currentNode = (Node) session.getItem(deploymentDescriptor.getTarget().getNodePath());

          Map<String, String> mapHistoryValue =
            Utils.getMapImportHistory(configurationManager.getInputStream(versionHistoryPath));
          Utils.processImportHistory(currentNode,
                                     configurationManager.getInputStream(versionHistoryPath),
                                     mapHistoryValue);
        }
        session.save();
      } catch (Exception ex) {
        if (LOG.isErrorEnabled()) {
          LOG.error("deploy " + deploymentDescriptor.getSourcePath() + " into "
                      + deploymentDescriptor.getTarget().getNodePath() + " is FAILURE at "
                      + new Date().toString() + "\n",
                  ex);
        }      
      }
    }
    if (LOG.isInfoEnabled()) {
      LOG.info(deploymentDescriptor.getSourcePath() + " is deployed succesfully into "
          + deploymentDescriptor.getTarget().getNodePath());
    }
  }
  
  /**
   * This method implement cleaning publication based on cleanPublicationType. The cleanPublicationType can accept one of 
     three values as following:
     -  clean-publication: This code allows to cleanup the publication lifecycle.
     -  keep-publication : This option allows to keep all current publications 
     -  publish-first-publication: This option allows to cleanup the publication lifecycle and publish the first version in the 
        target folder
   * @param cleanupPublicationType the type of cleaning publication
   * @throws Exception 
   * @throws NotInPublicationLifecycleException 
   */
  private void cleanPublication(Node node, String cleanupPublicationType, boolean updateLifecycle) throws NotInPublicationLifecycleException, Exception  {
    if (node.hasProperty("publication:liveRevision")
        && node.hasProperty("publication:currentState")) {
      if (LOG.isInfoEnabled()) {
        LOG.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
      }
      node.setProperty("publication:liveRevision", "");
      node.setProperty("publication:currentState", "published");
    }
    node.getSession().save();
    if(updateLifecycle && PUBLISH_FIRST_PUBLICATION.equalsIgnoreCase(cleanupPublicationType) && 
        org.exoplatform.services.cms.impl.Utils.isDocument(node)) {
      if(publicationService.isNodeEnrolledInLifecycle(node)) publicationService.unsubcribeLifecycle(node);
      wcmPublicationService.updateLifecyleOnChangeContent(node, "default", "__system", "published");
      node.save();
    }
    NodeIterator iter = node.getNodes(); 
    while (iter.hasNext()) {
      Node childNode = iter.nextNode();
      cleanPublication(childNode, cleanupPublicationType, false);
    }
  }  
  
}
