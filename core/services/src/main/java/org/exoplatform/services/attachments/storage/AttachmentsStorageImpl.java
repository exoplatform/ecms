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

import org.exoplatform.services.attachments.dao.AttachmentsDAO;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;
import org.exoplatform.services.attachments.model.AttachmentsEntityType;

import java.util.List;

public class AttachmentsStorageImpl implements AttachmentsStorage {

  AttachmentsDAO attachmentsDAO;

  public AttachmentsStorageImpl(AttachmentsDAO attachmentsDAO) {
    this.attachmentsDAO = attachmentsDAO;
  }

  @Override
  public void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentsIds) {
    attachmentsIds.forEach(attachmentId -> {
      AttachmentsContextEntity attachmentsContextEntity = new AttachmentsContextEntity();
      attachmentsContextEntity.setEntityId(entityId);
      attachmentsContextEntity.setEntityType(AttachmentsEntityType.valueOf(entityType.toUpperCase()).ordinal());
      attachmentsContextEntity.setAttachmentId(attachmentId);
      attachmentsDAO.create(attachmentsContextEntity);
    });

  }

  @Override
  public List<String> getAttachmentsIdsByEntity(long entityId, String entityType) {
    return attachmentsDAO.getAttachmentsIdsByEntity(entityId, AttachmentsEntityType.valueOf(entityType.toUpperCase()).ordinal());
  }

  @Override
  public AttachmentsContextEntity getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) {
    return attachmentsDAO.getAttachmentItemByEntity(entityId,
                                                    AttachmentsEntityType.valueOf(entityType.toUpperCase()).ordinal(),
                                                    attachmentId);
  }

  @Override
  public void deleteAllEntityAttachments(AttachmentsContextEntity attachmentsContextEntity) {
    attachmentsDAO.delete(attachmentsContextEntity);
  }

  @Override
  public void deleteAttachmentItemById(AttachmentsContextEntity attachmentsContextEntity) {
    attachmentsDAO.delete(attachmentsContextEntity);
  }
}
