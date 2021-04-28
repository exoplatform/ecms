package org.exoplatform.services.attachments.service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Attachment;

import java.util.List;

public interface AttachmentsService {

  List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception;

  void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentIds);

  void updateEntityAttachments(long entityId, String entityType, List<String> attachmentIds);

  void deleteEntityAttachments(long entityId, String entityType) throws ObjectNotFoundException;

  void deleteEntityAttachment(long entityId, String entityType, String attachmentId) throws ObjectNotFoundException;
}
