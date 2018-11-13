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
import javax.jcr.Session;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class EditFilePropertyActivityAction implements Action{
  private ListenerService listenerService=null;
  private ActivityCommonService activityService = null;
  public EditFilePropertyActivityAction() {
    listenerService =  WCMCoreUtils.getService(ListenerService.class);
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }
  @Override
  public boolean execute(Context context) throws Exception {
    Object item = context.get("currentItem");
    Node node = (item instanceof Property) ?((Property)item).getParent():(Node)item;
    String propertyName = (item instanceof Property) ?((Property)item).getName():((Node)item).getName();
    // Do not create / update activity for bellow cases
    if (!activityService.isAcceptedFileProperties(propertyName)) return false;
    if (ConversationState.getCurrent() == null) return false;    
    if(node.isNodeType(NodetypeConstant.NT_RESOURCE)) node = node.getParent();
    if(!node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) return false;
    if(propertyName.equals(NodetypeConstant.JCR_DATA)) {
      // Save temporary data in current session for system session can see the update
      node.save();

      SessionProvider systemSessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session systemSession = systemSessionProvider.getSession(node.getSession().getWorkspace().getName(), WCMCoreUtils.getRepository());

      Node parent = systemSession.itemExists(node.getPath()) ? ((Node)systemSession.getItem(node.getPath())).getParent() : node.getParent();

      if(parent.hasNode(NodetypeConstant.EXO_THUMBNAILS_FOLDER)) {
        Node thumnail = parent.getNode(NodetypeConstant.EXO_THUMBNAILS_FOLDER);
        if(thumnail.hasNode(node.getUUID())) thumnail.getNode(node.getUUID()).remove();
        parent.save();
      }
    }
    //Notify to update activity
    if(node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && activityService.isBroadcastNTFileEvents(node) 
        && !activityService.isCreating(node)) {
      listenerService.broadcast(ActivityCommonService.FILE_EDIT_ACTIVITY, context, propertyName);
    }    
    return false;
  }

}
