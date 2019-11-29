package org.exoplatform.wcm.ext.component.activity.listener;
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
import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.utils.ActivityTypeUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler Comment Removed event
 * 30 Jan 2013  
 */
public class CommentRemovedActivityListener extends Listener<Node, String> {
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node commentNode  = event.getSource();
    Node sourceNode  = commentNode.getParent().getParent();
    String activityID = ActivityTypeUtils.getActivityId(sourceNode);
    String commentNodeActivityID = ActivityTypeUtils.getActivityId(commentNode);
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = container.getComponentInstanceOfType(ActivityManager.class);
    try {
      activityManager.deleteComment(activityID, commentNodeActivityID);
    }catch (Exception e) {
      //CommentActivity's deleted do not update anymore
      return;
    }
  }
}
