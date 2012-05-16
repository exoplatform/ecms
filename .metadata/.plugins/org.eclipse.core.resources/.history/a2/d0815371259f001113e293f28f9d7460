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

import java.util.Collection;

import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 20, 2009
 */
public class TestWCMConfigurationService extends BaseWCMTestCase {

  /** The configuration service. */
  private WCMConfigurationService configurationService;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    configurationService = getService(WCMConfigurationService.class);
  }

  /**
   * Test get site drive config.
   */
  public void testGetSiteDriveConfig() {
    DriveData driveData = configurationService.getSiteDriveConfig();
    assertEquals("{siteName}", driveData.getName());
    assertEquals("{workspace}", driveData.getWorkspace());
    assertEquals("{accessPermission}", driveData.getPermissions());
    assertEquals("{sitePath}/categories/{siteName}", driveData.getHomePath());
    assertEquals("", driveData.getIcon());
    assertEquals("wcm-category-view", driveData.getViews());
    assertFalse(driveData.getViewPreferences());
    assertTrue(driveData.getViewNonDocument());
    assertTrue(driveData.getViewSideBar());
    assertFalse(driveData.getShowHiddenNode());
    assertEquals("nt:folder,nt:unstructured", driveData.getAllowCreateFolders());
  }

  /**
   * Test get live portals location.
   */
  public void testGetLivePortalsLocation() {
    NodeLocation nodeLocation = configurationService.getLivePortalsLocation();
    assertEquals("collaboration", nodeLocation.getWorkspace());
    assertEquals("/sites content/live", nodeLocation.getPath());
  }

  /**
   * Test get runtime context param.
   */
  public void testGetRuntimeContextParam() {
    assertEquals("redactor:/platform/web-contributors", configurationService.getRuntimeContextParam(WCMConfigurationService.NEWSLETTER_MANAGE_MEMBERSHIP));
    assertEquals("/detail", configurationService.getRuntimeContextParam(WCMConfigurationService.PARAMETERIZED_PAGE_URI));
    assertEquals("/printviewer", configurationService.getRuntimeContextParam(WCMConfigurationService.PRINT_PAGE_URI));
    assertEquals("printviewer", configurationService.getRuntimeContextParam(WCMConfigurationService.PRINT_VIEWER_PAGE));
    assertEquals("/presentation/ContentListViewerPortlet", configurationService.getRuntimeContextParam(WCMConfigurationService.CLV_PORTLET));
    assertEquals("/presentation/SingleContentViewer", configurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET));
    assertEquals("/exo:ecm/views/templates/content-list-viewer/list/UIContentListPresentationDefault.gtmpl", configurationService.getRuntimeContextParam(WCMConfigurationService.FORM_VIEW_TEMPLATE_PATH));
    assertEquals("/exo:ecm/views/templates/content-list-viewer/paginators/UIPaginatorDefault.gtmpl", configurationService.getRuntimeContextParam(WCMConfigurationService.PAGINATOR_TEMPLAET_PATH));
  }

  /**
   * Test get runtime context params.
   */
  public void testGetRuntimeContextParams() {
    Collection<String> runtimeContextParams = configurationService.getRuntimeContextParams();
    assertTrue(runtimeContextParams.contains("redactor:/platform/web-contributors"));
    assertTrue(runtimeContextParams.contains("/detail"));
    assertTrue(runtimeContextParams.contains("/printviewer"));
    assertTrue(runtimeContextParams.contains("printviewer"));
    assertTrue(runtimeContextParams.contains("/presentation/ContentListViewerPortlet"));
    assertTrue(runtimeContextParams.contains("/presentation/SingleContentViewer"));
    assertTrue(runtimeContextParams.contains("/exo:ecm/views/templates/content-list-viewer/list/UIContentListPresentationDefault.gtmpl"));
    assertTrue(runtimeContextParams.contains("/exo:ecm/views/templates/content-list-viewer/paginators/UIPaginatorDefault.gtmpl"));
    assertEquals(8, runtimeContextParams.size());
  }

  /**
   * Test get shared portal name.
   */
  public void testGetSharedPortalName() {
    assertEquals("shared", configurationService.getSharedPortalName("repository"));
  }

  /**
   * Test get all live portals location.
   */
  public void testGetAllLivePortalsLocation() {
    Collection<NodeLocation> nodeLocations = configurationService.getAllLivePortalsLocation();
    assertEquals(1, nodeLocations.size());
  }
}
