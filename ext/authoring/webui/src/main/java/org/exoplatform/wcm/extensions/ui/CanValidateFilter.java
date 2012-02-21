/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.extensions.ui;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 2 juillet 2009
 */
public class CanValidateFilter implements UIExtensionFilter {

  private static final Log LOG = ExoLogger.getLogger(CanValidateFilter.class);
  /**
   * This method checks if the current node is of the right type
   */
  public boolean accept(Map<String, Object> context) throws Exception {
    // Retrieve the current node from the context
      Node currentNode = (Node) context.get(Node.class.getName());
      if (currentNode.hasProperty("publication:currentState") && currentNode.hasProperty("publication:lifecycle")) {
        String currentState = currentNode.getProperty("publication:currentState").getString();

        if ("draft".equals(currentState)) {
          String userId;
          try {
            userId = Util.getPortalRequestContext().getRemoteUser();
          } catch (Exception e) {
            userId = currentNode.getSession().getUserID();
          }

          String nodeLifecycle = currentNode.getProperty("publication:lifecycle").getString();

          PublicationManager publicationManager = (PublicationManager)ExoContainerContext.getCurrentContainer().
              getComponentInstanceOfType(PublicationManager.class);
          List<Lifecycle> lifecycles = publicationManager.getLifecyclesFromUser(userId, "pending");

          for (Lifecycle lifecycle:lifecycles) {
            if (nodeLifecycle.equals(lifecycle.getName())) {
              return true;
            }
          }

        }
      }
    return false;
  }

    /**
   * This is the type of the filter
   */
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  /**
   * This is called when the filter has failed
   */
  public void onDeny(Map<String, Object> context) throws Exception {
    if (LOG.isWarnEnabled()) {
      LOG.warn("You can add a category in a exo:taxonomy node only.");
    }
  }
}
