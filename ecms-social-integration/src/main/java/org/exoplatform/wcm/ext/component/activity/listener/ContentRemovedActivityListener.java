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
package org.exoplatform.wcm.ext.component.activity.listener;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.utils.ActivityTypeUtils;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler for ContentRemoved event
 * 26 Feb 2013  
 */
public class ContentRemovedActivityListener extends Listener<Node, String>{
    private static final Log LOG               = ExoLogger.getExoLogger(ContentRemovedActivityListener.class);

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node currentNode = event.getSource();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    String nodeActivityID = StringUtils.EMPTY;
    if (currentNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = currentNode.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        activityManager.deleteActivity(nodeActivityID);
      }catch (Exception e){
          LOG.info("No activity is deleted, return no related activity");
      }
    }
  }
}
