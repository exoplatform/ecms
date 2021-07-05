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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class AttachmentStorageImpl implements AttachmentStorage {

  AttachmentDAO attachmentDAO;

  public AttachmentStorageImpl(AttachmentDAO attachmentDAO) {
    this.attachmentDAO = attachmentDAO;
  }

  @Override
  public void linkAttachmentToEntity(long entityId, String entityType, String attachmentId) {
      AttachmentContextEntity attachmentContextEntity = new AttachmentContextEntity();
      attachmentContextEntity.setEntityId(entityId);
      attachmentContextEntity.setEntityType(entityType.toUpperCase());
      attachmentContextEntity.setAttachmentId(attachmentId);
      attachmentContextEntity.setAttachedDate(System.currentTimeMillis());
      attachmentDAO.create(attachmentContextEntity);
  }

  @Override
  public List<AttachmentContextEntity> getAttachmentContextByEntity(long entityId, String entityType) {
    return attachmentDAO.getAttachmentContextByEntity(entityId, entityType.toUpperCase());
  }

  @Override
  public AttachmentContextEntity getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) {
    return attachmentDAO.getAttachmentItemByEntity(entityId, entityType.toUpperCase(), attachmentId);
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
