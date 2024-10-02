/*
 * Copyright (C) 2024 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.exoplatform.services.attachments.listener;

import java.util.List;
import java.util.Map;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class LinkNoteAttachmentsOnSaveListener extends Listener<String, Map<String, Object>> {

  private final AttachmentStorage attachmentService;

  private final String            WIKI_DRAFT_PAGES   = "WIKI_DRAFT_PAGES";


  private final String            WIKI_PAGE_VERSIONS = "WIKI_PAGE_VERSIONS";

  public LinkNoteAttachmentsOnSaveListener(AttachmentStorage attachmentService) {
    this.attachmentService = attachmentService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    if (event.getData() != null) {
      Map<String, Object> data = (Map<String, Object>) event.getData();
      String draftPageId = (String) data.get("draftPageId");
      String pageVersionId = (String) data.get("pageVersionId");
      String draftForExistingPageId = (String) data.get("draftForExistingPageId");


      if (draftPageId != null && pageVersionId != null) {
        moveAttachments(draftPageId, pageVersionId, WIKI_DRAFT_PAGES, WIKI_PAGE_VERSIONS);
      }

      if (draftForExistingPageId != null && pageVersionId != null) {
        copyAttachments(pageVersionId, draftForExistingPageId, WIKI_PAGE_VERSIONS, WIKI_DRAFT_PAGES);
      }
    }

  }

  private void moveAttachments(String sourceId,
                               String targetId,
                               String sourceEntityType,
                               String targetEntityType) throws Exception {
    List<Attachment> attachmentList = attachmentService.getAttachmentsByEntity(Long.parseLong(sourceId), sourceEntityType);

    if (attachmentList != null && !attachmentList.isEmpty()) {
      for (Attachment attachment : attachmentList) {
        attachmentService.linkAttachmentToEntity(Long.parseLong(targetId), targetEntityType, attachment.getId());
        attachmentService.deleteAttachmentItemByIdByEntity(Long.parseLong(sourceId), sourceEntityType, attachment.getId());
      }
    }
  }

  private void copyAttachments(String sourceId,
                               String targetId,
                               String sourceEntityType,
                               String targetEntityType) throws Exception {
    List<Attachment> attachmentList = attachmentService.getAttachmentsByEntity(Long.parseLong(sourceId), sourceEntityType);

    if (attachmentList != null && !attachmentList.isEmpty()) {
      for (Attachment attachment : attachmentList) {
        attachmentService.linkAttachmentToEntity(Long.parseLong(targetId), targetEntityType, attachment.getId());
      }
    }
  }
}
