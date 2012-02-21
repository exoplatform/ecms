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
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotHoldsLockFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotSameNameSiblingFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotSimpleLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
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
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
    events = {
      @EventConfig(listeners = LockManageComponent.LockActionListener.class)
    }
)

public class LockManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                              new IsNotSameNameSiblingFilter(),
                                              new IsNotHoldsLockFilter(),
                                              new IsNotSimpleLockedFilter(),
                                              new CanSetPropertyFilter(),
                                              new IsCheckedOutFilter(),
                                              new IsNotTrashHomeNodeFilter() });

  private static final Log LOG  = ExoLogger.getLogger(LockManageComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }


  private static void processMultiLock(String[] nodePaths, Event<?> event, UIJCRExplorer uiExplorer) throws Exception {
    for(int i=0; i< nodePaths.length; i++) {
      processLock(nodePaths[i], event, uiExplorer);
    }
    uiExplorer.updateAjax(event);
  }

  private static void processLock(String nodePath, Event<?> event,  UIJCRExplorer uiExplorer) throws Exception {
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      nodePath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ nodePath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    Node node;
    try {
      // Use the method getNodeByPath because it is link aware
      node = uiExplorer.getNodeByPath(nodePath, session);
      // Reset the path to manage the links that potentially create virtual path
      nodePath = node.getPath();
      // Reset the session to manage the links that potentially change of workspace
      session = node.getSession();
      // Reset the workspace name to manage the links that potentially change of workspace
      wsName = session.getWorkspace().getName();
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    
    try {
      if (!PermissionUtil.canSetProperty(node))
        throw new AccessDeniedException("access denied, can't lock node:" + node.getPath());
      
      if(node.canAddMixin(Utils.MIX_LOCKABLE)){
        node.addMixin(Utils.MIX_LOCKABLE);
        node.save();
      }
      Lock lock = node.lock(false, false);
      LockUtil.keepLock(lock);
      LockService lockService = uiExplorer.getApplicationComponent(LockService.class);
      List<String> settingLockList = lockService.getAllGroupsOrUsersForLock();
      for (String settingLock : settingLockList) {
        LockUtil.keepLock(lock, settingLock);
        if (!settingLock.startsWith("*"))
          continue;
        String lockTokenString = settingLock;
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        OrganizationService service = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
        List<MembershipType> memberships = (List<MembershipType>) service.getMembershipTypeHandler().findMembershipTypes();
        for (MembershipType membership : memberships) {
          lockTokenString = settingLock.replace("*", membership.getName());
          LockUtil.keepLock(lock, lockTokenString);
        }
      }
      session.save();
    } catch(LockException le) {
      ApplicationMessage appMessage = 
        new ApplicationMessage("UIPopupMenu.msg.cant-lock", 
                               new String[] {node.getPath()}, ApplicationMessage.ERROR);
      appMessage.setArgsLocalized(false);
      uiApp.addMessage(appMessage);
      
      uiExplorer.updateAjax(event);
      return;
    } catch (AccessDeniedException adEx) {
      ApplicationMessage appMessage = 
        new ApplicationMessage("UIPopupMenu.msg.can-not-lock-node", 
                               new String[] {node.getPath()}, ApplicationMessage.ERROR);
      appMessage.setArgsLocalized(false);
      uiApp.addMessage(appMessage);
      
      uiExplorer.updateAjax(event);
      return;
    }
    catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs while locking the node", e);
      }
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static void lockManage(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer) throws Exception {
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    if(nodePath.indexOf(";") > -1) {
      processMultiLock(nodePath.split(";"), event, uiExplorer);
    } else {
      processLock(nodePath, event, uiExplorer);
    }
  }

  public static class LockActionListener extends UIWorkingAreaActionListener<LockManageComponent> {
    public void processEvent(Event<LockManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      lockManage(event, uiExplorer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
