/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.clouddrive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.clouddrive.exodrive.service.FileStore;
import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestCloudDriveService.java 00000 Sep 12, 2012 pnedonosko $
 */
public abstract class BaseCloudDriveTest extends BaseCommonsTestCase {

  protected static final Log       LOG = ExoLogger.getLogger(BaseCloudDriveTest.class);
  
  protected final String         WORKSPACE_NAME = "portal-test";

  protected SessionProviderService sessionProviders;
  
  protected RepositoryService    repositoryService;

  protected Session              session;

  protected Node                 root;
  
  protected String workspaceName; 

  /**
   * setUp.
   * 
   * @throws java.lang.Exception
   */
  public void setUp() throws Exception {
    super.setUp();
    
    repositoryService = getService(RepositoryService.class);
    session = repositoryService.getCurrentRepository().getSystemSession(WORKSPACE_NAME);
    root = session.getRootNode();

    // String containerConf =
    // TestCloudDriveService.class.getResource("/conf/portal/test-clouddrive-configuration.xml").toString();
    // StandaloneContainer.addConfigurationURL(containerConf);

    sessionProviders = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);

    // login via Authenticator
    Authenticator authr = (Authenticator) container.getComponentInstanceOfType(Authenticator.class);
    String johnUser = authr.validateUser(new Credential[] { new UsernameCredential("john"), new PasswordCredential("") });
    ConversationState.setCurrent(new ConversationState(authr.createIdentity(johnUser)));

    // and set session provider to the service
    SessionProvider sessionProvider = new SessionProvider(ConversationState.getCurrent());
    sessionProvider.setCurrentRepository(repositoryService.getCurrentRepository());
    workspaceName = System.getProperty("gatein.jcr.workspace.default");
    sessionProvider.setCurrentWorkspace(workspaceName);
    sessionProviders.setSessionProvider(null, sessionProvider);
    
    session = userSession();
    root = session.getRootNode();
  }
  
  protected void tearDown() throws Exception {
    // Remove in user session, not Commons's system session to another workspace (portal-test)
    NodeIterator iter = root.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      node.remove();
    }
    session.save();
    super.tearDown();
  }
  
  protected Session userSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    return sessionProviders.getSessionProvider(null).getSession(workspaceName, repositoryService.getCurrentRepository());
  }
  
  protected Session systemSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    return repositoryService.getCurrentRepository().getSystemSession(workspaceName);
  }

  protected void assertNodesExist(NodeIterator nodes, String... expected) throws RepositoryException {
    List<String> names = new ArrayList<String>(Arrays.asList(expected));
    while (nodes.hasNext()) {
      Node node = nodes.nextNode();
      String nname = node.getName();
      if (names.contains(nname)) {
        names.remove(nname);
      }
    }

    if (names.size() > 0) {
      fail("Expected nodes not exist: " + names);
    }
  }

  protected void assertFilesExist(List<FileStore> files, String... expectedNames) {
    List<String> names = Arrays.asList(expectedNames);
    List<String> expected = new ArrayList<String>();
    for (FileStore f : files) {
      if (names.contains(f.getName())) {
        expected.add(f.getName());
      }
    }

    if (expected.size() != expectedNames.length) {
      fail("Expected files not exist: " + expectedNames);
    }
  }
}
