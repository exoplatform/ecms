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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.social.space.security.SpaceSecurityAudit;
import io.meeds.social.space.security.model.SpaceSecuritySettings;
import io.meeds.social.space.security.service.SpaceSecurityService;

/**
 * Decides, from the document as stored, which space a document belongs to,
 * and opens the operations that decide whether the platform may grant access
 * to it on a user's behalf under the space's security settings.
 */
public class DocumentSpaceGuard {

  public static final String   EXO_RESTORE_PATH          = "exo:restorePath";

  public static final String   ACCESS_MANAGEMENT_LIMITED = "documents.space.accessManagementLimited";

  public static final String   CHANGE_PERMISSIONS_ACTION = "changePermissions";

  private static final String  EXO_PRIVILEGEABLE         = "exo:privilegeable";

  private static final Log     LOG                       = ExoLogger.getLogger(DocumentSpaceGuard.class);

  private static final String  SPACES_SEGMENT   = "/spaces/";

  /** The root of the documents the DLP quarantines, which carry their restore path */
  private static final String  QUARANTINE_ROOT  = "/Quarantine";

  private final SpaceService   spaceService;

  private final SpaceSecurityService spaceSecurityService;

  private final NodeHierarchyCreator nodeHierarchyCreator;

  private final LinkManager    linkManager;

  private final TrashService   trashService;

  private final RepositoryService repositoryService;

  public DocumentSpaceGuard(SpaceService spaceService,
                            SpaceSecurityService spaceSecurityService,
                            NodeHierarchyCreator nodeHierarchyCreator,
                            LinkManager linkManager,
                            TrashService trashService,
                            RepositoryService repositoryService) {
    this.spaceService = spaceService;
    this.spaceSecurityService = spaceSecurityService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.linkManager = linkManager;
    this.trashService = trashService;
    this.repositoryService = repositoryService;
  }

  /**
   * @return a guard over the platform's services
   */
  public static DocumentSpaceGuard getInstance() {
    return new DocumentSpaceGuard(CommonsUtils.getService(SpaceService.class),
                                  CommonsUtils.getService(SpaceSecurityService.class),
                                  CommonsUtils.getService(NodeHierarchyCreator.class),
                                  CommonsUtils.getService(LinkManager.class),
                                  CommonsUtils.getService(TrashService.class),
                                  CommonsUtils.getService(RepositoryService.class));
  }

  /**
   * Resolves the space a document belongs to from its stored path: a version
   * by its versionable node, a symlink by its target, a trashed or
   * quarantined node by the restore path of the node trashed or quarantined
   * with it, read through a system session. The path alone decides otherwise:
   * nothing above the document is read with the caller's session, which may
   * not read the folders above its own drive. A personal drive belongs to no
   * space; a sub-space is a space of its own.
   *
   * @param node a document, a version, a symlink or a folder
   * @return the space whose drive holds it, or null
   * @throws RepositoryException when the node, its target or its versionable
   *           node cannot be read: the caller fails closed
   */
  public Space getDocumentSpace(Node node) throws RepositoryException {
    Node document = node;
    if (document.isNodeType("nt:frozenNode")) {
      String versionableId = document.getProperty("jcr:frozenUuid").getString();
      document = ((ExtendedSession) document.getSession()).getNodeByIdentifier(versionableId);
    }
    if (linkManager.isLink(document)) {
      document = linkManager.getTarget(document, true);
    }
    String path = getStoredPath(document);
    String spacesRoot = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + SPACES_SEGMENT;
    if (path == null || !path.startsWith(spacesRoot)) {
      return null;
    }
    String groupName = StringUtils.substringBefore(path.substring(spacesRoot.length()), "/");
    return StringUtils.isBlank(groupName) ? null : spaceService.getSpaceByGroupId(SPACES_SEGMENT + groupName);
  }

  /**
   * @param actor the user on whose behalf the platform grants access, null
   *          when unknown
   * @param source the grant's source, reported when it is skipped
   * @return an operation deciding grants, reading each space's settings once
   */
  public DocumentGrantOperation newGrantOperation(String actor, String source) {
    return new DocumentGrantOperation(this, spaceSecurityService, actor, source);
  }

