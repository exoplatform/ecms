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

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 14 Jan 2013  
 */
public class RemoveFileActivityAction implements Action{
  private ListenerService listenerService=null;
  private ActivityCommonService activityService = null;
  public RemoveFileActivityAction() {
    listenerService =  WCMCoreUtils.getService(ListenerService.class);
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }
  public boolean execute(Context context) throws Exception {
    if (listenerService ==null) return false;
    Object item = context.get("currentItem");
    if (item instanceof Node) {
      Node node = (Node)item;
      Node parent = node.getParent();
      if (node.getPrimaryNodeType().isNodeType(ActivityCommonService.NT_FILE)) {        
        if (activityService.isAcceptedNode(node) && activityService.isBroadcastNTFileEvents(node)) {
          listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, parent, node);
        }
      }
    }
    return false;
  }  
}
