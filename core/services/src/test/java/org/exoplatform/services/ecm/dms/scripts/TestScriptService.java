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
package org.exoplatform.services.ecm.dms.scripts;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.scripts.impl.ScriptServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SARL
 * June 09, 2009
 */
public class TestScriptService extends BaseWCMTestCase {

  private ScriptService scriptService;
  private String expectedECMScriptHomePath = "/exo:ecm/scripts/ecm-explorer";
  private String expectedBaseScriptPath = "/exo:ecm/scripts";
  private NodeHierarchyCreator nodeHierarchyCreator;
  private String cmsScriptsPath;
  private Session sessionDMS;
  
  public void setUp() throws Exception {
    super.setUp();
    scriptService = (ScriptService)container.getComponentInstanceOfType(ScriptService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    cmsScriptsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_SCRIPTS_PATH);
    applySystemSession();
    sessionDMS = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
  }


  /**
   * Test ScriptServiceImpl.init()
   * Input: repository      String
   *                        The name of repository
   * Expect: Return all data initiated from repository in test-scripts-configuration.xml file
   * @throws Exception
   */
  public void testInitRepo() throws Exception {
    scriptService.initRepo();
    assertTrue(sessionDMS.itemExists(cmsScriptsPath));
    assertTrue(sessionDMS.itemExists(cmsScriptsPath + "/ecm-explorer"));
  }

  /**
   * Test method: ScriptServiceImpl.getECMScriptHome()
   * Input: repository    String
   *                      The name of repository
   *        provider      SessionProvider
   * Expect: Return "/exo:ecm/scripts/ecm-explorer" is path of node for ECM Explorer Scripts
   * @throws Exception
   */
  public void testGetECMScriptHome() throws Exception {
    assertNotNull(scriptService.getECMScriptHome(sessionProviderService_.getSystemSessionProvider(null)));
    assertEquals(expectedECMScriptHomePath, scriptService.getECMScriptHome(
        sessionProviderService_.getSystemSessionProvider(null)).getPath());
  }

  /**
   * Test method: ScriptServiceImpl.getBaseScriptPath()
   * Expect: Return "/exo:ecm/scripts" is base path of script
   * @throws Exception
   */
  public void testGetBaseScriptPath() throws Exception {
    assertEquals(expectedBaseScriptPath, scriptService.getBaseScriptPath());
  }

  /**
   * Test ScriptServiceImpl.getScript() on a script that does not compile, then
   * on a script that does not exist
   * Expect: the first is logged at WARN with its name and the compiler's
   * message, the second at DEBUG only
   */
  public void testGetScriptLogsACompilationFailure() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(ScriptServiceImpl.class.getName());
    Level level = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.DEBUG);
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    try {
      scriptService.addScript("BrokenScript", "class BrokenScript {", sessionProvider);
      assertNull(scriptService.getScript("BrokenScript"));
      List<ILoggingEvent> warnings = appender.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
      assertEquals(appender.list.toString(), 1, warnings.size());
      assertTrue(warnings.get(0).getFormattedMessage(), warnings.get(0).getFormattedMessage().startsWith("Script BrokenScript doesn't compile: "));
      assertTrue(warnings.get(0).getFormattedMessage(), warnings.get(0).getFormattedMessage().contains("BrokenScript.groovy"));

      appender.list.clear();
      assertNull(scriptService.getScript("MissingScript"));
      assertTrue(appender.list.toString(), appender.list.stream().noneMatch(event -> event.getLevel() == Level.WARN));
      assertTrue(appender.list.toString(),
                 appender.list.stream()
                              .anyMatch(event -> event.getLevel() == Level.DEBUG
                                  && event.getFormattedMessage().equals("Script with name MissingScript doesn't exist")));
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(level);
      scriptService.removeScript("BrokenScript", sessionProvider);
    }
  }

  /**
   * Test method: ScriptServiceImpl.addScript()
   * Input: name          String
   *                      The name of script
   *        text          String
   *        repository    String
   *                      The name of repository
   *        provider      SessionProvider
   * Expect: Insert a new script
   * @throws Exception
   */
  public void testAddScript() throws Exception {
    scriptService.addScript("Hello Name", "Hello Text", sessionProviderService_.getSystemSessionProvider(null));
    Node hello = (Node)sessionDMS.getItem("/exo:ecm/scripts/Hello Name");
    assertNotNull(hello);
    assertEquals("Hello Text", hello.getNode("jcr:content").getProperty("jcr:data").getString());
  }

  /**
   * Test method: ScriptServiceImpl.getScriptNode()
   * Input: scriptName    String
   *                      The name of script
   *        repository    String
   *                      The name of repository
   *        provider      SessionProvider
   * Expect: Return script node
   * @throws Exception
   */
  public void testGetScriptNode() throws Exception {
    scriptService.addScript("My script 2", "This is my script as text 2", sessionProviderService_.getSystemSessionProvider(null));

    Node scriptNode = scriptService.getScriptNode("My script 2", sessionProviderService_.getSystemSessionProvider(null));
    assertEquals("My script 2", scriptNode.getName());
    assertEquals("This is my script as text 2", scriptNode.getNode("jcr:content").getProperty("jcr:data").getString());
  }

  /**
   * Test method: ScriptServiceImpl.removeScript()
   * Input: scriptPath    String
   *                      The path of script
   *        repository    String
   *                      The name of repository
   *        provider      SessionProvider
   * Expect: remove the script
   * @throws Exception
   */
  public void testRemoveScript() throws Exception {
    scriptService.addScript("Hello Name", "Hello Text", sessionProviderService_.getSystemSessionProvider(null));

    Node hello = scriptService.getScriptNode("Hello Name", sessionProviderService_.getSystemSessionProvider(null));
    assertNotNull(hello);
    assertEquals("Hello Text", hello.getNode("jcr:content").getProperty("jcr:data").getString());

    scriptService.removeScript("Hello Name", sessionProviderService_.getSystemSessionProvider(null));
    assertNull(scriptService.getScriptNode("Hello Name", sessionProviderService_.getSystemSessionProvider(null)));
  }

  /**
   * Clean all scripts for testing
   */
  public void tearDown() throws Exception {
    Node rootScripts = (Node)sessionDMS.getItem(cmsScriptsPath);
    String[] paths = new String[] {"My script", "My script 2", "Hello Name"};
    for (String path : paths) {
      if (rootScripts.hasNode(path)) {
        rootScripts.getNode(path).remove();
      }
    }
    sessionDMS.save();
    super.tearDown();
  }
}
