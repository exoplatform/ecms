package org.exoplatform.services.attachments.storage;

import org.exoplatform.services.attachments.dao.AttachmentsDAO;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;
import org.exoplatform.services.attachments.model.AttachmentsEntityType;

public class AttachmentsStorageImpl implements AttachmentsStorage {

  AttachmentsDAO attachmentsDAO;

  public AttachmentsStorageImpl(AttachmentsDAO attachmentsDAO) {
    this.attachmentsDAO = attachmentsDAO;
  }

  @Override
  public void linkAttachmentsToEntity(AttachmentsContextEntity attachmentsContextEntity) {
    if (attachmentsContextEntity.getId() == null) {
      attachmentsDAO.create(attachmentsContextEntity);
    } else {
      attachmentsDAO.update(attachmentsContextEntity);
    }
  }

  @Override
  public AttachmentsContextEntity getAttachmentByEntity(long entityId, String entityType) {
    return attachmentsDAO.getAttachmentContextByEntity(entityId, AttachmentsEntityType.valueOf(entityType.toUpperCase()).ordinal());
  }

  @Override
  public void deleteEntityAttachments(AttachmentsContextEntity attachmentsContextEntity) {
    attachmentsDAO.delete(attachmentsContextEntity);
  }
}
