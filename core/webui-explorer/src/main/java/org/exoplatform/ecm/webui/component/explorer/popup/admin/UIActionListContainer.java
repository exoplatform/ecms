/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.lock.Lock;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.action.EditDocumentActionComponent;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 7, 2007 1:35:17 PM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIActionListContainer extends UIContainer {

  public UIActionListContainer() throws Exception {
    addChild(UIActionList.class, null, null) ;
  }

  public void initEditPopup(Node actionNode) throws Exception {
    removeChildById("editActionPopup") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "editActionPopup") ;
    uiPopup.setShowMask(true);    
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node currentNode = uiExplorer.getCurrentNode() ;
    UIActionForm uiActionForm = createUIComponent(UIActionForm.class, null, "EditFormAction") ;
    uiActionForm.setWorkspace(currentNode.getSession().getWorkspace().getName()) ;
    uiActionForm.createNewAction(currentNode, actionNode.getPrimaryNodeType().getName(), false) ;
    uiActionForm.setIsUpdateSelect(false) ;
//    uiActionForm.setNode(actionNode) ;
    uiActionForm.setNodePath(actionNode.getPath()) ;
    uiActionForm.setCurrentAction(actionNode.getName());
    actionNode.refresh(true);
    // Check document is lock for editing
    uiActionForm.setIsKeepinglock(false);
    if (!actionNode.isLocked()) {
      synchronized (EditDocumentActionComponent.class) {
        actionNode.refresh(true);
        if (!actionNode.isLocked()) {
          if(actionNode.canAddMixin(Utils.MIX_LOCKABLE)){
            actionNode.addMixin(Utils.MIX_LOCKABLE);
            actionNode.save();
          }
          Lock lock = actionNode.lock(false, false);
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
          actionNode.save();
          uiActionForm.setIsKeepinglock(true);
        }
      }
    }
    // Update data avoid concurrent modification by other session
    actionNode.refresh(true);
    // Check again after node is locking by current user or another
    if (LockUtil.getLockTokenOfUser(actionNode) == null) {
      Object[] arg = { actionNode.getPath() };
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-editing", arg,
          ApplicationMessage.WARNING));
      
      return;
    }

    uiActionForm.setIsEditInList(true) ;
    uiActionForm.setIsKeepinglock(true);
    uiPopup.setWindowSize(650, 450);
    uiPopup.setUIComponent(uiActionForm) ;
    uiPopup.setRendered(true);
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void initPopup(UIComponent uiComp) throws Exception {
    removeChildById("PopupComponent") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "PopupComponent") ;
    uiPopup.setShowMask(true);
    uiPopup.setUIComponent(uiComp) ;
    uiPopup.setWindowSize(640, 300) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
