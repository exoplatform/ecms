/*
* Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform;

import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.exoplatform.ecm.connector.dlp.TestFileDlpConnector;
//import org.exoplatform.wcm.connector.authoring.TestCopyContentFile;
//import org.exoplatform.wcm.connector.authoring.TestLifecycleConnector;
import org.exoplatform.wcm.connector.collaboration.TestDownloadConnector;
import org.exoplatform.wcm.connector.collaboration.TestFavoriteRESTService;
import org.exoplatform.wcm.connector.collaboration.TestOpenInOfficeConnector;
import org.exoplatform.wcm.connector.collaboration.TestThumbnailRESTService;
import org.exoplatform.wcm.connector.fckeditor.TestPortalLinkConnector;
import org.exoplatform.wcm.connector.viewer.TestPDFViewerRESTService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Duy Dong
 *          dongpd@exoplatform.com
 */
@RunWith(Suite.class)
@SuiteClasses({ 
  TestPortalLinkConnector.class,
  TestPDFViewerRESTService.class,
//  TestCopyContentFile.class,
//  TestLifecycleConnector.class,
  TestDownloadConnector.class,
  TestOpenInOfficeConnector.class,
  TestThumbnailRESTService.class,
  TestFavoriteRESTService.class,
  TestFileDlpConnector.class
})
@ConfigTestCase(BaseConnectorTestCase.class)
public class BaseConnectorTestSuite extends BaseExoContainerTestSuite {

  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(BaseConnectorTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}
