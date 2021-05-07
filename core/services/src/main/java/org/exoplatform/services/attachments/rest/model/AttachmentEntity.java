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
  private String                        id;

  private String                        title;

  private long                          size;

  private String                        mimetype;

  private String                        path;

  private Boolean                       isPublic;

  private Permission                    acl;

  private IdentityEntity                creator;

  private String                        created;

  private IdentityEntity                updater;

  private String                        updated;

  private String                        downloadUrl;

  private String                        openUrl;

  private LinkedHashMap<String, String> previewBreadcrumb;

  private String                        version;

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
