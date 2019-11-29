/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.social.utils;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Dec 24, 2012  
 */

public class ActivityTypeUtils {
  private static final Log LOG               = ExoLogger.getLogger(ActivityTypeUtils.class);

  public static String     EXO_ACTIVITY_INFO = "exo:activityInfo";

  public static String     EXO_ACTIVITY_ID   = "exo:activityId";

  /**
   * Defines Mixin type exo:activityInfo for node that means to add
   * exo:activityId property into Node what is owner created activity.
   * 
   * @param ownerNode - the node's attachment.
   * @param activityId - the activity's id.
   */
  public static void attachActivityId(Node ownerNode, String activityId) {
    Node verNode = null;
    try {
      /*
       * SOC-5359 when attach activityId to versionable node for example: ECMS
       * document node, we have to checkout before editing
       */
      if (!ownerNode.isCheckedOut()) {
        verNode = checkout(ownerNode);
      }
      if (ownerNode.isNodeType(EXO_ACTIVITY_INFO) == false && ownerNode.canAddMixin(EXO_ACTIVITY_INFO)) {
        ownerNode.addMixin(EXO_ACTIVITY_INFO);
      }
      ownerNode.setProperty(EXO_ACTIVITY_ID, activityId);
    } catch (RepositoryException e) {
      LOG.error("Failed to attach activityId " + activityId, e);
    } finally {
      if (verNode != null) {
        try {
          verNode.save();
          verNode.checkin();
        } catch (Exception ex) {
          LOG.error("Can't checkin node", ex);
        }
      }
    }
  }

  private static Node checkout(Node ownerNode) throws RepositoryException {
    if (ownerNode.isNodeType("mix:versionable")) {
      ownerNode.checkout();
      return ownerNode;
    } else {
      try {
        return checkout(ownerNode.getParent());
      } catch (ItemNotFoundException ex) {
        LOG.debug("no parent for root node");
        return null;
      }
    }
  }

  /**
   * Get value of exo:activityId property in specified node. If property is not
   * existing then return null.
   * 
   * @param ownerNode
   * @return the value of activity's id.
   */
  public static String getActivityId(Node ownerNode) {
    try {
      if (ownerNode.isNodeType(EXO_ACTIVITY_INFO)) {
        return ownerNode.getProperty(EXO_ACTIVITY_ID).getString();
      }
    } catch (Exception e) {
      LOG.error("Failed to get value of exo:activityId.", e);
    }
    return null;
  }

  /**
   * Remove value of attach activityId in node. If node has not Mixin type
   * exo:activityInfo, auto added Mixin type exo:activityInfo
   * 
   * @param ownerNode
   */
  public static void removeAttchAtivityId(Node ownerNode) {
    attachActivityId(ownerNode, StringUtils.EMPTY);
  }

}
