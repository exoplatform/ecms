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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import javax.jcr.Session;
import java.util.*;
import java.util.stream.Collectors;

public class AttachmentServiceImpl implements AttachmentService {

  private static final Log                 LOG        = ExoLogger.getLogger(AttachmentServiceImpl.class.getName());

  private RepositoryService                repositoryService;

  private SessionProviderService           sessionProviderService;

  private AttachmentStorage                attachmentStorage;

  private DocumentService                  documentService;

  private IdentityManager                  identityManager;

  private Map<String, AttachmentACLPlugin> aclPlugins = new HashMap<>();

  public AttachmentServiceImpl(AttachmentStorage attachmentStorage,
                               RepositoryService repositoryService,
                               SessionProviderService sessionProviderService,
                               DocumentService documentService,
                               IdentityManager identityManager) {

    this.attachmentStorage = attachmentStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
    this.identityManager = identityManager;
  }

  @Override
  public void linkAttachmentsToEntity(long userIdentityId,
                                      long entityId,
                                      String entityType,
                                      List<String> attachmentsIds) throws IllegalAccessException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (attachmentsIds == null || attachmentsIds.isEmpty()) {
      throw new IllegalArgumentException("attachmentsIds must not be empty");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    Identity userIdentity = identityManager.getIdentity(String.valueOf(userIdentityId));
    if (userIdentity == null) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
    }
    attachmentStorage.linkAttachmentsToEntity(entityId, entityType, attachmentsIds);
  }

  @Override
  public void updateEntityAttachments(long userIdentityId,
                                      long entityId,
                                      String entityType,
                                      List<String> attachmentIds) throws ObjectNotFoundException, IllegalAccessException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    List<AttachmentContextEntity> existingAttachmentsContext =
                                                             attachmentStorage.getAttachmentContextByEntity(entityId, entityType);
    List<String> existingAttachmentsIds = existingAttachmentsContext.stream()
                                                                    .map(AttachmentContextEntity::getAttachmentId)
                                                                    .collect(Collectors.toList());
    // delete removed attachments
    if (attachmentIds == null || attachmentIds.isEmpty()) {
      deleteAllEntityAttachments(userIdentityId, entityId, entityType);
    } else {
      for (String attachmentId : existingAttachmentsIds) {
        if (!attachmentIds.contains(attachmentId)) {
          deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
        }
      }
    }

    // attach new added files
    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      for (String attachmentId : attachmentIds) {
        if (!existingAttachmentsIds.contains(attachmentId)) {
          linkAttachmentsToEntity(userIdentityId, entityId, entityType, Collections.singletonList(attachmentId));
        }
      }
    }
  }

  @Override
  public void deleteAllEntityAttachments(long userIdentityId, long entityId, String entityType) throws ObjectNotFoundException,
                                                                                                IllegalAccessException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    Identity userIdentity = identityManager.getIdentity(String.valueOf(userIdentityId));
    boolean canDelete = canDelete(userIdentityId, entityType, entityId);
    if (userIdentity == null || !canDelete) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
    }

    List<AttachmentContextEntity> existingAttachmentsContext =
                                                             attachmentStorage.getAttachmentContextByEntity(entityId, entityType);
    List<String> attachmentsIds = existingAttachmentsContext.stream()
                                                            .map(AttachmentContextEntity::getAttachmentId)
                                                            .collect(Collectors.toList());

    if (attachmentsIds.isEmpty()) {
      throw new ObjectNotFoundException("Entity with id " + entityId + " and type" + entityType + " has no attachment linked");
    }

    for (String attachmentId : attachmentsIds) {
      deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
    }
  }

  @Override
  public void deleteAttachmentItemById(long userIdentityId,
                                       long entityId,
                                       String entityType,
                                       String attachmentId) throws ObjectNotFoundException, IllegalAccessException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    Identity userIdentity = identityManager.getIdentity(String.valueOf(userIdentityId));
    boolean canDelete = canDelete(userIdentityId, entityType, entityId);
    if (userIdentity == null || !canDelete) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
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
  public List<Attachment> getAttachmentsByEntity(long userIdentityId, long entityId, String entityType) throws Exception {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id should be positive");
    }
    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    List<AttachmentContextEntity> attachmentsContextEntity = attachmentStorage.getAttachmentContextByEntity(entityId, entityType);
    sortAttachments(attachmentsContextEntity);
    List<Attachment> attachments = new ArrayList<>();
    if (!attachmentsContextEntity.isEmpty()) {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      String workspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());
      try {
        attachmentsContextEntity.forEach(attachmentContextEntity -> {
          try {
            Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                                     documentService,
                                                                     workspace,
                                                                     session,
                                                                     attachmentContextEntity);
            boolean canView = canView(userIdentityId, entityType, entityId);
            boolean canDelete = canDelete(userIdentityId, entityType, entityId);
            Permission attachmentACL = new Permission(canView, canDelete);
            attachment.setAcl(attachmentACL);
            attachments.add(attachment);
          } catch (Exception e) {
            LOG.error("Cannot get attachment with id {} of entity {} with id {}", attachmentContextEntity.getAttachmentId(), entityType, entityId, e);
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

  public void addACLPlugin(AttachmentACLPlugin aclPlugin) {
    this.aclPlugins.put(aclPlugin.getEntityType(), aclPlugin);
  }

  public boolean canView(long userIdentityId, String entityType, long entityId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    if (attachmentACLPlugin == null) {
      return false;
    }
    return attachmentACLPlugin.canView(userIdentityId, entityType, String.valueOf(entityId));
  }

  public boolean canDelete(long userIdentityId, String entityType, long entityId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    if (attachmentACLPlugin == null) {
      return false;
    }
    return attachmentACLPlugin.canDelete(userIdentityId, entityType, String.valueOf(entityId));
  }

  private void sortAttachments(List<AttachmentContextEntity> attachments) {
    attachments.sort((attachment1, attachment2) -> ObjectUtils.compare(attachment2.getAttachedDate(),
                                                                       attachment1.getAttachedDate()));
  }

}
