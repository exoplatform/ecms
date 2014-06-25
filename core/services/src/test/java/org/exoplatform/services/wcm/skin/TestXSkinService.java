/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.wcm.skin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.exoplatform.commons.xml.DocumentSource;
import org.exoplatform.component.test.web.ServletContextImpl;
import org.exoplatform.component.test.web.WebAppImpl;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.test.mocks.servlet.MockServletRequest;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * The Class TestXJavaScriptService.
 *
 * Created by The eXo Platform SAS
 * Author : Ngoc.Tran
 * ngoc.tran@exoplatform.com
 * July 21, 2008
 */
public class TestXSkinService extends BaseWCMTestCase {

  /** The skin service. */
  private XSkinService skinService;
  
  protected ControllerContext controllerCtx;

  private static ServletContext mockServletContext;

  protected static MockResourceResolver resResolver;
  
  SkinService configService;

  /** The Constant WEB_CONTENT_NODE_NAME. */
  private static final String WEB_CONTENT_NODE_NAME = "webContent";

  private Node documentNode;

  private Node sharedCssNode;

  public void setUp() throws Exception {
    super.setUp();
    applySystemSession();
    documentNode = (Node) session.getItem("/sites content/live/classic/documents");
    sharedCssNode = (Node) session.getItem("/sites content/live/classic/css");
    skinService = getService(XSkinService.class);
    configService = getService(SkinService.class);
    controllerCtx = getControllerContext();
    
    URL base = TestXSkinService.class.getClassLoader().getResource("mockwebapp");
    File f = new File(base.toURI());
    mockServletContext = new ServletContextImpl(f, "/mockwebapp", "mockwebapp");
    configService.registerContext(new WebAppImpl(mockServletContext, Thread.currentThread().getContextClassLoader()));

    resResolver = new MockResourceResolver();
    configService.addResourceResolver(resResolver);

    URL url = mockServletContext.getResource("/gatein-resources.xml");
    SkinConfigParser.processConfigResource(DocumentSource.create(url), configService, mockServletContext);
  }

