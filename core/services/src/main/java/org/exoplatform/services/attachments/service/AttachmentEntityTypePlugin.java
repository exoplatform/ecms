package org.exoplatform.services.attachments.service;


import org.exoplatform.container.component.BaseComponentPlugin;


import java.util.Collections;
import java.util.List;

import static org.exoplatform.services.attachments.utils.Utils.EMPTY_STRING;

/**
 * This class is the super-class that defines classes which provide the possibility
 * to customize how and where attachments will be saved after they are attached to a specific entity.
 */
public class AttachmentEntityTypePlugin extends BaseComponentPlugin {

  /**
   * Returns the ID of the attachment to link with the entity
   * @param entityType type of the entity
   * @param entityId ID of the entity
   * @param attachmentId the original attachment ID that may be changed after calling this function
   * @return the ID of the attachment to link with provided entity
   */
  public List<String> getlinkedAttachments(String entityType, long entityId, String attachmentId) {
    return Collections.singletonList(attachmentId);
  }

  /**
   * return the entity type that will be used to map the AttachmentEntityTypePlugin with the entity type
   * @return the entity type
   */
  public String getEntityType() {
    return EMPTY_STRING;
  }
}
