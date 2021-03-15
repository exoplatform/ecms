package org.exoplatform.services.attachments.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.services.attachments.model.AttachmentsEntityType;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentsContext  implements Serializable, Cloneable{
  private static final long serialVersionUID = -8173593686976273638L;
  private long id;
  private List<String> attachmentIds;
  private long entityId;
  private AttachmentsEntityType attachmentsEntityType;

  @Override
  public AttachmentsContext clone() {// NOSONAR
    return new AttachmentsContext(id, attachmentIds, entityId, attachmentsEntityType);
  }

}