  /**
   * Test get active Stylesheet_01.
   *
   * When parameter input is null
   */
  public void testGetActiveStylesheet_01() {
    try {
      skinService.getActiveStylesheet(null);
      fail();
    } catch (Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test get active Stylesheet_02.
   *
   * When node input node type is not exo:webcontent.
   */
  public void testGetActiveStylesheet_02() {
    try {
      Node nodeInput = documentNode.addNode(WEB_CONTENT_NODE_NAME);
      session.save();

      String cssData = skinService.getActiveStylesheet(nodeInput);
      assertEquals("", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active Stylesheet_03.
   *
   * When node input is exo:webcontent and have some child node but does not content mixin type.
   */
  public void testGetActiveStylesheet_03() {
    try {
      Node webContent = documentNode.addNode(WEB_CONTENT_NODE_NAME, "exo:webContent");
      webContent.setProperty("exo:title", WEB_CONTENT_NODE_NAME);
      session.save();

      String cssData = skinService.getActiveStylesheet(webContent);
      assertEquals("", cssData);
    } catch(Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test get active Stylesheet_04.
   *
   * Child node have properties normal and value of exo:active is:
   * - "exo:active": false
   */
  public void testGetActiveStylesheet_04() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node cssNode = webContent.getNode("css").getNode("default.css");
      cssNode.setProperty("exo:active", false);
      session.save();
      String cssData = skinService.getActiveStylesheet(webContent);
      assertEquals("", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active Stylesheet_05.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:mimeType": text/css
   */
  
  public void testGetActiveStylesheet_05() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("css").getNode("default.css");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:mimeType", "text/css");
      session.save();
      String cssData = skinService.getActiveStylesheet(webContent);
      assertEquals("This is the default.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active Stylesheet_06.
   *
   * Child node have properties normal and value of jcr:data is ""
   */
  
  public void testGetActiveStylesheet_06() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, "", null);
      String cssData = skinService.getActiveStylesheet(webContent);
      assertEquals("", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active Stylesheet_07.
   *
   * In case normal
   */
  
  public void testGetActiveStylesheet_07() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      String cssData = skinService.getActiveStylesheet(webContent);
      assertEquals("This is the default.css file.", cssData);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test update portal Skin on modify_01.
   * When node input is null.
   */
  
  public void testUpdatePortalSkinOnModify_01() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      createSharedCssNode(sharedCssNode);
      skinService.updatePortalSkinOnModify(portal, null);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal Skin on modify_02.
   * When Node portal input is null.
   */
  
  public void testUpdatePortalSkinOnModify_02() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("css").getNode("default.css");
      skinService.updatePortalSkinOnModify(null, jsNode);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal Skin on modify_03.
   * When Node input does not cssFile.
   */
  
  public void testUpdatePortalSkinOnModify_03() {
    try {
      Node portal = this.findPortalNode(sessionProvider, documentNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      createSharedCssNode(sharedCssNode);
      Node jsFolder = webContent.getNode("css");
      skinService.updatePortalSkinOnModify(portal, jsFolder);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal Skin on modify_04.
   * When node input have jcr:data is "".
   */
  
  public void testUpdatePortalSkinOnModify_04() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      SkinService configService = null;
      Node webcontent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node cssNode = webcontent.getNode("css").getNode("default.css");
      createSharedCssNode(sharedCssNode);
      configService = getService(SkinService.class);
      configService.addSkin(webcontent.getName(), "Default", "");
      skinService.updatePortalSkinOnModify(portal, cssNode);
      session.save();
      
      String resource = "/portal/css/jcr/"+XSkinService.createModuleName("classic")+"/Default/Stylesheet.css";
      String url = newSimpleSkin(resource).createURL(controllerCtx).toString();
      
      resResolver.addResource(resource, "foo");
      assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));
//      String cssData = configService.getCSS("/portal/css/jcr/classic/Default/Stylesheet.css");
//      assertEquals("This is the sharedJsFile.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal Skin on modify_05.
   * When node input have jcr:data is "Test XSkin Service".
   */
  
  public void testUpdatePortalSkinOnModify_05() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      SkinService configService = null;
      Node webcontent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node cssNode = webcontent.getNode("css").getNode("default.css");
      createSharedCssNode(sharedCssNode);
      configService = getService(SkinService.class);
      configService.addSkin("", "Default", "");
      skinService.updatePortalSkinOnModify(portal, cssNode);
      session.save();
      String resource = "/portal/css/jcr/"+XSkinService.createModuleName("classic")+"/Default/Stylesheet.css";
      String url = newSimpleSkin(resource).createURL(controllerCtx).toString();
      
      resResolver.addResource(resource, "foo");
      assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));
//      String cssData = configService.getCSS("/portal/css/jcr/classic/Default/Stylesheet.css");
//      assertEquals("This is the sharedJsFile.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal Skin on remove_01.
   * When node input is null.
   */
  
  public void testUpdatePortalSkinOnRemove_01() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      createSharedCssNode(sharedCssNode);
      skinService.updatePortalSkinOnRemove(portal, null);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal Skin on remove_02.
   * When Node portal input is null.
   */
  
  public void testUpdatePortalSkinOnRemove_02() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node cssNode = webContent.getNode("css").getNode("default.css");
      skinService.updatePortalSkinOnRemove(null, cssNode);
      fail();
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal Skin on remove_03.
   * When Node input does not cssFile.
   */
  
  public void testUpdatePortalSkinOnRemove_03() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      createSharedCssNode(sharedCssNode);
      Node cssFolder = webContent.getNode("css");
      skinService.updatePortalSkinOnRemove(portal, cssFolder);
    } catch(Exception e) {
      assertNotNull(e);
    }
  }

  /**
   * Test update portal Skin on remove_04.
   * When node input have jcr:data is "".
   */
  
  public void testUpdatePortalSkinOnRemove_04() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      SkinService configService = null;
      Node webcontent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, "", null);
      Node cssNode = webcontent.getNode("css").getNode("default.css");
      createSharedCssNode(sharedCssNode);
      configService = getService(SkinService.class);
      configService.removeSkin(XSkinService.createModuleName(portal.getName()), "Default");
      skinService.updatePortalSkinOnRemove(portal, cssNode);
      session.save();
      
      String resource = "/portal/css/jcr/"+XSkinService.createModuleName("classic")+"/Default/Stylesheet.css";
      String url = newSimpleSkin(resource).createURL(controllerCtx).toString();
      
      resResolver.addResource(resource, "foo");
      assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));
//      String cssData = configService.getCSS("/portal/css/jcr/classic/Default/Stylesheet.css");
//      assertEquals("This is the sharedJsFile.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal Skin on remove_05.
   * When node input have jcr:data is "Test XSkin Service".
   */
  
  public void testUpdatePortalSkinOnRemove_05() {
    try {
      Node portal = findPortalNode(sessionProvider, documentNode);
      SkinService configService = null;
      Node webcontent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, "Test XSkin Service.", null);
      Node cssNode = webcontent.getNode("css").getNode("default.css");
      createSharedCssNode(sharedCssNode);
      session.save();
      configService = getService(SkinService.class);
      configService.invalidateCachedSkin("/portal/css/jcr/"+XSkinService.createModuleName("classic")+"/Default/Stylesheet.css");
      configService.addSkin(portal.getName(), "Default", "");
      skinService.updatePortalSkinOnRemove(portal, cssNode);
      session.save();
      String resource = "/portal/css/jcr/"+XSkinService.createModuleName("classic")+"/Default/Stylesheet.css";
      String url = newSimpleSkin(resource).createURL(controllerCtx).toString();
      
      resResolver.addResource(resource, "foo");
      assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));
//      String cssData = configService.getCSS("/portal/css/jcr/classic/Default/Stylesheet.css");
//      assertEquals("This is the sharedJsFile.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal Skin on remove_07.
   * When portal node is shared portal.
   */
  
  public void testUpdatePortalSkinOnRemove_07() {
    try {
      WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);;
      LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
      String sharedPortalName = configurationService.getSharedPortalName();
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, sharedPortalName);
      SkinService configService = getService(SkinService.class);
      Node sharedNode = (Node) session.getItem("/sites content/live/" + sharedPortalName + "/css");
      createSharedCssNode(sharedNode);
      configService.invalidateCachedSkin("/portal/css/jcr/" + XSkinService.createModuleName(sharedPortalName) + "/Default/Stylesheet.css");
      skinService.updatePortalSkinOnRemove(portal, null);
      session.save();

      String resource = "/portal/css/jcr/" + XSkinService.createModuleName(sharedPortalName) + "/Default/Stylesheet.css";
      String url = newSimpleSkin(resource).createURL(controllerCtx).toString();
      
      resResolver.addResource(resource, "foo");
      assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));
//      String cssData = configService.getCSS("/portal/css/jcr/" + sharedPortalName + "/Default/Stylesheet.css");
//      assertEquals("This is the sharedJsFile.css file.", cssData);
    } catch(Exception e) {
      fail();
    }
  }
  
  public void testPortalSkins() {
    try {
      WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);;
      LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
      String sharedPortalName = configurationService.getSharedPortalName();
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, sharedPortalName);
      SkinService configService = getService(SkinService.class);
      Node sharedNode = (Node) session.getItem("/sites content/live/" + sharedPortalName + "/css");
      createSharedCssNode(sharedNode);
      configService.invalidateCachedSkin("/portal/css/jcr/" + XSkinService.createModuleName(sharedPortalName) + "/Default/Stylesheet.css");      
      skinService.updatePortalSkinOnModify(portal,sharedNode);
      session.save();

      Collection<SkinConfig> skins = configService.getPortalSkins("Default");
      assertEquals(skins.size(),1);
      for (SkinConfig skin : skins){
        String url = skin.createURL(controllerCtx).toString();
        resResolver.addResource(url, "foo");
        assertEquals("This is the sharedJsFile.css file.", configService.getCSS(newControllerContext(getRouter(), url), true));  
      }          
    } catch(Exception e) {
      fail();
    }
  }
  

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    Node sharedPortalNode = (Node) session.getItem("/sites content/live/shared/css");
    NodeIterator nodeIterator = documentNode.getNodes();
    NodeIterator cssNodeIterator = sharedCssNode.getNodes();
    NodeIterator sharedIterator = sharedPortalNode.getNodes();
    while(nodeIterator.hasNext()) {
      nodeIterator.nextNode().remove();
    }
    while(cssNodeIterator.hasNext()) {
      cssNodeIterator.nextNode().remove();
    }
    while(sharedIterator.hasNext()) {
      sharedIterator.nextNode().remove();
    }
    session.save();
  }

  private Node findPortalNode(SessionProvider sessionProvider, Node child) throws Exception{
    LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
    String portalName = null;
    for(String portalPath: livePortalManagerService.getLivePortalsPath()) {
      if(child.getPath().startsWith(portalPath)) {
        portalName = livePortalManagerService.getPortalNameByPath(portalPath);
        break;
      }
    }
    if(portalName == null) return null;
    return livePortalManagerService.getLivePortal(sessionProvider, portalName);
  }

  private void createSharedCssNode(Node parentNode) throws Exception {
    Node cssNode;
    cssNode = parentNode.addNode("sharedJsFile.css", "nt:file");
    if (!cssNode.isNodeType("exo:cssFile")) {
      cssNode.addMixin("exo:cssFile");
    }
    cssNode.setProperty("exo:active", true);
    cssNode.setProperty("exo:priority", 2);
    cssNode.setProperty("exo:sharedCSS", true);

    Node cssContent;
    try {
      cssContent = cssNode.getNode("jcr:content");
    } catch (Exception ex) {
      cssContent = cssNode.addNode("jcr:content", "nt:resource");
    }
    cssContent.setProperty("jcr:encoding", "UTF-8");
    cssContent.setProperty("jcr:mimeType", "text/css");
    cssContent.setProperty("jcr:lastModified", new Date().getTime());
    String cssData = "This is the sharedJsFile.css file.";
    cssContent.setProperty("jcr:data", cssData);
    session.save();
  }
  
  Router getRouter() {
    Router router;
    try {
        router = DescriptorBuilder.router().add(
                DescriptorBuilder.route("/skins/{gtn:version}/{gtn:resource}{gtn:compress}{gtn:orientation}.css")
                        .with(DescriptorBuilder.routeParam("gtn:handler").withValue("skin"))
                        .with(DescriptorBuilder.pathParam("gtn:version").matchedBy("[^/]*").preservePath())
                        .with(DescriptorBuilder.pathParam("gtn:orientation").matchedBy("-(lt)|-(rt)|").captureGroup(true))
                        .with(DescriptorBuilder.pathParam("gtn:compress").matchedBy("-(min)|").captureGroup(true))
                        .with(DescriptorBuilder.pathParam("gtn:resource").matchedBy(".+?").preservePath())).build();
        return router;
    } catch (RouterConfigException e) {
        return null;
    }
  }
  
  public static ControllerContext newControllerContext(Router router, String requestURI) {
    try {
        MockServletRequest request = new MockServletRequest(null, new URL("http://localhost" + requestURI), "/portal",
                null, false);
        String portalPath = request.getRequestURI().substring(request.getContextPath().length());

        //
        Iterator<Map<QualifiedName, String>> matcher = router.matcher(portalPath, request.getParameterMap());
        Map<QualifiedName, String> parameters = null;
        if (matcher.hasNext()) {
            parameters = matcher.next();
        }
        return new ControllerContext(null, router, request, null, parameters);
    } catch (MalformedURLException e) {
        return null;
    }
  }
  
  ControllerContext getControllerContext() {
    try {
        return newControllerContext(getRouter());
    } catch (Exception e) {
        throw new IllegalArgumentException("The controller context is not initialized properly", e);
    }
  }

  public static ControllerContext newControllerContext(Router router) {
    return newControllerContext(router, "/portal");
  }
  
  public SimpleSkin newSimpleSkin(String uri) {
    return new SimpleSkin(configService, "module", null, uri);
  }
}
