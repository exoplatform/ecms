/*
 * Copyright (C) 2024 eXo Platform SAS.
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
package org.exoplatform.services.attachments.listener;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.documents.security.DocumentGrantOperation;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.services.listener.Listener;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateContentPermissionsListener extends Listener<Map<String, Object>, Space> {

  private final RepositoryService      repositoryService;

  private final AttachmentStorage attachmentStorage;


  public static final String           QUARANTINE           = "/Quarantine/";

  public static final String           CONTENT              = "content";

  public static final String           SPACES               = "spaces";

  public static final String           AUDIENCE             = "audience";

  /** The user acting on the content, added to the event data by its broadcaster. */
  public static final String           USERNAME             = "username";

  private static final String          IMAGE_SRC_REGEX      = "src=\"/portal/rest/images/?(.+)?\"";


  public UpdateContentPermissionsListener(RepositoryService repositoryService, AttachmentStorage attachmentStorage) {
    this.repositoryService = repositoryService;
    this.attachmentStorage = attachmentStorage;
  }

  @Override
  public void onEvent(Event event) throws Exception {

    SessionProviderService sessionProviderService = ExoContainerContext.getService(SessionProviderService.class);
    SessionProvider currentSessionProvider = sessionProviderService.getSessionProvider(null);
    // set system session provider to avoid SessionProviderService NullPointerException with spring context
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    sessionProviderService.setSessionProvider(null, sessionProvider);
    try {
      Map<String, Object> data = (Map<String, Object>) event.getData();
      List<String> attachmentsIds = new ArrayList<>();
      String entityType = data.containsKey("entityType") ? (String) data.get("entityType") : null;
      String entityId = data.containsKey("entityId") ? (String) data.get("entityId") : null;
      if (StringUtils.isNotEmpty(entityId) && StringUtils.isNotEmpty(entityType)) {
        attachmentsIds.addAll(attachmentStorage.getAttachmentsByEntity(Long.valueOf(entityId), entityType).stream().map(Attachment::getId).toList());
      }
      String audience = (String) data.get(AUDIENCE);
      List<Space> spaces = (List<Space>) data.get(SPACES);
      String content = (String) data.get(CONTENT);
      if (content != null) {
        attachmentsIds.addAll(extractImageAttachmentUuidOrPath(content));
      }
      String username = (String) data.get(USERNAME);
      updateNodePermissions(attachmentsIds,
                            audience,
                            spaces,
                            sessionProviderService,
                            getDocumentSpaceGuard().newGrantOperation(username, DocumentGrantOperation.SOURCE_NEWS));
    } finally {
      // Give the rest of the caller's thread back the session provider it had
      sessionProviderService.setSessionProvider(null, currentSessionProvider);
      sessionProvider.close();
    }
  }

  protected DocumentSpaceGuard getDocumentSpaceGuard() {
    return DocumentSpaceGuard.getInstance();
  }

  private List<String> extractImageAttachmentUuidOrPath(String content) {
    List<String> imageNodeIds = new ArrayList<>();
    Matcher matcher = Pattern.compile(IMAGE_SRC_REGEX).matcher(content);
    while (matcher.find()) {
      String srcStringPart = matcher.group(1);
      if (srcStringPart.contains("\"")) {
        // content has many images
        matcher = Pattern.compile(IMAGE_SRC_REGEX).matcher(srcStringPart);
        srcStringPart = srcStringPart.substring(0, srcStringPart.indexOf("\""));
      }
      String attachmentNodeId = srcStringPart.substring(srcStringPart.lastIndexOf("/") + 1);
      imageNodeIds.add(attachmentNodeId);
    }
    StringBuilder existingUploadImagesSrcRegex = new StringBuilder();
    existingUploadImagesSrcRegex.append("src=\"");
    existingUploadImagesSrcRegex.append(CommonsUtils.getCurrentDomain());
    existingUploadImagesSrcRegex.append("/");
    existingUploadImagesSrcRegex.append(PortalContainer.getCurrentPortalContainerName());
    existingUploadImagesSrcRegex.append("/");
    existingUploadImagesSrcRegex.append(CommonsUtils.getRestContextName());
    existingUploadImagesSrcRegex.append("/jcr/?(.+)?\"");

    String repositoryAndWorkSpaceSuffix = "/repository/collaboration";
    Matcher pathMatcher = Pattern.compile(existingUploadImagesSrcRegex.toString()).matcher(content);
    while (pathMatcher.find()) {
      String srcStringPart = pathMatcher.group(1);
      if (srcStringPart.contains("\"")) {
        // content has many than images
        pathMatcher = Pattern.compile(existingUploadImagesSrcRegex.toString()).matcher(srcStringPart);
        srcStringPart = srcStringPart.substring(0, srcStringPart.indexOf("\""));
      }
      String imagePath = srcStringPart.substring(srcStringPart.indexOf(repositoryAndWorkSpaceSuffix)
          + repositoryAndWorkSpaceSuffix.length());
      imageNodeIds.add(imagePath);
    }
    return imageNodeIds;
  }

  private void updateNodePermissions(List<String> identifiers,
                                     String audience,
                                     List<Space> spaces,
                                     SessionProviderService sessionProviderService,
                                     DocumentGrantOperation grantOperation) throws RepositoryException {

    Session session = Utils.getSystemSession(sessionProviderService, repositoryService);
    if (!CollectionUtils.isEmpty(identifiers)) {
      for (String attachmentId : identifiers) {
        Node attachmentNode;
        boolean isNodeUUID = !attachmentId.contains("/");
        if (isNodeUUID) {
          attachmentNode = session.getNodeByUUID(attachmentId);
        } else {
          attachmentNode = (Node) session.getItem(URLDecoder.decode(attachmentId, StandardCharsets.UTF_8));
        }
        if (!attachmentNode.getPath().startsWith(QUARANTINE)) {
          // A target whose own READ entry the node holds is due no grant:
          // nothing is decided for it. The node gets an access list of its
          // own only for a write: adding one copies its folder's entries as
          // its own, which a refused grant must not leave behind
          Set<String> heldTargets = getHeldTargets(attachmentNode, spaces, audience);
          if (spaces != null) {
            for (Space space : spaces) {
              String target = "*:" + space.getGroupId();
              if (heldTargets.contains(target) || grantOperation.canGrant(attachmentNode, target)) {
                grantRead(attachmentNode, target);
              }
            }
          }
          // An event without an audience, a new article's draft for one,
          // leaves the attachment's "any" entry as it is
          if ("all".equals(audience)) {
            if (heldTargets.contains(IdentityConstants.ANY) || grantOperation.canGrant(attachmentNode, IdentityConstants.ANY)) {
              grantRead(attachmentNode, IdentityConstants.ANY);
            }
          } else if (audience != null && readsAny(attachmentNode)) {
            // Removing an entry the node reads from its folder gives it an
            // access list of its own, without that entry
            addAccessList(attachmentNode);
            ((ExtendedNode) attachmentNode).removePermission(IdentityConstants.ANY, PermissionType.READ);
            attachmentNode.save();
          }
        }
      }
    }
  }

  private static void grantRead(Node node, String target) throws RepositoryException {
    addAccessList(node);
    ((ExtendedNode) node).setPermission(target, new String[] { PermissionType.READ });
    node.save();
  }

  /**
   * Gives the node an access list of its own, a copy of its folder's, when it
   * has none
   */
  private static void addAccessList(Node node) throws RepositoryException {
    if (node.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
      node.addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
    }
  }

  /**
   * @return true when the node's access list, its own or its folder's, lets
   *         "any" read it
   */
  private static boolean readsAny(Node node) throws RepositoryException {
    AccessControlList accessList = ((ExtendedNode) node).getACL();
    return accessList != null && accessList.getPermissions(IdentityConstants.ANY).contains(PermissionType.READ);
  }

  /**
   * @return the targets of the grants whose own READ entry the node holds,
   *         read before the node gets an access list of its own: adding one
   *         copies its folder's entries as the node's own
   */
  private static Set<String> getHeldTargets(Node node, List<Space> spaces, String audience) {
    Set<String> targets = new HashSet<>();
    if (spaces != null) {
      spaces.forEach(space -> targets.add("*:" + space.getGroupId()));
    }
    if ("all".equals(audience)) {
      targets.add(IdentityConstants.ANY);
    }
    targets.removeIf(target -> !DocumentGrantOperation.holdsRead(node, target));
    return targets;
  }
}
