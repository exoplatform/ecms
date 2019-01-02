/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.services.wcm.extensions.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.deployment.PublicationDeploymentDescriptor;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 16, 2012
 */
public class PublicationUtils {
  private static final Log LOG = ExoLogger.getLogger(PublicationUtils.class.getName());

  public static void deployPublicationToPortal(InitParams initParams,
                             RepositoryService repositoryService,
                             WCMPublicationService wcmPublicationService,
                             SessionProvider sessionProvider,
                             String portalName) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    PublicationDeploymentDescriptor deploymentDescriptor = null;
    try {
      while (iterator.hasNext()) {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        deploymentDescriptor = (PublicationDeploymentDescriptor) objectParameter.getObject();
        List<String> contents = deploymentDescriptor.getContents();
        // sourcePath should looks like : collaboration:/sites
        // content/live/acme

        HashMap<String, String> context_ = new HashMap<String, String>();
        PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class);
        PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
        String containerName = containerInfo.getContainerName();
        context_.put("containerName", containerName);


        for (String sourcePath:contents) {
          try {
            if (portalName != null && portalName.length() > 0) {
              sourcePath = StringUtils.replace(sourcePath, "{portalName}", portalName);
            }
            String[] src = sourcePath.split(":");

            if (src.length == 2) {
              ManageableRepository repository = repositoryService.getCurrentRepository();
              Session session = sessionProvider.getSession(src[0], repository);
              Node nodeSrc = (Node) session.getItem(src[1]);
              if(publicationService.isNodeEnrolledInLifecycle(nodeSrc)) publicationService.unsubcribeLifecycle(nodeSrc);
              wcmPublicationService.updateLifecyleOnChangeContent(nodeSrc, "default", "__system", "published");
              nodeSrc.save();

            }
            if (LOG.isInfoEnabled()) {
              LOG.info(sourcePath + " has been published.");
            }
          } catch (Exception ex) {
            if (LOG.isErrorEnabled()) {
              LOG.error("publication for " + sourcePath + " FAILED at "
                          + new Date().toString() + "\n",
                      ex);
            }
          }
        }


      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("publication plugin FAILED at "
                    + new Date().toString() + "\n",
                ex);
      }
      throw ex;
    }
  }

}
