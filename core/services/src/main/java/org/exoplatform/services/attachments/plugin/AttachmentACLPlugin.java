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
package org.exoplatform.services.attachments.plugin;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.security.Identity;

/**
 * A plugin that will be used by AttachmentService to check ACL of attachments
 */
public abstract class AttachmentACLPlugin extends BaseComponentPlugin {

  /**
   * @return entity types that plugin handles
   */
  public abstract String getEntityType();

  /**
   * Checks whether the user has access to a given entity
   *
   * @param identity user {@link Identity} model
   * @param entityType entity type where the attachment has been added
   * @param entityId entity identifier on which the attachment has been added
   * @return true if the user can view the attachments of entity, else false.
   */
  public abstract boolean canView(Identity identity, String entityType, String entityId);

  /**
   * Checks whether the user can delete a given entity
   *
   * @param identity user {@link Identity} model
   * @param entityType entity type where the attachment has been added
   * @param entityId entity identifier on which the attachment has been added
   * @return true if the user can delete the attachment from entity, else false.
   */
  public abstract boolean canDelete(Identity identity, String entityType, String entityId);

}
