package org.exoplatform.services.attachments.service;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;
import org.exoplatform.services.attachments.rest.model.AttachmentsContext;
import org.exoplatform.services.attachments.storage.AttachmentsStorage;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AttachmentsServiceImpl implements AttachmentsService {

  private static final Log LOG = ExoLogger.getLogger(AttachmentsServiceImpl.class.getName());

  private RepositoryService repositoryService;

  private SessionProviderService sessionProviderService;

  AttachmentsStorage attachmentsStorage;

  public AttachmentsServiceImpl(AttachmentsStorage attachmentsStorage,
                                RepositoryService repositoryService,
                                SessionProviderService sessionProviderService) {

    this.attachmentsStorage = attachmentsStorage;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  @Override
  public void linkAttachmentsToContext(AttachmentsContext attachmentsContext) {
    if (attachmentsContext == null) {
      throw new IllegalArgumentException("attachmentsContext object is mandatory");
    }

    if (attachmentsContext.getAttachmentIds() == null || attachmentsContext.getAttachmentIds().size() <= 0) {
      throw new IllegalArgumentException("attachmentsIds must not be empty");
    }

    if (attachmentsContext.getEntityId() <= 0) {
      throw new IllegalArgumentException("Entity Id must be positive");
    }

    if (attachmentsContext.getAttachmentsEntityType() == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    long entityId = attachmentsContext.getEntityId();
    String entityType = attachmentsContext.getAttachmentsEntityType().toString();
    AttachmentsContextEntity attachmentsContextEntity = attachmentsStorage.getAttachmentContextByEntity(entityId, entityType);

    if (attachmentsContextEntity == null) {
      attachmentsContextEntity = new AttachmentsContextEntity();
    }

    attachmentsContextEntity.setEntityId(entityId);
    attachmentsContextEntity.setEntityType(entityType);
    List<String> attachmentIds = attachmentsContext.getAttachmentIds();
    attachmentsContextEntity.setAttachmentIds(attachmentIds.stream().map(Object::toString).collect(Collectors.joining(",")));

    attachmentsStorage.linkAttachmentsToContext(attachmentsContextEntity);
  }

  @Override
  public List<Attachment> getAttachmentsByEntity(long entityId, String entityType) throws Exception {
    if (entityId <= 0) {
      throw new IllegalArgumentException("Entity Id should be positive");
    }
    if (StringUtils.isEmpty(entityType)) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }

    AttachmentsContextEntity attachmentsContext = attachmentsStorage.getAttachmentContextByEntity(entityId, entityType);
    List<Attachment> attachments = new ArrayList<>();
    if (attachmentsContext != null && StringUtils.isNotEmpty(attachmentsContext.getAttachmentIds())) {
      List<String> attachmentsIds = new ArrayList<>(Arrays.asList(
              attachmentsContext.getAttachmentIds().split(",")
      ));
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Session session = sessionProvider.getSession(
              repositoryService.getCurrentRepository()
                      .getConfiguration()
                      .getDefaultWorkspaceName(),
              repositoryService.getCurrentRepository());
      attachmentsIds.forEach(attachmentId -> {
        try {
          Node attachmentNode = session.getNodeByUUID(attachmentId);
          Attachment attachment = new Attachment();
          attachment.setId(attachmentNode.getUUID());
          attachment.setTitle(getStringProperty(attachmentNode, "exo:title"));
          attachment.setPath(attachmentNode.getPath());
          attachment.setCreated(getStringProperty(attachmentNode, "exo:dateCreated"));
          //attachment.setCreatorId(Long.parseLong(getStringProperty(attachmentNode, "exo:owner")));
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
          attachment.setMimetype(getStringProperty(attachmentNode, "jcr:primaryType"));

          boolean canRemove = true;
          try {
            session.checkPermission(attachmentNode.getPath(), PermissionType.REMOVE);
          } catch (Exception e) {
            canRemove = false;
          }

          boolean canEdit = true;
          try {
            session.checkPermission(attachmentNode.getPath(), PermissionType.SET_PROPERTY);
          } catch (Exception e) {
            canEdit = false;
          }

          Permission permission = new Permission(canEdit, canRemove);
          attachment.setAcl(permission);
          long size = attachmentNode.getNode("jcr:content").getProperty("jcr:data").getLength();
          attachment.setSize(size);
          attachments.add(attachment);
        } catch (Exception e) {
          LOG.error("Cannot get attachment with id " + attachmentId + " of entity " + entityType + " with id " + entityId, e);
        }
      });
    }
    return attachments;
  }

  private String getStringProperty(Node node, String propertyName) throws RepositoryException {
    if (node.hasProperty(propertyName)) {
      return node.getProperty(propertyName).getString();
    }
    return "";
  }

}
