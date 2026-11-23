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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.cms.clouddrives.CloudUser;
import org.exoplatform.services.cms.clouddrives.DriveTrashedException;
import org.exoplatform.services.cms.clouddrives.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class JCRLocalCloudDriveTrashTest {

  private static final String TRASH_FOLDER       = "exo:trashFolder";

  private static final String RECYCLE_BIN_FOLDER = "exo:recycleBinFolder";

  private Session             session;

  private NodeTypeManager     nodeTypes;

  private ObservationManager  observation;

  private Node                driveNode;

  private Node                trashFolder;

  private Node                recycleBinFolder;

  private Node                folder;

  @Before
  public void setUp() throws Exception {
    session = mock(Session.class);
    Workspace workspace = mock(Workspace.class);
    nodeTypes = mock(NodeTypeManager.class);
    observation = mock(ObservationManager.class);
    when(session.getWorkspace()).thenReturn(workspace);
    when(session.getRepository()).thenReturn(mock(ManageableRepository.class));
    when(workspace.getName()).thenReturn("collaboration");
    when(workspace.getNodeTypeManager()).thenReturn(nodeTypes);
    when(workspace.getObservationManager()).thenReturn(observation);
    trashFolder = mock(Node.class);
    when(trashFolder.isNodeType(TRASH_FOLDER)).thenReturn(true);
    recycleBinFolder = mock(Node.class);
    when(recycleBinFolder.isNodeType(RECYCLE_BIN_FOLDER)).thenReturn(true);
    folder = mock(Node.class);
    driveNode = mock(Node.class);
    when(driveNode.getSession()).thenReturn(session);
    when(driveNode.getPath()).thenReturn("/Users/root/Private/drive");
    when(driveNode.getName()).thenReturn("drive");
    when(driveNode.getUUID()).thenReturn("drive-uuid");
    when(driveNode.isNodeType(JCRLocalCloudDrive.ECD_CLOUDDRIVE)).thenReturn(true);
    when(driveNode.getParent()).thenReturn(folder);
  }

  @Test
  public void testTheTrashAndARecycleBinFolderAreTrashFolders() throws Exception {
    assertTrue(JCRLocalCloudDrive.isTrashFolder(trashFolder));
    assertTrue(JCRLocalCloudDrive.isTrashFolder(recycleBinFolder));
    assertFalse(JCRLocalCloudDrive.isTrashFolder(folder));
  }

  @Test
  public void testTheRecycleBinFoldersAreWatchedWhenTheRepositoryKnowsTheirType() throws Exception {
    assertArrayEquals(new String[] { TRASH_FOLDER, RECYCLE_BIN_FOLDER }, JCRLocalCloudDrive.trashFolderTypes(session));
  }

  @Test
  public void testOnlyTheTrashIsWatchedWhenTheRepositoryDoesNotKnowTheRecycleBinFolderType() throws Exception {
    when(nodeTypes.getNodeType(RECYCLE_BIN_FOLDER)).thenThrow(new NoSuchNodeTypeException(RECYCLE_BIN_FOLDER));

    assertArrayEquals(new String[] { TRASH_FOLDER }, JCRLocalCloudDrive.trashFolderTypes(session));
  }

  @Test
  public void testADriveInARecycleBinFolderIsTrashed() throws Exception {
    when(driveNode.getParent()).thenReturn(recycleBinFolder);

    assertThrows(DriveTrashedException.class, () -> JCRLocalCloudDrive.checkNotTrashed(driveNode));
  }

  @Test
  public void testADriveIsWatchedForItsMoveToARecycleBinFolder() throws Exception {
    JCRLocalCloudDrive drive = newDrive();

    verify(observation).addEventListener(same(drive.jcrListener.trashListener),
                                         eq(Event.NODE_ADDED),
                                         isNull(),
                                         eq(false),
                                         isNull(),
                                         aryEq(new String[] { TRASH_FOLDER, RECYCLE_BIN_FOLDER }),
                                         eq(false));
  }

  @Test
  public void testAnItemInARecycleBinFolderIsInTheTrash() throws Exception {
    JCRLocalCloudDrive drive = newDrive();
    Node item = itemIn(recycleBinFolder);

    assertTrue(drive.isInTrash(item));
  }

  @Test
  public void testAnItemInAFolderIsNotInTheTrash() throws Exception {
    JCRLocalCloudDrive drive = newDrive();
    Node item = itemIn(folder);

    assertFalse(drive.isInTrash(item));
  }

  @Test
  public void testADriveMovedToARecycleBinFolderIsFinishedAsTrashed() throws Exception {
    JCRLocalCloudDrive drive = newDrive();
    Node driveRoot = movedDriveRoot(recycleBinFolder);

    drive.jcrListener.checkTrashed(driveRoot);

    // finishing the trashed drive resets the flags
    assertFalse(drive.jcrListener.trashed);
  }

  @Test
  public void testADriveMovedToAFolderIsNotFinishedAsTrashed() throws Exception {
    JCRLocalCloudDrive drive = newDrive();
    Node driveRoot = movedDriveRoot(folder);

    drive.jcrListener.checkTrashed(driveRoot);

    assertTrue(drive.jcrListener.trashed);
  }

  private JCRLocalCloudDrive newDrive() {
    SessionProviderService sessionProviders = mock(SessionProviderService.class);
    SessionProvider systemProvider = mock(SessionProvider.class);
    when(sessionProviders.getSystemSessionProvider(null)).thenReturn(systemProvider);
    try {
      when(systemProvider.getSession(anyString(), any(ManageableRepository.class))).thenReturn(session);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return mock(LoadedDrive.class,
                withSettings().useConstructor(mock(CloudUser.class),
                                              driveNode,
                                              sessionProviders,
                                              mock(NodeFinder.class),
                                              mock(ExtendedMimeTypeResolver.class))
                              .defaultAnswer(CALLS_REAL_METHODS));
  }

  private Node itemIn(Node parent) throws Exception {
    Node item = mock(Node.class);
    when(item.getPath()).thenReturn("/item");
    Node systemItem = mock(Node.class);
    when(systemItem.getParent()).thenReturn(parent);
    when(session.getItem("/item")).thenReturn(systemItem);
    return item;
  }

  private Node movedDriveRoot(Node parent) throws Exception {
    Node driveRoot = mock(Node.class);
    when(driveRoot.getParent()).thenReturn(parent);
    when(driveRoot.getSession()).thenReturn(session);
    when(driveRoot.getPath()).thenReturn("/moved/drive");
    return driveRoot;
  }

  /**
   * A drive loaded from its existing node, with nothing to check against the
   * cloud provider and no local history.
   */
  abstract static class LoadedDrive extends JCRLocalCloudDrive {

    LoadedDrive(CloudUser user,
                Node driveNode,
                SessionProviderService sessionProviders,
                NodeFinder finder,
                ExtendedMimeTypeResolver mimeTypes)
        throws Exception {
      super(user, driveNode, sessionProviders, finder, mimeTypes);
      jcrListener.trashed = true;
      jcrListener.added = true;
    }

    @Override
    protected void ensureSame(CloudUser user, Node driveNode) {
      // nothing to check
    }

    @Override
    protected void loadHistory() {
      // no local history
    }
  }

}
