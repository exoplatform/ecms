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

import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.*;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.clouddrives.NotFoundException;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

public class AttachmentServiceImpl implements AttachmentService {

  private static final Log                 LOG        = ExoLogger.getLogger(AttachmentServiceImpl.class);

  private RepositoryService                repositoryService;

  private SessionProviderService           sessionProviderService;

  private AttachmentStorage                attachmentStorage;

  private DocumentService                  documentService;

  private IdentityManager                  identityManager;

  private ManageDriveService               manageDriveService;

  private NodeHierarchyCreator             nodeHierarchyCreator;

  private NodeFinder                       nodeFinder;

  private Map<String, AttachmentACLPlugin> aclPlugins = new HashMap<>();

  public AttachmentServiceImpl(AttachmentStorage attachmentStorage,
                               RepositoryService repositoryService,
                               SessionProviderService sessionProviderService,
                               DocumentService documentService,
                               IdentityManager identityManager,
                               ManageDriveService manageDriveService,
                               NodeHierarchyCreator nodeHierarchyCreator,
                               NodeFinder nodeFinder) {

    this.attachmentStorage = attachmentStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
    this.identityManager = identityManager;
    this.manageDriveService = manageDriveService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.nodeFinder = nodeFinder;

  }

  @Override
  public Attachment linkAttachmentToEntity(long userIdentityId,
                                            long entityId,
                                            String entityType,
                                            String attachmentId) throws Exception {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (StringUtils.isBlank(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (StringUtils.isBlank(attachmentId)) {
      throw new IllegalArgumentException("attachments Id must not be empty");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    Identity userIdentity = identityManager.getIdentity(String.valueOf(userIdentityId));
    if (userIdentity == null) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
    }

    attachmentStorage.linkAttachmentToEntity(entityId, entityType, attachmentId);
    return getAttachmentById(entityType, entityId, attachmentId, userIdentityId);
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

    attachmentIds.forEach(attachmentId -> {
      if (StringUtils.isBlank(attachmentId)) {
        throw new IllegalArgumentException("attachmentId must not be empty");
      }
    });

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
        if (!attachmentIds.contains(attachmentId) || StringUtils.isEmpty(attachmentId)) {
          deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
        }
      }
    }

    // attach new added files
    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      for (String attachmentId : attachmentIds) {
        if (!existingAttachmentsIds.contains(attachmentId) && StringUtils.isNotEmpty(attachmentId)) {
          try {
            linkAttachmentToEntity(userIdentityId, entityId, entityType, attachmentId);
          } catch (Exception e) {
            throw new ObjectNotFoundException("Can't link attachment with id " + attachmentId + " to entity type " + entityType
                + " with id " + entityId);
          }
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

    if (StringUtils.isBlank(attachmentId)) {
      throw new IllegalAccessException("AttachmentId must not be empty");
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
      Session session = getSession();
      try {
        for (AttachmentContextEntity attachmentContextEntity : attachmentsContextEntity) {
          Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                                   documentService,
                                                                   getCurrentWorkspace(),
                                                                   session,
                                                                   attachmentContextEntity.getAttachmentId());
          boolean canView = canView(userIdentityId, entityType, entityId);
          boolean canDelete = canDelete(userIdentityId, entityType, entityId);
          boolean canEdit = canEdit(userIdentityId, entityType, entityId);
          Permission attachmentACL = new Permission(canView, canDelete, canEdit);
          attachment.setAcl(attachmentACL);
          attachments.add(attachment);
        }
      } catch (Exception e) {
        throw new NotFoundException("Can't convert attachment JCR node to entity", e);
      } finally{
        if (session != null) {
          session.logout();
        }
      }
    }
    return attachments;
  }

  @Override
  public Attachment getAttachmentById(String entityType,
                                      long entityId,
                                      String attachmentId,
                                      long userIdentityId) throws Exception {
    Attachment attachment = new Attachment();
    Session session = getSession();
    try {
      attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                    documentService,
                                                    getCurrentWorkspace(),
                                                    session,
                                                    attachmentId);

      boolean canView = canView(userIdentityId, entityType, entityId);
      boolean canDelete = canDelete(userIdentityId, entityType, entityId);
      boolean canEdit = canEdit(userIdentityId, entityType, entityId);
      Permission attachmentACL = new Permission(canView, canDelete, canEdit);

      attachment.setAcl(attachmentACL);
    } catch (NotFoundException e) {
      throw new NotFoundException("Can't convert attachment JCR node with id " + attachmentId + " to entity", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return attachment;
  }

  @Override
  public void moveAttachmentToNewPath(long userIdentityId,
                                      String attachmentId,
                                      String newPathDrive,
                                      String newPath,
                                      String entityType,
                                      long entityId) throws Exception {
    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }
    if (StringUtils.isEmpty(attachmentId)) {
      throw new IllegalArgumentException("Attachment id is mandatory");
    }
    if (StringUtils.isEmpty(newPathDrive)) {
      throw new IllegalArgumentException("New destination path's drive must not be empty");
    }
    if (!canEdit(userIdentityId, entityType, entityId)) {
      throw new IllegalAccessException("User with identity id" + userIdentityId + "is not allowed to move attachment with id" + attachmentId);
    }

    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    String workspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());
    try {
      Node attachmentNode = session.getNodeByUUID(attachmentId);
      Node destNode = getParentFolderNode(session, newPathDrive, newPath);
      String destPath = destNode.getPath().concat("/").concat(attachmentNode.getName());
      session.move(attachmentNode.getPath(), destPath);
      session.save();
    } catch (Exception e) {
      throw new ItemNotFoundException("Error while trying to move attachment node with id " + attachmentId + " to the new path " + newPath);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
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

  public boolean canEdit(long userIdentityId, String entityType, long entityId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    if (attachmentACLPlugin == null) {
      return false;
    }
    return attachmentACLPlugin.canEdit(userIdentityId, entityType, String.valueOf(entityId));
  }

  private void sortAttachments(List<AttachmentContextEntity> attachments) {
    attachments.sort((attachment1, attachment2) -> ObjectUtils.compare(attachment2.getAttachedDate(),
                                                                       attachment1.getAttachedDate()));
  }

  /**
   * Gets the parent folder node.
   *
   * @param session jcr session
   * @param driverName the driver name
   * @param currentFolder the current folder
   *
   * @return the parent folder node
   *
   * @throws Exception the exception
   */
  private Node getParentFolderNode(Session session, String driverName, String currentFolder) throws Exception {
    DriveData driveData = manageDriveService.getDriveByName(driverName);
    StringBuilder parentPath = new StringBuilder("");
    if (driveData != null) {
      parentPath.append(driveData.getHomePath());

      if (driveData.getHomePath().startsWith(nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH) + "/${userId}")) {
        parentPath.setLength(0);
        parentPath.append(Utils.getPersonalDrivePath(driveData.getHomePath(),
                                                     ConversationState.getCurrent().getIdentity().getUserId()));
      }
    }

    if (StringUtils.isNotBlank(currentFolder)) {
      parentPath.append("/").append(currentFolder);
    }
    String parentPathStr = parentPath.toString().replace("//", "/");
    return getTargetNode(session, parentPathStr);
  }

  private Node getTargetNode(Session session, String path) throws Exception {
    return (Node) nodeFinder.getItem(session, path, true);
  }

  private Session getSession() throws RepositoryException {
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    return sessionProvider.getSession(getCurrentWorkspace(), repositoryService.getCurrentRepository());
  }
  private String getCurrentWorkspace() throws RepositoryException {
    return repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
  }
}
