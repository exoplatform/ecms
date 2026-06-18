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
package org.exoplatform.services.wcm;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.exoplatform.services.attachments.service.AttachmentServiceTest;
import org.exoplatform.services.cms.documents.TestDocumentService;
import org.exoplatform.services.cms.documents.TestDocumentTypeService;
import org.exoplatform.services.cms.documents.impl.DocumentServiceImplTest;
import org.exoplatform.services.cms.lock.impl.TestLockService;
import org.exoplatform.services.ecm.dms.cms.TestCmsService;
import org.exoplatform.services.ecm.dms.configuration.TestDMSConfigurationService;
import org.exoplatform.services.ecm.dms.documents.TestTrashService;
import org.exoplatform.services.ecm.dms.drive.TestDriveService;
import org.exoplatform.services.ecm.dms.link.TestLinkManager;
import org.exoplatform.services.ecm.dms.relation.TestRelationsService;
import org.exoplatform.services.ecm.dms.scripts.TestScriptService;
import org.exoplatform.services.ecm.dms.test.LinkUtilsTest;
import org.exoplatform.services.ecm.dms.test.TestSymLink;
import org.exoplatform.services.ecm.dms.thumbnail.TestThumbnailService;
import org.exoplatform.services.pdfviewer.TestPDFViewerService;
import org.exoplatform.services.rest.AttachmentsRestServiceTest;
import org.exoplatform.services.rest.TestDocumentsAppRedirectService;
import org.exoplatform.services.wcm.core.TestWCMConfigurationService;
import org.exoplatform.services.wcm.core.TestWCMService;
import org.exoplatform.services.wcm.friendly.TestFriendlyService;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Duy Dong
 *          dongpd@exoplatform.com
 */
@RunWith(Suite.class)
@SuiteClasses({ 
  TestWCMService.class,
  TestWCMConfigurationService.class,
  TestFriendlyService.class,
  TestDocumentTypeService.class,
  TestLockService.class,
  TestTrashService.class,
  TestRelationsService.class,
  TestDriveService.class,
  TestScriptService.class,
  TestCmsService.class,
  TestLinkManager.class,
  LinkUtilsTest.class,
  TestSymLink.class,
  TestDMSConfigurationService.class,
  TestThumbnailService.class,
  TestPDFViewerService.class,
  AttachmentsRestServiceTest.class,
  TestDocumentsAppRedirectService.class,
  TestDocumentService.class,
  AttachmentServiceTest.class,
  DocumentServiceImplTest.class
})
@ConfigTestCase(BaseWCMTestCase.class)
public class BaseWCMTestSuite extends BaseExoContainerTestSuite {

  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(BaseWCMTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}
