/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.clouddrives.webui.document;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveSecurity;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudDriveStorage;
import org.exoplatform.services.cms.clouddrives.CloudDriveStorage.Change;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.clouddrives.webui.action.CloudFileActionService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;

public class CloudDriveShareDocumentServiceTest {

  private static final String           SPACE_GROUP = "/spaces/space";

  private MockedStatic<WCMCoreUtils>    wcmCoreUtils;

  private TrashService                  trashService;

  private CloudFileActionService        cloudFileActions;

  private CloudDrive                    drive;

  private Node                          cloudFile;

  private CloudDriveShareDocumentService service;

  @Before
  public void setUp() throws Exception {
    trashService = mock(TrashService.class);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(TrashService.class)).thenReturn(trashService);
    cloudFile = mock(Node.class);
    when(cloudFile.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)).thenReturn(true);
    drive = mock(CloudDrive.class, withSettings().extraInterfaces(CloudDriveStorage.class, CloudDriveSecurity.class));
    when(((CloudDriveStorage) drive).localChange(any())).thenAnswer(invocation -> ((Change<?>) invocation.getArgument(0)).apply());
    CloudDriveService cloudDrives = mock(CloudDriveService.class);
    when(cloudDrives.findDrive(cloudFile)).thenReturn(drive);
    cloudFileActions = mock(CloudFileActionService.class);
    service = spy(new CloudDriveShareDocumentService(mock(RepositoryService.class),
                                                     mock(LinkManager.class),
                                                     mock(SessionProviderService.class),
                                                     mock(ActivityManager.class),
                                                     mock(NodeHierarchyCreator.class),
                                                     mock(IdentityManager.class),
                                                     cloudDrives,
                                                     cloudFileActions,
                                                     mock(SpaceService.class)));
    // The document's own sharing, which the cloud file's completes
    doReturn("activity").when(service).publishDocumentToSpace(SPACE_GROUP, cloudFile, "", "read", true);
  }

  @After
  public void tearDown() {
    wcmCoreUtils.close();
  }

  @Test
  public void testACloudFileIsSharedToTheSpaceInTheDriveAndItsCloud() throws Exception {
    when(((CloudDriveSecurity) drive).isSharingSupported()).thenReturn(true);

    assertEquals("activity", service.publishDocumentToSpace(SPACE_GROUP, cloudFile, "", "read"));

    verify(cloudFileActions).shareToGroup(cloudFile, SPACE_GROUP, false);
    verify((CloudDriveSecurity) drive).shareFile(cloudFile, SPACE_GROUP);
  }

  /**
   * No code calls the 4-argument method today: posting and sharing an activity
   * call the 5-argument one, which the parent guards. This pins the answer an
   * extension calling the interface's 4-argument method gets.
   */
  @Test
  public void testAFileOfACloudDriveInTheTrashIsNotShared() throws Exception {
    when(((CloudDriveSecurity) drive).isSharingSupported()).thenReturn(true);
    when(trashService.isInTrash(cloudFile)).thenReturn(true);

    assertEquals("", service.publishDocumentToSpace(SPACE_GROUP, cloudFile, "", "read"));

    verify(service, never()).publishDocumentToSpace(anyString(), any(), anyString(), anyString(), anyBoolean());
    verify(cloudFileActions, never()).shareToGroup(any(), anyString(), anyBoolean());
    verify((CloudDriveSecurity) drive, never()).shareFile(any(), anyString());
  }

}
