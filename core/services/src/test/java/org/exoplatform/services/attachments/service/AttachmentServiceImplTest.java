/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.attachments.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.AccessControlException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import javax.jcr.ItemNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * The attachment writes that start from the list of an entity's attachments,
 * as the storage builds it for the current user: an attachment the user can't
 * read is in that list with {@code acl.canAccess == false}, and it was never
 * shown to them, so a save of what they were shown must not detach it. And the
 * move of an attachment to another folder, which gives it that folder's
 * visibility.
 */
public class AttachmentServiceImplTest {

  private static final long       USER_IDENTITY_ID  = 5;

  private static final long       ENTITY_ID         = 1;

  private static final String     ENTITY_TYPE       = "task";

  private static final String     ATTACHMENT_ID     = "attachment-uuid";

  private static final String     ATTACHMENT_NAME   = "docx3.docx";

  private static final String     ATTACHMENT_PATH   = "/Groups/spaces/space/Documents/Activity Stream Documents/"
      + ATTACHMENT_NAME;

  private static final String     DESTINATION_PATH  = "/Groups/spaces/space/Documents/folder1";

  private final AttachmentStorage attachmentStorage = mock(AttachmentStorage.class);

  private static final String     DOCUMENT_ID       = "doc1";

  private static final String     DOCUMENT_PATH     = "/Users/doc1";

  private final ExtendedSession   session           = mock(ExtendedSession.class);

  private final AttachmentACLPlugin aclPlugin       = mock(AttachmentACLPlugin.class);

  private AttachmentServiceImpl   attachmentService;

  @Before
  public void setUp() throws Exception {
    SessionProviderService sessionProviderService = mock(SessionProviderService.class);
    RepositoryService repositoryService = mock(RepositoryService.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    Node documentNode = mock(Node.class);
    when(sessionProviderService.getSessionProvider(null)).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession("collaboration", repository)).thenReturn(session);
    when(session.getNodeByIdentifier(DOCUMENT_ID)).thenReturn(documentNode);
    when(documentNode.getPath()).thenReturn(DOCUMENT_PATH);
    when(aclPlugin.getEntityType()).thenReturn(ENTITY_TYPE);
    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getIdentity(String.valueOf(USER_IDENTITY_ID))).thenReturn(mock(Identity.class));
    when(attachmentStorage.getAttachmentItemByEntity(anyLong(), anyString(), anyString())).thenReturn(new Attachment());

