package org.exoplatform.services.attachments.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.attachments.model.AttachmentsContextEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class AttachmentsDAO extends GenericDAOJPAImpl<AttachmentsContextEntity, Long> {

  public AttachmentsContextEntity getAttachmentContextByEntity(long entityId, int entityType) {
    TypedQuery<AttachmentsContextEntity> query = getEntityManager().createNamedQuery("AttachmentsContext.getAttachmentContextByEntity",
            AttachmentsContextEntity.class);
    query.setParameter("entityId", entityId);
    query.setParameter("entityType", entityType);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
