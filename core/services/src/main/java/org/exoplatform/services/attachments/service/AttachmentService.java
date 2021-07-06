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
import org.exoplatform.services.cms.clouddrives.NotFoundException;

import javax.jcr.ItemNotFoundException;
import java.util.List;

public interface AttachmentService {

  List<Attachment> getAttachmentsByEntity(long userIdentityId, long entityId, String entityType) throws Exception;

  Attachment getAttachmentById(String entityType, long entityId, String attachmentId, long userIdentityId) throws Exception;

  Attachment linkAttachmentToEntity(long userIdentityId, long entityId, String entityType, String attachmentId) throws Exception;

  void updateEntityAttachments(long userIdentityId,
                               long entityId,
                               String entityType,
                               List<String> attachmentIds) throws ObjectNotFoundException,
                                                           IllegalAccessException,
                                                           ItemNotFoundException,
                                                           NotFoundException;

  void deleteAllEntityAttachments(long userIdentityId, long entityId, String entityType) throws ObjectNotFoundException,
                                                                                         IllegalAccessException,
                                                                                         ItemNotFoundException,
                                                                                         NotFoundException;

  void deleteAttachmentItemById(long userIdentityId,
                                long entityId,
                                String entityType,
                                String attachmentId) throws ObjectNotFoundException, IllegalAccessException, NotFoundException;

  void moveAttachmentToNewPath(long userIdentityId,
                               String attachmentId,
                               String newPathDrive,
                               String newPath,
                               String entityType,
                               long entityId) throws Exception;
}
