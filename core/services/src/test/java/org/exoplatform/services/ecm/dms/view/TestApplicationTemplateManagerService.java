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
package org.exoplatform.services.ecm.dms.view;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL
 * June 09, 2009
 */
public class TestApplicationTemplateManagerService extends BaseDMSTestCase {

  private ApplicationTemplateManagerService appTemplateManagerService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private String basedApplicationTemplatesPath;
  private Session sessionDMS;

  public void setUp() throws Exception {
    super.setUp();
    appTemplateManagerService = (ApplicationTemplateManagerService)container.getComponentInstanceOfType(
        ApplicationTemplateManagerService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    basedApplicationTemplatesPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWTEMPLATES_PATH);
    System.out.println("basedApplicationTemplatesPath :" + basedApplicationTemplatesPath );
    sessionDMS = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.getAllManagedPortletName()
   * Input: repository        String
   *                          The name of repository
   * Expect: Return The templates by category
   * @throws Exception
   */
  public void testGetAllManagedPortletName() throws Exception {
    List<String> listTemplateManager = appTemplateManagerService.getAllManagedPortletName(REPO_NAME);
    assertEquals(0, listTemplateManager.size());
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.getTemplatesByApplication()
   * Input: repository        String
   *                          The name of repository
   *        portletName       String
   *                          The name of portlet
   *        sessionProvider   SessionProvider
   * Expect: Return The templates by category
   * @throws Exception
   */
  public void testGetTemplatesByApplication() throws Exception {
    assertNull(appTemplateManagerService.getTemplatesByApplication(REPO_NAME,
        "UIBrowseContentPortlet", sessionProviderService_.getSystemSessionProvider(null)));
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.getTemplatesByCategory()
   * Input: repository        String
   *                          The name of repository
   *        portletName       String
   *                          The name of portlet
   *        category          String
   *                          The name of category
   *        sessionProvider   SessionProvider
   * Expect: Return The templates by category
   * @throws Exception
   */
  public void testGetTemplatesByCategory() throws Exception {
    assertEquals(1, appTemplateManagerService.getTemplatesByCategory(REPO_NAME, "content-browser",
        "detail-document", sessionProviderService_.getSystemSessionProvider(null)).size());
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.getTemplateByName()
   * Input: repository        String
   *                          The name of repository
   *        portletName       String
   *                          The name of portlet
   *        category          String
   *                          The name of category
   *        templateName      String
   *                          The name of template
   *        sessionProvider   SessionProvider
   * Expect: Return the template by name
   * @throws Exception
   */
  public void testGetTemplateByName() throws Exception {
    assertNotNull(appTemplateManagerService.getTemplateByName(REPO_NAME, "content-browser",
        "detail-document", "DocumentView", sessionProviderService_.getSystemSessionProvider(null)));
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.getTemplateByPath()
   * Input: repository    String
   *                      The name of repository
   *        templatePath  String
   *                      The path of template
   *      sessionProvider SessionProvider
   * Expect: Return template by path
   * @throws Exception
   */
  public void testGetTemplateByPath() throws Exception {
    assertNotNull(appTemplateManagerService.getTemplateByPath(REPO_NAME,
                                                              "/exo:ecm/views/templates/content-browser/detail-document/DocumentView",
                                                              sessionProviderService_.getSystemSessionProvider(null)));
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.addTemplate()
   * Input: portletTemplateHome the portlet template home
   *        config the config
   * Expect: Add the teamplate
   * @throws Exception
   */
  public void testAddTemplate() throws Exception {
    Node portletTemplateHome = (Node) sessionDMS.getItem(basedApplicationTemplatesPath);

    PortletTemplateConfig config = new PortletTemplateConfig();
    ArrayList<String> accessPermissions = new ArrayList<String>();
    accessPermissions.add("*:/platform/administrators");
    config.setCategory("categoryA");
    config.setAccessPermissions(accessPermissions);
    config.setEditPermissions(accessPermissions);
    config.setTemplateName("HelloName");
    config.setTemplateData("Hello teamplate data");
    appTemplateManagerService.addTemplate(portletTemplateHome, config);

    assertNotNull(appTemplateManagerService.getTemplateByPath(REPO_NAME,
        "/exo:ecm/views/templates/categoryA/HelloName", sessionProviderService_.getSystemSessionProvider(null)));

    sessionDMS.getItem("/exo:ecm/views/templates/categoryA").remove();
    sessionDMS.save();
  }

  /**
   * Test ApplicationTemplateManagerServiceImpl.removeTemplate()
   * Input: repository    String
   *                      The name of repository
   *        portletName   String
   *                      The name of portlet
   *        catgory       String
   *                      The category
   *        templateName  String
   *                      The name of template
   *        sessionProvider SessionProvider
   * Expect: Remove the teamplate
   * @throws Exception
   */
  public void testRemoveTemplate() throws Exception {
//    appTemplateManagerService.removeTemplate(REPO_NAME, "content-browser", "detail-document",
//        "DocumentView", SessionProviderFactory.createSessionProvider());
//
//    assertEquals(0, appTemplateManagerService.getTemplatesByCategory(REPO_NAME, "content-browser",
//        "detail-document", SessionProviderFactory.createSessionProvider()).size());
  }

  public void tearDown() throws Exception {
    Node nodeAppTemplate = (Node) sessionDMS.getItem(basedApplicationTemplatesPath);
    if (nodeAppTemplate.hasNode("categoryA")) {
      nodeAppTemplate.getNode("categoryA").remove();
    }
    sessionDMS.save();

    System.out.println("/exo:ecm/views/templates/ecm-explorer");
    Node n = (Node)sessionDMS.getItem("/exo:ecm/views/templates/ecm-explorer");
    for(NodeIterator iter = n.getNodes(); iter.hasNext();)
      System.out.println(iter.nextNode().getPath());
    super.tearDown();
  }
}
