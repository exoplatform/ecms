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
package org.exoplatform.social.space.customization.plugin;

import java.util.Iterator;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.exoplatform.social.space.customization.SpaceCustomizationService;

public class XMLDeploymentPlugin extends SpaceListenerPlugin {

  /** The init params. */
  private InitParams initParams;

  private SpaceCustomizationService spaceCustomizationService = null;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(XMLDeploymentPlugin.class);

  /**
   * Instantiates a new XML deployment plugin.
   * 
   * @param initParams
   *          the init params
   * @param spaceCustomizationService
   *          the space customization service
   * @param nodeHierarchyCreator
   *          the nodeHierarchyCreator service
   */
  public XMLDeploymentPlugin(InitParams initParams, SpaceCustomizationService spaceCustomizationService,
      NodeHierarchyCreator nodeHierarchyCreator) {
    this.spaceCustomizationService = spaceCustomizationService;
    this.initParams = initParams;
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent lifeCycleEvent) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Iterator<?> iterator = initParams.getObjectParamIterator();
      while (iterator.hasNext()) {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        DeploymentDescriptor deploymentDescriptor = (DeploymentDescriptor) objectParameter.getObject();
        spaceCustomizationService.deployContentToSpaceDrive(sessionProvider, lifeCycleEvent.getSpace().getGroupId(),
            deploymentDescriptor);
      }
    } catch (Exception e) {
      LOG.error("An unexpected problem occurs while deploying contents", e);
    } finally {
      sessionProvider.close();
    }
  }
}
