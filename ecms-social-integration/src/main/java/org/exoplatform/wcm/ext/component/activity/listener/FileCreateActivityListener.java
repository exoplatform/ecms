/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity.listener;

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.manager.ActivityManager;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileCreateActivityListener extends Listener<Object, Node> {
  
  private static final String RESOURCE_BUNDLE_KEY_CREATED_BY = "SocialIntegration.messages.createdBy";
  private static final String FILES_SPACES = "files:spaces";
  private static final String CREATION_COMMENT = "files:spaces.CREATION_COMMENT";

  /**
   * Instantiates a new post create content event listener.
   */
  public FileCreateActivityListener() {
  }

  @Override
  public void onEvent(Event<Object, Node> event) throws Exception {
    Node currentNode = event.getData();
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    if(activityManager.isActivityTypeEnabled(FILES_SPACES) && activityManager.isActivityTypeEnabled(CREATION_COMMENT)) {
      Utils.postFileActivity(currentNode, RESOURCE_BUNDLE_KEY_CREATED_BY, true, true, "", "");
    }
  }
}
