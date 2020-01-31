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

import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 17 Jan 2013  
 */
public class PublicationStateActivityListener extends Listener<Node, String> {
  private String[] handledState = {"pending", "approved", "staged", "published"};
  private String   bundlePrefix = "SocialIntegration.messages.stateChange.";
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    String stateName = event.getData();
    ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
    Node currentNode = event.getSource();
    Node parent = currentNode.getParent();
    for (int i=0; i< handledState.length; i++) {
      if (handledState[i].equals(stateName)) {
      	if(!currentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) || 
      			(currentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && 
      					activityService.isBroadcastNTFileEvents(currentNode)))
        Utils.postActivity(currentNode, bundlePrefix + handledState[i], true, true, "", "");
      }
    }
  }
}
