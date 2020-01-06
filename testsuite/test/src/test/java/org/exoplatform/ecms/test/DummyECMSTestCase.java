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
package org.exoplatform.ecms.test;

import java.io.StringWriter;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPMethods;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.mock.MockRestService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SAS
 * @author : Pham Duy Dong
 *          dongpd@exoplatform.com
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/mock-rest-configuration.xml")
})
public class DummyECMSTestCase extends BaseECMSResourceTestCase {
  private final String MOCK_RESOURCE_URL = "/mock/guest";

  public void setUp() throws Exception {
    super.setUp();
    MockRestService restService = (MockRestService) this.container.getComponentInstanceOfType(MockRestService.class);
    this.binder.addResource(restService, null);
    applySystemSession();
  }

  public void testInitServices() throws Exception {
    assertNotNull(repositoryService);
    assertEquals(repositoryService.getDefaultRepository().getConfiguration().getName(),
                 "repository");
    assertEquals(repositoryService.getDefaultRepository()
                                  .getConfiguration()
                                  .getDefaultWorkspaceName(), "collaboration");
    assertNotNull(container);
   // assertEquals(repositoryService.getCurrentRepository().getWorkspaceNames().length, 4);
    
    assertNotNull(getService(LocaleConfigService.class));
//    System.out.println("Num of workspace: " + repositoryService.getCurrentRepository().getWorkspaceNames().length);
//    System.out.println("Num of node types: " + repositoryService.getCurrentRepository().getNodeTypeManager().getAllNodeTypes().getSize());
//    System.out.println("Num of locales: " + getService(LocaleConfigService.class).getLocalConfigs().size());
//    System.out.println("hibernate" + (getService(HibernateService.class) == null));
//    System.out.println("Cache: " + (getService(CacheService.class) == null));
//    System.out.println("Document Reader : " + (getService(DocumentReaderService.class) == null));
//    System.out.println("DescriptionService : " + (getService(DescriptionService.class) == null));
//    System.out.println("DataStorage : " + (getService(DataStorage.class) == null));
//    System.out.println("TransactionManagerLookup : " + (getService(TransactionManagerLookup.class)));
//    System.out.println("TransactionService : " + (getService(TransactionService.class)));
//    System.out.println("POMSessionManager : " + (getService(POMSessionManager.class)));
//    System.out.println("PicketLinkIDMService : " + (getService(PicketLinkIDMService.class)));
//    System.out.println("PicketLinkIDMCacheService : " + (getService(PicketLinkIDMCacheService.class)));
//    System.out.println("ResourceCompressor : " + (getService(ResourceCompressor.class)));
//    System.out.println("ModelDataStorage : " + (getService(ModelDataStorage.class)));
//    System.out.println("NavigationService : " + (getService(NavigationService.class)));
//    System.out.println("JTAUserTransactionLifecycleService : " + (getService(JTAUserTransactionLifecycleService.class)));
//    System.out.println("SkinService : " + (getService(SkinService.class)));
//    System.out.println("PortalContainerInfo : " + (getService(PortalContainerInfo.class)));
//    System.out.println("LogConfigurationInitializer : " + (getService(LogConfigurationInitializer.class)));
  }
  
  public void testInitializedServices() {
    assertNotNull(this.container);
    assertNotNull(this.orgService);
    assertNotNull(this.repositoryService);
    assertNotNull(this.sessionProviderService_);
  }
  
  public void testApplySession() throws RepositoryConfigurationException, RepositoryException{
    assertNotNull(this.session);
    assertNotNull(this.repository);
    assertEquals(IdentityConstants.SYSTEM, session.getUserID());

    applyUserSession("john", "gtn", COLLABORATION_WS);
    assertNotNull(this.session);
    assertEquals("john", session.getUserID());
    
    applyUserSession("john", "gtn", DMSSYSTEM_WS);
    assertNotNull(this.session);
    assertEquals("john", session.getUserID());
  }
  
  public void testAchieveResource() throws Exception{
    StringWriter writer = new StringWriter().append("name=guest");
    byte[] data = writer.getBuffer().toString().getBytes("UTF-8");
    MultivaluedMap<String, String> h = new MultivaluedMapImpl();
    h.putSingle("content-type", MediaType.APPLICATION_FORM_URLENCODED.toString());
    h.putSingle("content-length", "" + data.length);
    ContainerResponse response = service(HTTPMethods.POST.toString(), MOCK_RESOURCE_URL, StringUtils.EMPTY, h, data);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Registed guest", response.getEntity().toString());
    
    response = service(HTTPMethods.GET.toString(), MOCK_RESOURCE_URL + "?name=guest", StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Hello guest", response.getEntity().toString());
    
    response = service(HTTPMethods.DELETE.toString(), MOCK_RESOURCE_URL + "?name=guest", StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Removed guest", response.getEntity().toString());    
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
