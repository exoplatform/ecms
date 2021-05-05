package org.exoplatform.services.attachments.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Attachment implements Cloneable {
  private String id;
  private String title;
  private long size;
  private String mimetype;
  private String path;
  private Boolean isPublic;
  private Permission acl;
  private long creatorId;
  private String created;
  private String updater;
  private String updated;
  private String downloadUrl;
  private String openUrl;
  private LinkedHashMap<String, String> previewBreadcrumb;
  private String version;

  @Override
  public Attachment clone() { // NOSONAR
    return new Attachment(id,
            title,
            size,
            mimetype,
            path,
            isPublic,
            acl,
            creatorId,
            created,
            updater,
            updated,
            downloadUrl,
            openUrl,
            previewBreadcrumb,
            version);
  }
}
