/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.seo;

import java.util.ArrayList;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.gatein.pc.api.PortletInvoker;
import org.mockito.Mockito;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 21, 2011  
 */

public class TestSEOService extends BaseWCMTestCase{
  
  /** The SEO Service. */
  private SEOService seoService;
  
  public void setUp() throws Exception {
    super.setUp();
      sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    seoService = (SEOService) container.getComponentInstanceOfType(SEOService.class);
    applySystemSession();
  }
  
  public void testConstruct() throws Exception{
    assertNotNull(seoService);
  }
  
  /**
   * test store page metadata
   * @throws Exception
   */
  public void testStorePageMetadata() throws Exception {
   PageMetadataModel metaModel = new PageMetadataModel();
    metaModel.setUri("home");
    metaModel.setPageReference("home");
    metaModel.setKeywords("test");
    metaModel.setRobotsContent("index,follow");
    metaModel.setSiteMap(true);
    metaModel.setDescription("test description");
    metaModel.setPriority(0);
    String portalName = "classic";
    WebuiRequestContext context = Mockito.mock(WebuiRequestContext.class); 
    WebuiRequestContext.setCurrentInstance(context);
    PortalRequestContext ctx = Mockito.mock(PortalRequestContext.class);
    Mockito.when(Util.getPortalRequestContext()).thenReturn(ctx);
    
    UIPortal uiPortal = Mockito.mock(UIPortal.class);
    UIPortalApplication uiPortalApp = Mockito.mock(UIPortalApplication.class);
    
    uiPortalApp.setCurrentSite(uiPortal);
    Mockito.when(ctx.getUIApplication()).thenReturn(uiPortalApp);
    Mockito.when(uiPortalApp.getCurrentSite()).thenReturn(uiPortal);
    
    UserNode userNode = Mockito.mock(UserNode.class);
    Mockito.when(uiPortal.getName()).thenReturn(portalName);
    Mockito.when(uiPortal.getSelectedUserNode()).thenReturn(userNode);
    session = sessionProvider.getSession("collaboration", repository);
    Node rootNode = session.getRootNode(); 
    Node seoNode = rootNode.addNode("SEO");
    seoNode.addMixin("mix:referenceable");
    session.save();
    Mockito.when(userNode.getId()).thenReturn(seoNode.getUUID());
    
    seoService.storeMetadata(metaModel,portalName,false, "en");
    PageMetadataModel retrieveModel = seoService.getPageMetadata("home", "en");
    assertEquals(retrieveModel.getKeywords(), "test");
  }
  
  /**
   * test store content metadata
   * @throws Exception
   */
  public void testStoreContentMetadata() throws Exception {
    applyUserSession("john", "gtn", "collaboration");
    WebuiRequestContext context = Mockito.mock(WebuiRequestContext.class); 
    WebuiRequestContext.setCurrentInstance(context);
    PortalRequestContext ctx = Mockito.mock(PortalRequestContext.class);
    Mockito.when(Util.getPortalRequestContext()).thenReturn(ctx);
    PageMetadataModel metaModel = new PageMetadataModel();
    Node seoNode = session.getRootNode().addNode("parentNode").addNode("childNode");
    if(!seoNode.isNodeType("mix:referenceable")) {
    	seoNode.addMixin("mix:referenceable");
    }
    session.save();
    metaModel.setUri(seoNode.getUUID());
    metaModel.setKeywords("test");
    metaModel.setRobotsContent("index,follow");
    metaModel.setSiteMap(true);
    metaModel.setDescription("test description");
    metaModel.setPriority(0);
    seoService.storeMetadata(metaModel,"classic",true, "en");
    ArrayList<String> params = new ArrayList<String>();
    params.add("/repository/collaboration/parentNode/childNode");
    PageMetadataModel retrieveModel = seoService.getContentMetadata(params, "en");
    assertEquals(retrieveModel.getKeywords(), "test");
  }
  
  /**
   * test remove page metedate
   */
  public void tesRemovePageMetadata() throws Exception{
    PageMetadataModel metaModel = new PageMetadataModel();    
    metaModel.setUri("home");
    metaModel.setPageReference("home");
    metaModel.setKeywords("test");    
    metaModel.setRobotsContent("index,follow");    
    seoService.storeMetadata(metaModel,"classic",false, "en");
    assertEquals("test", seoService.getPageMetadata("home", "en").getKeywords());
    seoService.removePageMetadata(metaModel, "classic",false, "en");
    assertNull(seoService.getPageMetadata("home", "en"));     
  }
  
  /**
   * test remove content metedate
   */
  public void tesRemoveContentMetadata() throws Exception{
    PageMetadataModel metaModel = new PageMetadataModel();    
    metaModel.setUri("home");
    metaModel.setKeywords("test");    
    metaModel.setRobotsContent("index,follow");    
    seoService.storeMetadata(metaModel,"classic",true, "en");
    ArrayList<String> params = new ArrayList<String>();
    params.add("home");
    assertEquals("test", seoService.getContentMetadata(params, "en").getKeywords());
    seoService.removePageMetadata(metaModel, "classic",true, "en");
    assertNull(seoService.getPageMetadata("home", "en"));     
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
