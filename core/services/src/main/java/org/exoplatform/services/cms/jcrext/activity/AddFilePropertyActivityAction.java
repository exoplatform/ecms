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
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 11 Jan 2013  
 */
public class AddFilePropertyActivityAction implements Action{
  private ListenerService listenerService=null;
  private ActivityCommonService activityService = null;
  public AddFilePropertyActivityAction() {
    listenerService =  WCMCoreUtils.getService(ListenerService.class);
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }
  @Override
  public boolean execute(Context context) throws Exception {
    Object item = context.get("currentItem");
    Node node = (item instanceof Property) ?((Property)item).getParent():(Node)item;
    Node nodeTemp = node;
    String propertyName = (item instanceof Property) ?((Property)item).getName():((Node)item).getName();
    // Do not create / update activity for bellow cases
    if (!activityService.isAcceptedFileProperties(propertyName)) return false;
    if (ConversationState.getCurrent() == null) return false;

    if(node.isNodeType("nt:resource")) node = node.getParent();
    //filter node type
    if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && activityService.isBroadcastNTFileEvents(node)
        && !activityService.isCreating(node)) {
      //Notify to update activity
      listenerService.broadcast(ActivityCommonService.FILE_ADD_ACTIVITY, nodeTemp, propertyName);    	
    }
    return false;
  }

}
