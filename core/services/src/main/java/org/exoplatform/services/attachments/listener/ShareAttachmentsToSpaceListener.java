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
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.services.listener.Listener;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShareAttachmentsToSpaceListener extends Listener<Map<String, Object>, Space> {

  private static final Log             LOG                  =
                                           ExoLogger.getLogger(ShareAttachmentsToSpaceListener.class.getName());

  private final RepositoryService      repositoryService;

  private final SessionProviderService sessionProviderService;

  public static final String           NEWS_ATTACHMENTS_IDS = "attachmentsIds";

  public static final String           QUARANTINE           = "/Quarantine/";

  public static final String           CONTENT              = "content";

  public static final String           SPACE                = "space";

  public static final String           AUDIENCE             = "audience";

  private static final String          IMAGE_SRC_REGEX      = "src=\"/portal/rest/images/?(.+)?\"";
  public ShareAttachmentsToSpaceListener(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    Map<String, Object> data = (Map<String, Object>) event.getData();
    List<String> attachmentsIds = (List<String>) data.get(NEWS_ATTACHMENTS_IDS);
    String audience = (String) data.get(AUDIENCE);
    Space space = (Space) data.get(SPACE);
    String content = (String) data.get(CONTENT);
    if (content != null) {
      attachmentsIds.addAll(extractImageAttachmentUuidOrPath(content));
    }
    updateNodePermissions(attachmentsIds, audience, space);
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
      String imagePath = srcStringPart.substring(srcStringPart.indexOf(repositoryAndWorkSpaceSuffix) + repositoryAndWorkSpaceSuffix.length());
      imageNodeIds.add(imagePath);
    }
    return imageNodeIds;
  }
  private void updateNodePermissions(List<String> identifiers,String audience, Space space) throws RepositoryException {
    Session session = Utils.getSystemSession(sessionProviderService, repositoryService);
    if (!CollectionUtils.isEmpty(identifiers)) {
      for (String attachmentId : identifiers) {
        try {
          Node attachmentNode;
          boolean isNodeUUID = !attachmentId.contains("/");
          if (isNodeUUID) {
            attachmentNode = session.getNodeByUUID(attachmentId);
          } else {
            attachmentNode = (Node) session.getItem(URLDecoder.decode(attachmentId, StandardCharsets.UTF_8));
          }
          if (!attachmentNode.getPath().startsWith(QUARANTINE)) {
            if (attachmentNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
              attachmentNode.addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
            }
            // make the attachment public
            if (audience.equals("all")) {
              ((ExtendedNode) attachmentNode).setPermission("*:/platform/users", new String[] { PermissionType.READ });
              attachmentNode.save();
            }
            // share the attachment with the space
            else if (space != null) {
              ((ExtendedNode) attachmentNode).setPermission("*:" + space.getGroupId(), new String[] { PermissionType.READ });
              attachmentNode.save();
            }
          }
        } catch (Exception e) {
          LOG.error("Error while sharing attachment of id : {} to space: {}", attachmentId, space.getDisplayName(), e);
        }
      }
    }

  }
}
