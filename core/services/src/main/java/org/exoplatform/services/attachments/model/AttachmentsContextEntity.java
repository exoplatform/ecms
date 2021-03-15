package org.exoplatform.services.attachments.model;

import org.exoplatform.commons.api.persistence.ExoEntity;
import javax.persistence.*;

@Entity(name = "AttachmentsContext")
@ExoEntity
@Table(name = "EXO_ATTACHMENTS_CONTEXT")
@NamedQueries(
  {
    @NamedQuery(
      name = "AttachmentsContext.getAttachmentContextByEntity",
      query = "SELECT ac FROM AttachmentsContext ac WHERE ac.entityId = :entityId AND ac.entityType = :entityType"
    )
  }
)
public class AttachmentsContextEntity {

  @Id
  @SequenceGenerator(name = "SEQ_ATTACHMENTS_CONTEXT_ID", sequenceName = "SEQ_ATTACHMENTS_CONTEXT_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_ATTACHMENTS_CONTEXT_ID")
  @Column(name = "ATTACHMENTS_CONTEXT_ID")
  private Long id;

  @Column(name = "ATTACHMENT_IDS")
  private String attachmentIds;

  @Column(name = "ENTITY_ID")
  private Long entityId;

  @Column(name = "ENTITY_TYPE")
  private String entityType;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAttachmentIds() {
    return attachmentIds;
  }

  public void setAttachmentIds(String attachmentIds) {
    this.attachmentIds = attachmentIds;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }
}
