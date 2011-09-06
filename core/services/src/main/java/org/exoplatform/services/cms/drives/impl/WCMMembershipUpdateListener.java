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
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.impl.MembershipUpdateListener;
import org.exoplatform.services.security.ConversationRegistry;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Sep 5, 2011 5:29:04 PM
 */
public class WCMMembershipUpdateListener extends MembershipUpdateListener {

  private ManageDriveService driveService_;

  public WCMMembershipUpdateListener(ConversationRegistry conversationRegistry,
      ManageDriveService driveService) {
    super(conversationRegistry);
    driveService_ = driveService;
  }

  public void postSave(Membership m, boolean isNew) throws Exception {
    super.postSave(m, isNew);
    clearGroupCache(m);
  }

  public void postDelete(Membership m) throws Exception {
    super.postDelete(m);
    clearGroupCache(m);
  }

  private void clearGroupCache(Membership m) {
    driveService_.clearGroupCache(m.getUserName());
    driveService_.setNewRoleUpdated(true);
  }

}
