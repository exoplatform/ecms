/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.cms.drives.impl;

import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.organization.Group;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Aug 25, 2011
 * 1:21:53 PM  
 */
public class NewGroupEventListener extends GroupEventListener {
  
  private ManageDriveService driveService_ ;
  
  public NewGroupEventListener(ManageDriveService driveService) throws Exception {
    driveService_ = driveService;
  }
  
  public void postSave(Group group, boolean isNew) throws Exception {
    clearGroupCache();
  }

  public void postDelete(Group group) throws Exception {
    clearGroupCache();
  }
  
  private void clearGroupCache() {
    ConversationState conversationState = ConversationState.getCurrent();
    if(conversationState != null) 
      driveService_.clearGroupCache(conversationState.getIdentity().getUserId());
    driveService_.setNewRoleUpdated(true);
  }
}
