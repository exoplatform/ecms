package org.exoplatform.services.attachments.service;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.rest.model.AttachmentsContext;
import java.util.List;

public interface AttachmentsService {

  void linkAttachmentsToContext(AttachmentsContext attachmentsContext);

  List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception;
}
