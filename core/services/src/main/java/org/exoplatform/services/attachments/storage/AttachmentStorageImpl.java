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

import org.exoplatform.services.attachments.dao.AttachmentDAO;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.clouddrives.NotFoundException;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

public class AttachmentStorageImpl implements AttachmentStorage {

  AttachmentDAO                  attachmentDAO;

  private RepositoryService      repositoryService;

  private SessionProviderService sessionProviderService;

  private DocumentService        documentService;

  private LinkManager            linkManager;

  public AttachmentStorageImpl(AttachmentDAO attachmentDAO,
                               RepositoryService repositoryService,
                               SessionProviderService sessionProviderService,
                               DocumentService documentService,
                               LinkManager linkManager) {
    this.attachmentDAO = attachmentDAO;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.documentService = documentService;
    this.linkManager = linkManager;
  }

  @Override
  public void linkAttachmentToEntity(long entityId, String entityType, String attachmentId) {
    AttachmentContextEntity attachmentContextEntity = new AttachmentContextEntity();
    attachmentContextEntity.setEntityId(entityId);
    attachmentContextEntity.setEntityType(entityType.toUpperCase());
    attachmentContextEntity.setAttachmentId(attachmentId);
    attachmentContextEntity.setAttachedDate(System.currentTimeMillis());
    attachmentDAO.create(attachmentContextEntity);
  }

  @Override
  public List<Attachment> getAttachmentsByEntity(long userIdentityId,
                                                 long entityId,
                                                 String entityType) throws RepositoryException, NotFoundException {
    List<AttachmentContextEntity> attachmentsContextEntity = attachmentDAO.getAttachmentContextByEntity(entityId,
                                                                                                        entityType.toUpperCase());
    Utils.sortAttachmentsByDate(attachmentsContextEntity);
    List<Attachment> attachments = new ArrayList<>();
    if (!attachmentsContextEntity.isEmpty()) {
      Session session = Utils.getSession(sessionProviderService, repositoryService);
      try {
        for (AttachmentContextEntity attachmentContextEntity : attachmentsContextEntity) {
          Attachment attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                                   documentService,
                                                                   linkManager,
                                                                   Utils.getCurrentWorkspace(repositoryService),
                                                                   session,
                                                                   attachmentContextEntity.getAttachmentId());
          attachments.add(attachment);
        }
      } catch (Exception e) {
        throw new NotFoundException("Can't convert attachment JCR node to Attachment DTO", e);
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    }
    return attachments;
  }

  @Override
  public Attachment getAttachmentItemByEntity(long entityId, String entityType, String attachmentId) throws RepositoryException,
                                                                                                     NotFoundException {
    AttachmentContextEntity attachmentEntity = attachmentDAO.getAttachmentItemByEntity(entityId,
                                                                                       entityType.toUpperCase(),
                                                                                       attachmentId);
    Session session = Utils.getSession(sessionProviderService, repositoryService);
    Attachment attachment = null;
    try {
      attachment = EntityBuilder.fromAttachmentNode(repositoryService,
                                                    documentService,
                                                    linkManager,
                                                    Utils.getCurrentWorkspace(repositoryService),
                                                    session,
                                                    attachmentEntity.getAttachmentId());
    } catch (Exception e) {
      throw new NotFoundException("Can't convert attachment JCR node to Attachment DTO", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return attachment;
  }

  @Override
  public void deleteAllEntityAttachments(AttachmentContextEntity attachmentContextEntity) {
    attachmentDAO.delete(attachmentContextEntity);
  }

  @Override
  public void deleteAttachmentItemByIdByEntity(long entityId, String entityType, String attachmentId) {
    AttachmentContextEntity attachmentEntity = attachmentDAO.getAttachmentItemByEntity(entityId,
                                                                                       entityType.toUpperCase(),
                                                                                       attachmentId);
    attachmentDAO.delete(attachmentEntity);
  }
}
