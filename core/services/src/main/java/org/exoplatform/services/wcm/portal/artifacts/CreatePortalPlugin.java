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
package org.exoplatform.services.wcm.portal.artifacts;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 22, 2008
 */
public abstract class CreatePortalPlugin extends XMLDeploymentPlugin {

  /**
   * Instantiates a new base portal artifacts plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   */
  public CreatePortalPlugin(InitParams initParams, ConfigurationManager configurationManager,
      RepositoryService repositoryService) {
    super(initParams, configurationManager, repositoryService);
  }

  /**
   * Deploy to portal.
   *
   * @param portalName the portal name
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public abstract void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception;
}
