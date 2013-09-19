/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class CreateLinkPlugin extends CreatePortalPlugin {

  private LinkManager linkManager;
  
  /** The configuration manager. */
  private InitParams initParams;

  /** The repository service. */
  private RepositoryService repositoryService;

  /*
   * Create Link Plugin
   */
  public CreateLinkPlugin (InitParams initParams, ConfigurationManager configurationManager,
      RepositoryService repositoryService, LinkManager linkManager) {
    super(initParams, configurationManager, repositoryService);
    this.initParams = initParams;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
  }
  
  /* 
   * (non-Javadoc)
   * @see org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin#deployToPortal(SessionProvider, String)
   */
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    WCMCoreUtils.deployLinkToPortal(initParams, repositoryService, linkManager, sessionProvider, portalName);
  }
}
