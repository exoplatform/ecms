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
package org.exoplatform.services.attachments.utils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.services.attachments.rest.model.AttachmentEntity;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.rest.entity.IdentityEntity;

import com.sun.star.lang.IllegalAccessException;

import java.util.LinkedHashMap;

import javax.jcr.*;

public class EntityBuilder {

  private static final Log    LOG                  = ExoLogger.getLogger(EntityBuilder.class);

  private static final String IDENTITIES_REST_PATH = "/v1/social/identities";                 // NOSONAR

  private static final String IDENTITIES_EXPAND    = "all";

  public static final AttachmentEntity fromAttachment(IdentityManager identityManager, Attachment attachment) {
    return new AttachmentEntity(attachment.getId(),
                                attachment.getTitle(),
                                attachment.getSize(),
                                attachment.getMimetype(),
                                attachment.getPath(),
                                attachment.getIsPublic(),
                                attachment.getAcl(),
                                null,
                                attachment.getCreated(),
                                getIdentityEntity(identityManager, attachment.getUpdater()),
                                attachment.getUpdated(),
                                attachment.getDownloadUrl(),
                                attachment.getOpenUrl(),
                                attachment.getPreviewBreadcrumb(),
                                attachment.getVersion()

    );
  }

  public static final Attachment fromAttachmentNode(RepositoryService repositoryService,
                                                    DocumentService documentService,
                                                    String workspace,
                                                    Session session,
                                                    String attachmentId) throws Exception {
    Node attachmentNode = session.getNodeByUUID(attachmentId);
    if (attachmentNode == null) {
      throw new PathNotFoundException("Node with id " + attachmentId + " wasn't found");
    }

    LinkManager linkManager = ExoContainerContext.getService(LinkManager.class);
    if (linkManager.isLink(attachmentNode)) {
      attachmentNode = linkManager.getTarget(attachmentNode);
      if (attachmentNode == null) {
        throw new PathNotFoundException("Target Node with of symlink " + attachmentId + " wasn't found");
      }
    }
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

    long size = attachmentNode.getNode("jcr:content").getProperty("jcr:data").getLength();
    attachment.setSize(size);

    String downloadUrl = getDownloadUrl(repositoryService, workspace, attachmentsPath);
    attachment.setDownloadUrl(downloadUrl);

    String openUrl = getUrl(documentService, attachmentsPath);
    attachment.setOpenUrl(openUrl);

    String attachmentsVersion = getStringProperty(attachmentNode, "exo:baseVersion");
    attachment.setVersion(attachmentsVersion);

    LinkedHashMap<String, String> previewBreadcrumb = new LinkedHashMap<>();
    try {
      previewBreadcrumb = documentService.getFilePreviewBreadCrumb(attachmentNode);
    } catch (Exception e) {
      LOG.error("Error while getting file preview breadcrumb " + attachmentNode.getUUID(), e);
    }
    attachment.setPreviewBreadcrumb(previewBreadcrumb);
    return attachment;
  }

  private static IdentityEntity getIdentityEntity(IdentityManager identityManager, String ownerId) {
    Identity identity = getIdentity(identityManager, ownerId);
    if (identity == null) {
      return null;
    }
    return org.exoplatform.social.rest.api.EntityBuilder.buildEntityIdentity(identity, IDENTITIES_REST_PATH, IDENTITIES_EXPAND);
  }

  private static final Identity getIdentity(IdentityManager identityManager, String identityId) {
    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, identityId);
  }

  private static String getStringProperty(Node node, String propertyName) throws RepositoryException {
    if (node.hasProperty(propertyName)) {
      return node.getProperty(propertyName).getString();
    }
    return "";
  }

  private static String getDownloadUrl(RepositoryService repositoryService, String workspace, String nodePath) {
    String restContextName = WCMCoreUtils.getRestContextName();

    String repositoryName = getRepositoryName(repositoryService);

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/')
               .append(restContextName)
               .append("/jcr/")
               .append(repositoryName)
               .append('/')
               .append(workspace)
               .append(nodePath);
    return downloadUrl.toString();
  }

  private static String getUrl(DocumentService documentService, String nodePath) {
    String url = "";
    try {
      url = documentService.getLinkInDocumentsApp(nodePath);
    } catch (Exception e) {
      LOG.error("Cannot get url of document " + nodePath, e);
    }
    return url;
  }

  private static String getRepositoryName(RepositoryService repositoryService) {
    try {
      return repositoryService.getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      LOG.debug("Cannot get repository name", e);
      return "repository";
    }
  }
}
