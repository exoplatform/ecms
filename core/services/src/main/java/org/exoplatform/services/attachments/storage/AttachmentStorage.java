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

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.cms.clouddrives.NotFoundException;

import javax.jcr.RepositoryException;
import java.util.List;

public interface AttachmentStorage {
  void linkAttachmentToEntity(long entityId, String entityType, String attachmentsId);

  List<Attachment> getAttachmentsByEntity(long userIdentityId, long entityId, String entityType) throws RepositoryException,
                                                                                                 NotFoundException;

  Attachment getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) throws RepositoryException,
                                                                                              NotFoundException;

  void deleteAllEntityAttachments(AttachmentContextEntity attachmentContextEntity);

  void deleteAttachmentItemByIdByEntity(long entityId, String entityType, String attachmentId);

}
