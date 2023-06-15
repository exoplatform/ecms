/*
 * Copyright (C) 2023 eXo Platform SAS
 *
 *  This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <gnu.org/licenses>.
 */
package org.exoplatform.services.attachments.listener;

import org.exoplatform.services.attachments.service.AttachmentService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class AttachmentViewListener extends Listener<String, String> {

  private final AttachmentService attachmentService;

  public AttachmentViewListener(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    String attachmentId = (String) event.getData();
    String viewer = (String) event.getSource();
    if (attachmentId != null && viewer != null) {
      attachmentService.markAttachmentAsViewed(attachmentId, viewer);
    }
  }
}
