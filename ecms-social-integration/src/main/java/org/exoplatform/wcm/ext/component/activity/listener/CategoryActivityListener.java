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

import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler Category Add/Remove event
 * 15 Jan 2013  
 */
public class CategoryActivityListener extends Listener<Node, String> {
  private static String CATEGORY_ADDED_BUNDLE        = "SocialIntegration.messages.categoryAdded";
  private static String CATEGORY_REMOVED_BUNDLE      =  "SocialIntegration.messages.categoryRemoved";
  
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node currentNode = event.getSource();
    String categoryName = event.getData();
    String eventName = event.getEventName();
    String bundleMessage = eventName.equals(ActivityCommonService.CATEGORY_ADDED_ACTIVITY) ?
                           CATEGORY_ADDED_BUNDLE:CATEGORY_REMOVED_BUNDLE;
    if (eventName.equals(ActivityCommonService.CATEGORY_ADDED_ACTIVITY) || eventName.equals(ActivityCommonService.CATEGORY_REMOVED_ACTIVITY)) {
      Utils.postActivity(currentNode, bundleMessage, false, true, categoryName, "");
    }
  }

}
