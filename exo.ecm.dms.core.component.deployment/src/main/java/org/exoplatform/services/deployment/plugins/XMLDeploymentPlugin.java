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

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Session;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class XMLDeploymentPlugin extends DeploymentPlugin {

  private InitParams initParams;
  private ConfigurationManager configurationManager;
  private RepositoryService repositoryService;
  private Log log = ExoLogger.getLogger(this.getClass());

  public XMLDeploymentPlugin(InitParams initParams, ConfigurationManager configurationManager, RepositoryService repositoryService) {
    this.initParams = initParams;
    this.configurationManager = configurationManager;
    this.repositoryService = repositoryService;
  }

  public void deploy(SessionProvider sessionProvider) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();    
    while(iterator.hasNext()) {
      ObjectParameter objectParameter = (ObjectParameter)iterator.next();
      DeploymentDescriptor deploymentDescriptor = (DeploymentDescriptor)objectParameter.getObject();
      String sourcePath = deploymentDescriptor.getSourcePath();
      // sourcePath should start with: war:/, jar:/, classpath:/, file:/
      InputStream inputStream = configurationManager.getInputStream(sourcePath);
      ManageableRepository repository = repositoryService.getRepository(deploymentDescriptor.getTarget().getRepository());
      Session session = sessionProvider.getSession(deploymentDescriptor.getTarget().getWorkspace(), repository);
      session.importXML(deploymentDescriptor.getTarget().getNodePath(), inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW );
      session.save();
      session.logout();
      if(log.isInfoEnabled()) {
        log.info(this.getName() + " is deployed succesful at " + new Date().toString());
      }
    }   
  }

}
