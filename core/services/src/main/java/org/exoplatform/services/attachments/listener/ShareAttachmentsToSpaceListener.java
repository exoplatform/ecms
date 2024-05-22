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
import javax.jcr.Session;
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

  private static final String          IMAGE_SRC_REGEX      =
                                                       "<img\\s+src=\"(/portal/rest/(?:images|jcr)/repository/collaboration/[^\"]*)\"";

  public ShareAttachmentsToSpaceListener(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    Map<String, Object> source = (Map<String, Object>) event.getSource();
    List<String> attachmentsIds = (List<String>) source.get(NEWS_ATTACHMENTS_IDS);
    String content = (String) source.get(CONTENT);
    if (content != null) {
      attachmentsIds.addAll(extractImageAttachmentIdsFromContent(content));
    }
    Space space = (Space) event.getData();
    if (!CollectionUtils.isEmpty(attachmentsIds) && space != null) {
      for (String attachmentId : attachmentsIds) {
        try {
          Session session = Utils.getSystemSession(sessionProviderService, repositoryService);
          Node attachmentNode = session.getNodeByUUID(attachmentId);
          if (!attachmentNode.getPath().startsWith(QUARANTINE)) {
            if (attachmentNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
              attachmentNode.addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
            }
            ((ExtendedNode) attachmentNode).setPermission("*:" + space.getGroupId(), new String[] { PermissionType.READ });
            attachmentNode.save();
          }
        } catch (Exception e) {
          LOG.error("Error while sharing attachment of id : {} to space: {}", attachmentId, space.getDisplayName(), e);
        }
      }
    }
  }

  private List<String> extractImageAttachmentIdsFromContent(String content) {
    List<String> attachmentIds = new ArrayList<>();
    Matcher matcher = Pattern.compile(IMAGE_SRC_REGEX).matcher(content);
    while (matcher.find()) {
      String match = matcher.group(1);
      String attachmentNodeId = match.substring(match.lastIndexOf("/") + 1);
      attachmentIds.add(attachmentNodeId);
    }
    return attachmentIds;
  }
}
