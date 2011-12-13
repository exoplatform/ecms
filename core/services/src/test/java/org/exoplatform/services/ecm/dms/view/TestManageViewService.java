/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 18, 2009
 */
public class TestManageViewService extends BaseDMSTestCase {

  ManageViewService manageViewService;

  NodeHierarchyCreator nodeHierarchyCreator;

  Session sessionDMS;

  private static final String VIEW_NODETYPE = "exo:view";

  private static final String TAB_NODETYPE = "exo:tab";

  protected final static String TEMPLATE_NODETYPE = "exo:template";

  private String                      viewsPath;

  private String                      templatesPathEx;

  private String                      templatesPathCb;

  private String                      templatesQuery;

  private String                      templatesScripts;

  private String                      templatesDetail;

  public void setUp() throws Exception {
    super.setUp();
    manageViewService = (ManageViewService)container.getComponentInstanceOfType(ManageViewService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    sessionDMS = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    viewsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWS_PATH);
    templatesPathEx = nodeHierarchyCreator.getJcrPath(BasePath.ECM_EXPLORER_TEMPLATES);
    templatesPathCb = nodeHierarchyCreator.getJcrPath(BasePath.CB_PATH_TEMPLATES);
    System.out.println("templatesPathCb: " + templatesPathCb);
    templatesQuery = nodeHierarchyCreator.getJcrPath(BasePath.CB_QUERY_TEMPLATES);
    templatesScripts = nodeHierarchyCreator.getJcrPath(BasePath.CB_SCRIPT_TEMPLATES);
    templatesDetail = nodeHierarchyCreator.getJcrPath(BasePath.CB_DETAIL_VIEW_TEMPLATES);
  }

