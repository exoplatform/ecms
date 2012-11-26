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
package org.exoplatform.wcm.webui.utils;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * 22-06-2011
 */
public class Utils {

  /**
   * This method check whether to show the publish button for the current node or not
   * @param currentNode the input current node
   * @return <code>true</code> if the current node can be published,
   * otherwise <code>false</code>
   * @throws Exception
   */
  public static boolean isShowFastPublish(Node currentNode) throws Exception {
    if (currentNode.hasProperty("publication:currentState")
        && currentNode.hasProperty("publication:lifecycle")) {
      String currentState = currentNode.getProperty("publication:currentState").getString();

      if (!"published".equals(currentState)) {

        String userId;
        try {
          userId = Util.getPortalRequestContext().getRemoteUser();
        } catch (Exception e) {
          userId = currentNode.getSession().getUserID();
        }

        String nodeLifecycle = currentNode.getProperty("publication:lifecycle").getString();

        PublicationManager publicationManager = WCMCoreUtils.getService(PublicationManager.class);
        List<Lifecycle> lifecycles =
          publicationManager.getLifecyclesFromUser(userId, "published");

        for (Lifecycle lifecycle : lifecycles) {
          if (nodeLifecycle.equals(lifecycle.getName())) {
            return true;
          }
        }
      }

    }
    return false;
  }
}
