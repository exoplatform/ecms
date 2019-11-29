package org.exoplatform.wcm.ext.component.activity.listener;
import java.util.Map;

import javax.jcr.Node;

/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
 */
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.utils.ActivityTypeUtils;
import org.exoplatform.wcm.ext.component.activity.ContentUIActivity;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler Comment Updated event
 * 30 Jan 2013  
 */
public class CommentUpdatedActivityListener extends Listener<Node, Node> {
  @Override
  public void onEvent(Event<Node, Node> event) throws Exception {
    Node commentNode = event.getData();
    String commentContent = "";
    if (commentNode.hasProperty("exo:commentContent")) {
      try {
        commentContent = commentNode.getProperty("exo:commentContent").getValue().getString();
      }catch (Exception e) {
        commentContent =null;
      }
    }
    if (commentContent==null) return;
    try {
      Utils.setAvatarUrl(commentNode);
    }catch (Exception e) {
      commentNode.setProperty("exo:commentorAvatar", Utils.DEFAULT_AVATAR);
    }
    commentContent = commentContent.replaceAll("&#64;","@");
    String activityID = ActivityTypeUtils.getActivityId(commentNode);
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    ExoSocialActivity commentActivity =null;
    try {
      commentActivity = activityManager.getActivity(activityID);
    }catch (Exception e) {
      //CommentActivity's deleted do not update anymore
      return;
    }
    Map<String, String> paramsMap = commentActivity.getTemplateParams();
    commentActivity.setTitle(commentContent);
    paramsMap.put(ContentUIActivity.SYSTEM_COMMENT, commentContent);
    commentActivity.setTemplateParams(paramsMap);
    activityManager.updateActivity(commentActivity);
    commentContent = Utils.processMentions(commentContent);
    commentNode.setProperty("exo:commentContent", commentContent);
    commentNode.save();
    commentNode.getSession().save();
  }
}
