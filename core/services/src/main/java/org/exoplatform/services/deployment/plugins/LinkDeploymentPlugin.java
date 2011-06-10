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
package org.exoplatform.services.deployment.plugins;

import java.util.Date;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class LinkDeploymentPlugin extends DeploymentPlugin {

  /** The init params. */
  private InitParams initParams;

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The link manager service. */
  private LinkManager linkManager;

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());
  public static final String UPDATE_EVENT = "WCMPublicationService.event.updateState";

  /**
   * Instantiates a new xML deployment plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param publicationService the publication service
   */
  public LinkDeploymentPlugin(InitParams initParams,
                              RepositoryService repositoryService,
                              LinkManager linkManager,
                              TaxonomyService taxonomyService) {
    this.initParams = initParams;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deploy(SessionProvider sessionProvider) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    LinkDeploymentDescriptor deploymentDescriptor = null;
    try {
      while (iterator.hasNext()) {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        deploymentDescriptor = (LinkDeploymentDescriptor) objectParameter.getObject();
        String sourcePath = deploymentDescriptor.getSourcePath();
        String targetPath = deploymentDescriptor.getTargetPath();
        // sourcePath should looks like : repository:collaboration:/sites
        // content/live/acme

        String[] src = sourcePath.split(":");
        String[] tgt = targetPath.split(":");

        if (src.length == 3 && tgt.length == 3) {
          ManageableRepository repository = repositoryService.getCurrentRepository();
          Session session = sessionProvider.getSession(src[1], repository);
          ManageableRepository repository2 = repositoryService.getCurrentRepository();
          Session session2 = sessionProvider.getSession(tgt[1], repository2);
          Node nodeSrc = session.getRootNode().getNode(src[2].substring(1));
          Node nodeTgt = session2.getRootNode().getNode(tgt[2].substring(1));
          linkManager.createLink(nodeTgt, "exo:taxonomyLink", nodeSrc);
          ExoContainer container = ExoContainerContext.getCurrentContainer();
          PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
          String containerName = containerInfo.getContainerName();
          ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class,
                                                                    containerName);
          CmsService cmsService = WCMCoreUtils.getService(CmsService.class, containerName);
          listenerService.broadcast(UPDATE_EVENT, cmsService, nodeSrc);
        }
        if (log.isInfoEnabled()) {
          log.info(sourcePath + " has a link into " + targetPath);
        }
      }
    } catch (Exception ex) {
      log.error("create link from " + deploymentDescriptor.getSourcePath() + " to "
                    + deploymentDescriptor.getTargetPath() + " is FAILURE at "
                    + new Date().toString() + "\n",
                ex);
      throw ex;
    }
  }
}
