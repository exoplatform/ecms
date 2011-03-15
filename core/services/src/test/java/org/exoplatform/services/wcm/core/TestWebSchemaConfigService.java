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
package org.exoplatform.services.wcm.core;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.javascript.JSFileHandler;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.skin.CSSFileHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.HTMLFileSchemaHandler;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 15, 2009
 */
public class TestWebSchemaConfigService extends BaseWCMTestCase {

  /** The web schema config service. */
  private WebSchemaConfigService webSchemaConfigService;

  /** The live node. */
  private Node documentNode;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    webSchemaConfigService = getService(WebSchemaConfigService.class);
    webSchemaConfigService.getAllWebSchemaHandler().clear();
    documentNode = (Node) session.getItem("/sites content/live/classic/documents");
  }

  /**
   * Test add css file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddCSSFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new CSSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());  }

  /**
   * Test add double css file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddDoubleCSSFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new CSSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    ComponentPlugin componentPlugin2 = new CSSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin2);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());  }

  /**
   * Test add js file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddJSFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new JSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());  }

  /**
   * Test add double js file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddDoubleJSFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new JSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    ComponentPlugin componentPlugin2 = new JSFileHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin2);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add html file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddHTMLFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new HTMLFileSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add double html file schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddDoubleHTMLFileSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new HTMLFileSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    ComponentPlugin componentPlugin2 = new HTMLFileSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin2);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add portal folder schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddPortalFolderSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new PortalFolderSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add double portal folder schema handler.
   * css
   *
   * @throws Exception the exception
   */
  public void testAddDoublePortalFolderSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new PortalFolderSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    ComponentPlugin componentPlugin2 = new PortalFolderSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin2);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add web content schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddWebcontentSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new WebContentSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test add double web content schema handler.
   *
   * @throws Exception the exception
   */
  public void testAddDoubleWebcontentSchemaHandler() throws Exception {
    ComponentPlugin componentPlugin = new WebContentSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin);
    ComponentPlugin componentPlugin2 = new WebContentSchemaHandler();
    webSchemaConfigService.addWebSchemaHandler(componentPlugin2);
    assertEquals(1, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test get all web schema handler.
   *
   * @throws Exception the exception
   */
  public void testGetAllWebSchemaHandler() throws Exception {
    webSchemaConfigService.addWebSchemaHandler(new JSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new CSSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new HTMLFileSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new PortalFolderSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new WebContentSchemaHandler());
    assertEquals(5, webSchemaConfigService.getAllWebSchemaHandler().size());
  }

  /**
   * Test get web schema handler by type.
   *
   * @throws Exception the exception
   */
  public void testGetWebSchemaHandlerByType() throws Exception {
    webSchemaConfigService.addWebSchemaHandler(new JSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new CSSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new HTMLFileSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new PortalFolderSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new WebContentSchemaHandler());

    CSSFileHandler cssFileSchemaHandler = webSchemaConfigService.getWebSchemaHandlerByType(CSSFileHandler.class);
    assertTrue(cssFileSchemaHandler instanceof CSSFileHandler);

    JSFileHandler jsFileSchemaHandler = webSchemaConfigService.getWebSchemaHandlerByType(JSFileHandler.class);
    assertTrue(jsFileSchemaHandler instanceof JSFileHandler);

    HTMLFileSchemaHandler htmlFileSchemaHandler = webSchemaConfigService.getWebSchemaHandlerByType(HTMLFileSchemaHandler.class);
    assertTrue(htmlFileSchemaHandler instanceof HTMLFileSchemaHandler);

    PortalFolderSchemaHandler portalFolderSchemaHandler= webSchemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
    assertTrue(portalFolderSchemaHandler instanceof PortalFolderSchemaHandler);

    WebContentSchemaHandler webContentSchemaHandler = webSchemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    assertTrue(webContentSchemaHandler instanceof WebContentSchemaHandler);
  }

  /**
   * Test create css file schema handler.
   *
   * @throws Exception the exception
   */
  public void testCreateCSSFileSchemaHandler() throws Exception {
    webSchemaConfigService.addWebSchemaHandler(new CSSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new PortalFolderSchemaHandler());
    Node cssFolder = documentNode.addNode("css", NodetypeConstant.EXO_CSS_FOLDER);
    Node cssNode = cssFolder.addNode("default.css", NodetypeConstant.NT_FILE);
    cssNode.setProperty(NodetypeConstant.EXO_ACTIVE, true);
    cssNode.setProperty(NodetypeConstant.EXO_PRIORITY, 1);
    cssNode.setProperty(NodetypeConstant.EXO_SHARED_CSS, true);

    Node cssContent = cssNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
    cssContent.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
    cssContent.setProperty(NodetypeConstant.JCR_MIME_TYPE, "text/css");
    cssContent.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new Date().getTime());
    cssContent.setProperty(NodetypeConstant.JCR_DATA, "This is the default.css file.");
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    webSchemaConfigService.createSchema(sessionProvider, cssNode);

    Node result = (Node)session.getItem("/sites content/live/classic/documents/css/default.css");
    assertTrue(result.isNodeType(NodetypeConstant.EXO_CSS_FILE));
    assertTrue(result.isNodeType(NodetypeConstant.EXO_OWNEABLE));
    assertEquals(result.getProperty(NodetypeConstant.EXO_PRESENTATION_TYPE).getString(), NodetypeConstant.EXO_CSS_FILE);

    cssFolder.remove();
    session.save();
  }

  /**
   * Test create js file schema handler.
   *
   * @throws Exception the exception
   */
  public void testCreateJSFileSchemaHandler() throws Exception {
    webSchemaConfigService.addWebSchemaHandler(new JSFileHandler());
    webSchemaConfigService.addWebSchemaHandler(new PortalFolderSchemaHandler());
    Node jsFolder = documentNode.addNode("js", NodetypeConstant.EXO_JS_FOLDER);
    Node jsNode = jsFolder.addNode("default.js", NodetypeConstant.NT_FILE);
    jsNode.setProperty(NodetypeConstant.EXO_ACTIVE, true);
    jsNode.setProperty(NodetypeConstant.EXO_PRIORITY, 1);
    jsNode.setProperty(NodetypeConstant.EXO_SHARED_JS, true);

    Node jsContent = jsNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
    jsContent.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
    jsContent.setProperty(NodetypeConstant.JCR_MIME_TYPE, "text/javascript");
    jsContent.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new Date().getTime());
    jsContent.setProperty(NodetypeConstant.JCR_DATA, "This is the default.js file.");
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    webSchemaConfigService.createSchema(sessionProvider, jsNode);

    Node result = (Node)session.getItem("/sites content/live/classic/documents/js/default.js");
    assertTrue(result.isNodeType(NodetypeConstant.EXO_JS_FILE));
    assertTrue(result.isNodeType(NodetypeConstant.EXO_OWNEABLE));
    assertEquals(result.getProperty(NodetypeConstant.EXO_PRESENTATION_TYPE).getString(), NodetypeConstant.EXO_JS_FILE);

    jsFolder.remove();
    session.save();
  }

  /**
   * Test create html file schema handler with no pre-defined folder.
   *
   * @throws Exception the exception
   */
  public void testCreateWebcontentSchemaHandler_01() throws Exception {
    webSchemaConfigService.addWebSchemaHandler(new WebContentSchemaHandler());
    Node webcontentNode = documentNode.addNode("webcontent", NodetypeConstant.EXO_WEBCONTENT);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    webSchemaConfigService.createSchema(sessionProvider, webcontentNode);

    Node result = (Node)session.getItem("/sites content/live/classic/documents/webcontent");
    assertEquals("css", result.getNode("css").getName());
    assertEquals(NodetypeConstant.EXO_CSS_FOLDER, result.getNode("css").getPrimaryNodeType().getName());
    assertEquals("js", result.getNode("js").getName());
    assertEquals(NodetypeConstant.EXO_JS_FOLDER, result.getNode("js").getPrimaryNodeType().getName());
    assertEquals("documents", result.getNode("documents").getName());
    assertEquals(NodetypeConstant.NT_UNSTRUCTURED, result.getNode("documents").getPrimaryNodeType().getName());
    Node mediasNode = result.getNode("medias");
    assertEquals("medias", mediasNode.getName());
    assertEquals(NodetypeConstant.EXO_MULTIMEDIA_FOLDER, result.getNode("medias").getPrimaryNodeType().getName());
    assertEquals("images", mediasNode.getNode("images").getName());
    assertEquals(NodetypeConstant.NT_FOLDER, mediasNode.getNode("images").getPrimaryNodeType().getName());
    assertEquals("videos", mediasNode.getNode("videos").getName());
    assertEquals(NodetypeConstant.NT_FOLDER, mediasNode.getNode("videos").getPrimaryNodeType().getName());
    assertEquals("audio", mediasNode.getNode("audio").getName());
    assertEquals(NodetypeConstant.NT_FOLDER, mediasNode.getNode("audio").getPrimaryNodeType().getName());

    result.remove();
    session.save();
  }

  /**
   * Test create html file schema handler.
   */
  public void testCreateHTMLFileSchemaHandler() throws Exception {
    Node htmlFolder = documentNode.addNode("html", NodetypeConstant.EXO_WEB_FOLDER);

    Node htmlFile = htmlFolder.addNode("htmlFile", NodetypeConstant.NT_FILE);

    Node htmlContent = htmlFile.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
    htmlContent.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
    htmlContent.setProperty(NodetypeConstant.JCR_MIME_TYPE, "text/html");
    htmlContent.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new Date().getTime());
    htmlContent.setProperty(NodetypeConstant.JCR_DATA, "This is the default.html file.");
    session.save();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    webSchemaConfigService.addWebSchemaHandler(new HTMLFileSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new WebContentSchemaHandler());
    webSchemaConfigService.createSchema(sessionProvider, htmlFile);
    session.save();
    Node result = (Node)session.getItem("/sites content/live/classic/documents/html/htmlFile/default.html");
    assertTrue(result.isNodeType(NodetypeConstant.EXO_HTML_FILE));
    assertEquals(result.getProperty(NodetypeConstant.EXO_PRESENTATION_TYPE).getString(), NodetypeConstant.EXO_HTML_FILE);
  }

  /**
   * Test modified css file schema handler.
   *
   * @throws Exception the exception
   */
  public void testUpdateCSSFileSchemaHandlerOnModify() throws Exception {

  }

  /**
   * Test modified js file schema handler.
   *
   * @throws Exception the exception
   */
  public void testUpdateJSFileSchemaHandlerOnModify() throws Exception {}

  /**
   * Test modified html file schema handler.
   */
  public void testUpdateHTMLFileSchemaHandlerOnModify() throws Exception {
    String htmlData = "<html>" + "<head>" + "<title>My own HTML file</title>"
      + "</head>" + "<body>" + "<h1>the first h1 tag</h1>"
      + "<h2>the first h2 tag</h2>"
      + "<h2>the second h2 tag</h2>"
      + "<h1>the second h1 tag</h1>"
      + "<h2>the third second h2 tag</h2>"
      + "<h3>the first h3 tag</h3>"
      + "<a href=" + "#" + ">Test</a>"
      + "</body>" + "</html>";
    Node htmlFolder = documentNode.addNode("html", NodetypeConstant.EXO_WEB_FOLDER);

    Node htmlFile = htmlFolder.addNode("htmlFile", NodetypeConstant.NT_FILE);

    Node htmlContent = htmlFile.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
    htmlContent.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
    htmlContent.setProperty(NodetypeConstant.JCR_MIME_TYPE, "text/html");
    htmlContent.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new Date().getTime());
    htmlContent.setProperty(NodetypeConstant.JCR_DATA, htmlData);

    session.save();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    webSchemaConfigService.addWebSchemaHandler(new HTMLFileSchemaHandler());
    webSchemaConfigService.addWebSchemaHandler(new WebContentSchemaHandler());
    webSchemaConfigService.createSchema(sessionProvider, htmlFile);
    session.save();
    Node result = (Node)session.getItem("/sites content/live/classic/documents/html/htmlFile/default.html");

    assertTrue(result.isNodeType(NodetypeConstant.EXO_HTML_FILE));
    assertEquals(result.getProperty(NodetypeConstant.EXO_PRESENTATION_TYPE).getString(), NodetypeConstant.EXO_HTML_FILE);
    webSchemaConfigService.updateSchemaOnModify(sessionProvider, htmlFile);

    Node webContent = (Node)session.getItem("/sites content/live/classic/documents/html/htmlFile");
    assertTrue(webContent.hasProperty("exo:links"));
  }

  /**
   * Test remove css file schema handler.
   */
  public void testUpdateCSSFileSchemaHandlerOnRemove() {

  }

  /**
   * Test remove js file schema handler.
   *
   * @throws Exception the exception
   */
  public void testUpdateJSFileSchemaHandlerOnRemove() throws Exception {}

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    super.tearDown();
    NodeIterator nodeIterator = documentNode.getNodes();
    while (nodeIterator.hasNext()) {
      nodeIterator.nextNode().remove();
    }
    session.save();
  }
}
