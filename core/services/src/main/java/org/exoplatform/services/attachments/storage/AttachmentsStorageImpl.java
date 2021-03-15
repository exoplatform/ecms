package org.exoplatform.services.attachments.storage;

import org.exoplatform.services.attachments.dao.AttachmentsDAO;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;

public class AttachmentsStorageImpl implements AttachmentsStorage {

  AttachmentsDAO attachmentsDAO;

  public AttachmentsStorageImpl(AttachmentsDAO attachmentsDAO) {
    this.attachmentsDAO = attachmentsDAO;
  }
  @Override
  public void linkAttachmentsToContext(AttachmentsContextEntity attachmentsContextEntity) {
    if (attachmentsContextEntity.getId() == null) {
      attachmentsDAO.create(attachmentsContextEntity);
    } else {
      attachmentsDAO.update(attachmentsContextEntity);
    }
  }

  @Override
  public AttachmentsContextEntity getAttachmentContextByEntity(long entityId, String entityType) {
    return attachmentsDAO.getAttachmentContextByEntity(entityId, entityType);
  }

}
