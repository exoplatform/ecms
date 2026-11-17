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
 *
*/
package org.exoplatform.services.cms.documents.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.AccessDeniedException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.social.space.security.SpaceSecurityAudit;
import io.meeds.social.space.security.model.SpaceSecuritySettings;
import io.meeds.social.space.security.service.SpaceSecurityService;

/**
 * Pins which space a document belongs to, decided from the node as stored,
 * and the grant decisions under "Limit Access Management".
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentSpaceGuardTest {

  private static final String           DOCUMENT_PATH = "/Groups/spaces/marketing/Documents/a.docx";

  @Mock
  private SpaceService                  spaceService;

  @Mock
  private SpaceSecurityService          spaceSecurityService;

  @Mock
  private NodeHierarchyCreator          nodeHierarchyCreator;

  @Mock
  private LinkManager                   linkManager;

  @Mock
  private TrashService                  trashService;

  @Mock
  private RepositoryService             repositoryService;

  private MockedStatic<SpaceSecurityAudit> audit;

  private DocumentSpaceGuard            guard;

  private Space                         space;

  @Before
  public void setUp() throws Exception {
    audit = mockStatic(SpaceSecurityAudit.class);
    guard = spy(new DocumentSpaceGuard(spaceService,
                                       spaceSecurityService,
                                       nodeHierarchyCreator,
                                       linkManager,
                                       trashService,
                                       repositoryService));
    ExtendedNode trashHome = mock(ExtendedNode.class);
    lenient().when(trashHome.getPath()).thenReturn("/Trash");
    lenient().when(trashService.getTrashHomeNode()).thenReturn(trashHome);
    lenient().when(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH)).thenReturn("/Groups");
    space = new Space();
    space.setId(5L);
    space.setGroupId("/spaces/marketing");
    lenient().when(spaceService.getSpaceByGroupId("/spaces/marketing")).thenReturn(space);
  }

  @After
  public void tearDown() {
    audit.close();
  }

  @Test
  public void testSpaceOfADocumentInASpaceDrive() throws Exception {
    assertEquals(space, guard.getDocumentSpace(node(DOCUMENT_PATH)));
  }

  @Test
  public void testSpaceOfAPersonalDocument() throws Exception {
    assertNull(guard.getDocumentSpace(node("/Users/m___/ma___/mar___/mary/Private/a.docx")));
  }

  @Test
  public void testSpaceOfASymlinkIsItsTarget() throws Exception {
    ExtendedNode link = node("/Groups/spaces/other/Documents/Shared/a.docx");
    ExtendedNode target = node(DOCUMENT_PATH);
    when(linkManager.isLink(link)).thenReturn(true);
    when(linkManager.getTarget(link, true)).thenReturn(target);
    assertEquals(space, guard.getDocumentSpace(link));
  }

  @Test
  public void testSpaceOfAVersionIsItsVersionableNode() throws Exception {
    ExtendedNode version = node("/jcr:system/jcr:versionStorage/x/1/jcr:frozenNode");
    when(version.isNodeType("nt:frozenNode")).thenReturn(true);
    Property frozenUuid = mock(Property.class);
    when(frozenUuid.getString()).thenReturn("doc-id");
    when(version.getProperty("jcr:frozenUuid")).thenReturn(frozenUuid);
    ExtendedSession session = mock(ExtendedSession.class);
    when(version.getSession()).thenReturn(session);
    ExtendedNode document = node(DOCUMENT_PATH);
    when(session.getNodeByIdentifier("doc-id")).thenReturn(document);
    assertEquals(space, guard.getDocumentSpace(version));
  }

  @Test
  public void testSpaceOfATrashedDocumentIsItsRestorePath() throws Exception {
    ExtendedNode trashedDocument = nodeInWorkspace("/Trash/folder/a.docx");
    doReturn("/Groups/spaces/marketing/Documents/folder").when(guard).getRestorePath("collaboration", "/Trash/folder");

    assertEquals(space, guard.getDocumentSpace(trashedDocument));
    verify(trashedDocument, never()).getParent();
  }

  @Test
  public void testSpaceOfAQuarantinedDocumentIsItsRestorePath() throws Exception {
    ExtendedNode quarantinedDocument = nodeInWorkspace("/Quarantine/a.docx[2]");
    doReturn("/Groups/spaces/marketing/Documents/a.docx").when(guard).getRestorePath("collaboration", "/Quarantine/a.docx[2]");

    assertEquals(space, guard.getDocumentSpace(quarantinedDocument));
  }

  @Test
  public void testATrashedDocumentWithNoRestorePathBelongsToNoSpace() throws Exception {
    ExtendedNode trashedDocument = nodeInWorkspace("/Trash/a.docx");
    doReturn(null).when(guard).getRestorePath("collaboration", "/Trash/a.docx");

    assertNull(guard.getDocumentSpace(trashedDocument));
  }

  @Test
  public void testAPersonalDocumentBelongsToNoSpaceWithoutReadingAboveIt() throws Exception {
    // the user's session cannot read the folders above the user's own drive
    ExtendedNode personalDocument = node("/Users/j___/jo___/joh___/john/Private/plan.docx");
    lenient().when(personalDocument.getDepth()).thenReturn(7);
    lenient().when(personalDocument.getParent()).thenThrow(new AccessDeniedException("Access denied"));

    assertNull(guard.getDocumentSpace(personalDocument));
    assertTrue(guard.newGrantOperation("john", DocumentGrantOperation.SOURCE_TASK)
                    .canGrant(personalDocument, "*:/spaces/target"));
    verify(personalDocument, never()).getParent();
    verify(guard, never()).getRestorePath(anyString(), anyString());
  }

  @Test
  public void testGrantAllowedWhenLimitAccessManagementIsOff() throws Exception {
    when(spaceSecurityService.getSettings(5L)).thenReturn(new SpaceSecuritySettings(false, false, true));
    assertTrue(guard.newGrantOperation("mary", DocumentGrantOperation.SOURCE_NEWS).canGrant(node(DOCUMENT_PATH), "*:/spaces/target"));
    verify(spaceSecurityService, never()).canManageDocumentsAccess(any(), any(), anyString());
  }

  @Test
  public void testGrantSkippedForARefusedMemberAndSettingsReadOncePerSpace() throws Exception {
    SpaceSecuritySettings settings = new SpaceSecuritySettings(true, false, true);
    when(spaceSecurityService.getSettings(5L)).thenReturn(settings);
    when(spaceSecurityService.canManageDocumentsAccess(space, settings, "mary")).thenReturn(false);
    DocumentGrantOperation operation = guard.newGrantOperation("mary", DocumentGrantOperation.SOURCE_ACTIVITY_SHARE);

    assertFalse(operation.canGrant(node(DOCUMENT_PATH), "*:/spaces/target"));
    assertFalse(operation.canGrant(node("/Groups/spaces/marketing/Documents/b.docx"), "*:/spaces/target"));

    verify(spaceSecurityService, times(1)).getSettings(5L);
    audit.verify(() -> SpaceSecurityAudit.grantSkipped(5L, "id", "*:/spaces/target", "activityShare"), times(2));
  }

  @Test
  public void testGrantAllowedForAPublisher() throws Exception {
    SpaceSecuritySettings settings = new SpaceSecuritySettings(true, false, true);
    when(spaceSecurityService.getSettings(5L)).thenReturn(settings);
    when(spaceSecurityService.canManageDocumentsAccess(space, settings, "pub")).thenReturn(true);
    assertTrue(guard.newGrantOperation("pub", DocumentGrantOperation.SOURCE_TASK).canGrant(node(DOCUMENT_PATH), "mary"));
    audit.verify(() -> SpaceSecurityAudit.grantSkipped(anyLong(), any(), any(), any()), never());
  }

  @Test
  public void testGrantWithNoUserSkippedUnderLimitAccessManagement() throws Exception {
    when(spaceSecurityService.getSettings(5L)).thenReturn(new SpaceSecuritySettings(true, false, true));
    assertFalse(guard.newGrantOperation(null, DocumentGrantOperation.SOURCE_NEWS).canGrant(node(DOCUMENT_PATH), "any"));
    verify(spaceSecurityService, never()).canManageDocumentsAccess(any(), any(), any());
  }

  @Test
  public void testGrantSkippedWhenTheSettingsCannotBeRead() throws Exception {
    when(spaceSecurityService.getSettings(5L)).thenThrow(new ObjectNotFoundException("gone"));
    assertFalse(guard.newGrantOperation("root", DocumentGrantOperation.SOURCE_NEWS).canGrant(node(DOCUMENT_PATH), "any"));
  }

  @Test
  public void testAccessManagementRefusalIsReported() throws Exception {
    SpaceSecuritySettings settings = new SpaceSecuritySettings(true, false, true);
    when(spaceSecurityService.getSettings(5L)).thenReturn(settings);
    when(spaceSecurityService.canManageDocumentsAccess(space, settings, "mary")).thenReturn(false);

    assertFalse(guard.canManageAccess(node(DOCUMENT_PATH), "mary", "legacyWebDAV"));
    audit.verify(() -> SpaceSecurityAudit.actionRefused(5L, "id", "changePermissions", "mary", "legacyWebDAV"));

    assertFalse(guard.canManageAccess(node(DOCUMENT_PATH), "mary", null));
    audit.verify(() -> SpaceSecurityAudit.actionRefused(anyLong(), any(), any(), any(), any()), times(1));
  }

  @Test
  public void testAPersonalPathIsNoSpaceWhateverItsSegments() throws Exception {
    // used only by a resolution that reads the space's name off a personal path
    lenient().when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);

    assertNull(guard.getDocumentSpace(node("/Users/m___/ma___/mar___/mary/Private/Groups/spaces/marketing/a.docx")));
  }

  @Test
  public void testAGrantOnADocumentOfNoSpaceIsWritten() throws Exception {
    assertTrue(guard.newGrantOperation("mary", DocumentGrantOperation.SOURCE_TASK)
                    .canGrant(node("/Users/m___/ma___/mar___/mary/Private/a.docx"), "john"));
    verify(spaceSecurityService, never()).getSettings(anyLong());
  }

  @Test
  public void testAGrantIsSkippedWhenTheDocumentSpaceCannotBeRead() throws Exception {
    ExtendedNode unreadable = node(DOCUMENT_PATH);
    when(unreadable.isNodeType("nt:frozenNode")).thenThrow(new RepositoryException("unreadable"));

    assertFalse(guard.newGrantOperation("root", DocumentGrantOperation.SOURCE_TASK).canGrant(unreadable, "john"));
  }

  @Test
  public void testTheAccessOfADocumentOfNoSpaceIsManaged() throws Exception {
    assertTrue(guard.canManageAccess(node("/Users/m___/ma___/mar___/mary/Private/a.docx"), "mary", "legacyWebDAV"));
    verify(spaceSecurityService, never()).getSettings(anyLong());
  }

  @Test
  public void testAccessManagementIsRefusedWhenTheSettingsCannotBeRead() throws Exception {
    when(spaceSecurityService.getSettings(5L)).thenThrow(new ObjectNotFoundException("gone"));

    assertFalse(guard.canManageAccess(node(DOCUMENT_PATH), "root", "legacyWebDAV"));
  }

  private ExtendedNode nodeInWorkspace(String path) throws Exception {
    ExtendedNode node = node(path);
    ExtendedSession session = mock(ExtendedSession.class);
    Workspace workspace = mock(Workspace.class);
    when(workspace.getName()).thenReturn("collaboration");
    when(session.getWorkspace()).thenReturn(workspace);
    when(node.getSession()).thenReturn(session);
    return node;
  }

  private ExtendedNode node(String path) throws Exception {
    ExtendedNode node = mock(ExtendedNode.class);
    lenient().when(node.getPath()).thenReturn(path);
    lenient().when(node.getIdentifier()).thenReturn("id");
    return node;
  }

}
