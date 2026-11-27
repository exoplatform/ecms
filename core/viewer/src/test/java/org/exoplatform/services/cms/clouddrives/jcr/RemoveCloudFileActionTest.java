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
package org.exoplatform.services.cms.clouddrives.jcr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;

public class RemoveCloudFileActionTest {

  private Context context;

  private Node    driveRoot;

  @Before
  public void setUp() throws Exception {
    Node file = mock(Node.class);
    Session session = mock(Session.class);
    when(file.getSession()).thenReturn(session);
    CloudDrive drive = mock(CloudDrive.class);
    when(drive.isConnected()).thenReturn(false);
    when(drive.getPath()).thenReturn("/drive");
    driveRoot = mock(Node.class);
    when(session.getItem("/drive")).thenReturn(driveRoot);
    CloudDriveService drives = mock(CloudDriveService.class);
    when(drives.findDrive(file)).thenReturn(drive);
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(CloudDriveService.class)).thenReturn(drives);
    context = new ContextBase();
    context.put(InvocationContext.EXO_CONTAINER, container);
    context.put(InvocationContext.CURRENT_ITEM, file);
    context.put(InvocationContext.EVENT, ExtendedEvent.NODE_REMOVED);
  }

  @Test
  public void testAFileOfADriveInARecycleBinFolderIsIgnored() throws Exception {
    Node recycleBinFolder = mock(Node.class);
    when(recycleBinFolder.isNodeType("exo:recycleBinFolder")).thenReturn(true);
    when(driveRoot.getParent()).thenReturn(recycleBinFolder);

    assertTrue(new RemoveCloudFileAction().execute(context));
  }

  @Test
  public void testAFileOfADisconnectedDriveInAFolderIsNotIgnored() throws Exception {
    when(driveRoot.getParent()).thenReturn(mock(Node.class));

    assertFalse(new RemoveCloudFileAction().execute(context));
  }

}
