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

package org.exoplatform.services.wcm.javascript;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestXJavaScriptService.
 *
 * Created by The eXo Platform SAS
 * Author : Ngoc.Tran
 * ngoc.tran@exoplatform.com
 * July 15, 2008
 */
public class TestXJavaScriptService extends BaseWCMTestCase {

  /** The javascript service. */
  private XJavascriptService javascriptService;

  /** The Constant WEB_CONTENT_NODE_NAME. */
  private static final String WEB_CONTENT_NODE_NAME = "webContent";

  private Node documentNode;
  private Node sharedJsNode;

  SessionProvider sessionProvider;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {

    super.setUp();
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    javascriptService = getService(XJavascriptService.class);
    documentNode = (Node) session.getItem("/sites content/live/classic/documents");
    sharedJsNode = (Node) session.getItem("/sites content/live/classic/js");
  }

  /**
   * Test get active java script_01.
   *
   * When parameter input is null
   */
  public void testGetActiveJavaScript_01() {
    try {
      javascriptService.getActiveJavaScript(null);
    } catch (Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test get active java script_02.
   *
   * When node input node type is not exo:webcontent.
   */
  public void testGetActiveJavaScript_02() {
    try {
      Node nodeInput = documentNode.addNode(WEB_CONTENT_NODE_NAME);
      session.save();
      String jsData = javascriptService.getActiveJavaScript(nodeInput);
      assertEquals("", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_03.
   *
   * When node input is exo:webcontent and have some child node but does not content mixin type.
   */
  public void testGetActiveJavaScript_03() {
    try {
      Node webContent = documentNode.addNode(WEB_CONTENT_NODE_NAME, "exo:webContent");

      webContent.setProperty("exo:title", WEB_CONTENT_NODE_NAME);
      webContent.addNode("jsFolder", "exo:jsFolder");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);

      assertEquals("", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_04.
   *
   * When node input is exo:webcontent and have some child node but have mixin type does not exo:jsFile.
   */
  public void testGetActiveJavaScript_04() {
    try {
      Node webContent = documentNode.addNode(WEB_CONTENT_NODE_NAME, "exo:webContent");
      webContent.setProperty("exo:title", WEB_CONTENT_NODE_NAME);
      Node jsFolder = webContent.addNode("jsFolder", "exo:jsFolder");
      Node jsNode = jsFolder.addNode("default.js", "nt:file");

      Node jsContent = jsNode.addNode("jcr:content", "nt:resource");
      jsContent.setProperty("jcr:encoding", "UTF-8");
      jsContent.setProperty("jcr:mimeType", "text/javascript");
      jsContent.setProperty("jcr:lastModified", new Date().getTime());
      jsContent.setProperty("jcr:data", "This is the default.js file.");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_05.
   *
   * Child node have properties normal and value of exo:active is:
   * - "exo:active": false
   */
  public void testGetActiveJavaScript_05() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      jsNode.setProperty("exo:active", false);
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_06.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:mimeType": text/html
   */
  public void testGetActiveJavaScript_06() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:mimeType", "text/html");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_07.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:mimeType": text/javascript
   */
  public void testGetActiveJavaScript_07() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:mimeType", "text/javascript");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_08.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:mimeType": application/x-javascript
   */
  public void testGetActiveJavaScript_08() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:mimeType", "application/x-javascript");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_09.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:mimeType": text/ecmascript
   */
  public void testGetActiveJavaScript_09() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:mimeType", "text/ecmascript");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_10.
   *
   * Child node have properties normal and value of jcr:data is ""
   */
  public void testGetActiveJavaScript_10() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:data", "");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_11.
   *
   * Child node have properties normal and value of jcr:data is:
   * - "jcr:data": This is the default.js file.
   */
  public void testGetActiveJavaScript_11() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      Node jsContent = jsNode.getNode("jcr:content");
      jsContent.setProperty("jcr:data", "This is the default.js file.");
      session.save();

      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_12.
   *
   * Child node have properties normal and value of jcr:mimeType is:
   * - "jcr:data": alert('Test method getActiveJavaScript()');.
   */
  public void testGetActiveJavaScript_12() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "alert('Test method getActiveJavaScript()');");
      session.save();
      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("alert('Test method getActiveJavaScript()');", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test get active java script_13.
   *
   * In case normal
   */
  public void testGetActiveJavaScript_13() {
    try {
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      String jsData = javascriptService.getActiveJavaScript(webContent);
      assertEquals("This is the default.js file.", jsData);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test update portal js on modify_01.
   * When node input is null.
   */
  public void testUpdatePortalJSOnModify_01() {
    try {
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      javascriptService.updatePortalJSOnModify(portalNode, null);
      session.save();
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal js on modify_02.
   * When Node input does not jsFile.
   */
  public void testUpdatePortalJSOnModify_02() {
    try {
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, null);
      createSharedJsNode(sharedJsNode);
      Node jsFolder = webContent.getNode("js");
      javascriptService.updatePortalJSOnModify(portalNode, jsFolder);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal js on modify_03.
   * When node input have jcr:data is "".
   */
  public void testUpdatePortalJSOnModify_03() {
    try {
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "");
      createSharedJsNode(sharedJsNode);
      Node jsNode = webContent.getNode("js").getNode("default.js");
      javascriptService.updatePortalJSOnModify(portalNode, jsNode);
      session.save();
    } catch(Exception e) {
      fail();
    }
  }

  /**
   * Test update portal js on modify_04.
   * When node input have jcr:data is "When perform testUpdatePortalJSOnModify...".
   *//*
  public void testUpdatePortalJSOnModify_04() {
    try {
      JavascriptConfigService configService = null;
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      createSharedJsNode(sharedJsNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "When perform testUpdatePortalJSOnModify...");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      javascriptService.updatePortalJSOnModify(portalNode, jsNode);
      session.save();
      configService = getService(JavascriptConfigService.class);
      String jsData = new String(configService.getMergedJavascript());
      assertEquals("\nWhen perform testUpdatePortalJSOnModify...", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  *//**
   * Test update portal js on modify_05.
   * When node input have jcr:data is "alert('testUpdatePortalJSOnModify...');".
   *//*
  public void testUpdatePortalJSOnModify_05() {
    try {
      JavascriptConfigService configService = null;
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      createSharedJsNode(sharedJsNode);
      Node js = sharedJsNode.getNode("sharedJsFile.js");
      js.setProperty("exo:priority", 2);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "alert('testUpdatePortalJSOnModify...');");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      javascriptService.updatePortalJSOnModify(portalNode, jsNode);
      session.save();

      configService = getService(JavascriptConfigService.class);
      String jsData = new String(configService.getMergedJavascript());
      assertEquals("\nalert('testUpdatePortalJSOnModify...');", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  public void testUpdatePortalJSOnModify_06() {
    try {
      JavascriptConfigService configService = null;
      WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);;
      LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
      String sharedPortalName = configurationService.getSharedPortalName(REPO_NAME);
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, sharedPortalName);
      Node sharedNode = (Node) session.getItem("/sites content/live/" + sharedPortalName + "/js");
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "alert('testUpdatePortalJSOnModify...');");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      createSharedJsNode(sharedNode);
      Node js = sharedNode.getNode("sharedJsFile.js");
      js.setProperty("exo:priority", 1);
      javascriptService.updatePortalJSOnModify(portal, jsNode);
      session.save();
      String jsData = "";
      configService = getService(JavascriptConfigService.class);
      jsData = new String(configService.getMergedJavascript());
      assertEquals("\nThis is the default.js file.alert('testUpdatePortalJSOnModify...');", jsData);

      session.save();
    } catch(Exception e) {
      fail();
    }
  }*/

  /**
   * Test update portal js on remove_01.
   * When node input is null.
   */
  public void testUpdatePortalJSOnRemove_01() {
    try {
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      createSharedJsNode(sharedJsNode);
      javascriptService.updatePortalJSOnRemove(portalNode, null);
    } catch(Exception e) {
      assertNotNull(e.getStackTrace());
    }
  }

  /**
   * Test update portal js on remove_03.
   * When Node input does not jsFile.
   */
  public void testUpdatePortalJSOnRemove_03() {
    try {
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      createSharedJsNode(sharedJsNode);
      javascriptService.updatePortalJSOnRemove(portalNode, sharedJsNode);
    } catch(Exception e) {
      assertNotNull(e);
    }
  }

  /**
   * Test update portal js on remove_04.
   * When node input have jcr:data is "".
   *//*
  public void testUpdatePortalJSOnRemove_04() {
    try {
      JavascriptConfigService configService = null;
      Node portalNode = findPortalNode(sessionProvider, documentNode);
      createSharedJsNode(sharedJsNode);
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      configService = getService(JavascriptConfigService.class);
      String jsData = "";
      javascriptService.updatePortalJSOnRemove(portalNode, jsNode);
      session.save();
      jsData = new String(configService.getMergedJavascript());
      assertEquals("\nThis is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  *//**
   * Test update portal js on remove_05.
   * When node input have jcr:data is "alert('testUpdatePortalJSOnModify...');".
   *//*
  public void testUpdatePortalJSOnRemove_05() {
    try {
      JavascriptConfigService configService = null;
      Node portalNode = findPortalNode(sessionProvider, documentNode);

      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "alert('testUpdatePortalJSOnModify...');");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      createSharedJsNode(sharedJsNode);
      Node js = sharedJsNode.getNode("sharedJsFile.js");
      js.setProperty("exo:priority", 1);
      session.save();
      String jsData = "";
      configService = getService(JavascriptConfigService.class);
      javascriptService.updatePortalJSOnRemove(portalNode, jsNode);
      session.save();
      jsData = new String(configService.getMergedJavascript());
      assertEquals("\nThis is the default.js file.This is the default.js file.", jsData);
    } catch(Exception e) {
      fail();
    }
  }

  *//**
   * Test update portal js on remove_06.
   * When portal node is shared portal.
   *//*
  public void testUpdatePortalJSOnRemove_06() {
    try {
      JavascriptConfigService configService = null;
      WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);;
      LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
      String sharedPortalName = configurationService.getSharedPortalName(REPO_NAME);
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, sharedPortalName);
      Node sharedNode = (Node) session.getItem("/sites content/live/" + sharedPortalName + "/js");
      Node webContent = createWebcontentNode(documentNode, WEB_CONTENT_NODE_NAME, null, null, "alert('testUpdatePortalJSOnModify...');");
      Node jsNode = webContent.getNode("js").getNode("default.js");
      createSharedJsNode(sharedNode);
      String jsData = "";
      configService = getService(JavascriptConfigService.class);
      javascriptService.updatePortalJSOnRemove(portal, jsNode);
      session.save();
      jsData = new String(configService.getMergedJavascript());
      assertEquals("\nThis is the default.js file.alert('testUpdatePortalJSOnModify...');", jsData);
      session.save();
    } catch(Exception e) {
      fail();
    }
  }*/

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

  private void createSharedJsNode(Node parentNode) throws Exception {
    Node jsNode;
    jsNode = parentNode.addNode("sharedJsFile.js", "nt:file");
    if (!jsNode.isNodeType("exo:jsFile")) {
      jsNode.addMixin("exo:jsFile");
    }
    jsNode.setProperty("exo:active", true);
    jsNode.setProperty("exo:priority", 2);
    jsNode.setProperty("exo:sharedJS", true);

    Node jsContent;
    try {
      jsContent = jsNode.getNode("jcr:content");
    } catch (Exception ex) {
      jsContent = jsNode.addNode("jcr:content", "nt:resource");
    }
    jsContent.setProperty("jcr:encoding", "UTF-8");
    jsContent.setProperty("jcr:mimeType", "text/javascript");
    jsContent.setProperty("jcr:lastModified", new Date().getTime());
    String jsData = "This is the default.js file.";
    jsContent.setProperty("jcr:data", jsData);
    session.save();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    super.tearDown();
    Node sharedPortalNode = (Node) session.getItem("/sites content/live/shared/js");
    NodeIterator nodeIterator = documentNode.getNodes();
    NodeIterator sharedNode = sharedJsNode.getNodes();
    NodeIterator sharedIterator = sharedPortalNode.getNodes();
    while(nodeIterator.hasNext()) {
        nodeIterator.nextNode().remove();
    }
    while(sharedNode.hasNext()) {
      sharedNode.nextNode().remove();
    }
    while(sharedIterator.hasNext()) {
      sharedIterator.nextNode().remove();
    }
    session.save();
  }
}
