package org.exoplatform.services.attachments.storage;

import org.exoplatform.services.attachments.model.AttachmentsContextEntity;

public interface AttachmentsStorage {
  void linkAttachmentsToContext(AttachmentsContextEntity attachmentsContextEntity);

  AttachmentsContextEntity getAttachmentContextByEntity(long entityId, String entityType);
}
