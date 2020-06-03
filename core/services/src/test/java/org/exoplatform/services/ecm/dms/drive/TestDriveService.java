/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.ecm.dms.drive;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jun 11, 2009
 */

public class TestDriveService extends BaseWCMTestCase {
  private ManageDriveService driveService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private CacheService caService;
  private Node rootNode;
  private String drivePath;

  private static String WORKSPACE = "exo:workspace" ;
  private static String PERMISSIONS = "exo:accessPermissions" ;
  private static String VIEWS = "exo:views" ;
  private static String ICON = "exo:icon" ;
  private static String PATH = "exo:path" ;
  private static String VIEW_REFERENCES = "exo:viewPreferences" ;
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument" ;
  private static String VIEW_SIDEBAR = "exo:viewSideBar" ;
  private static String SHOW_HIDDEN_NODE = "exo:showHiddenNode" ;
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolders" ;

  private static String ALL_DRIVES_CACHED_BY_ROLES = "_allDrivesByRoles";
  private static String ALL_MAIN_CACHED_DRIVE = "_mainDrives";
  private static String ALL_PERSONAL_CACHED_DRIVE = "_personalDrives";
  private static String ALL_GROUP_CACHED_DRIVES = "_groupDrives";

  private static String CACHE_NAME = "ecms.drive";

