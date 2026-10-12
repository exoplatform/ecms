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
package org.exoplatform.services.attachments.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.AccessDeniedException;

import org.junit.Test;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

public class EntityBuilderTest {

  private static final String   WORKSPACE         = "collaboration";

  private final RepositoryService repositoryService = mock(RepositoryService.class);

  private final DocumentService   documentService   = mock(DocumentService.class);

  private final LinkManager       linkManager       = mock(LinkManager.class);

  private final ExtendedSession   session           = mock(ExtendedSession.class);

  /**
   * A symlink the user can read, one in a folder whose ACL grants them read,
   * whose target they can't read: the attachment is private to them, and the
   * rest of the entity's attachments can still be listed. Mutant: without the
   * guard the {@link AccessDeniedException} escapes and the whole list fails.
   */
  @Test
  public void symlinkWithUnreadableTargetIsAPrivateAttachment() throws Exception {
    NodeImpl linkNode = mock(NodeImpl.class);
    when(session.getNodeByIdentifier("link-id")).thenReturn(linkNode);
    when(linkManager.isLink(linkNode)).thenReturn(true);
    when(linkManager.getTarget(linkNode)).thenThrow(new AccessDeniedException("Access denied for requester"));

    Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                             documentService,
                                                             linkManager,
                                                             WORKSPACE,
                                                             session,
                                                             "link-id");

    assertPrivate(attachment, "link-id");
  }

  /**
   * A node the user can't read at all, the case the builder already handled
   */
  @Test
  public void unreadableNodeIsAPrivateAttachment() throws Exception {
    when(session.getNodeByIdentifier("file-id")).thenThrow(new AccessDeniedException("Access denied for requester"));

    Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                             documentService,
                                                             linkManager,
                                                             WORKSPACE,
                                                             session,
                                                             "file-id");

    assertPrivate(attachment, "file-id");
  }

  private static void assertPrivate(Attachment attachment, String attachmentId) {
    assertNotNull(attachment);
    assertEquals(attachmentId, attachment.getId());
    assertNull(attachment.getTitle());
    assertNull(attachment.getPath());
    assertNotNull(attachment.getAcl());
    assertFalse(attachment.getAcl().isCanAccess());
  }
}
