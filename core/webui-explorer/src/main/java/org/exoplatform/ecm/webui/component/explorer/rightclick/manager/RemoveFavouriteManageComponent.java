/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
 */
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.HasRemovePermissionFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsFavouriteFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 14, 2009
 * 5:23:00 PM
 */

@ComponentConfig(
      events = {
        @EventConfig(listeners = RemoveFavouriteManageComponent.RemoveFromFavouriteActionListener.class)
      }
  )

public class RemoveFavouriteManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[] { new IsNotInTrashFilter(),
                                                new IsFavouriteFilter(),
                                                 new IsNotLockedFilter(),
                                                 new IsCheckedOutFilter(),
                                                 new HasRemovePermissionFilter(),
                                                 new IsNotTrashHomeNodeFilter() });

  private final static Log       LOG  = ExoLogger.getLogger(RemoveFavouriteManageComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private static void multiRemoveFromFavourite(String[] paths, Event<UIComponent> event) throws Exception {
    for (String path : paths) {
      removeFromFavourite(path, event);
    }
  }

  private static void removeFromFavourite(String srcPath, Event<UIComponent> event) throws Exception {
      UIWorkingArea uiWorkingArea = ((UIComponent)event.getSource()).getParent();
      UIJCRExplorer uiExplorer = uiWorkingArea.getAncestorOfType(UIJCRExplorer.class);

      ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
      FavoriteService favoriteService = (FavoriteService)myContainer.getComponentInstanceOfType(FavoriteService.class);

      UIApplication uiApp = uiWorkingArea.getAncestorOfType(UIApplication.class);
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
      String wsName = null;
      Node node = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        srcPath = matcher.group(2);
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
      }
      Session session = uiExplorer.getSessionByWorkspace(wsName);
      try {
        // Use the method getNodeByPath because it is link aware
        node = uiExplorer.getNodeByPath(srcPath, session, false);
        //check if node is link
        LinkManager lnkManager = uiExplorer.getApplicationComponent(LinkManager.class);
        if (lnkManager.isLink(node) && lnkManager.isTargetReachable(node)) {
          node = lnkManager.getTarget(node);
        }
        // Reset the path to manage the links that potentially create virtual path
        //srcPath = node.getPath();
        // Reset the session to manage the links that potentially change of workspace
        session = node.getSession();
        // Reset the workspace name to manage the links that potentially change of workspace
        // wsName = session.getWorkspace().getName();
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
            null,ApplicationMessage.WARNING));
        
        return;
      }

      try {
        uiExplorer.addLockToken(node);
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }

      try {
        if (!node.isCheckedOut())
          throw new VersionException("node is locked, can't remove favourite of node :" + node.getPath());
        if (!PermissionUtil.canRemoveNode(node))
          throw new AccessDeniedException("access denied, can't remove favourite of node:" + node.getPath());
        favoriteService.removeFavorite(node, session.getUserID());
        uiExplorer.updateAjax(event);
      } catch (LockException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("node is locked, can't remove favourite of node :" + node.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
        uiExplorer.updateAjax(event);
      } catch (VersionException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("node is checked in, can't remove favourite of node:" + node.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
        uiExplorer.updateAjax(event);
      } catch (AccessDeniedException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("access denied, can't remove favourite of node:" + node.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
        uiExplorer.updateAjax(event);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs", e);
        }
        JCRExceptionManager.process(uiApp, e);
          
        uiExplorer.updateAjax(event);
      }
  }

  public static void removeFavouriteManage(Event<UIComponent> event) throws Exception {
    String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
    if (srcPath.indexOf(';') > -1) {
      multiRemoveFromFavourite(srcPath.split(";"), event);
    } else {
      removeFromFavourite(srcPath, event);
    }
  }

  public static class RemoveFromFavouriteActionListener extends UIWorkingAreaActionListener<RemoveFavouriteManageComponent> {
      public void processEvent(Event<RemoveFavouriteManageComponent> event) throws Exception {
          Event<UIComponent> event_ = new Event<UIComponent>( event.getSource(), event.getName(),event.getRequestContext());
          removeFavouriteManage(event_);
        }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}

