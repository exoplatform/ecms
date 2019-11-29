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
package org.exoplatform.services.deployment;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform annb@exoplatform.com May
 * 18, 2012
 */
public class ContentInitializerServiceTest extends TestCase {

  private static final Log LOGGER = ExoLogger.getLogger(ContentInitializerServiceTest.class);
  private ContentInitializerService contentInitializerServiceActual;
  
  
  /**
   * test constructor
   */
  
  public void setUp() {
    LOGGER.info("testContentInitializerService");
    // Actual
    RepositoryService repositoryServiceActual = mock(RepositoryService.class);
    NodeHierarchyCreator nodeHierarchyCreatorActual = mock(NodeHierarchyCreator.class);
    OrganizationService organizationServiceActual = mock(OrganizationService.class);
    contentInitializerServiceActual = new ContentInitializerService(repositoryServiceActual,
                                                                                              nodeHierarchyCreatorActual,
                                                                                              organizationServiceActual);

    //verify if class ContentInitializerService
    assertEquals(contentInitializerServiceActual.getClass(), ContentInitializerService.class);
    //verify if mock class is not null
    assertNotNull(contentInitializerServiceActual);
  }

  /**
   * test method addplugin
   */
  @SuppressWarnings("rawtypes")
  public void testAddPlugin() {
    LOGGER.info("testAddPlugin");

    ContentInitializerService contentMock = mock(ContentInitializerService.class);

    final List<DeploymentPlugin> listDeploymentPluginActual = new ArrayList<DeploymentPlugin>();
    // Actual
    DeploymentPlugin deploymentPluginActual = mock(DeploymentPlugin.class);

    // return arraylist when mock calls method addplugin
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        LOGGER.info("argument add is " + args[0]);
        System.out.println("add for mock");
        listDeploymentPluginActual.add((DeploymentPlugin) args[0]);
        return listDeploymentPluginActual;
      }
    }).when(contentMock).addPlugin(any(DeploymentPlugin.class));

    // call method
    contentMock.addPlugin(deploymentPluginActual);

    // Expected
    List<DeploymentPlugin> listDeploymentPluginExpected = new ArrayList<DeploymentPlugin>();
    DeploymentPlugin deploymentPluginExpected = mock(DeploymentPlugin.class);
    listDeploymentPluginExpected.add(deploymentPluginExpected);
    
    contentInitializerServiceActual.addPlugin(deploymentPluginActual);

    // verify call method
    verify(contentMock).addPlugin(deploymentPluginActual);
    assertEquals(listDeploymentPluginActual.size(), 1);
 
  }

  /**
   * test method start
   */
  public void testStart() {

    LOGGER.info("testStart");

    Node contentInitializerServiceMock = mock(Node.class);
    Node contentInitializerServiceLogMock = mock(Node.class);
    Node contentInitializerServiceLogContentMock = mock(Node.class);
    try {
      when(contentInitializerServiceMock.addNode("ContentInitializerServiceLog", "nt:file")).thenReturn(contentInitializerServiceLogMock);
      when(contentInitializerServiceLogMock.addNode("jcr:content", "nt:resource")).thenReturn(contentInitializerServiceLogContentMock);
    } catch (Exception e) {
    }
    //
        try {
            assertEquals(contentInitializerServiceLogMock,
                         (contentInitializerServiceMock.addNode("ContentInitializerServiceLog", "nt:file")));
            assertEquals(contentInitializerServiceLogContentMock,
                         (contentInitializerServiceLogMock.addNode("jcr:content", "nt:resource")));
          } catch (Exception e) {
          }

    ContentInitializerService contentMock = mock(ContentInitializerService.class);
    contentMock.start();

    
    
    //verify call method    
    verify(contentMock).start();

  }

  /**
   * test method stop
   */
  public void testStop() {
    LOGGER.info("testStop");
    ContentInitializerService contentMock = mock(ContentInitializerService.class);
    contentMock.stop();
    
    //verify call method
    verify(contentMock).stop();

  }

}
