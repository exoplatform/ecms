/*
 * Copyright (C) 2020 eXo Platform SAS.
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
package org.exoplatform.services.attachments.model;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "AttachmentsContext")
@ExoEntity
@Table(name = "EXO_ATTACHMENTS_CONTEXT")
@NamedQueries({
    @NamedQuery(name = "AttachmentsContext.getAttachmentContextByEntity", query = "SELECT ac.attachmentId FROM AttachmentsContext ac "
        + "WHERE ac.entityId = :entityId AND ac.entityType = :entityType"),
    @NamedQuery(name = "AttachmentsContext.getAttachmentItemByEntity", query = "SELECT ac FROM AttachmentsContext ac "
        + "WHERE ac.entityId = :entityId AND ac.entityType = :entityType AND ac.attachmentId = :attachmentId") })
public class AttachmentsContextEntity implements Serializable {

  private static final long serialVersionUID = -6445215481619188461L;

  @Id
  @SequenceGenerator(name = "SEQ_ATTACHMENTS_CONTEXT_ID", sequenceName = "SEQ_ATTACHMENTS_CONTEXT_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_ATTACHMENTS_CONTEXT_ID")
  @Column(name = "ATTACHMENTS_CONTEXT_ID")
  private Long id;

  @Column(name = "ATTACHMENT_IDS")
  private String attachmentId;

  @Column(name = "ENTITY_ID")
  private Long entityId;

  @Column(name = "ENTITY_TYPE")
  private int entityType;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public void setAttachmentId(String attachmentId) {
    this.attachmentId = attachmentId;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public int getEntityType() {
    return entityType;
  }

  public void setEntityType(int entityType) {
    this.entityType = entityType;
  }
}
