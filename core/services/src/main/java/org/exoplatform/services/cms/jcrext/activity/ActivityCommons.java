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
package org.exoplatform.services.cms.jcrext.activity;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * List of available activity event
 * List of node-type with allow the activity raise on.
 * 15 Jan 2013  
 */
public class ActivityCommons {
  public static String acceptedNodeTypes = "{exo:accessibleMedia}{exo:article}{exo:contact_us}{exo:event}{exo:htmlFile}" +
                                           "{rma:filePlan}{exo:webContent}{kfx:document}{exo:pictureOnHeadWebcontent}" +
                                           "{exo:podcast}{exo:sample}{exo:link}";
  public static String NT_FILE                    = "nt:file";
  public static String EDIT_ACTIVITY              = "ActivityNotify.event.PropertyUpdated";
  
  public static String ATTACH_ADDED_ACTIVITY      = "ActivityNotify.event.AttachmentAdded";
  public static String ATTACH_REMOVED_ACTIVITY    = "ActivityNotify.event.AttachmentRemoved";
  
  public static String NODE_CREATED_ACTIVITY      = "ActivityNotify.event.NodeCreated";
  public static String NODE_REMOVED_ACTIVITY      = "ActivityNotify.event.NodeRemoved";
  public static String NODE_MOVED_ACTIVITY        = "ActivityNotify.event.NodeMoved";
  public static String NODE_REVISION_CHANGED      = "ActivityNotify.event.RevisionChanged";
  
  public static String CATEGORY_ADDED_ACTIVITY    = "ActivityNotify.event.CategoryAdded";
  public static String CATEGORY_REMOVED_ACTIVITY  = "ActivityNotify.event.CategoryRemoved";
  
  public static String TAG_ADDED_ACTIVITY         = "ActivityNotify.event.TagAdded";
  public static String TAG_REMOVED_ACTIVITY       = "ActivityNotify.event.TagRemoved";
  
  public static String COMMENT_ADDED_ACTIVITY     = "ActivityNotify.event.CommentAdded";
  public static String STATE_CHANGED_ACTIVITY     = "ActivityNotify.event.StateChanged";
  
  
  public static String VALUE_SEPERATOR            = ",";
  public static boolean isAcceptedNode(Node node) {
    try {
      return node==null?false:acceptedNodeTypes.indexOf(node.getPrimaryNodeType().getName())>0;
    } catch (RepositoryException e) {
      return false;
    }
  }
}
