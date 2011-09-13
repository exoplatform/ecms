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
package org.exoplatform.ecm.webui.component.admin.unlock;

import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006
 * 11:30:17 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/unlock/UILockHolderList.gtmpl",
    events = {
        @EventConfig(listeners = UILockHolderList.DeleteLockActionListener.class)
    }
)
public class UILockHolderList extends UIPagingGridDecorator {
  final static public String[] ACTIONS = {};
  final static public String ST_EDIT = "EditUnLockForm";

  public UILockHolderList() throws Exception {
    getUIPageIterator().setId("LockHolderListIterator");
  }

  public String[] getActions() { return ACTIONS ; }

  public void refresh(int currentPage) throws Exception {
    ListAccess<String> groupsAndUsersForLockList = new ListAccessImpl<String>(String.class,
                                                                              getAllGroupsOrUsersForLock());
    LazyPageList<String> pageList = new LazyPageList<String>(groupsAndUsersForLockList,
                                                             getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }


  public List getGroupsOrUsersForLock() throws Exception {
    return getUIPageIterator().getCurrentPageData();
  }

  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    LockService lockService = getApplicationComponent(LockService.class);

    return lockService.getAllGroupsOrUsersForLock();
  }

  static public class DeleteLockActionListener extends EventListener<UILockHolderList> {
    public void execute(Event<UILockHolderList> event) throws Exception {
      UILockHolderList uiLockHolderList = event.getSource();
      UILockHolderContainer uiLockHolderContainer = uiLockHolderList.getAncestorOfType(UILockHolderContainer.class);
      String settingLock = event.getRequestContext().getRequestParameter(OBJECTID);
      LockService lockService = uiLockHolderContainer.getApplicationComponent(LockService.class);
      if (!lockService.getPreSettingLockList().contains(settingLock)) {
        lockService.removeGroupsOrUsersForLock(settingLock);
      } else {
        Object[] args = {settingLock};
        UIApplication uiApp = uiLockHolderList.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UILockHolderList.msg.can-not-delete-lock-holder", args,
            ApplicationMessage.WARNING));
        
        event.getRequestContext().addUIComponentToUpdateByAjax(uiLockHolderContainer.getParent());
      }
      UILockHolderList uiHolderList = uiLockHolderContainer.getChild(UILockHolderList.class);
      uiHolderList.refresh(uiHolderList.getUIPageIterator().getCurrentPage());
      UIUnLockManager uiUnLockManager = uiLockHolderContainer.getParent();
      uiUnLockManager.getChild(UILockNodeList.class).setRendered(false);
      uiLockHolderContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiLockHolderContainer.getParent());
    }
  }
}
