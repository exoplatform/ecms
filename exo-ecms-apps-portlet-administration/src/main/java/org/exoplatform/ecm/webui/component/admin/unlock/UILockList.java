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

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
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
    template = "app:/groovy/webui/component/admin/unlock/UILockList.gtmpl",
    events = {
        @EventConfig(listeners = UILockList.DeleteLockActionListener.class)
    }
)
public class UILockList extends UIComponentDecorator {
  final static public String[] ACTIONS = {};
  final static public String ST_EDIT = "EditUnLockForm";
  private UIPageIterator uiPageIterator_;
  
  public UILockList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "LockListIterator");
    setUIComponent(uiPageIterator_) ;    
  }
  
  public String[] getActions() { return ACTIONS ; }
  
  public void updateLockedNodesGrid(int currentPage) throws Exception {
    PageList pageList = new ObjectPageList(getAllGroupsOrUsersForLock(), 10);
    uiPageIterator_.setPageList(pageList);
    if(currentPage > getUIPageIterator().getAvailablePage())
      uiPageIterator_.setCurrentPage(currentPage-1);
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_ ; }
  
  public List getGroupsOrUsersForLock() throws Exception { return uiPageIterator_.getCurrentPageData(); } 
  
  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    LockService lockService = getApplicationComponent(LockService.class);
        
    return lockService.getAllGroupsOrUsersForLock();
  }
  
  static public class DeleteLockActionListener extends EventListener<UILockList> {
    public void execute(Event<UILockList> event) throws Exception {
      UIUnLockManager uiUnLockManager = event.getSource().getParent();
      String settingLock = event.getRequestContext().getRequestParameter(OBJECTID);
      LockService lockService = uiUnLockManager.getApplicationComponent(LockService.class);
      lockService.removeGroupsOrUsersForLock(settingLock);
      UILockList uiLockList = uiUnLockManager.getChild(UILockList.class);
      uiLockList.updateLockedNodesGrid(uiLockList.uiPageIterator_.getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUnLockManager);
    }
  }  
}