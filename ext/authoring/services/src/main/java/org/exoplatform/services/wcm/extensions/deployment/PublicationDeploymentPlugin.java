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
package org.exoplatform.services.wcm.extensions.deployment;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.utils.PublicationUtils;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class PublicationDeploymentPlugin extends DeploymentPlugin {

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The publication service */
  private WCMPublicationService wcmPublicationService;

  /** The publication service */
  private PublicationService publicationService;

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());
  public static final String UPDATE_EVENT = "WCMPublicationService.event.updateState";

  /**
   * Instantiates a new xML deployment plugin.
   *
   * @param initParams the init params
   * @param repositoryService the repository service
   * @param publicationService the publication service
   */
  public PublicationDeploymentPlugin(InitParams initParams,
                                     RepositoryService repositoryService,
                                     PublicationService publicationService,
                                     WCMPublicationService wcmPublicationService) {
    super(initParams);
    this.repositoryService = repositoryService;
    this.publicationService = publicationService;
    this.wcmPublicationService = wcmPublicationService;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deploy(SessionProvider sessionProvider) throws Exception {
    PublicationUtils.deployPublicationToPortal(initParams, repositoryService, wcmPublicationService, sessionProvider, null);
  }
}
