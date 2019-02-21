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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
 * Feb 19, 2013
 * 4:26:03 PM  
 */

@ComponentConfig(
  template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
  events = {
    @EventConfig(listeners = UIViewPermissionList.DeleteActionListener.class, confirm = "UIViewPermissionList.msg.confirm-delete")
  }
)
public class UIViewPermissionList extends UIPagingGrid {

  public static String[] PERMISSION_BEAN_FIELD = {"friendlyPermission"} ;
  
  private String viewName;
  
  public UIViewPermissionList() throws Exception {
    getUIPageIterator().setId("PermissionListPageIterator") ;
    configure("permission", UIViewPermissionList.PERMISSION_BEAN_FIELD, new String[] {"Delete"});
  }
  
  public String[] getActions() { return new String[] {} ;}

  public String getViewName() {
    return viewName;
  }
  
  public void setViewName(String name) {
    viewName = name;
  }  
  
  @Override
  public void refresh(int currentPage) throws Exception {
    UIViewPermissionContainer uiPerContainer = getParent();
    UIViewFormTabPane uiTabPane = uiPerContainer.getParent();
    UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
    List<PermissionBean> permissions = new ArrayList<PermissionBean>();
    if(uiPerContainer.isView()) {
      ManageViewService viewService = WCMCoreUtils.getService(ManageViewService.class);
      Node viewNode = viewService.getViewByName(viewName, WCMCoreUtils.getSystemSessionProvider());
      String strPermission = viewNode.getProperty("exo:accessPermissions").getString();
      permissions = getBeanList(strPermission);
    } else {
      permissions = getBeanList(uiViewForm.getPermission());
    }
    Collections.sort(permissions, new ViewPermissionComparator());
    ListAccess<PermissionBean> permissionBeanList = new ListAccessImpl<PermissionBean>(PermissionBean.class, permissions);
    getUIPageIterator().setPageList(new LazyPageList<PermissionBean>(permissionBeanList, getUIPageIterator().getItemsPerPage()));
    getUIPageIterator().setTotalItems(permissions.size());
    if (currentPage > getUIPageIterator().getAvailablePage()) {
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    } else {
      getUIPageIterator().setCurrentPage(currentPage);
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UIViewPermissionList> {
    public void execute(Event<UIViewPermissionList> event) throws Exception {
      UIViewPermissionList uiPermissionList = event.getSource();
      UIViewPermissionContainer uiContainer = uiPermissionList.getParent();
      UIViewFormTabPane uiTabPane = uiContainer.getParent();
      UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
      String permission = event.getRequestContext().getRequestParameter(OBJECTID);
      String permissions = uiPermissionList.removePermission(permission, uiViewForm.getPermission());
      if(permissions.length() == 0) {
        UIApplication uiApp = uiPermissionList.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIViewPermissionList.msg.permission-cannot-empty",
                                              null,
                                              ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
        uiTabPane.setSelectedTab(uiContainer.getId());
        return;
      }
      uiViewForm.setPermission(permissions); 
      uiPermissionList.refresh(uiPermissionList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPermissionList.getParent());
    }
  }  
  
  static public class ViewPermissionComparator implements Comparator<PermissionBean> {
    public int compare(PermissionBean t1, PermissionBean t2) throws ClassCastException {
      String per1 = t1.getPermission();
      String per2 = t2.getPermission();
      return per1.compareToIgnoreCase(per2);
    }
  } 
  
  public static class PermissionBean {
    
    private String permssion ;
    private String friendlyPermission;

    public PermissionBean() {}

    public String getPermission(){ return this.permssion ; }
    public void setPermission( String permission) { this.permssion = permission ; }

    public String getFriendlyPermission() { return friendlyPermission; }
    public void setFriendlyPermission(String friendlyPer) { this.friendlyPermission = friendlyPer; }
  }
  
  private String removePermission(String removePermission, String permissions) {
    StringBuilder perBuilder = new StringBuilder();
    if(permissions.indexOf(",") > -1) {
      String[] arrPer = permissions.split(",");
      for(String per : arrPer) {
        if(per.equals(removePermission)) continue;
        if(perBuilder.length() > 0) perBuilder.append(",");
        perBuilder.append(per);
      }
    }
    return perBuilder.toString();
  }
  
  private List<PermissionBean> getBeanList(String permissions) throws Exception {
    UIViewContainer uiContainer = getAncestorOfType(UIViewContainer.class);
    List<PermissionBean> listBean = new ArrayList<PermissionBean>();
    String[] arrPers = new String[] {};
    if(permissions.contains(",")) {
      arrPers = permissions.split(",");
    } else if(permissions.length() > 0) {
      arrPers = new String[] {permissions};
    }
    for(String per : arrPers) {
      PermissionBean bean = new PermissionBean();
      bean.setPermission(per);
      bean.setFriendlyPermission(uiContainer.getFriendlyPermission(per));
      listBean.add(bean);
    }
    return listBean;
  }
}
