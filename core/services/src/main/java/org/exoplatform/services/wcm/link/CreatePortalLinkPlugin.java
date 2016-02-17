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
package org.exoplatform.services.wcm.link;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class CreatePortalLinkPlugin extends CreatePortalPlugin {

  /** The init params. */
  private InitParams initParams;

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The link manager service. */
  private LinkManager linkManager;

  /**
   * Instantiates a new xML deployment plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param linkManager the linkManager service
   * @param taxonomyService the taxonomy service
   */
  public CreatePortalLinkPlugin(InitParams initParams,
                              ConfigurationManager configurationManager,
                              RepositoryService repositoryService,
                              LinkManager linkManager,
                              TaxonomyService taxonomyService) {
    super(initParams, configurationManager, repositoryService);
    this.initParams = initParams;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    WCMCoreUtils.deployLinkToPortal(initParams, repositoryService, linkManager, sessionProvider, portalName);
  }
}
