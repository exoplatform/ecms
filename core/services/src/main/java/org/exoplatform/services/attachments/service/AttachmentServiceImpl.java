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

import java.security.AccessControlException;
import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

public class AttachmentServiceImpl implements AttachmentService {

  private static final Log LOG = ExoLogger.getExoLogger(AttachmentServiceImpl.class);

  private RepositoryService                repositoryService;

  private SessionProviderService           sessionProviderService;

  private AttachmentStorage                attachmentStorage;

  private DocumentService                  documentService;

  private IdentityManager                  identityManager;

  private ManageDriveService               manageDriveService;

  private NodeHierarchyCreator             nodeHierarchyCreator;

  private NodeFinder                       nodeFinder;

  private LinkManager                      linkManager;

  private Map<String, AttachmentACLPlugin> aclPlugins = new HashMap<>();

  public AttachmentServiceImpl(AttachmentStorage attachmentStorage,
                               RepositoryService repositoryService,
                               SessionProviderService sessionProviderService,
                               DocumentService documentService,
                               IdentityManager identityManager,
                               ManageDriveService manageDriveService,
                               NodeHierarchyCreator nodeHierarchyCreator,
                               NodeFinder nodeFinder,
                               LinkManager linkManager) {

    this.attachmentStorage = attachmentStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
    this.identityManager = identityManager;
    this.manageDriveService = manageDriveService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.nodeFinder = nodeFinder;
    this.linkManager = linkManager;
  }

  @Override
  public Attachment linkAttachmentToEntity(long userIdentityId,
                                           long entityId,
                                           String entityType,
                                           String attachmentId) throws IllegalAccessException {
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
    return getAttachmentByIdByEntity(entityType, entityId, attachmentId, userIdentityId);
  }

  @Override
  public void updateEntityAttachments(long userIdentityId,
                                      long entityId,
                                      String entityType,
                                      List<String> attachmentIds) throws IllegalAccessException, ObjectNotFoundException {
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

    List<Attachment> existingEntityAttachments;
    try {
      existingEntityAttachments = attachmentStorage.getAttachmentsByEntity(entityId, entityType);
    } catch (Exception e) {
      throw new IllegalStateException("Can't get attachments of entity with type " + entityType + " and id " + entityId, e);
    }
    List<String> existingAttachmentsIds = existingEntityAttachments.stream().map(Attachment::getId).collect(Collectors.toList());
    // delete removed attachments
    if (attachmentIds.isEmpty()) {
      deleteAllEntityAttachments(userIdentityId, entityId, entityType);
    } else {
      for (String attachmentId : existingAttachmentsIds) {
        if (!attachmentIds.contains(attachmentId) && attachmentId != null) {
          deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
        }
      }
    }

    // attach new added files
    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      for (String attachmentId : attachmentIds) {
        if (!existingAttachmentsIds.contains(attachmentId) && StringUtils.isNotEmpty(attachmentId)) {
          linkAttachmentToEntity(userIdentityId, entityId, entityType, attachmentId);
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
    boolean canDetach = canDetach(userIdentityId, entityType, entityId, "");
    if (userIdentity == null || !canDetach) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
    }

    List<Attachment> existingEntityAttachments;
    try {
      existingEntityAttachments = attachmentStorage.getAttachmentsByEntity(entityId, entityType);
    } catch (Exception e) {
      throw new IllegalStateException("Can't get attachments of entity with type " + entityType + " and id " + entityId, e);
    }
    List<String> attachmentsIds = existingEntityAttachments.stream().map(Attachment::getId).collect(Collectors.toList());

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
    boolean canDetach = canDetach(userIdentityId, entityType, entityId, attachmentId);
    if (userIdentity == null || !canDetach) {
      throw new IllegalAccessException("User with name " + userIdentityId + " doesn't exist");
    }

    try {
      Attachment attachment = attachmentStorage.getAttachmentItemByEntity(entityId,
                                                                          entityType,
                                                                          attachmentId);
      if (attachment == null) {
        throw new ObjectNotFoundException("Attachment with id" + attachmentId + " linked to entity with id " + entityId
            + " and type" + entityType + " not found");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't get attachment with jcr uuid " + attachmentId + " of entity with type " + entityType
          + " and id " + entityId);
    }

    attachmentStorage.deleteAttachmentItemByIdByEntity(entityId, entityType, attachmentId);
  }

  @Override
  public List<Attachment> getAttachmentsByEntity(long userIdentityId,
                                                 long entityId,
                                                 String entityType) throws IllegalAccessException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id should be positive");
    }

    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (userIdentityId <= 0) {
      throw new IllegalAccessException("User identity must be positive");
    }

