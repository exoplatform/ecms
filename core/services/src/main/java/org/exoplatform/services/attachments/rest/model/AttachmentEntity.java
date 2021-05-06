package org.exoplatform.services.attachments.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.services.attachments.model.Permission;
import org.exoplatform.social.rest.entity.IdentityEntity;

import java.util.LinkedHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentEntity {
  private String id;
  private String title;
  private long size;
  private String mimetype;
  private String path;
  private Boolean isPublic;
  private Permission acl;
  private IdentityEntity creator;
  private String created;
  private IdentityEntity updater;
  private String updated;
  private String downloadUrl;
  private String openUrl;
  private LinkedHashMap<String, String> previewBreadcrumb;
  private String version;

  @Override
  public AttachmentEntity clone() { // NOSONAR
    return new AttachmentEntity(id,
            title,
            size,
            mimetype,
            path,
            isPublic,
            acl,
            creator,
            created,
            updater,
            updated,
            downloadUrl,
            openUrl,
            previewBreadcrumb,
            version);
  }

}
