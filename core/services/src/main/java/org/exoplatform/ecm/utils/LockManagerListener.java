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
package org.exoplatform.ecm.utils;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.lock.LockManagerImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Sep 30, 2009
 */
public class LockManagerListener extends Listener<ConversationRegistry, ConversationState > {

  protected static Log log  = ExoLogger.getLogger(LockManagerListener.class);

  @Override
  @SuppressWarnings("unchecked")
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    if (log.isInfoEnabled()) {
      log.info("Removing the locks of all locked nodes");
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    ConversationState conversationState = event.getData();
    String userid = conversationState.getIdentity().getUserId();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if(container == null) {
      RootContainer rootContainer = RootContainer.getInstance() ;
      container = rootContainer.getPortalContainer("portal") ;
    }
    CacheService cacheService = (CacheService)container.getComponentInstanceOfType(CacheService.class);
    ExoCache lockcache = cacheService.getCacheInstance(LockManagerImpl.class.getName());
    try {
      Map<String,String> lockedNodes = (Map<String,String>)lockcache.get(userid);
      if(lockedNodes == null || lockedNodes.values().isEmpty()) return;
      RepositoryService repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      String key = null, nodePath = null, workspaceName = null, lockToken= null ;
      String[] temp = null, location = null ;
      Session session = null;
      for(Iterator<String> iter = lockedNodes.keySet().iterator(); iter.hasNext();) {
        try {
          //The key structure is built in org.exoplatform.ecm.webui.utils.LockUtil.createLockKey() method
          key = iter.next();
          temp = key.split(":/:");
          nodePath = temp[1];
          location = temp[0].split("/::/");
          workspaceName = location[1] ;
          session = sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());
          lockToken = lockedNodes.get(key);
          session.addLockToken(lockToken);
          Node node = (Node)session.getItem(nodePath);
          node.unlock();
          node.removeMixin("mix:lockable");
          node.save();
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Error while unlocking the locked nodes",e);
          }
        } finally {
          if(session != null) session.logout();
        }
      }
      lockedNodes.clear();
    } catch(Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("Error during the time unlocking the locked nodes",ex);
      }
    } finally {
      sessionProvider.close();
    }
  }

}
