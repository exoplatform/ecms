/*
* Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.wcm.notification.plugin;

import java.util.*;

import javax.jcr.Node;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.LinkProviderUtils;


public class ShareFileToSpacePlugin extends BaseNotificationPlugin {
  private static final Log LOG                          = ExoLogger.getLogger(ShareFileToSpacePlugin.class);

  public static final String ID                           = "ShareFileToSpacePlugin";

  public final static String DOCUMENT_URL = "documentUrl" ;
  public final static String SPACE_URL = "spaceUrl" ;
  public final static String DOCUMENT_NAME = "documentName" ;
  public final static String SPACE_NAME = "spaceName" ;
  public final static String DOCUMENT_ICON = "documentIcon" ;
  public final static String COMMENT = "comment" ;
  public final static String PERMISSION = "permission" ;
  public static final String REPLY = "reply";
  public static final String FULL_DISUSSION = "fullDiscussion";
  public static final String TYPE = "type";
  public static final String NODE_ID = "nodeId";

  public static ArgumentLiteral<Node> NODE = new ArgumentLiteral<Node>(Node.class, "node");;
  public static ArgumentLiteral<String> SENDER = new ArgumentLiteral<String>(String.class, "sender");;
  public static ArgumentLiteral<String> NODEID = new ArgumentLiteral<String>(String.class, "nodeId");;
  public static ArgumentLiteral<String> RECEIVER = new ArgumentLiteral<String>(String.class, "receiver");;
  public static ArgumentLiteral<String> PERM = new ArgumentLiteral<String>(String.class, "perm");;
  public static ArgumentLiteral<String> URL = new ArgumentLiteral<String>(String.class, "url");
  public static ArgumentLiteral<String> MESSAGE = new ArgumentLiteral<String>(String.class, "message");;
  public static ArgumentLiteral<String> ICON = new ArgumentLiteral<String>(String.class, "icon");;
  public static ArgumentLiteral<String> ACTIVITY_ID = new ArgumentLiteral<String>(String.class, "activityId");;
  public static ArgumentLiteral<String> MIMETYPE = new ArgumentLiteral<String>(String.class, "mimeType");;

  public ShareFileToSpacePlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {

    String receiver = ctx.value(RECEIVER);
    String sender = ctx.value(SENDER);
    Node node = ctx.value(NODE);
    String activityId = ctx.value(ACTIVITY_ID);

    Space space = WCMCoreUtils.getService(SpaceService.class).getSpaceByGroupId(receiver);
    List<String> list = new LinkedList<String>(Arrays.asList(space.getMembers()));
    list.remove(sender);

    try {
      return NotificationInfo.instance()
          .setFrom(sender)
          .to(list)
          .with(NODE_ID, ctx.value(NODEID))
          .with(DOCUMENT_URL, ctx.value(URL))
          .with(SPACE_URL, LinkProviderUtils.getRedirectUrl("space", space.getId()))
          .with(DOCUMENT_NAME, node.getName())
          .with(SPACE_NAME, space.getDisplayName())
          .with(DOCUMENT_ICON, ctx.value(ICON))
          .with(PERMISSION, ctx.value(PERM))
          .with(COMMENT, ctx.value(MESSAGE))
          .with(TYPE, ctx.value(MIMETYPE))
          .with(REPLY, getReplyLink(activityId))
          .with(FULL_DISUSSION, getDiscussionLink(activityId))
          .key(getId()).end();
    }  catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return NotificationInfo.instance()
        .setFrom(ctx.value(SENDER))
        .to(receiver)
        .with(COMMENT, ctx.value(MESSAGE))
        .key(getId()).end();
  }

  private String getDiscussionLink(String activityId) {
    return LinkProviderUtils.getRedirectUrl("view_full_activity", activityId);
  }

  private String getReplyLink(String activityId) {
    return LinkProviderUtils.getRedirectUrl("reply_activity", activityId);
  }

  public static String capitalizeFirstLetter(String word) {
    if (word == null) {
      return null;
    }
    if (word.length() == 0) {
      return word;
    }
    StringBuilder result = new StringBuilder(word);
    result.replace(0, 1, result.substring(0, 1).toUpperCase());
    return result.toString();
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

}
