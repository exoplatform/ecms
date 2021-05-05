package org.exoplatform.services.attachments.service;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.AttachmentsEntityType;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;
import org.exoplatform.services.attachments.storage.AttachmentsStorage;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.stream.Collectors;

public class AttachmentsServiceImpl implements AttachmentsService {

  private static final Log LOG = ExoLogger.getLogger(AttachmentsServiceImpl.class.getName());

  private RepositoryService repositoryService;

  private SessionProviderService sessionProviderService;

  AttachmentsStorage attachmentsStorage;

  private DocumentService documentService;

  public AttachmentsServiceImpl(AttachmentsStorage attachmentsStorage,
                                RepositoryService repositoryService,
                                SessionProviderService sessionProviderService,
                                DocumentService documentService) {

    this.attachmentsStorage = attachmentsStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
  }

  @Override
  public void linkAttachmentsToEntity(long entityId, String entityType, List<String> attachmentIds) {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (entityType == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    if (attachmentIds == null || attachmentIds.size() <= 0) {
      throw new IllegalArgumentException("attachmentsIds must not be empty");
    }

    AttachmentsContextEntity attachmentsContextEntity = attachmentsStorage.getAttachmentByEntity(entityId, entityType);

    if (attachmentsContextEntity == null) {
      attachmentsContextEntity = new AttachmentsContextEntity();
    }

    attachmentsContextEntity.setEntityId(entityId);
    attachmentsContextEntity.setEntityType(AttachmentsEntityType.valueOf(entityType.toUpperCase()).ordinal());
    attachmentsContextEntity.setAttachmentIds(attachmentIds.stream().map(Object::toString).collect(Collectors.joining(",")));

    attachmentsStorage.linkAttachmentsToEntity(attachmentsContextEntity);
  }

  @Override
  public void updateEntityAttachments(long entityId, String entityType, List<String> attachmentIds) {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (entityType == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentsContextEntity attachmentsContextEntity = attachmentsStorage.getAttachmentByEntity(entityId, entityType);

    if (attachmentsContextEntity == null) {
      throw new IllegalArgumentException("Entity with id " + entityId + " and type" + entityType + " not found");
    }
    attachmentsContextEntity.setAttachmentIds(attachmentIds.stream().map(Object::toString).collect(Collectors.joining(",")));
    attachmentsStorage.linkAttachmentsToEntity(attachmentsContextEntity);
  }

  @Override
  public void deleteEntityAttachments(long entityId, String entityType) throws ObjectNotFoundException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (entityType == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentsContextEntity attachmentsContextEntity = attachmentsStorage.getAttachmentByEntity(entityId, entityType);

    if (attachmentsContextEntity == null) {
      throw new ObjectNotFoundException("Entity with id " + entityId + " and type" + entityType + " not found");
    }

    attachmentsStorage.deleteEntityAttachments(attachmentsContextEntity);
  }

  @Override
  public void deleteEntityAttachment(long entityId, String entityType, String attachmentId) throws ObjectNotFoundException {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (entityType == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentsContextEntity attachmentsContextEntity = attachmentsStorage.getAttachmentByEntity(entityId, entityType);
    if (attachmentsContextEntity == null) {
      throw new ObjectNotFoundException("Entity with id " + entityId + " and type" + entityType + " not found");
    }

    List<String> attachmentsIds = new ArrayList<>(Arrays.asList(attachmentsContextEntity.getAttachmentIds().split(",")));
    attachmentsIds.remove(attachmentId);
    updateEntityAttachments(entityId, entityType, attachmentsIds);
  }

  @Override
  public List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id should be positive");
    }
    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentsContextEntity attachmentsContext = attachmentsStorage.getAttachmentByEntity(entityId, entityType);
    List<Attachment> attachments = new ArrayList<>();
    if (attachmentsContext != null && StringUtils.isNotEmpty(attachmentsContext.getAttachmentIds())) {
      List<String> attachmentsIds = new ArrayList<>(Arrays.asList(
              attachmentsContext.getAttachmentIds().split(",")
      ));
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      String workspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());
      try {
        attachmentsIds.forEach(attachmentId -> {
          try {
            Node attachmentNode = session.getNodeByUUID(attachmentId);
            Attachment attachment = new Attachment();
            attachment.setId(attachmentNode.getUUID());
            String attachmentsTitle = getStringProperty(attachmentNode, "exo:title");
            attachment.setTitle(attachmentsTitle);
            String attachmentsPath = attachmentNode.getPath();
            attachment.setPath(attachmentsPath);
            attachment.setCreated(getStringProperty(attachmentNode, "exo:dateCreated"));
            if (attachmentNode.hasProperty("exo:dateModified")) {
              attachment.setUpdated(getStringProperty(attachmentNode, "exo:dateModified"));
            } else {
              attachment.setUpdated(null);
            }
            if (attachmentNode.hasProperty("exo:lastModifier")) {
              attachment.setUpdater(getStringProperty(attachmentNode, "exo:lastModifier"));
            } else {
              attachment.setUpdater(null);
            }
            DMSMimeTypeResolver mimeTypeResolver = DMSMimeTypeResolver.getInstance();
            String mimetype = mimeTypeResolver.getMimeType(attachmentsTitle);
            attachment.setMimetype(mimetype);

            boolean canRemove = true;
            try {
              session.checkPermission(attachmentsPath, PermissionType.REMOVE);
            } catch (Exception e) {
              canRemove = false;
            }

            boolean canEdit = true;
            try {
              session.checkPermission(attachmentsPath, PermissionType.SET_PROPERTY);
            } catch (Exception e) {
              canEdit = false;
            }

            Permission permission = new Permission(canEdit, canRemove);
            attachment.setAcl(permission);

            long size = attachmentNode.getNode("jcr:content").getProperty("jcr:data").getLength();
            attachment.setSize(size);

            String downloadUrl = getDownloadUrl(workspace, attachmentsPath);
            attachment.setDownloadUrl(downloadUrl);

            attachment.setOpenUrl(downloadUrl);

            String attachmentsVersion = getStringProperty(attachmentNode, "exo:baseVersion");
            attachment.setVersion(attachmentsVersion);

            LinkedHashMap<String, String> previewBreadcrumb = new LinkedHashMap<>();
            try {
              previewBreadcrumb = documentService.getFilePreviewBreadCrumb(attachmentNode);
            } catch (Exception e) {
              LOG.error("Error while getting file preview breadcrumb " + attachmentNode.getUUID(), e);
            }
            attachment.setPreviewBreadcrumb(previewBreadcrumb);

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

  private String getStringProperty(Node node, String propertyName) throws RepositoryException {
    if (node.hasProperty(propertyName)) {
      return node.getProperty(propertyName).getString();
    }
    return "";
  }


  private String getDownloadUrl(String workspace, String nodePath) {
    String restContextName =  WCMCoreUtils.getRestContextName();

    String repositoryName = getRepositoryName();

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/').append(restContextName).append("/jcr/").
      append(repositoryName).append('/').
      append(workspace).append(nodePath);
    return downloadUrl.toString();
  }

  protected String getRepositoryName() {
    try {
      return repositoryService.getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      LOG.debug("Cannot get repository name", e);
      return "repository";
    }
  }

}