  /**
   * Check data that is defined from test-taxonomies-configuration.xml file
   * Expect: All data is registered
   * @throws Exception
   */
  private void checkInitData() throws Exception {
    List<?> buttons = manageViewService.getButtons();
    assertTrue(buttons.size() > 0);
    assertTrue(buttons.contains("ManageAuditing"));
    assertTrue(buttons.contains("ManagePublications"));
    assertTrue(buttons.contains("ManageVersions"));
    assertTrue(buttons.contains("ViewNodeType"));
    assertTrue(buttons.contains("WatchDocument"));
    assertTrue(sessionDMS.itemExists(viewsPath));
    assertTrue(sessionDMS.itemExists(templatesPathEx));
    assertTrue(sessionDMS.itemExists(templatesQuery));
    assertTrue(sessionDMS.itemExists(viewsPath + "/system-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/system-view/Info"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Admin"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Info"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Collaboration"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/icon-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/icon-view/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/icon-view/Collaboration"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/cover-flow"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/cover-flow/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/cover-flow/Collaboration"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/anonymous-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/anonymous-view/Actions"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/taxonomy-list"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/taxonomy-list/Info"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/taxonomy-list/Actions"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/taxonomy-icons"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/taxonomy-icons/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/simple-view"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/simple-view/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/simple-view/Collaboration"));

    assertTrue(sessionDMS.itemExists(templatesPathEx + "/SystemView"));
    assertTrue(sessionDMS.itemExists(templatesPathEx + "/ListView"));
    assertTrue(sessionDMS.itemExists(templatesPathEx + "/CoverFlow"));
    assertTrue(sessionDMS.itemExists(templatesPathEx + "/IconView"));
    assertTrue(sessionDMS.itemExists(templatesPathEx + "/ThumbnailsView"));

    assertTrue(sessionDMS.itemExists(templatesPathCb + "/PathList"));
    assertTrue(sessionDMS.itemExists(templatesPathCb + "/TreeList"));
    assertTrue(sessionDMS.itemExists(templatesQuery + "/QueryList"));
    assertTrue(sessionDMS.itemExists(templatesScripts + "/ScriptList"));
    assertTrue(sessionDMS.itemExists(templatesDetail + "/DocumentView"));
  }

  /**
   * Test ManageViewServiceImpl.start()
   * Check all data initiated
   * @throws Exception
   */
  public void testStart() throws Exception {
    checkInitData();
  }

  /**
   * Test ManageViewServiceImpl.init()
   * Check all data initiated from repository
   * @throws Exception
   */
  public void testInit() throws Exception {
    manageViewService.init(REPO_NAME);
    checkInitData();
  }

  /**
   * Test ManageViewServiceImpl.addView()
   * Input: Add one view with tab to system:
   *        name = templateTest,
   *        permission = *:/platform/administrators,
   *        path to template = /exo:ecm/views/templates/ecm-explorer/ListView
   *        2 tab: detail tab with 2 buttons: ViewNodeType,ViewContent; glide tab with one button: ViewNodeType
   * Output: Node view is registered with all component and properties defined above
   * @throws Exception
   */
  public void testAddView() throws Exception {
    String name = "templateTest";
    String permission = "*:/platform/administrators";
    String template = "/exo:ecm/views/templates/ecm-explorer/ListView";
    Tab tab1 = new Tab();
    Tab tab2 = new Tab();
    tab1.setTabName("detail");
    tab1.setButtons("ViewNodeType;ViewContent");
    tab2.setTabName("glide");
    tab2.setButtons("ViewNodeType");
    List<Tab> lstTab = new ArrayList<Tab>();
    lstTab.add(tab1);
    lstTab.add(tab2);
    manageViewService.addView(name, permission, template, lstTab, REPO_NAME);
    Node templateTest = (Node)sessionDMS.getItem(viewsPath + "/" + name);

    assertEquals(VIEW_NODETYPE, templateTest.getPrimaryNodeType().getName());
    assertEquals(permission, templateTest.getProperty("exo:accessPermissions").getString());
    assertEquals(template, templateTest.getProperty("exo:template").getString());

    Node templateTestTab1 = templateTest.getNode("detail");
    assertEquals(TAB_NODETYPE, templateTestTab1.getPrimaryNodeType().getName());
    assertEquals("ViewNodeType;ViewContent", templateTestTab1.getProperty("exo:buttons").getString());
    Node templateTestTab2 = templateTest.getNode("glide");
    assertEquals(TAB_NODETYPE, templateTestTab2.getPrimaryNodeType().getName());

  }

  /**
   * Test ManageViewServiceImpl.getViewByName()
   * Input: name of view: admin-view
   * Expect: Return node admin-view with node type = exo:view
   * @throws Exception
   */
  public void testGetViewByName() throws Exception {
    Node adminView = manageViewService.getViewByName("admin-view", REPO_NAME, WCMCoreUtils.getSystemSessionProvider());
    assertEquals(VIEW_NODETYPE, adminView.getPrimaryNodeType().getName());
  }
  /**
   * Test ManageViewServiceImpl.getButtons()
   * Get all buttons that are registered
   */
  public void testGetButtons() {
    //refer to testStart() method
  }

  /**
   * Test ManageViewServiceImpl.removeView()
   * Input: Remove one view anonymous-view
   * Expect: anonymous-view in viewsPath does not exist
   * @throws Exception
   */
  public void testRemoveView() throws Exception {
    manageViewService.removeView("anonymous-view", REPO_NAME);
    assertFalse(sessionDMS.itemExists(viewsPath + "/anonymous-view"));
  }


  /**
   * Test ManageViewServiceImpl.getAllViews()
   * Input: Get all views (after remove anonymous-view in testRemoveView method
   * Expect: Return 8 views (view total is 9 views)
   * @throws Exception
   */
  public void testGetAllViews() throws Exception {
    List<ViewConfig> viewList = manageViewService.getAllViews(REPO_NAME);
    assertNotNull(viewList.size());
  }

  /**
   * Test ManageViewServiceImpl.hasView()
   * Input: admin-view, admin_view
   * Expect: 1. Return true with view name = admin-view in path = /exo:ecm/views/userviews in dms-system
   *         2. Return false with view name = admin_view in path = /exo:ecm/views/userviews in dms-system
   * @throws Exception
   */
  public void testHasView() throws Exception {
    assertTrue(manageViewService.hasView("admin-view", REPO_NAME));
    assertFalse(manageViewService.hasView("admin_view", REPO_NAME));
  }

  /**
   * Test ManageViewServiceImpl.getTemplateHome()
   * Input: Path alias to template
   * Expect: Node in dms-system workspace
   * @throws Exception
   */
  public void testGetTemplateHome() throws Exception {
    Node homeNode = manageViewService.getTemplateHome(BasePath.CMS_VIEWS_PATH,
                                                      REPO_NAME,
                                                      WCMCoreUtils.getSystemSessionProvider());
    assertEquals("/exo:ecm/views/userviews", homeNode.getPath());
  }

  /**
   * Test ManageViewServiceImpl.getAllTemplates()
   * Input: Get all template in alias path = ecmExplorerTemplates
   * Expect: Return 5 view template: SystemView, CoverFlow, IconView, ListView, ThumbnailsView
   * @throws Exception
   */
  public void testGetAllTemplates() throws Exception {
    List<Node> lstNode = manageViewService.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES,
                                                           REPO_NAME,
                                                           WCMCoreUtils.getSystemSessionProvider());
    assertEquals(5, lstNode.size());
    List<String> templates = Arrays.asList(new String[] { lstNode.get(0).getName(),
        lstNode.get(1).getName(), lstNode.get(2).getName(), lstNode.get(3).getName(),
        lstNode.get(4).getName() });
    assertTrue(templates.contains("SystemView"));
    assertTrue(templates.contains("CoverFlow"));
    assertTrue(templates.contains("IconView"));
    assertTrue(templates.contains("ListView"));
    assertTrue(templates.contains("ThumbnailsView"));
  }

  /**
   * Test ManageViewServiceImpl.getTemplate()
   * Input: path = "/exo:ecm/views/templates/content-browser/detail-document/DocumentView"
   * Expect: No exception when get template data
   * @throws Exception
   */
  public void testGetTemplate1() throws Exception {
    manageViewService.getTemplate("/exo:ecm/views/templates/content-browser/detail-document/DocumentView",
                                  REPO_NAME,
                                  WCMCoreUtils.getSystemSessionProvider());
  }

  /**
   * Test ManageViewServiceImpl.getTemplate()
   * Input: path = "/exo:ecm/views/templates/content-browser/detail-document/DocumentView1"
   * Expect: Fail
   * @throws Exception
   */
  public void testGetTemplate2() throws Exception {
    try {
      manageViewService.getTemplate("/exo:ecm/views/templates/content-browser/detail-document/DocumentView1",
                                    REPO_NAME,
                                    WCMCoreUtils.getSystemSessionProvider());
    } catch (PathNotFoundException e) {
    }
  }

  /**
   * Test ManageViewServiceImpl.addTemplate()
   * Input: Add new template SimpleView, SystemView with data content is defined in variable
   *        templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; " +
                            "import org.exoplatform.web.application.Parameter;" +
                            "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>" +
                            "<div id=$componentId></div>"
   * Expect: 2 new templates (SimpleView, SystemView) are added with property exo:templateFile is value of variable templateFile
   * @throws Exception
   */
  public void testAddTemplate() throws Exception {
    String templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; " +
        "import org.exoplatform.web.application.Parameter;" +
        "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>" +
        "<div id=$componentId></div>";

    manageViewService.addTemplate("SimpleView", templateFile, templatesPathEx, REPO_NAME);
    Node simpleViewNode = (Node)sessionDMS.getItem(templatesPathEx + "/SimpleView");
    assertEquals("nt:file", simpleViewNode.getPrimaryNodeType().getName());
    assertEquals(templateFile, simpleViewNode.getNode("jcr:content").getProperty("jcr:data").getString());

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView", REPO_NAME);
  }

  /**
   * Test ManageViewServiceImpl.removeTemplate()
   * Input: Remove template with name = /PathList in path templatesPathCb
   * Expect: Node with above path does not exist
   * @throws Exception
   */
  public void testRemoveTemplate() throws Exception {
    String templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; " +
    "import org.exoplatform.web.application.Parameter;" +
    "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>" +
    "<div id=$componentId></div>";
    manageViewService.addTemplate("SimpleView", templateFile, templatesPathEx, REPO_NAME);

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView", REPO_NAME);
    assertFalse(sessionDMS.itemExists(templatesPathEx + "/SimpleView"));
  }

  /**
   * Test ManageViewServiceImpl.addTab()
   * Input: Add more tab in icon-view: tab = My View, buttons = Zoom Out; Zoom In
   * Expect: One tab with these buttons are added, all tabs and buttons of old views does not change
   * @throws Exception
   */
  public void testAddTab() throws Exception {
    Node nodeHome = (Node)sessionDMS.getItem(viewsPath + "/icon-view");
    String buttons = "Zoom Out; Zoom In";
    manageViewService.addTab(nodeHome, "My View", buttons);
    Node tab = (Node)sessionDMS.getItem(viewsPath + "/icon-view/My View");
    assertEquals(TAB_NODETYPE, tab.getPrimaryNodeType().getName());
    assertEquals(buttons, tab.getProperty("exo:buttons").getString());

    tab = (Node)sessionDMS.getItem(viewsPath + "/icon-view/Actions");
    assertEquals(TAB_NODETYPE, tab.getPrimaryNodeType().getName());
    assertEquals("addFolder; addDocument; editDocument; upload; addSymLink", tab.getProperty("exo:buttons").getString().trim());

    manageViewService.addTab(nodeHome, "Actions", buttons);
    tab = (Node)sessionDMS.getItem(viewsPath + "/icon-view/Actions");
    assertEquals(TAB_NODETYPE, tab.getPrimaryNodeType().getName());
    assertEquals(buttons, tab.getProperty("exo:buttons").getString());
  }

  /**
   * Clean templateTest node
   */
  public void tearDown() throws Exception {
    if (sessionDMS.itemExists(viewsPath + "/templateTest")) {
      Node templateTest = (Node)sessionDMS.getItem(viewsPath + "/templateTest");
      templateTest.remove();
      sessionDMS.save();
      sessionDMS.logout();
    }
    super.tearDown();
  }

}
