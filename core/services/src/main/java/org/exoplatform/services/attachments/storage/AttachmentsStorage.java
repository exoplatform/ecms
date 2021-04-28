package org.exoplatform.services.attachments.storage;

import org.exoplatform.services.attachments.model.AttachmentsContextEntity;

public interface AttachmentsStorage {
  void linkAttachmentsToEntity(AttachmentsContextEntity attachmentsContextEntity);

  AttachmentsContextEntity getAttachmentByEntity(long entityId, String entityType);

  void deleteEntityAttachments(AttachmentsContextEntity attachmentsContextEntity);

}
