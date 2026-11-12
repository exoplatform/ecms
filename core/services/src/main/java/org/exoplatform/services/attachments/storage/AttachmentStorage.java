/*
 * Copyright (C) 2021 eXo Platform SAS.
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
package org.exoplatform.services.attachments.storage;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;

import javax.jcr.Session;
import java.util.List;

public interface AttachmentStorage {
  void linkAttachmentToEntity(long entityId, String entityType, String attachmentsId);

  /**
   * @param entityId entity id
   * @param entityType entity type
   * @return the attachments linked to the entity, as the current user sees
   *         them: one the user can't read carries only its identifier; a
   *         missing, trashed or quarantined node, one the user's session no
   *         longer finds, or one whose shortcut target or content is gone, is
   *         left out; a shortcut listed with its original, or two shortcuts to
   *         the same target, are listed once
   * @throws Exception when any other error prevents building an attachment
   */
  List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception;

  Attachment getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) throws Exception;

  void deleteAllEntityAttachments(AttachmentContextEntity attachmentContextEntity);

  void deleteAttachmentItemByIdByEntity(long entityId, String entityType, String attachmentId);

}
