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

import org.exoplatform.services.attachments.dao.AttachmentDAO;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.attachments.model.AttachmentEntityType;

import java.util.List;

public class AttachmentStorageImpl implements AttachmentStorage {

  AttachmentDAO attachmentDAO;

  public AttachmentStorageImpl(AttachmentDAO attachmentDAO) {
    this.attachmentDAO = attachmentDAO;
  }

  @Override
  public void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentsIds) {
    attachmentsIds.forEach(attachmentId -> {
      AttachmentContextEntity attachmentContextEntity = new AttachmentContextEntity();
      attachmentContextEntity.setEntityId(entityId);
      attachmentContextEntity.setEntityType(AttachmentEntityType.valueOf(entityType.toUpperCase()).ordinal());
      attachmentContextEntity.setAttachmentId(attachmentId);
      attachmentDAO.create(attachmentContextEntity);
    });

  }

  @Override
  public List<String> getAttachmentsIdsByEntity(long entityId, String entityType) {
    return attachmentDAO.getAttachmentsIdsByEntity(entityId, AttachmentEntityType.valueOf(entityType.toUpperCase()).ordinal());
  }

  @Override
  public AttachmentContextEntity getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) {
    return attachmentDAO.getAttachmentItemByEntity(entityId,
                                                    AttachmentEntityType.valueOf(entityType.toUpperCase()).ordinal(),
                                                    attachmentId);
  }

  @Override
  public void deleteAllEntityAttachments(AttachmentContextEntity attachmentContextEntity) {
    attachmentDAO.delete(attachmentContextEntity);
  }

  @Override
  public void deleteAttachmentItemById(AttachmentContextEntity attachmentContextEntity) {
    attachmentDAO.delete(attachmentContextEntity);
  }
}
