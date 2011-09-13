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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 7, 2006 8:31:56 AM
 */
@ComponentConfigs({
  @ComponentConfig(
      template = "app:/groovy/webui/component/explorer/popup/info/UIGroupSelector.gtmpl",
      events = {
          @EventConfig(listeners = UIGroupSelector.ChangeNodeActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectMembershipActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectPathActionListener.class)
      }
  ),
  @ComponentConfig(
      type = UITree.class, id = "UITreeGroupSelector",
      template = "system:/groovy/webui/core/UITree.gtmpl",
      events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)
  ),
  @ComponentConfig(
      type = UIBreadcumbs.class, id = "BreadcumbGroupSelector",
      template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
      events = @EventConfig(listeners = UIBreadcumbs.SelectPathActionListener.class)
  )
})

public class UIGroupSelector extends UIGroupMembershipSelector implements ComponentSelector {

  private UIComponent uiComponent ;
  private String returnFieldName = null ;
  private boolean isSelectGroup_ = false ;
  private boolean isSelectMember_ = false ;
  private boolean isSelectUSer_ = false ;

  public UIGroupSelector() throws Exception {}

  public UIComponent getSourceComponent() { return uiComponent ; }
  public String getReturnField() { return returnFieldName ; }

  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    uiComponent = uicomponent ;
    if(initParams == null || initParams.length < 0) return ;
    for(int i = 0; i < initParams.length; i ++) {
      if(initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=") ;
        returnFieldName = array[1] ;
        break ;
      }
      returnFieldName = initParams[0] ;
    }
  }

  public void setSelectGroup(boolean isSelect) { isSelectGroup_ = isSelect ;}
  public void setSelectMember(boolean isSelect) { isSelectMember_ = isSelect ;}
  public void setSelectUser(boolean isSelect) { isSelectUSer_ = isSelect ;}

  public boolean isSelectGroup() {return isSelectGroup_ ;}
  public boolean isSelectMember() {return isSelectMember_ ;}
  public boolean isSelectUser() {return isSelectUSer_ ;}

  private void setDefaultValue() {
    isSelectGroup_ = false ;
    isSelectMember_ = false ;
    isSelectUSer_ = false ;
  }

  @SuppressWarnings({ "unchecked", "cast" })
  public List getChildGroup() throws Exception {
    List children = new ArrayList() ;
    OrganizationService service = getApplicationComponent(OrganizationService.class) ;
    for (Object child : service.getGroupHandler().findGroups(this.getCurrentGroup())) {
      children.add((Group)child) ;
    }
    return children ;
  }

  @SuppressWarnings({ "unchecked", "cast" })
  public List getUsers() throws Exception {
    List children = new ArrayList() ;
    OrganizationService service = getApplicationComponent(OrganizationService.class) ;
    PageList userPageList = service.getUserHandler().findUsersByGroup(this.getCurrentGroup().getId()) ;
    for(Object child : userPageList.getAll()){
      children.add((User)child) ;
    }
    return children ;
  }

  static  public class SelectMembershipActionListener extends EventListener<UIGroupSelector> {
    public void execute(Event<UIGroupSelector> event) throws Exception {
      UIGroupSelector uiGroupSelector = event.getSource();
      String user = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIJCRExplorer uiExplorer = uiGroupSelector.getAncestorOfType(UIJCRExplorer.class) ;
      Node node = uiExplorer.getCurrentNode() ;
      if(user.equals(Utils.getNodeOwner(node))) {
        UIApplication uiApp = uiGroupSelector.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIGroupSelector.msg.not-change-owner", new Object[]{user})) ;
        
        return ;
      }
      String returnField = uiGroupSelector.getReturnField() ;
      ((UISelectable)uiGroupSelector.getSourceComponent()).doSelect(returnField, user) ;
      UIPopupWindow uiPopup = uiGroupSelector.getParent() ;
      uiGroupSelector.setDefaultValue() ;
      uiPopup.setShow(false) ;
      UIPermissionManager uiManager = uiGroupSelector.getAncestorOfType(UIPermissionManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static  public class ChangeNodeActionListener extends EventListener<UITree> {
    public void execute(Event<UITree> event) throws Exception {
      UIGroupSelector uiGroupSelector = event.getSource().getAncestorOfType(UIGroupSelector.class) ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID)  ;
      uiGroupSelector.changeGroup(groupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector) ;
    }
  }

  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource() ;
      UIGroupSelector uiGroupSelector = uiBreadcumbs.getParent() ;
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiBreadcumbs.setSelectPath(objectId);
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId() ;
      uiGroupSelector.changeGroup(selectGroupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector) ;
    }
  }
}
