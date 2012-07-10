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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.StringWriter;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.mock.MockRestService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.security.IdentityConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 6, 2012  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/mock-rest-configuration.xml")
  })
public class DummyECMSTestCase extends BaseECMSResourceTestCase {
  
  private final String MOCK_RESOURCE_URL = "/mock/guest";
  
  /* (non-Javadoc)
   * @see org.exoplatform.ecms.test.BaseECMSTestCase#afterContainerStart()
   */
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    MockRestService restService = (MockRestService) this.container.getComponentInstanceOfType(MockRestService.class);
    this.binder.addResource(restService, null);
  }
  
  @BeforeMethod
  protected void setUp() throws Exception {
    applySystemSession();
  }

  @Test
  public void testInitializedServices() {
    assertNotNull(this.container);
    assertNotNull(this.orgService);
    assertNotNull(this.repositoryService);
    assertNotNull(this.sessionProviderService_);
  }
  
  @Test
  public void testApplySession() throws RepositoryConfigurationException, RepositoryException{
    assertNotNull(this.session);
    assertNotNull(this.repository);
    assertEquals(IdentityConstants.SYSTEM, session.getUserID());

    applyUserSession("john", "exo", COLLABORATION_WS);
    assertNotNull(this.session);
    assertEquals("john", session.getUserID());
    
    applyUserSession("john", "exo", DMSSYSTEM_WS);
    assertNotNull(this.session);
    assertEquals("john", session.getUserID());
  }
  
  @Test
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
}
