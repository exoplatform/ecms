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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * The attachment writes that start from the list of an entity's attachments,
 * as the storage builds it for the current user: an attachment the user can't
 * read is in that list with {@code acl.canAccess == false}, and it was never
 * shown to them, so a save of what they were shown must not detach it.
 */
public class AttachmentServiceImplTest {

  private static final long       USER_IDENTITY_ID  = 5;

  private static final long       ENTITY_ID         = 1;

  private static final String     ENTITY_TYPE       = "task";

  private final AttachmentStorage attachmentStorage = mock(AttachmentStorage.class);

  private AttachmentServiceImpl   attachmentService;

  @Before
  public void setUp() throws Exception {
    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getIdentity(String.valueOf(USER_IDENTITY_ID))).thenReturn(mock(Identity.class));
    when(attachmentStorage.getAttachmentItemByEntity(anyLong(), anyString(), anyString())).thenReturn(new Attachment());

    attachmentService = spy(new AttachmentServiceImpl(attachmentStorage,
                                                      mock(RepositoryService.class),
                                                      mock(SessionProviderService.class),
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
