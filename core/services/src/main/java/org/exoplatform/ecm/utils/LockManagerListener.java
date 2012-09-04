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
package org.exoplatform.ecm.utils;

import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * This listener will be used to unlock all locked node of current user in the case session destroyed.
 * @author minh_dang
 *
 */
public class LockManagerListener extends Listener<ConversationRegistry, ConversationState > {

  @Override
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    /*
     * In the case system stop working then we have to check the available of connection to repository.
     * If connection lost we will do nothing in this listener.
     */
    if(repositoryService.getCurrentRepository() == null) return;
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    /*
     * Remove all locked node of current user
     */
    lockService.removeLocksOfUser(event.getData().getIdentity().getUserId());
  }

}
