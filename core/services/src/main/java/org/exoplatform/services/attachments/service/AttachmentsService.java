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
package org.exoplatform.services.attachments.service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Attachment;

import java.util.List;

public interface AttachmentsService {

  List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception;

  void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentIds);

  void updateEntityAttachments(long entityId, String entityType, List<String> attachmentIds);

  void deleteAllEntityAttachments(long entityId, String entityType) throws ObjectNotFoundException;

  void deleteAttachmentItemById(long entityId, String entityType, String attachmentId) throws ObjectNotFoundException;
}
