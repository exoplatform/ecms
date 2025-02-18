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
    configurationService = container.getComponentInstanceOfType(WCMConfigurationService.class);
    applySystemSession();
  }

  /**
   * Test get runtime context params.
   */
  public void testGetRuntimeContextParams() {
    Collection<String> runtimeContextParams = configurationService.getRuntimeContextParams();
    assertTrue(runtimeContextParams.contains("/detail"));
    assertTrue(runtimeContextParams.contains("/printviewer"));
    assertTrue(runtimeContextParams.contains("printviewer"));
    assertTrue(runtimeContextParams.contains("/presentation/ContentListViewerPortlet"));
    assertTrue(runtimeContextParams.contains("/presentation/SingleContentViewer"));
    assertTrue(runtimeContextParams.contains("/exo:ecm/views/templates/content-list-viewer/paginators/DefaultPaginator.gtmpl"));
    assertEquals(7, runtimeContextParams.size());
  }
}