  /**
   * Decides, on the committed settings and rows, whether the user may change
   * the document's access list under its space's "Limit Access Management"
   * setting. A refusal is logged with its channel; a document whose space or
   * settings cannot be read is refused.
   *
   * @param document a document, a version, a symlink or a folder
   * @param username the user changing the access list
   * @param channel the channel of the change, reported on a refusal; null to
   *          decide without reporting
   * @return true when the change may be made
   */
  public boolean canManageAccess(Node document, String username, String channel) {
    try {
      Space space = getDocumentSpace(document);
      if (space == null) {
        return true;
      }
      SpaceSecuritySettings settings = spaceSecurityService.getSettings(space.getSpaceId());
      if (spaceSecurityService.canManageDocumentsAccess(space, settings, username)) {
        return true;
      }
      if (channel == null) {
        return false;
      }
      SpaceSecurityAudit.actionRefused(space.getSpaceId(),
                                       ((ExtendedNode) document).getIdentifier(),
                                       CHANGE_PERMISSIONS_ACTION,
                                       username,
                                       channel);
      return false;
    } catch (Exception e) {
      LOG.warn("Access list change refused: the document's space settings could not be read", e);
      return false;
    }
  }

  /**
   * @param node a node
   * @return the node's own access list, identity by identity, or null when it
   *         has none and reads its parent's
   * @throws RepositoryException when the list cannot be read
   */
  public Map<String, List<String>> getAccessList(Node node) throws RepositoryException {
    if (!node.isNodeType(EXO_PRIVILEGEABLE)) {
      return null;
    }
    Map<String, List<String>> accessList = new LinkedHashMap<>();
    for (AccessControlEntry entry : ((ExtendedNode) node).getACL().getPermissionEntries()) {
      accessList.computeIfAbsent(entry.getIdentity(), identity -> new ArrayList<>()).add(entry.getPermission());
    }
    return accessList;
  }

  /**
   * Puts back an access list read before a change, through the node's
   * permission API, and saves the node. A change such as a version restore may
   * have taken the node's own access list away, or given it one: the node gets
   * one again, or reads its parent's again. A node a restore left checked in
   * is checked out to be written, as documents are kept.
   *
   * @param node a node
   * @param accessList the access list to keep, null for the parent's
   * @throws RepositoryException when the list cannot be written
   */
  public void restoreAccessList(Node node, Map<String, List<String>> accessList) throws RepositoryException {
    Map<String, List<String>> current = getAccessList(node);
    if (Objects.equals(current, accessList)) {
      return;
    }
    if (!node.isCheckedOut()) {
      node.checkout();
    }
    if (accessList == null) {
      node.removeMixin(EXO_PRIVILEGEABLE);
      node.save();
      return;
    }
    if (current == null) {
      node.addMixin(EXO_PRIVILEGEABLE);
      current = getAccessList(node);
    }
    ExtendedNode extendedNode = (ExtendedNode) node;
    for (String identity : current.keySet()) {
      if (!accessList.containsKey(identity)) {
        extendedNode.removePermission(identity);
      }
    }
    for (Map.Entry<String, List<String>> entry : accessList.entrySet()) {
      if (!entry.getValue().equals(current.get(entry.getKey()))) {
        extendedNode.setPermission(entry.getKey(), entry.getValue().toArray(new String[0]));
      }
    }
    node.save();
  }

  private String getStoredPath(Node document) throws RepositoryException {
    String path = document.getPath();
    String groupsRoot = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    if (path.startsWith(groupsRoot + "/")) {
      return path;
    }
    Node trashHome = trashService.getTrashHomeNode();
    for (String root : new String[] { trashHome == null ? null : trashHome.getPath(), QUARANTINE_ROOT }) {
      if (root != null && path.startsWith(root + "/")) {
        String restoredPath = root + "/" + StringUtils.substringBefore(path.substring(root.length() + 1), "/");
        String restorePath = getRestorePath(document.getSession().getWorkspace().getName(), restoredPath);
        return restorePath == null ? path : restorePath + path.substring(restoredPath.length());
      }
    }
    return path;
  }

  /**
   * Reads the restore path of a trashed or quarantined node through a system
   * session
   *
   * @param workspace the node's workspace
   * @param path the node trashed or quarantined, directly under its root
   * @return its restore path, or null when it carries none
   * @throws RepositoryException when the node cannot be read
   */
  protected String getRestorePath(String workspace, String path) throws RepositoryException {
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      Session session = systemProvider.getSession(workspace, repositoryService.getCurrentRepository());
      if (!session.itemExists(path)) {
        return null;
      }
      Node node = (Node) session.getItem(path);
      return node.hasProperty(EXO_RESTORE_PATH) ? node.getProperty(EXO_RESTORE_PATH).getString() : null;
    } finally {
      systemProvider.close();
    }
  }

}
