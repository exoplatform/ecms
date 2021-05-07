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
package org.exoplatform.services.attachments.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

public class AttachmentDAO extends GenericDAOJPAImpl<AttachmentContextEntity, Long> {

  public List<String> getAttachmentsIdsByEntity(long entityId, int entityType) {
    TypedQuery<String> query =
                             getEntityManager().createNamedQuery("AttachmentsContext.getAttachmentContextByEntity", String.class);
    query.setParameter("entityId", entityId);
    query.setParameter("entityType", entityType);
    List<String> resultList = query.getResultList();
    return resultList == null ? Collections.emptyList() : resultList;
  }

  public AttachmentContextEntity getAttachmentItemByEntity(long entityId, int entityType, String attachmentId) {
    TypedQuery<AttachmentContextEntity> query =
                                               getEntityManager().createNamedQuery("AttachmentsContext.getAttachmentItemByEntity",
                                                                                   AttachmentContextEntity.class);
    query.setParameter("entityId", entityId);
    query.setParameter("entityType", entityType);
    query.setParameter("attachmentId", attachmentId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