    List<Attachment> entityAttachments;
    try {
      entityAttachments = attachmentStorage.getAttachmentsByEntity(entityId, entityType);
    } catch (Exception e) {
      throw new IllegalStateException("Can't get attachments of entity with type " + entityType + " and id " + entityId, e);
    }

    if (!entityAttachments.isEmpty()) {
      entityAttachments.forEach(attachment -> {
        boolean canView = canView(userIdentityId, entityType, entityId, attachment.getId());
        boolean canEdit = canEdit(userIdentityId, entityType, entityId, attachment.getId());
        boolean canDetach = canDetach(userIdentityId, entityType, entityId, attachment.getId());
        Permission attachmentACL = new Permission(attachment.getAcl().isCanAccess(), canView, canDetach, canEdit);
        attachment.setAcl(attachmentACL);
      });
    }
    return entityAttachments;
  }

  @Override
  public Attachment getAttachmentByIdByEntity(String entityType, long entityId, String attachmentId, long userIdentityId) {
    Attachment attachment;
    Session session = null;
    try {
      session = Utils.getSession(sessionProviderService, repositoryService);

      attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                    documentService,
                                                    linkManager,
                                                    Utils.getCurrentWorkspace(repositoryService),
                                                    session,
                                                    attachmentId);

      boolean canView = canView(userIdentityId, entityType, entityId, attachment.getId());
      boolean canDetach = canDetach(userIdentityId, entityType, entityId, attachment.getId());
      boolean canEdit = canEdit(userIdentityId, entityType, entityId, attachment.getId());
      Permission attachmentACL = new Permission(attachment.getAcl().isCanAccess(), canView, canDetach, canEdit);

      attachment.setAcl(attachmentACL);
    } catch (Exception e) {
      throw new IllegalStateException("Can't convert attachment JCR node with id " + attachmentId + " to entity", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return attachment;
  }

  @Override
  public Attachment getAttachmentById(String attachmentId) throws ObjectNotFoundException {
    Session session = null;
    try {
      session = Utils.getSession(sessionProviderService, repositoryService);
      return EntityBuilder.fromAttachmentNode(repositoryService,
                                              documentService,
                                              linkManager,
                                              Utils.getCurrentWorkspace(repositoryService),
                                              session,
                                              attachmentId);
    } catch (ObjectNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Can't convert attachment JCR node with id " + attachmentId + " to entity", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @Override
  public Attachment moveAttachmentToNewPath(long userIdentityId,
                                            String attachmentId,
                                            String newPathDrive,
                                            String newPath,
                                            String entityType,
                                            long entityId) throws IllegalAccessException {
    if (userIdentityId <= 0) {
      throw new IllegalArgumentException("User identity must be positive");
    }
    if (StringUtils.isEmpty(attachmentId)) {
      throw new IllegalArgumentException("Attachment id is mandatory");
    }
    if (StringUtils.isEmpty(newPathDrive)) {
      throw new IllegalArgumentException("New destination path's drive must not be empty");
    }
    if (!canEdit(userIdentityId, entityType, entityId, attachmentId)) {
      throw new IllegalAccessException("User with identity id" + userIdentityId + "is not allowed to move attachment with id"
          + attachmentId);
    }

    Session session = null;
    try {
      session = Utils.getSession(sessionProviderService, repositoryService);
      Node attachmentNode = session.getNodeByUUID(attachmentId);
      Node destNode = Utils.getParentFolderNode(session,
                                                manageDriveService,
                                                nodeHierarchyCreator,
                                                nodeFinder,
                                                newPathDrive,
                                                newPath);
      String destPath = destNode.getPath().concat("/").concat(attachmentNode.getName());
      session.move(attachmentNode.getPath(), destPath);
      session.save();
      return EntityBuilder.fromAttachmentNode(repositoryService,
                                              documentService,
                                              linkManager,
                                              Utils.getCurrentWorkspace(repositoryService),
                                              session,
                                              attachmentId);
    } catch (Exception e) {
      throw new IllegalStateException("Error while trying to move attachment node with id " + attachmentId + " to the new path "
          + newPath);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @Override
  public Attachment createNewDocument(org.exoplatform.services.security.Identity userIdentity,
                                      String title,
                                      String path,
                                      String pathDrive,
                                      String templateName) throws Exception {
    if (userIdentity == null) {
      throw new IllegalArgumentException("User identity is mandatory");
    }
    if (StringUtils.isEmpty(title)) {
      throw new IllegalArgumentException("New document title is mandatory");
    }
    if (StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("new document path is mandatory");
    }
    if (StringUtils.isEmpty(pathDrive)) {
      throw new IllegalArgumentException("new document path's drive is mandatory");
    }
    if (StringUtils.isEmpty(templateName)) {
      throw new IllegalArgumentException("template name is mandatory");
    }

    Session session = null;

    try {
      session = Utils.getSession(sessionProviderService, repositoryService);
      Node currentNode =
                       Utils.getParentFolderNode(session, manageDriveService, nodeHierarchyCreator, nodeFinder, pathDrive, path);
      if(currentNode.hasNode(title)) {
        throw new ItemExistsException("Document with the same name " + title + " already exist in this current path");
      }
      List<NewDocumentTemplate> documentTemplates = getDocumentTemplateList(userIdentity);
      NewDocumentTemplate documentTemplate = documentTemplates.stream()
                                                              .filter(template -> template.getName().equals(templateName))
                                                              .findFirst()
                                                              .orElse(null);
      if (documentTemplate != null) {
        Node createdDocument = documentService.createDocumentFromTemplate(currentNode, title, documentTemplate);
        session.save();
        Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                documentService,
                                                linkManager,
                                                Utils.getCurrentWorkspace(repositoryService),
                                                session,
                                                createdDocument.getUUID());

        boolean canView = checkAttachmentJCRPermission(attachment.getId(), PermissionType.READ);
        boolean canDetach = checkAttachmentJCRPermission(attachment.getId(), PermissionType.REMOVE);
        boolean canEdit = checkAttachmentJCRPermission(attachment.getId(), PermissionType.SET_PROPERTY);
        Permission attachmentACL = new Permission(attachment.getAcl().isCanAccess(), canView, canDetach, canEdit);

        attachment.setAcl(attachmentACL);
        return attachment;
      } else {
        throw new IllegalStateException("Document template not available with " + templateName + " as a name");
      }
    } catch (Exception e) {
      LOG.error("Error while trying to create a new document", e);
      throw e;
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private List<NewDocumentTemplate> getDocumentTemplateList(org.exoplatform.services.security.Identity identity) {
    List<NewDocumentTemplate> options = new ArrayList<>();
    List<NewDocumentTemplateProvider> templateProviders = documentService.getNewDocumentTemplateProviders();
    templateProviders.forEach(provider -> {
      if (provider.getEditor().isAvailableForUser(identity)) {
        options.addAll(provider.getTemplates());
      }
    });
    return options;
  }

  public void addACLPlugin(AttachmentACLPlugin aclPlugin) {
    this.aclPlugins.put(aclPlugin.getEntityType(), aclPlugin);
  }

  public boolean canView(long userIdentityId, String entityType, long entityId, String attachmentId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    boolean jcrViewPermission = true;
    if (!StringUtils.isBlank(attachmentId)) {
      jcrViewPermission = checkAttachmentJCRPermission(attachmentId, PermissionType.READ);
    }
    if (attachmentACLPlugin == null) {
      return jcrViewPermission;
    }
    return jcrViewPermission && attachmentACLPlugin.canView(userIdentityId, entityType, String.valueOf(entityId));
  }

  public boolean canDetach(long userIdentityId, String entityType, long entityId, String attachmentId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    boolean jcrDeletePermission = true;
    if (!StringUtils.isBlank(attachmentId)) {
      jcrDeletePermission = checkAttachmentJCRPermission(attachmentId, PermissionType.REMOVE);
    }

    if (attachmentACLPlugin == null) {
      return jcrDeletePermission;
    }

    return attachmentACLPlugin.canDetach(userIdentityId, entityType, String.valueOf(entityId));
  }

  public boolean canEdit(long userIdentityId, String entityType, long entityId, String attachmentId) {
    AttachmentACLPlugin attachmentACLPlugin = this.aclPlugins.get(entityType);
    boolean jcrEditPermission = true;
    if (!StringUtils.isBlank(attachmentId)) {
      jcrEditPermission = checkAttachmentJCRPermission(attachmentId, PermissionType.SET_PROPERTY);
    }
    if (attachmentACLPlugin == null) {
      return jcrEditPermission;
    }
    return jcrEditPermission && attachmentACLPlugin.canEdit(userIdentityId, entityType, String.valueOf(entityId));
  }

  private boolean checkAttachmentJCRPermission(String attachmentId, String permissionType) {
    Session session;
    boolean attachmentPermission = true;
    Node attachmentNode;
    try {
      session = Utils.getSession(sessionProviderService, repositoryService);
      attachmentNode = session.getNodeByUUID(attachmentId);
      session.checkPermission(attachmentNode.getPath(), permissionType);
    } catch (AccessControlException | AccessDeniedException e) {
      attachmentPermission = false;
    } catch (RepositoryException e) {
      throw new IllegalStateException("Can't get attachment node with id" + attachmentId, e);
    }
    return attachmentPermission;
  }

}
