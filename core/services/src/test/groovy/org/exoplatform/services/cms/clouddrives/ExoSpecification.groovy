
/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives

import spock.lang.Specification

import org.exoplatform.services.cms.clouddrives.exodrive.ExoDriveConnector
import org.exoplatform.services.cms.clouddrives.exodrive.service.ExoDriveService
import org.exoplatform.services.cms.clouddrives.features.CloudDriveFeatures;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.clouddrives.jcr.TestJCRRemoveObservation
import org.exoplatform.services.cms.clouddrives.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.PortalContainer
import org.exoplatform.container.xml.InitParams
import org.exoplatform.container.xml.PropertiesParam
import org.exoplatform.services.jcr.RepositoryService
import org.exoplatform.services.jcr.ext.app.SessionProviderService
import org.exoplatform.services.jcr.ext.common.SessionProvider
import org.exoplatform.services.log.ExoLogger
import org.exoplatform.services.log.Log
import org.exoplatform.services.organization.OrganizationService
import org.exoplatform.services.security.Authenticator
import org.exoplatform.services.security.ConversationState
import org.exoplatform.services.security.Credential
import org.exoplatform.services.security.PasswordCredential
import org.exoplatform.services.security.UsernameCredential

import javax.jcr.Node
import javax.jcr.Session


/**
 * Basic support for eXo environment setup in spec tests.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoSpecification.groovy 00000 Jan 31, 2014 pnedonosko $
 * 
 */
class ExoSpecification extends Specification {

  protected final Log            LOG = ExoLogger.getLogger(TestJCRRemoveObservation.class)

  protected RepositoryService      repositoryService

  protected SessionProviderService sessionProviders

  protected Session                session

  protected Node                testRoot

  def setup() {
    //println ">>>>>>>>>> setup"

    PortalContainer container = PortalContainer.getInstance()

    repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class)
    repositoryService.setCurrentRepositoryName(System.getProperty("gatein.jcr.repository.default"))

    sessionProviders = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class)

    session = login("root")

    testRoot = session.getRootNode().addNode("specCloudDriveService", "nt:folder")
    session.save()
  }

  def cleanup() {
    testRoot.remove()
    session.save()
    session.logout()
  }

  // helpers

  def createExoDriveConnector() {
    PortalContainer container = PortalContainer.getInstance()

    ExoDriveService exoDrives = (ExoDriveService) container.getComponentInstanceOfType(ExoDriveService.class)
    OrganizationService orgService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class)

    NodeFinder finder =  (NodeFinder) container.getComponentInstanceOfType(NodeFinder.class)
    ExtendedMimeTypeResolver mimeTypes = (ExtendedMimeTypeResolver) container.getComponentInstanceOfType(ExtendedMimeTypeResolver.class)

    InitParams params = Stub(InitParams) {
      getPropertiesParam("drive-configuration") >> {
        Stub(PropertiesParam.class) {
          getProperties() >> ['provider-id' : 'exo', 'provider-name' : 'eXo Test Drive']
        }
      }
    }
    new ExoDriveConnector(repositoryService, sessionProviders, exoDrives, orgService, finder, mimeTypes, params)
  }


  /**
   * Authenticate an user, set its identity as current in ConversationState and login to JCR session.
   * 
   * @param userName String
   * @return Session
   */
  Session login(String userName) {
    ConversationState convo = ConversationState.getCurrent()
    if (convo == null || convo.getIdentity().getUserId() != userName) {
      // login an user
      PortalContainer container = PortalContainer.getInstance()
      // login via Authenticator
      Authenticator authr = (Authenticator) container.getComponentInstanceOfType(Authenticator.class)
      String user = authr.validateUser([
        new UsernameCredential(userName),
        new PasswordCredential("") ] as Credential[])
      ConversationState.setCurrent(new ConversationState(authr.createIdentity(user)))

      // and set session provider to the service
      SessionProvider sessionProvider = new SessionProvider(ConversationState.getCurrent())
      sessionProvider.setCurrentRepository(repositoryService.getCurrentRepository())
      sessionProvider.setCurrentWorkspace("collaboration")
      sessionProviders.setSessionProvider(null, sessionProvider)

      sessionProviders.getSessionProvider(null).getSession(sessionProvider.getCurrentWorkspace(),
          sessionProvider.getCurrentRepository())
    } else {
      // this user already set as current
      session()
    }
  }

  /**
   * JCR session based on current ConversationState.
   * 
   * @return Session
   */
  Session session() {
    sessionProviders.getSessionProvider(null).getSession("collaboration", repositoryService.getCurrentRepository())
  }
  
  CloudDriveService cloudDrives(CloudDriveFeatures features) {
    CloudDriveServiceImpl cloudDrives = new CloudDriveServiceImpl(repositoryService, sessionProviders, features)
    cloudDrives.addPlugin(createExoDriveConnector())
    cloudDrives
  }
  
  CloudDrive connectDrive(String parentPath, CloudDriveService cloudDrives) {
    Session session = session()
    
    Node userNode = session.getItem(parentPath).addNode("drive-${session.userID}", "nt:folder")
    session.save()
    
    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user = cloudDrives.authenticate(provider, session.userID)
    
    CloudDrive drive = cloudDrives.createDrive(user, userNode)
    drive.connect()
    drive
  }
  
  CloudDrive connectDrive(Node userNode, CloudDriveService cloudDrives) {
    Session session = session()
    
    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user = cloudDrives.authenticate(provider, session.userID)
    
    CloudDrive drive = cloudDrives.createDrive(user, userNode)
    drive.connect()
    drive
  }

}
