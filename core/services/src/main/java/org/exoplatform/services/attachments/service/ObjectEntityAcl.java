/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.attachments.service;

import io.meeds.portal.plugin.AclPlugin;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * The portal {@link AclPlugin} of an object type: view is VIEW, edit is EDIT
 * and detach is EDIT as well, since DELETE is the right to delete the object
 * itself. An entity that is not created yet (no positive id) has no object to
 * check.
 */
final class ObjectEntityAcl implements EntityAcl {
  private final UserACL         acl;

  private final IdentityManager identityManager;

  private final String          objectType;

  ObjectEntityAcl(UserACL acl, IdentityManager identityManager, String objectType) {
    this.acl = acl;
    this.identityManager = identityManager;
    this.objectType = objectType;
  }

  @Override
  public boolean canView(long userIdentityId, long entityId) {
    return hasPermission(userIdentityId, entityId, AclPlugin.VIEW_PERMISSION_TYPE);
  }

  @Override
  public boolean canEdit(long userIdentityId, long entityId) {
    return hasPermission(userIdentityId, entityId, AclPlugin.EDIT_PERMISSION_TYPE);
  }

  @Override
  public boolean canDetach(long userIdentityId, long entityId) {
    return hasPermission(userIdentityId, entityId, AclPlugin.EDIT_PERMISSION_TYPE);
  }

  @Override
  public boolean requiresJcrToDetach() {
    return true;
  }

  private boolean hasPermission(long userIdentityId, long entityId, String permissionType) {
    if (entityId <= 0) {
      return true;
    }
    Identity socialIdentity = identityManager.getIdentity(String.valueOf(userIdentityId));
    if (socialIdentity == null) {
      return false;
    }
    org.exoplatform.services.security.Identity identity = acl.getUserIdentity(socialIdentity.getRemoteId());
    return identity != null && acl.hasPermission(objectType, String.valueOf(entityId), permissionType, identity);
  }
}
