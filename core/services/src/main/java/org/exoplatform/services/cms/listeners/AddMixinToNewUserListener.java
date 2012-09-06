/***************************************************************************
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.cms.listeners;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 5, 2012
 * 3:28:02 PM  
 */
/**
 * To avoid performance decrease by using OrganizationService to get all users. This listener will be used to add a mixin
 * node type named exo:userFolder to the user folder node during the time creating user.
 *
 */
public class AddMixinToNewUserListener extends UserEventListener {
  
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  
  public AddMixinToNewUserListener(NodeHierarchyCreator nodeHierarchyCreatorService) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreatorService ;
  }
  
  @Override
  @SuppressWarnings("unused") 
  public void preSave(User user, boolean isNew) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, user.getUserName());
    if(userNode.canAddMixin("exo:userFolder")) {
      userNode.addMixin("exo:userFolder");
      userNode.save();
    }
  }
}
