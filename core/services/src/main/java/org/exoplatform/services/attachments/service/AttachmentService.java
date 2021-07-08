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

public interface AttachmentService {

  /**
   * Retrieve the list of attachments linked to the given entity.
   *
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param entityId entity id
   * @param entityType entity type
   * @return {@link List} of {@link Attachment} accessible to user
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  List<Attachment> getAttachmentsByEntity(long userIdentityId, long entityId, String entityType) throws IllegalAccessException;

  /**
   * Retrieve an attachment with its jcr uuid linked to the given entity.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @param attachmentId attachment jcr uuid
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @return the {@link Attachment} linked to the given entity
   * @throws IllegalStateException when user is not allowed to access the entity
   */
  Attachment getAttachmentByIdByEntity(String entityType,
                                       long entityId,
                                       String attachmentId,
                                       long userIdentityId) throws IllegalStateException;

  /**
   * Get an attachment with its jcr uuid
   * 
   * @param attachmentId attachment jcr uuid
   * @return {@link Attachment}
   */
  Attachment getAttachmentById(String attachmentId);

  /**
   * link an attachment with its jcr uuid to a given entity.
   *
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param entityId entity id
   * @param entityType entity type
   * @param attachmentId attachment jcr uuid to be linked
   * @return the linked {@link Attachment}
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  Attachment linkAttachmentToEntity(long userIdentityId,
                                    long entityId,
                                    String entityType,
                                    String attachmentId) throws IllegalAccessException;

  /**
   * update an existing list of attachment ids linked to a given entity.
   * 
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param entityId entity id
   * @param entityType entity type
   * @param attachmentIds list of {@link Attachment} jcr uuid.
   * @throws ObjectNotFoundException when the attachment identified by its jcr
   *           uuid is not found
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  void updateEntityAttachments(long userIdentityId,
                               long entityId,
                               String entityType,
                               List<String> attachmentIds) throws ObjectNotFoundException, IllegalAccessException;

  /**
   * Delete all attachments linked to a given entity.
   * 
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param entityId entity id
   * @param entityType entity type
   * @throws ObjectNotFoundException when the attachment identified by its jcr
   *           uuid is not found
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  void deleteAllEntityAttachments(long userIdentityId, long entityId, String entityType) throws ObjectNotFoundException,
                                                                                         IllegalAccessException;

  /**
   * Delete an attachment with its jcr uuid linked to a given entity.
   * 
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param entityId entity id
   * @param entityType entity type
   * @param attachmentId attachment jcr uuid
   * @throws ObjectNotFoundException when the attachment identified by its jcr
   *           uuid is not found
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  void deleteAttachmentItemById(long userIdentityId,
                                long entityId,
                                String entityType,
                                String attachmentId) throws ObjectNotFoundException, IllegalAccessException;

  /**
   * Move an attachment to a new destination path.
   * 
   * @param userIdentityId user
   *          {@link org.exoplatform.social.core.identity.model.Identity}
   *          identifier
   * @param attachmentId the {@link Attachment} jcr uuid to be moved
   * @param newPathDrive the new drive of the new destination path
   * @param newPath the new destination path
   * @param entityType entity type
   * @param entityId entity id
   * @throws IllegalAccessException when user is not allowed to access the entity
   */
  void moveAttachmentToNewPath(long userIdentityId,
                               String attachmentId,
                               String newPathDrive,
                               String newPath,
                               String entityType,
                               long entityId) throws IllegalAccessException;
}
