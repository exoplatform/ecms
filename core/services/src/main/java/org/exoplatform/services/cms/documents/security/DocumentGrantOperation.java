/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
*/
package org.exoplatform.services.cms.documents.security;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;

import io.meeds.social.space.security.SpaceSecurityAudit;
import io.meeds.social.space.security.model.SpaceSecuritySettings;
import io.meeds.social.space.security.service.SpaceSecurityService;

/**
 * One operation's decisions on the grants the platform writes on a user's
 * behalf: an activity shared or posted in another space, a news published, a
 * document attached to a task. Each space's settings, and whether the actor
 * may manage its documents' access, are read once for the operation, from the
 * committed rows. A grant whose decision cannot be read is skipped.
 */
public class DocumentGrantOperation {

  public static final String           SOURCE_ACTIVITY_SHARE = "activityShare";

  public static final String           SOURCE_ACTIVITY_POST  = "activityPost";

  public static final String           SOURCE_NEWS           = "news";

  public static final String           SOURCE_TASK           = "task";

  public static final String           SOURCE_ONLYOFFICE_RENAME = "onlyofficeRename";

  private static final String          EXO_PRIVILEGEABLE     = "exo:privilegeable";

  private static final Log             LOG                   = ExoLogger.getLogger(DocumentGrantOperation.class);

  private final DocumentSpaceGuard     guard;

  private final SpaceSecurityService   spaceSecurityService;

  private final String                 actor;

  private final String                 source;

  private final Map<Long, Boolean>     grantAllowedBySpace   = new HashMap<>();

  DocumentGrantOperation(DocumentSpaceGuard guard, SpaceSecurityService spaceSecurityService, String actor, String source) {
    this.guard = guard;
    this.spaceSecurityService = spaceSecurityService;
    this.actor = actor;
    this.source = source;
  }

  /**
   * @param document the document the grant gives access to
   * @param target the identity the grant gives access to, as written on the
   *          access list
   * @return true when the grant may be written; false when the document's
   *         space refuses it, which is logged as a skipped grant
   */
  public boolean canGrant(Node document, String target) {
    Space space;
    String documentId = getDocumentId(document);
    try {
      space = guard.getDocumentSpace(document);
    } catch (Exception e) {
      LOG.warn("Grant of document {} to {} skipped: the document's space could not be read", documentId, target, e);
      return false;
    }
    if (space == null) {
      return true;
    }
    boolean allowed = grantAllowedBySpace.computeIfAbsent(space.getSpaceId(), spaceId -> isGrantAllowed(space));
    if (!allowed) {
      SpaceSecurityAudit.grantSkipped(space.getSpaceId(), documentId, target, source);
    }
    return allowed;
  }

  /**
   * @param document a document
   * @param target an identity, as written on the access list
   * @return true when the document's own access list holds the target's own
   *         READ entry, so that a READ grant to it is not due. Another entry
   *         that lets the target read does not count: "any", or an entry of
   *         the folder whose list a document without its own reads. A target
   *         entry written on its strength would outlive its removal
   */
  public static boolean holdsRead(Node document, String target) {
    try {
      if (!(document instanceof ExtendedNode extendedNode) || !document.isNodeType(EXO_PRIVILEGEABLE)) {
        return false;
      }
      AccessControlList accessList = extendedNode.getACL();
      return accessList != null && accessList.getPermissions(target).contains(PermissionType.READ);
    } catch (RepositoryException e) {
      LOG.debug("Access list of document {} not read: a grant to {} is decided", document, target, e);
      return false;
    }
  }

  private boolean isGrantAllowed(Space space) {
    SpaceSecuritySettings settings;
    try {
      settings = spaceSecurityService.getSettings(space.getSpaceId());
    } catch (Exception e) {
      LOG.warn("Grants on documents of space {} skipped: its settings could not be read", space.getId(), e);
      return false;
    }
    return !settings.isLimitAccessManagementOn()
           || (actor != null && spaceSecurityService.canManageDocumentsAccess(space, settings, actor));
  }

  private String getDocumentId(Node document) {
    try {
      return ((ExtendedNode) document).getIdentifier();
    } catch (Exception e) {
      return null;
    }
  }

}