    attachmentService = spy(new AttachmentServiceImpl(attachmentStorage,
                                                      repositoryService,
                                                      sessionProviderService,
                                                      mock(DocumentService.class),
                                                      identityManager,
                                                      mock(ManageDriveService.class),
                                                      mock(NodeHierarchyCreator.class),
                                                      mock(NodeFinder.class),
                                                      mock(LinkManager.class)));
    // The user may detach every attachment of the entity: the JCR and ACL-plugin
    // checks are not under test, the service's decision on what it detaches is
    doReturn(true).when(attachmentService).canDetach(anyLong(), anyString(), anyLong(), anyString());
  }

  /**
   * Mutant: without the hidden-attachment guard the unsent hidden id is
   * detached like the unsent visible one.
   */
  @Test
  public void updateKeepsTheAttachmentsHiddenToTheUser() throws Exception {
    when(attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE)).thenReturn(List.of(visible("kept"),
                                                                                             visible("removed"),
                                                                                             hidden("hidden")));

    attachmentService.updateEntityAttachments(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE, List.of("kept"));

    verify(attachmentStorage).deleteAttachmentItemByIdByEntity(ENTITY_ID, ENTITY_TYPE, "removed");
    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(ENTITY_ID, ENTITY_TYPE, "kept");
    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(ENTITY_ID, ENTITY_TYPE, "hidden");
    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
  }

  /**
   * An empty list detaches everything the user was shown, and nothing else.
   */
  @Test
  public void deleteAllKeepsTheAttachmentsHiddenToTheUser() throws Exception {
    when(attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE)).thenReturn(List.of(visible("removed"),
                                                                                             hidden("hidden")));

    attachmentService.deleteAllEntityAttachments(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE);

    verify(attachmentStorage).deleteAttachmentItemByIdByEntity(ENTITY_ID, ENTITY_TYPE, "removed");
    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(ENTITY_ID, ENTITY_TYPE, "hidden");
  }

  /**
   * An entity whose attachments are all hidden to the user has attachments:
   * nothing is detached and nothing is reported missing.
   */
  @Test
  public void deleteAllWithOnlyHiddenAttachmentsDetachesNothing() throws Exception {
    when(attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE)).thenReturn(List.of(hidden("hidden")));

    attachmentService.deleteAllEntityAttachments(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE);

    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(anyLong(), anyString(), anyString());
  }

  /**
   * A file uploaded to the hidden activity folder is hidden; moved to a visible
   * folder, it becomes visible before the session is saved. Mutant: without the
   * visibility alignment the hidden mixin is never removed.
   */
  @Test
  public void moveToVisibleFolderMakesHiddenAttachmentVisible() throws Exception {
    Node attachmentNode = attachmentNode(true);
    Session session = moveAttachment(attachmentNode, folder(false));

    InOrder inOrder = inOrder(session, attachmentNode);
    inOrder.verify(session).move(ATTACHMENT_PATH, DESTINATION_PATH + "/" + ATTACHMENT_NAME);
    inOrder.verify(attachmentNode).removeMixin(NodetypeConstant.EXO_HIDDENABLE);
    inOrder.verify(session).save();
    verify(attachmentNode, never()).addMixin(anyString());
  }

  /**
   * The other branch: a visible file moved to a hidden folder becomes hidden.
   */
  @Test
  public void moveToHiddenFolderHidesVisibleAttachment() throws Exception {
    Node attachmentNode = attachmentNode(false);
    when(attachmentNode.canAddMixin(NodetypeConstant.EXO_HIDDENABLE)).thenReturn(true);
    Session session = moveAttachment(attachmentNode, folder(true));

    InOrder inOrder = inOrder(attachmentNode, session);
    inOrder.verify(attachmentNode).addMixin(NodetypeConstant.EXO_HIDDENABLE);
    inOrder.verify(session).save();
    verify(attachmentNode, never()).removeMixin(anyString());
  }

  /**
   * A file that already has its destination's visibility keeps its mixins.
   */
  @Test
  public void moveBetweenVisibleFoldersKeepsTheMixins() throws Exception {
    Node attachmentNode = attachmentNode(false);
    Session session = moveAttachment(attachmentNode, folder(false));

    verify(session).save();
    verify(attachmentNode, never()).addMixin(anyString());
    verify(attachmentNode, never()).removeMixin(anyString());
  }

  /**
   * A hidden file moved to a hidden folder gets no second mixin, even where the
   * node would accept one. Mutant: without the {@code !nodeHidden} guard, the
   * mixin is added again.
   */
  @Test
  public void moveBetweenHiddenFoldersKeepsTheMixins() throws Exception {
    Node attachmentNode = attachmentNode(true);
    when(attachmentNode.canAddMixin(NodetypeConstant.EXO_HIDDENABLE)).thenReturn(true);
    Session session = moveAttachment(attachmentNode, folder(true));

    verify(session).save();
    verify(attachmentNode, never()).addMixin(anyString());
    verify(attachmentNode, never()).removeMixin(anyString());
  }

  /**
   * A visible file moved to a hidden folder whose node refuses the mixin is
   * left as it is. Mutant: without the {@code canAddMixin} guard the refused
   * mixin is added.
   */
  @Test
  public void moveToHiddenFolderSkipsAMixinTheNodeRefuses() throws Exception {
    Node attachmentNode = attachmentNode(false);
    when(attachmentNode.canAddMixin(NodetypeConstant.EXO_HIDDENABLE)).thenReturn(false);
    Session session = moveAttachment(attachmentNode, folder(true));

    verify(session).save();
    verify(attachmentNode, never()).addMixin(anyString());
  }

  private Session moveAttachment(Node attachmentNode, Node destinationFolder) throws Exception {
    doReturn(true).when(attachmentService).canEdit(anyLong(), anyString(), anyLong(), anyString());
    Session session = mock(Session.class);
    when(session.getNodeByUUID(ATTACHMENT_ID)).thenReturn(attachmentNode);
    try (MockedStatic<Utils> utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
        MockedStatic<EntityBuilder> entityBuilder = mockStatic(EntityBuilder.class)) {
      utils.when(() -> Utils.getSession(any(), any())).thenReturn(session);
      utils.when(() -> Utils.getCurrentWorkspace(any())).thenReturn("collaboration");
      utils.when(() -> Utils.getParentFolderNode(any(), any(), any(), any(), any(), any())).thenReturn(destinationFolder);
      entityBuilder.when(() -> EntityBuilder.fromAttachmentNode(any(), any(), any(), any(), any(), any()))
                   .thenReturn(new Attachment());

      attachmentService.moveAttachmentToNewPath(USER_IDENTITY_ID, ATTACHMENT_ID, ".spaces.space", "folder1", "activity", 0);
    }
    return session;
  }

  private static Node attachmentNode(boolean hidden) throws Exception {
    Node node = mock(Node.class);
    when(node.getPath()).thenReturn(ATTACHMENT_PATH);
    when(node.getName()).thenReturn(ATTACHMENT_NAME);
    when(node.isNodeType(NodetypeConstant.EXO_HIDDENABLE)).thenReturn(hidden);
    return node;
  }

  private static Node folder(boolean hidden) throws Exception {
    Node folder = mock(Node.class);
    when(folder.getPath()).thenReturn(DESTINATION_PATH);
    when(folder.isNodeType(NodetypeConstant.EXO_HIDDENABLE)).thenReturn(hidden);
    return folder;
  }

  /**
   * Mutant: without the entity edit check the link is stored for a user who
   * can't edit the entity.
   */
  @Test
  public void linkIsRefusedWithoutTheRightToEditTheEntity() {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(false);

    assertThrows(IllegalAccessException.class,
                 () -> attachmentService.linkAttachmentToEntity(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE, DOCUMENT_ID));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
  }

  /**
   * Mutant: without the document read check the link is stored for a user who
   * can't read the document.
   */
  @Test
  public void linkIsRefusedWithoutTheRightToReadTheDocument() throws Exception {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(true);
    doThrow(new AccessControlException("read")).when(session).checkPermission(DOCUMENT_PATH, PermissionType.READ);

    assertThrows(ObjectNotFoundException.class,
                 () -> attachmentService.linkAttachmentToEntity(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE, DOCUMENT_ID));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
  }

  @Test
  public void linkIsStoredWithTheRightsToEditTheEntityAndReadTheDocument() throws Exception {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(true);
    Attachment linked = new Attachment();
    doReturn(linked).when(attachmentService).getAttachmentByIdByEntity(ENTITY_TYPE, ENTITY_ID, DOCUMENT_ID, USER_IDENTITY_ID);

    Attachment result = attachmentService.linkAttachmentToEntity(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE, DOCUMENT_ID);

    assertSame(linked, result);
    verify(session).checkPermission(eq(DOCUMENT_PATH), eq(PermissionType.READ));
    verify(attachmentStorage, times(1)).linkAttachmentToEntity(ENTITY_ID, ENTITY_TYPE, DOCUMENT_ID);
  }

  /**
   * Mutant: without the checks before the removals, the unsent link is removed
   * before the refused id is met.
   */
  @Test
  public void updateChangesNothingWhenOneNewAttachmentIsRefused() throws Exception {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(true);
    Node refusedNode = mock(Node.class);
    when(refusedNode.getPath()).thenReturn("/Users/refused");
    when(session.getNodeByIdentifier("refused")).thenReturn(refusedNode);
    doThrow(new AccessControlException("read")).when(session).checkPermission("/Users/refused", PermissionType.READ);
    when(attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE)).thenReturn(List.of(visible("removed")));

    assertThrows(ObjectNotFoundException.class,
                 () -> attachmentService.updateEntityAttachments(USER_IDENTITY_ID,
                                                                 ENTITY_ID,
                                                                 ENTITY_TYPE,
                                                                 List.of(DOCUMENT_ID, "refused")));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(anyLong(), anyString(), anyString());
  }

  @Test
  public void linkIsRefusedWhenTheDocumentDoesNotExist() throws Exception {
    when(session.getNodeByIdentifier("missing")).thenThrow(new ItemNotFoundException("missing"));

    assertThrows(ObjectNotFoundException.class,
                 () -> attachmentService.linkAttachmentToEntity(USER_IDENTITY_ID, ENTITY_ID, ENTITY_TYPE, "missing"));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
  }

  /**
   * Mutant: with an exact lookup of the type, the rule registered under the
   * lower-case type is not found for the upper-case one.
   */
  @Test
  public void linkIsRefusedWithoutTheRightToEditTheEntityWhateverTheCaseOfItsType() {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(false);

    assertThrows(IllegalAccessException.class,
                 () -> attachmentService.linkAttachmentToEntity(USER_IDENTITY_ID,
                                                                ENTITY_ID,
                                                                ENTITY_TYPE.toUpperCase(),
                                                                DOCUMENT_ID));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
  }

  @Test
  public void updateIsRefusedWithoutTheRightToEditTheEntityWhateverTheCaseOfItsType() throws Exception {
    attachmentService.addACLPlugin(aclPlugin);
    when(aclPlugin.canEdit(USER_IDENTITY_ID, ENTITY_TYPE, String.valueOf(ENTITY_ID))).thenReturn(false);
    when(attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE.toUpperCase())).thenReturn(List.of(visible("removed")));

    assertThrows(IllegalAccessException.class,
                 () -> attachmentService.updateEntityAttachments(USER_IDENTITY_ID,
                                                                 ENTITY_ID,
                                                                 ENTITY_TYPE.toUpperCase(),
                                                                 List.of(DOCUMENT_ID)));

    verify(attachmentStorage, never()).linkAttachmentToEntity(anyLong(), anyString(), anyString());
    verify(attachmentStorage, never()).deleteAttachmentItemByIdByEntity(anyLong(), anyString(), anyString());
  }

  private static Attachment visible(String attachmentId) {
    return attachment(attachmentId, true);
  }

  private static Attachment hidden(String attachmentId) {
    return attachment(attachmentId, false);
  }

  private static Attachment attachment(String attachmentId, boolean canAccess) {
    Attachment attachment = new Attachment();
    attachment.setId(attachmentId);
    Permission acl = new Permission();
    acl.setCanAccess(canAccess);
    attachment.setAcl(acl);
    return attachment;
  }
}
