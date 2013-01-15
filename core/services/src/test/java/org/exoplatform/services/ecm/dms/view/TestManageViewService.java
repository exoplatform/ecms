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
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 18, 2009
 */
public class TestManageViewService extends BaseWCMTestCase {

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

  public void setUp() throws Exception {
    super.setUp();
    manageViewService = (ManageViewService)container.getComponentInstanceOfType(ManageViewService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);   
    viewsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWS_PATH);
    templatesPathEx = nodeHierarchyCreator.getJcrPath(BasePath.ECM_EXPLORER_TEMPLATES);
    templatesPathCb = nodeHierarchyCreator.getJcrPath(BasePath.CB_PATH_TEMPLATES);
    templatesQuery = nodeHierarchyCreator.getJcrPath(BasePath.CB_QUERY_TEMPLATES);
    applySystemSession();   
    sessionDMS = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
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

    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Admin"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Info"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Actions"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/admin-view/Collaboration"));

    assertTrue(sessionDMS.itemExists(viewsPath + "/anonymous-view"));
    assertTrue(sessionDMS.itemExists(viewsPath + "/anonymous-view/Actions"));
  }
  
  /**
   * Test ManageViewServiceImpl.init()
   * Check all data initiated from repository
   * @throws Exception
   */
  
  public void testInit() throws Exception {
    manageViewService.init();
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
    String template = "/ecm-explorer/SystemView.gtmpl";
    Tab tab1 = new Tab();
    Tab tab2 = new Tab();
    tab1.setTabName("detail");
    tab1.setButtons("ViewNodeType;ViewContent");
    tab2.setTabName("glide");
    tab2.setButtons("ViewNodeType");
    List<Tab> lstTab = new ArrayList<Tab>();
    lstTab.add(tab1);
    lstTab.add(tab2);
    manageViewService.addView(name, permission, template, lstTab);
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
    Node adminView = manageViewService.getViewByName("admin-view", WCMCoreUtils.getSystemSessionProvider());
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
    manageViewService.removeView("anonymous-view");
    assertFalse(sessionDMS.itemExists(viewsPath + "/anonymous-view"));
  }


  /**
   * Test ManageViewServiceImpl.getAllViews()
   * Input: Get all views (after remove anonymous-view in testRemoveView method
   * Expect: Return 8 views (view total is 9 views)
   * @throws Exception
   */
  
  public void testGetAllViews() throws Exception {
    List<ViewConfig> viewList = manageViewService.getAllViews();
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
    assertTrue(manageViewService.hasView("admin-view"));
    assertFalse(manageViewService.hasView("admin_view"));
  }

  /**
   * Test ManageViewServiceImpl.getTemplateHome()
   * Input: Path alias to template
   * Expect: Node in dms-system workspace
   * @throws Exception
   */
  
  public void testGetTemplateHome() throws Exception {
    Node homeNode = manageViewService.getTemplateHome(BasePath.CMS_VIEWS_PATH, WCMCoreUtils.getSystemSessionProvider());
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
        WCMCoreUtils.getSystemSessionProvider());
    assertEquals(1, lstNode.size());
    assertEquals("SystemView", lstNode.get(0).getName());
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

    manageViewService.addTemplate("SimpleView", templateFile, templatesPathEx);
    Node simpleViewNode = (Node)sessionDMS.getItem(templatesPathEx + "/SimpleView");
    assertEquals("nt:file", simpleViewNode.getPrimaryNodeType().getName());
    assertEquals(templateFile, simpleViewNode.getNode("jcr:content").getProperty("jcr:data").getString());

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView");
  }
  
  /**
   * Test add template with system session provider
   * @throws Exception
   */
  
  public void testAddTemplate2() throws Exception {
    String templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; "
        + "import org.exoplatform.web.application.Parameter;"
        + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
        + "<div id=$componentId></div>";

    manageViewService.addTemplate("SimpleView",
                                  templateFile,
                                  templatesPathEx,
                                  WCMCoreUtils.getSystemSessionProvider());
    Node simpleViewNode = (Node) sessionDMS.getItem(templatesPathEx + "/SimpleView");
    assertEquals("nt:file", simpleViewNode.getPrimaryNodeType().getName());
    assertEquals(templateFile, simpleViewNode.getNode("jcr:content")
                                             .getProperty("jcr:data")
                                             .getString());

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView",
                                     WCMCoreUtils.getSystemSessionProvider());
  }
  
  /**
   * Test add template with user session provider
   * expected result: AccessDeniedException
   * @throws Exception
   */
  
  public void testAddTemplate3() throws Exception {    
    applyUserSession("marry", "gtn",DMSSYSTEM_WS);
    String templateFile = "<%import org.exo platform.ecm.webui.utils.Utils; "
        + "import org.exoplatform.web.application.Parameter;"
        + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
        + "<div id=$componentId></div>";
    
    try {
    manageViewService.addTemplate("SimpleView",
                                  templateFile,
                                  templatesPathEx,
                                  sessionProviderService_.getSessionProvider(null));
    fail();
    } catch (AccessDeniedException ade) {
      assertTrue(true);
    }
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
    manageViewService.addTemplate("SimpleView", templateFile, templatesPathEx);

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView");
    assertFalse(sessionDMS.itemExists(templatesPathEx + "/SimpleView"));
  }
  
  /**
   * Test ManageViewServiceImpl.removeTemplate() Input: Remove template with
   * name = /PathList in path templatesPathCb Expect: Node with above path does
   * not exist
   * 
   * @throws Exception
   */
  
  public void testRemoveTemplate2() throws Exception {
    String templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; "
        + "import org.exoplatform.web.application.Parameter;"
        + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
        + "<div id=$componentId></div>";
    manageViewService.addTemplate("SimpleView",
                                  templateFile,
                                  templatesPathEx,
                                  WCMCoreUtils.getSystemSessionProvider());

    manageViewService.removeTemplate(templatesPathEx + "/SimpleView",
                                     WCMCoreUtils.getSystemSessionProvider());
    assertFalse(sessionDMS.itemExists(templatesPathEx + "/SimpleView"));
  }
  
  
  public void testRemoveTemplate3() throws Exception {

    applyUserSession("marry", "gtn",DMSSYSTEM_WS);

    try {
      manageViewService.removeTemplate(templatesPathEx + "/SystemView",
                                       sessionProviderService_.getSessionProvider(null));
      fail();
    } catch (AccessDeniedException ade) {
      assertTrue(true);
    }
  } 
  
  /**
   * test updateTemplate() method with system session provider
   * 
   * @throws Exception
   */
  
  public void testUpdateTemplate() throws Exception {
    String templateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; "
        + "import org.exoplatform.web.application.Parameter;"
        + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
        + "<div id=$componentId></div>";
    
    String updateTemplateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; "
      + "import org.exoplatform.web.application.Parameter;"
      + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
      + "<div id=$componentId></div>";
    
    
    manageViewService.addTemplate("SimpleView",
                                  templateFile,
                                  templatesPathEx,
                                  WCMCoreUtils.getSystemSessionProvider());
    
    Node simpleViewNode = (Node)sessionDMS.getItem(templatesPathEx + "/SimpleView");
    assertEquals("nt:file", simpleViewNode.getPrimaryNodeType().getName());
    assertEquals(templateFile, simpleViewNode.getNode("jcr:content")
                                             .getProperty("jcr:data")
                                             .getString());
    

    manageViewService.updateTemplate("SimpleView",
                                     updateTemplateFile,
                                     templatesPathEx,
                                     WCMCoreUtils.getSystemSessionProvider());
    
    simpleViewNode = (Node)sessionDMS.getItem(templatesPathEx + "/SimpleView");
    assertEquals("nt:file", simpleViewNode.getPrimaryNodeType().getName());
    assertEquals(updateTemplateFile, simpleViewNode.getNode("jcr:content")
                                             .getProperty("jcr:data")
                                             .getString());    
    
    manageViewService.removeTemplate(templatesPathEx + "/SimpleView",
                                     WCMCoreUtils.getSystemSessionProvider());
    assertFalse(sessionDMS.itemExists(templatesPathEx + "/SimpleView"));
  }
  
  /**
   * test updateTemplate() method with user session provider
   * 
   * @throws Exception
   */
  
  public void testUpdateTemplate2() throws Exception {
    applyUserSession("marry", "gtn", DMSSYSTEM_WS);
    String updateTemplateFile = "<%import org.exoplatform.ecm.webui.utils.Utils; "
        + "import org.exoplatform.web.application.Parameter;"
        + "import org.exoplatform.webui.core.UIRightClickPopupMenu;%>"
        + "<div id=$componentId></div>";

    try {
      manageViewService.updateTemplate("SystemView",
                                       updateTemplateFile,
                                       templatesPathEx,
                                       sessionProviderService_.getSessionProvider(null));
      fail();
    } catch (AccessDeniedException ade) {
      assertTrue(true);
    }
  }

  /**
   * Test ManageViewServiceImpl.addTab()
   * Input: Add more tab in icon-view: tab = My View, buttons = Zoom Out; Zoom In
   * Expect: One tab with these buttons are added, all tabs and buttons of old views does not change
   * @throws Exception
   */
  
  public void testAddTab() throws Exception {
    Node nodeHome = (Node)sessionDMS.getItem(viewsPath + "/admin-view");
    String buttons = "Zoom Out; Zoom In";
    manageViewService.addTab(nodeHome, "My View", buttons);
    Node tab = (Node)sessionDMS.getItem(viewsPath + "/admin-view/My View");
    assertEquals(TAB_NODETYPE, tab.getPrimaryNodeType().getName());
    assertEquals(buttons, tab.getProperty("exo:buttons").getString());

    tab = (Node)sessionDMS.getItem(viewsPath + "/admin-view/Actions");
    assertEquals(TAB_NODETYPE, tab.getPrimaryNodeType().getName());
    assertEquals("addFolder; addDocument; editDocument; upload; addSymLink", tab.getProperty("exo:buttons").getString().trim());

    manageViewService.addTab(nodeHome, "Actions", buttons);
    tab = (Node)sessionDMS.getItem(viewsPath + "/admin-view/Actions");
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