  /**
   * Set up for testing
   *
   * In Collaboration workspace
   *
   *  /---TestTreeNode
   *        |
   *        |_____A1
   *        |     |___A1_1
   *        |         |___A1_1_1
   *        |     |___A1_2
   *        |
   *        |_____B1
   *              |___B1_1
   *
   */
  public void setUp() throws Exception {
    super.setUp();
    driveService = (ManageDriveService)container.getComponentInstanceOfType(ManageDriveService.class);
    caService = (CacheService)container.getComponentInstanceOfType(CacheService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    drivePath = nodeHierarchyCreator.getJcrPath(BasePath.EXO_DRIVES_PATH);
    applySystemSession();
    
    // clean and setup data
    removeAllDrives();
    createTree();
  }

  public void createTree() throws Exception {
    rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("TestTreeNode");
    Node nodeA1 = testNode.addNode("A1");
    nodeA1.addNode("A1_1").addNode("A1_1_1");
    nodeA1.addNode("A1_2");
    testNode.addNode("B1").addNode("B1_1");
    session.save();
  }

  /**
   * Register all drive plugins to repository
   * Input:
   *    Init three param which is configured in test-drives-configuration.xml
   * Expect:
   *    Size of list node = 3, contains 'System files' node, 'Collaboration Center' node,
   *    'Backup Administration' node
   * @throws Exception
   */
  public void testInit() throws Exception {
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    Node myDrive = (Node)mySession.getItem(drivePath);
    assertNotNull(myDrive.getNodes().getSize());
  }

  /**
   * Register a new drive to workspace or update if the drive is existing
   * Input:
   *    name = "MyDrive", workspace = COLLABORATION_WS, permissions = "*:/platform/administrators",
   *    homePath = "/TestTreeNode/A1", views = "admin-view", icon = "", viewReferences = true,
   *    viewNonDocument = true, viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *    allowCreateFolder = "nt:folder"
   * Expect:
   *    node: name = MyDrive is not null
   *    property of this node is mapped exactly
   * @throws Exception
   */
  public void testAddDrive() throws Exception {
    driveService.addDrive("MyDrive", COLLABORATION_WS, "*:/platform/administrators",
        "/TestTreeNode/A1", "admin-view", "", true, true, true, true, "nt:folder", "*");
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    Node myDrive = (Node)mySession.getItem(drivePath + "/MyDrive");
    assertNotNull(myDrive);
    assertEquals(myDrive.getProperty(WORKSPACE).getString(), COLLABORATION_WS) ;
    assertEquals(myDrive.getProperty(PERMISSIONS).getString(), "*:/platform/administrators");
    assertEquals(myDrive.getProperty(PATH).getString(), "/TestTreeNode/A1");
    assertEquals(myDrive.getProperty(VIEWS).getString(), "admin-view");
    assertEquals(myDrive.getProperty(ICON).getString(), "");
    assertEquals(myDrive.getProperty(VIEW_REFERENCES).getBoolean(), true);
    assertEquals(myDrive.getProperty(VIEW_NON_DOCUMENT).getBoolean(), true);
    assertEquals(myDrive.getProperty(VIEW_SIDEBAR).getBoolean(), true);
    assertEquals(myDrive.getProperty(ALLOW_CREATE_FOLDER).getString(), "nt:folder");
    assertEquals(myDrive.getProperty(SHOW_HIDDEN_NODE).getBoolean(), true);
  }

  /**
   * Test addDrive: in case drive already existed
   * @throws Exception
   */
  public void testAddDrive2() throws Exception {
    driveService.addDrive("MyDrive", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive", COLLABORATION_WS, "*:/platform/administrators",
        "/TestTreeNode/A1", "admin-view", "", true, true, true, true, "nt:folder", "*");
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    Node myDrive = (Node)mySession.getItem(drivePath + "/MyDrive");
    assertNotNull(myDrive);
    assertEquals(myDrive.getProperty(WORKSPACE).getString(), COLLABORATION_WS) ;
    assertEquals(myDrive.getProperty(PERMISSIONS).getString(), "*:/platform/administrators");
    assertEquals(myDrive.getProperty(PATH).getString(), "/TestTreeNode/A1");
    assertEquals(myDrive.getProperty(VIEWS).getString(), "admin-view");
    assertEquals(myDrive.getProperty(ICON).getString(), "");
    assertEquals(myDrive.getProperty(VIEW_REFERENCES).getBoolean(), true);
    assertEquals(myDrive.getProperty(VIEW_NON_DOCUMENT).getBoolean(), true);
    assertEquals(myDrive.getProperty(VIEW_SIDEBAR).getBoolean(), true);
    assertEquals(myDrive.getProperty(ALLOW_CREATE_FOLDER).getString(), "nt:folder");
    assertEquals(myDrive.getProperty(SHOW_HIDDEN_NODE).getBoolean(), true);

  }

  /**
   * Return an DriveData Object
   * Input: Add a new drive
   *    name = "MyDrive", workspace = COLLABORATION_WS, permissions = "*:/platform/administrators",
   *    homePath = "/TestTreeNode/A1", views = "admin-view", icon = "", viewReferences = true,
   *    viewNonDocument = true, viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *    allowCreateFolder = "nt:folder"
   * Input:
   *    driveName = "abc", repository = REPO_NAME
   * Expect:
   *    node: name = "abc" is null
   * Input:
   *    driveName = "MyDrive", repository = REPO_NAME
   * Expect:
   *    node: name = MyDrive is not null
   * @throws Exception
   */
  public void testGetDriveByName() throws Exception {
    driveService.addDrive("MyDrive", COLLABORATION_WS, "*:/platform/administrators",
        "/TestTreeNode/A1", "admin-view", "", true, true, true, true, "nt:folder", "*");
    DriveData driveData1 = driveService.getDriveByName("abc");
    assertNull(driveData1);
    DriveData driveData2 = driveService.getDriveByName("MyDrive");
    assertNotNull(driveData2);
    assertEquals(driveData2.getWorkspace(), COLLABORATION_WS) ;
    assertEquals(driveData2.getPermissions(), "*:/platform/administrators");
    assertEquals(driveData2.getHomePath(), "/TestTreeNode/A1");
    assertEquals(driveData2.getViews(), "admin-view");
    assertEquals(driveData2.getIcon(), "");
    assertEquals(driveData2.getViewPreferences(), true);
    assertEquals(driveData2.getViewNonDocument(), true);
    assertEquals(driveData2.getViewSideBar(), true);
    assertEquals(driveData2.getAllowCreateFolders(), "nt:folder");
    assertEquals(driveData2.getShowHiddenNode(), true);
  }

  /**
   * Test GetDriveByName: groupdrivetemplate is inactive
   * @throws Exception
   */
  public void testGetDriveByNameWithInActiveGroupDriveTemplate() throws Exception {
    deActivateGroupDriveTemplate();
    driveService.addDrive("MyDrive", COLLABORATION_WS, "*:/platform/administrators",
        "/TestTreeNode/A1", "admin-view", "", true, true, true, true, "nt:folder", null);

    DriveData driveData1 = driveService.getDriveByName("abc");
    DriveData driveData2 = driveService.getDriveByName("MyDrive");
    DriveData groupDrive = driveService.getDriveByName(".platform.administrators");

    assertNull(driveData1);
    assertNotNull(driveData2);
    assertEquals(driveData2.getWorkspace(), COLLABORATION_WS) ;
    assertEquals(driveData2.getPermissions(), "*:/platform/administrators");
    assertEquals(driveData2.getHomePath(), "/TestTreeNode/A1");
    assertEquals(driveData2.getViews(), "admin-view");
    assertEquals(driveData2.getIcon(), "");
    assertEquals(driveData2.getViewPreferences(), true);
    assertEquals(driveData2.getViewNonDocument(), true);
    assertEquals(driveData2.getViewSideBar(), true);
    assertEquals(driveData2.getAllowCreateFolders(), "nt:folder");
    assertEquals(driveData2.getShowHiddenNode(), true);
    assertEquals(driveData2.getAllowNodeTypesOnTree(), "*");
    assertNull(groupDrive);
  }

  /**
   * This method will look up in all workspaces of repository to find DriveData
   * Input:
   *    Add two drive
   *    1.  name = "MyDrive1", workspace = COLLABORATION_WS,
   *        permissions = "*:/platform/administrators", homePath = "/TestTreeNode/A1",
   *        views = "admin-view", icon = "", viewReferences = true, viewNonDocument = true,
   *        viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *        allowCreateFolder = "nt:folder"
   *    2.  name = "MyDrive2", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_1", views = "admin-view, system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = false, repository = REPO_NAME, allowCreateFolder = "nt:folder"
   * Expect:
   *    Size of list node = 2, contains node MyDrive1 and MyDrive2
   * @throws Exception
   */
  public void testGetAllDrives() throws Exception {
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");
    List<DriveData> listDriveData = driveService.getAllDrives();
    assertEquals(listDriveData.size(), 2);
    assertEquals(listDriveData.get(0).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(listDriveData.get(0).getPermissions(), "*:/platform/administrators");
    assertEquals(listDriveData.get(0).getHomePath(), "/TestTreeNode/A1");
    assertEquals(listDriveData.get(0).getViews(), "admin-view");
    assertEquals(listDriveData.get(0).getIcon(), "");
    assertEquals(listDriveData.get(0).getViewPreferences(), true);
    assertEquals(listDriveData.get(0).getViewNonDocument(), true);
    assertEquals(listDriveData.get(0).getViewSideBar(), true);
    assertEquals(listDriveData.get(0).getAllowCreateFolders(), "nt:folder");
    assertEquals(listDriveData.get(0).getShowHiddenNode(), true);

    assertEquals(listDriveData.get(1).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(listDriveData.get(1).getPermissions(), "*:/platform/user");
    assertEquals(listDriveData.get(1).getHomePath(), "/TestTreeNode/A1_1");
    assertEquals(listDriveData.get(1).getViews(), "admin-view, system-view");
    assertEquals(listDriveData.get(1).getIcon(), "");
    assertEquals(listDriveData.get(1).getViewPreferences(), true);
    assertEquals(listDriveData.get(1).getViewNonDocument(), true);
    assertEquals(listDriveData.get(1).getViewSideBar(), true);
    assertEquals(listDriveData.get(1).getAllowCreateFolders(), "nt:folder,nt:unstructured");
    assertEquals(listDriveData.get(1).getShowHiddenNode(), false);
  }

  /**
   * Test getAllDrives(boolean withVirtualDrives): withVirtualDrives = true;
   *
   * @throws Exception
   */
  public void testGetAllDrivesWithVitualIsTrue() throws Exception {
    activateGroupDriveTemplate("john");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");

    List<DriveData> drives = driveService.getAllDrives(true);

    assertTrue(drives.contains(driveService.getDriveByName("MyDrive1")));
    assertTrue(drives.contains(driveService.getDriveByName("MyDrive2")));
    assertTrue(drives.contains(driveService.getDriveByName("Groups")));
  }

  /**
   * Test getAllDrives(boolean withVirtualDrives): withVirtualDrives = false;
   *
   * @throws Exception
   */
  public void testGetAllDrivesWithVitualIsFalse() throws Exception {
    activateGroupDriveTemplate("john");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");

    List<DriveData> drives = driveService.getAllDrives(false);

    assertTrue(drives.contains(driveService.getDriveByName("MyDrive1")));
    assertTrue(drives.contains(driveService.getDriveByName("MyDrive2")));
    assertFalse(drives.contains(driveService.getDriveByName("Groups")));
  }

  /**
   * Remove drive with specified drive name and repository
   * Input:
   *    Add two drive
   *    1.  name = "MyDrive1", workspace = COLLABORATION_WS,
   *        permissions = "*:/platform/administrators", homePath = "/TestTreeNode/A1",
   *        views = "admin-view", icon = "", viewReferences = true, viewNonDocument = true,
   *        viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *        allowCreateFolder = "nt:folder"
   *    2.  name = "MyDrive2", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_1", views = "admin-view, system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = false, repository = REPO_NAME, allowCreateFolder = "nt:folder"
   * Input: Remove drive
   *    driveName = "MyDrive1", repository = REPO_NAME
   * Expect:
   *    Size of list node = 1
   * Input: Remove drive
   *    driveName = "xXx", repository = REPO_NAME
   * Expect:
   *    Size of list node = 1
   * @throws Exception
   */
  public void testRemoveDrive() throws Exception {
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");
    assertEquals(driveService.getAllDrives().size(), 2);
    driveService.removeDrive("MyDrive1");
    assertEquals(driveService.getAllDrives().size(), 1);
    driveService.removeDrive("xXx");
    assertEquals(driveService.getAllDrives().size(), 1);
  }

  /**
   * Return the list of DriveData
   * Input:
   *    Add three drive
   *    1.  name = "MyDrive1", workspace = COLLABORATION_WS,
   *        permissions = "*:/platform/administrators", homePath = "/TestTreeNode/A1",
   *        views = "admin-view", icon = "", viewReferences = true, viewNonDocument = true,
   *        viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *        allowCreateFolder = "nt:folder"
   *    2.  name = "MyDrive2", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_1", views = "admin-view, system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = false, repository = REPO_NAME, allowCreateFolder = "nt:unstructured"
   *    2.  name = "MyDrive3", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_2", views = "system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = true, repository = REPO_NAME, allowCreateFolder = "nt:unstructured"
   * Input:
   *    permission = "*:/platform/user", repository = REPO_NAME
   * Expect:
   *    Size of list node = 2, contains node MyDrive2 and MyDrive3
   *
   * Input:
   *    permission = "*:/platform/xXx", repository = REPO_NAME
   * Expect:
   *    Size of list node = 0
   * @throws Exception
   */
  public void testGetAllDriveByPermission() throws Exception {
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators",
        "/TestTreeNode/A1", "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:unstructured", "*");
    driveService.addDrive("MyDrive3", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_2",
        "system-view", "", true, true, true, true, "nt:unstructured", "*");
    List<DriveData> listDriveData = driveService.getAllDrives();
    assertEquals(listDriveData.size(), 3);
    List<DriveData> driveDatas = driveService.getAllDriveByPermission("*:/platform/user");
    assertEquals(driveDatas.size(), 2);
    assertEquals(driveDatas.get(0).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(driveDatas.get(0).getPermissions(), "*:/platform/user");
    assertEquals(driveDatas.get(0).getHomePath(), "/TestTreeNode/A1_1");
    assertEquals(driveDatas.get(0).getViews(), "admin-view, system-view");
    assertEquals(driveDatas.get(0).getIcon(), "");
    assertEquals(driveDatas.get(0).getViewPreferences(), true);
    assertEquals(driveDatas.get(0).getViewNonDocument(), true);
    assertEquals(driveDatas.get(0).getViewSideBar(), true);
    assertEquals(driveDatas.get(0).getAllowCreateFolders(), "nt:unstructured");
    assertEquals(driveDatas.get(0).getShowHiddenNode(), false);

    assertEquals(driveDatas.get(1).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(driveDatas.get(1).getPermissions(), "*:/platform/user");
    assertEquals(driveDatas.get(1).getHomePath(), "/TestTreeNode/A1_2");
    assertEquals(driveDatas.get(1).getViews(), "system-view");
    assertEquals(driveDatas.get(1).getIcon(), "");
    assertEquals(driveDatas.get(1).getViewPreferences(), true);
    assertEquals(driveDatas.get(1).getViewNonDocument(), true);
    assertEquals(driveDatas.get(1).getViewSideBar(), true);
    assertEquals(driveDatas.get(1).getAllowCreateFolders(), "nt:unstructured");
    assertEquals(driveDatas.get(1).getShowHiddenNode(), true);

    List<DriveData> driveDatas2 = driveService.getAllDriveByPermission("*:/platform/xXx");
    assertEquals(driveDatas2.size(), 0);
  }

  /**
   * This method will check to make sure the view is not in used before remove this view
   * Input:
   *    Add three drive
   *    1.  name = "MyDrive1", workspace = COLLABORATION_WS,
   *        permissions = "*:/platform/administrators", homePath = "/TestTreeNode/A1",
   *        views = "admin-view", icon = "", viewReferences = true, viewNonDocument = true,
   *        viewSideBar = true, showHiddenNode = true, repository = REPO_NAME,
   *        allowCreateFolder = "nt:folder"
   *    2.  name = "MyDrive2", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_1", views = "admin-view, system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = false, repository = REPO_NAME, allowCreateFolder = "nt:folder,nt:unstructured"
   *    2.  name = "MyDrive3", workspace = COLLABORATION_WS, permissions = "*:/platform/user",
   *        homePath = "/TestTreeNode/A1_2", views = "system-view", icon = "",
   *        viewReferences = true, viewNonDocument = true, viewSideBar = true,
   *        showHiddenNode = true, repository = REPO_NAME, allowCreateFolder = "nt:unstructured"
   * Input:
   *    viewName = "system-view", repository = REPO_NAME
   * Expect:
   *    result: true
   * Input:
   *    viewName = "xXx", repository = REPO_NAME
   * Expect:
   *    result: false
   * @throws Exception
   */
  public void testIsUsedView() throws Exception {
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_1",
        "admin-view, system-view", "", true, true, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive3", COLLABORATION_WS, "*:/platform/user", "/TestTreeNode/A1_2",
        "system-view", "", true, true, true, true, "nt:unstructured", "*");

    assertTrue(driveService.isUsedView("system-view"));
    assertFalse(driveService.isUsedView("xXx"));
  }

  /**
   * Test getGroupDrives: groupDriveTemplate is activated.
   *
   * @throws Exception
   */
  public void testGetGroupDrivesWithActivatedGroupDriveTemplate() throws Exception {
    activateGroupDriveTemplate("john");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("john");
    userRoles.add("manager:/organization/management/executive-board");
    userRoles.add("*:/platform/web-contributors");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/users");

    List<DriveData> drives = driveService.getGroupDrives("john", userRoles);

    assertEquals(drives.size(), 4);
   }

  /**
   * Test getGroupDrives: groupDriveTemplate is deactivated
   *
   * @throws Exception
   */
  public void testGetGroupDrivesWithDeactivatedGroupDriveTemplate() throws Exception {

    List<String> userRoles = new ArrayList<String>();
    userRoles.add("john");
    userRoles.add("manager:/organization/management/executive-board");
    userRoles.add("*:/platform/web-contributors");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/users");
    Field field = ManageDriveServiceImpl.class.getDeclaredField("groupDriveTemplate");
    field.setAccessible(true);
    field.set(driveService, null);
    driveService.clearAllDrivesCache();

    List<DriveData> drives = driveService.getGroupDrives("john", userRoles);

    assertEquals(drives.size(), 0);
   }

  /**
   * Test getGroupDrives: drivecache
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testGetGroupDrivesWithDriveCacheAndActivatedGroupDriveTemplate() throws Exception {
    activateGroupDriveTemplate("john");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("john");
    userRoles.add("manager:/organization/management/executive-board");
    userRoles.add("*:/platform/web-contributors");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/users");

    driveService.getGroupDrives("john", userRoles);

    Object drivesInCache = caService.getCacheInstance(CACHE_NAME).get(REPO_NAME + "_" + "john" + ALL_GROUP_CACHED_DRIVES);
    assertTrue(drivesInCache != null);
    assertEquals(((List<DriveData>)drivesInCache).size(), 4);
   }

  /**
   * Test getPersonalDrives.
   * Input 4 drives: 3 personal drives
   * Expected: 3 personal drives
   *
   * @throws Exception
   */
  public void testGetPersonalDrives() throws Exception {
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private",
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", userNode.getPath() + "/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");

    List<DriveData> drives = driveService.getPersonalDrives("john");

    assertEquals(drives.size(), 3);
    assertEquals(drives.get(0).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(drives.get(0).getPermissions(), "${userId}");
    assertEquals(drives.get(0).getHomePath(), "/Users/${userId}/Private");
    assertEquals(drives.get(0).getViews(), "timeline-view");
    assertEquals(drives.get(0).getIcon(), "");
    assertEquals(drives.get(0).getViewPreferences(), true);
    assertEquals(drives.get(0).getViewNonDocument(), false);
    assertEquals(drives.get(0).getViewSideBar(), true);
    assertEquals(drives.get(0).getAllowCreateFolders(), "nt:folder,nt:unstructured");
    assertEquals(drives.get(0).getShowHiddenNode(), false);
    assertEquals(drives.get(1).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(drives.get(1).getPermissions(), "*:/platform/users");
    assertEquals(drives.get(1).getHomePath(), "/Users/${userId}/Public");
    assertEquals(drives.get(1).getViews(), "simple-view, admin-view");
    assertEquals(drives.get(1).getIcon(), "");
    assertEquals(drives.get(1).getViewPreferences(), false);
    assertEquals(drives.get(1).getViewNonDocument(), false);
    assertEquals(drives.get(1).getViewSideBar(), true);
    assertEquals(drives.get(1).getAllowCreateFolders(), "nt:folder,nt:unstructured");
    assertEquals(drives.get(1).getShowHiddenNode(), false);
    assertEquals(drives.get(2).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(drives.get(2).getPermissions(), "*:/platform/users");
    assertEquals(drives.get(2).getHomePath(), userNode.getPath() + "/Public2");
    assertEquals(drives.get(2).getViews(), "simple-view, admin-view");
    assertEquals(drives.get(2).getIcon(), "");
    assertEquals(drives.get(2).getViewPreferences(), false);
    assertEquals(drives.get(2).getViewNonDocument(), false);
    assertEquals(drives.get(2).getViewSideBar(), true);
    assertEquals(drives.get(2).getAllowCreateFolders(), "nt:folder,nt:unstructured");
    assertEquals(drives.get(2).getShowHiddenNode(), false);
   }

  /**
   * Test getPersonalDrives: with drivecache
   * Input 4 drives: 3 personal drives
   * Expected: 3 personal drives
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testGetPersonalDrivesWithCache() throws Exception {
    driveService.clearAllDrivesCache();
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private",
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", userNode.getPath() + "/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");

    driveService.getPersonalDrives("john");
    driveService.getPersonalDrives("john");
    Object drivesInCache = caService.getCacheInstance(CACHE_NAME).get(REPO_NAME + "_" + "john" + ALL_PERSONAL_CACHED_DRIVE);
    assertTrue(drivesInCache != null);
    assertEquals(((List<DriveData>)drivesInCache).size(), 3);
  }

  /**
   * Test isVitualDrive: activate GroupDriveTemplate
   *
   * @throws Exception
   */
  public void testIsVitualDriveWithActiveGroupDriveTemplate() throws Exception {
    activateGroupDriveTemplate("john");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", "/Users/john/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");

    assertFalse(driveService.isVitualDrive("MyDrive1"));
    assertTrue(driveService.isVitualDrive("Groups"));
  }

  /**
   * Test isVitualDrive: deactivate GroupDriveTemplate
   *
   * @throws Exception
   */
  public void testIsVitualDriveWithDeactivatedGroupDriveTemplate() throws Exception {
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", "/Users/john/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    Field field = ManageDriveServiceImpl.class.getDeclaredField("groupDriveTemplate");
    field.setAccessible(true);
    field.set(driveService, null);

    assertFalse(driveService.isVitualDrive("MyDrive1"));
    assertFalse(driveService.isVitualDrive("Groups"));
  }

  /**
   * Test testNewRoleUpdated
   *
   * @throws Exception
   */
  public void testNewRoleUpdated() throws Exception {
    driveService.setNewRoleUpdated(true);
    assertTrue(driveService.newRoleUpdated());

    driveService.setNewRoleUpdated(false);
    assertFalse(driveService.newRoleUpdated());
  }

  /**
   * Test getMainDrives
   * Input: drives including personal, group, general drives
   * Expect: only general drives
   *
   * @throws Exception
   */
  public void testGetMainDrives() throws Exception {
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private",
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", userNode.getPath() + "/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    activateGroupDriveTemplate("john");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("john");
    userRoles.add("manager:/organization/management/executive-board");
    userRoles.add("*:/platform/web-contributors");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/users");

    List<DriveData> drives = driveService.getMainDrives("john", userRoles);

    assertEquals(drives.size(), 1);
    assertEquals(drives.get(0).getWorkspace(), COLLABORATION_WS) ;
    assertEquals(drives.get(0).getPermissions(), "*:/platform/administrators");
    assertEquals(drives.get(0).getHomePath(), "/TestTreeNode/A1");
    assertEquals(drives.get(0).getViews(), "admin-view");
    assertEquals(drives.get(0).getIcon(), "");
    assertEquals(drives.get(0).getViewPreferences(), true);
    assertEquals(drives.get(0).getViewNonDocument(), true);
    assertEquals(drives.get(0).getViewSideBar(), true);
    assertEquals(drives.get(0).getAllowCreateFolders(), "nt:folder");
    assertEquals(drives.get(0).getShowHiddenNode(), true);
  }

  /**
   * Test getMainDrives: using driveCache
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testGetMainDrivesWithCaches() throws Exception {
    driveService.clearAllDrivesCache();
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private",
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", userNode.getPath() + "/Public2",
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    activateGroupDriveTemplate("john");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("john");
    userRoles.add("manager:/organization/management/executive-board");
    userRoles.add("*:/platform/web-contributors");
    userRoles.add("*:/platform/administrators");
    userRoles.add("*:/platform/users");

    driveService.getMainDrives("john", userRoles);
    driveService.getMainDrives("john", userRoles);

    Object drivesInCache = caService.getCacheInstance("ecms.drive").get(REPO_NAME + "_" + "john" + ALL_MAIN_CACHED_DRIVE);
    assertEquals(((List<DriveData>)drivesInCache).size(), 2);
  }

  /**
   * Test getDriveByUserRoles: userId not null
   *
   * @throws Exception
   */
  public void testGetDriveByUserRolesWithUserIdNotNull() throws Exception {
    driveService.clearAllDrivesCache();
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private", // Marry have permission
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive3", COLLABORATION_WS, "*", "/TestTreeNode/A1", // Marry have permission
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", "/Users/john/Public2", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    activateGroupDriveTemplate("marry");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("marry");
    userRoles.add("*:/platform/web-contributors"); // Marry have permission
    userRoles.add("*:/platform/users"); // Marry have permission

    List<DriveData> drives = driveService.getDriveByUserRoles("marry", userRoles);

    assertEquals(drives.size(), 6);
    assertTrue(drives.contains(driveService.getDriveByName("Public")));
    assertTrue(drives.contains(driveService.getDriveByName("Private")));
    assertFalse(drives.contains(driveService.getDriveByName("MyDrive1")));
    assertFalse(drives.contains(driveService.getDriveByName("MyDrive2")));
    assertTrue(drives.contains(driveService.getDriveByName("Public2")));
    assertTrue(drives.contains(driveService.getDriveByName("MyDrive3")));
    assertTrue(drives.contains(driveService.getDriveByName(".platform.web-contributors")));
    assertTrue(drives.contains(driveService.getDriveByName(".platform.users")));
  }

  /**
   * Test getDriveByUserRoles: userId null
   *
   * @throws Exception
   */
  public void testGetDriveByUserRolesWithUserIdNull() throws Exception {
    driveService.clearAllDrivesCache();
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private", // Marry have permission
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive3", COLLABORATION_WS, "*", "/TestTreeNode/A1", // Marry have permission
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", "/Users/john/Public2", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    activateGroupDriveTemplate("marry");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("marry");
    userRoles.add("*:/platform/web-contributors"); // Marry have permission
    userRoles.add("*:/platform/users"); // Marry have permission

    List<DriveData> drives = driveService.getDriveByUserRoles(null, userRoles);
    assertEquals(drives.size(), 1);
    assertFalse(drives.contains(driveService.getDriveByName("Public")));
    assertFalse(drives.contains(driveService.getDriveByName("Private")));
    assertFalse(drives.contains(driveService.getDriveByName("MyDrive1")));
    assertFalse(drives.contains(driveService.getDriveByName("MyDrive2")));
    assertFalse(drives.contains(driveService.getDriveByName("Public2")));
    assertTrue(drives.contains(driveService.getDriveByName("MyDrive3")));
    assertFalse(drives.contains(driveService.getDriveByName(".platform.web-contributors")));
    assertFalse(drives.contains(driveService.getDriveByName(".platform.users")));
  }

  /**
   * Test getDriveByUserRoles: using driveCache
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testGetDriveByUserRolesWithDriveCache() throws Exception {
    driveService.clearAllDrivesCache();
    driveService.addDrive("Public", COLLABORATION_WS, "*:/platform/users", "/Users/${userId}/Public", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("Private", COLLABORATION_WS, "${userId}", "/Users/${userId}/Private", // Marry have permission
        "timeline-view", "", true, false, true, false, "nt:folder,nt:unstructured", "*");
    driveService.addDrive("MyDrive1", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive2", COLLABORATION_WS, "*:/platform/administrators", "/TestTreeNode/A1",
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("MyDrive3", COLLABORATION_WS, "*", "/TestTreeNode/A1", // Marry have permission
        "admin-view", "", true, true, true, true, "nt:folder", "*");
    driveService.addDrive("Public2", COLLABORATION_WS, "*:/platform/users", "/Users/john/Public2", // Marry have permission
        "simple-view, admin-view", "", false, false, true, false, "nt:folder,nt:unstructured", "*");
    activateGroupDriveTemplate("marry");
    List<String> userRoles = new ArrayList<String>();
    userRoles.add("marry");
    userRoles.add("*:/platform/web-contributors"); // Marry have permission
    userRoles.add("*:/platform/users"); // Marry have permission

    driveService.getDriveByUserRoles("marry", userRoles);
    driveService.getDriveByUserRoles("marry", userRoles);

    Object drivesInCache = caService.getCacheInstance(CACHE_NAME).get(REPO_NAME + "_" + "marry" + ALL_DRIVES_CACHED_BY_ROLES);
    assertEquals(((List<DriveData>)drivesInCache).size(), 6);

  }

  /**
   * Test init()
   */
  public void testInitMethod() {
    try {
      driveService.init();
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Add Groups Drive Template
   *
   * @throws Exception
   */
  private void activateGroupDriveTemplate(String userId) throws Exception {
    driveService.clearGroupCache(userId);
    driveService.addDrive("Groups",
        COLLABORATION_WS,
        "*:${groupId}",
        "/Groups${groupId}",
        "simple-view", "",
        false,
        true,
        true,
        false,
        "nt:folder,nt:unstructured",
        "*");
    driveService.getAllDrives();
  }

  /**
   * Deactivate group drive template
   * @throws Exception
   */
  private void deActivateGroupDriveTemplate() throws Exception {
    Field field = ManageDriveServiceImpl.class.getDeclaredField("groupDriveTemplate");
    field.setAccessible(true);
    field.set(driveService, null);
  }

  public void tearDown() throws Exception {
    removeAllDrives();

    session.getRootNode().getNode("TestTreeNode").remove();
    session.save();
    
    super.tearDown();
  }
  
  private void removeAllDrives() throws Exception {
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    Node rootDrive = (Node)mySession.getItem(drivePath);
    NodeIterator iter = rootDrive.getNodes();
    while (iter.hasNext()) {
      iter.nextNode().remove();
    }
    rootDrive.getSession().save();

    session.save();
  }
}
