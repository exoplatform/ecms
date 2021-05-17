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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import javax.jcr.Session;
import java.util.*;

public class AttachmentServiceImpl implements AttachmentService {

  private static final Log       LOG = ExoLogger.getLogger(AttachmentServiceImpl.class.getName());

  private RepositoryService      repositoryService;

  private SessionProviderService sessionProviderService;

  AttachmentStorage              attachmentStorage;

  private DocumentService        documentService;

  public AttachmentServiceImpl(AttachmentStorage attachmentStorage,
                               RepositoryService repositoryService,
                               SessionProviderService sessionProviderService,
                               DocumentService documentService) {

    this.attachmentStorage = attachmentStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
  }

  @Override
  public void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentsIds) {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (attachmentsIds == null || attachmentsIds.isEmpty()) {
      throw new IllegalArgumentException("attachmentsIds must not be empty");
    }

    attachmentStorage.linkAttachmentsToEntity(entityId, entityType, attachmentsIds);
  }

  @Override
  public void updateEntityAttachments(long entityId, String entityType, List<String> attachmentIds) {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    List<String> existingAttachmentsIds = attachmentStorage.getAttachmentsIdsByEntity(entityId, entityType);

    // delete removed attachments
    existingAttachmentsIds.stream().filter(attachmentId -> !attachmentIds.contains(attachmentId)).forEach(attachmentId -> {
      try {
        deleteAttachmentItemById(entityId, entityType, attachmentId);
      } catch (ObjectNotFoundException e) {
        e.printStackTrace();
      }
    });

    // attach new added files
    attachmentIds.stream().filter(attachmentId -> !existingAttachmentsIds.contains(attachmentId)).forEach(attachmentId -> {
      linkAttachmentsToEntity(entityId, entityType, Collections.singletonList(attachmentId));
    });
  }

  @Override
  public void deleteAllEntityAttachments(long entityId, String entityType) throws ObjectNotFoundException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    List<String> attachmentsIds = attachmentStorage.getAttachmentsIdsByEntity(entityId, entityType);

    if (attachmentsIds.isEmpty()) {
      throw new ObjectNotFoundException("Entity with id " + entityId + " and type" + entityType + " has no attachment linked");
    }
    attachmentsIds.forEach(attachmentId -> {
      try {
        deleteAttachmentItemById(entityId, entityType, attachmentId);
      } catch (ObjectNotFoundException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void deleteAttachmentItemById(long entityId, String entityType, String attachmentId) throws ObjectNotFoundException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentContextEntity attachmentContextEntity = attachmentStorage.getAttachmentItemByEntity(entityId,
                                                                                                  entityType,
                                                                                                  attachmentId);
    if (attachmentContextEntity == null) {
      throw new ObjectNotFoundException("Attachment with id" + attachmentId + " linked to entity with id " + entityId
          + " and type" + entityType + " not found");
    }
    attachmentStorage.deleteAttachmentItemById(attachmentContextEntity);
  }

  @Override
  public List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id should be positive");
    }
    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    List<String> attachmentsIds = attachmentStorage.getAttachmentsIdsByEntity(entityId, entityType);
    List<Attachment> attachments = new ArrayList<>();
    if (attachmentsIds.size() > 0) {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      String workspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());
      try {
        attachmentsIds.forEach(attachmentId -> {
          try {
            Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                                     documentService,
                                                                     workspace,
                                                                     session,
                                                                     attachmentId);
            attachments.add(attachment);
          } catch (Exception e) {
            LOG.error("Cannot get attachment with id " + attachmentId + " of entity " + entityType + " with id " + entityId, e);
          }
        });
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    }
    return attachments;
  }

}
