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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 11:45:11 AM
 */
@ComponentConfig(
    template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIViewList.DeleteActionListener.class, confirm = "UIViewList.msg.confirm-delete"),
        @EventConfig(listeners = UIViewList.EditInfoActionListener.class),
        @EventConfig(listeners = UIViewList.ViewActionListener.class),
        @EventConfig(listeners = UIViewList.AddViewActionListener.class)
    }
)

public class UIViewList extends UIPagingGrid {

  final static public String[] ACTIONS         = { "AddView" };

  final static public String   ST_VIEW         = "ViewPopup";

  final static public String   ST_EDIT         = "EditPopup";

  final static public String   ST_ADD          = "AddPopup";

  private static String[]      VIEW_BEAN_FIELD = { "name", "permissions", "tabList", "baseVersion" };

  private static String[]      VIEW_ACTION     = { "View", "EditInfo", "Delete" };

  public UIViewList() throws Exception {
    getUIPageIterator().setId("UIViewsGrid") ;
    configure("id", VIEW_BEAN_FIELD, VIEW_ACTION) ;
  }

  private String getBaseVersion(String name) throws Exception {
    Node node = 
      getApplicationComponent(ManageViewService.class).getViewByName(name, WCMCoreUtils.getSystemSessionProvider());
    if(node == null) return null ;
    if(!node.isNodeType(Utils.MIX_VERSIONABLE) || node.isNodeType(Utils.NT_FROZEN)) return "";
    return node.getBaseVersion().getName();
  }

  public String[] getActions() { return ACTIONS ; }

  @SuppressWarnings("unchecked")
  public void refresh(int currentPage) throws Exception {
    List<ViewBean> viewBean = getViewsBean();
    Collections.sort(viewBean, new ViewComparator());
    ListAccess<ViewBean> viewBeanList = new ListAccessImpl<ViewBean>(ViewBean.class, viewBean);
    getUIPageIterator().setPageList(new LazyPageList<ViewBean>(viewBeanList,
                                                               getUIPageIterator().getItemsPerPage()));
    getUIPageIterator().setTotalItems(viewBean.size());
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  private List<ViewBean> getViewsBean() throws Exception {
    List<ViewConfig> views = getApplicationComponent(ManageViewService.class).getAllViews();
    List<ViewBean> viewBeans = new ArrayList<ViewBean>();
    for (ViewConfig view : views) {
      List<String> tabsName = new ArrayList<String>();
      for (ViewConfig.Tab tab : view.getTabList()) {
        tabsName.add(tab.getTabName());
      }
      ViewBean bean = new ViewBean(view.getName(), view.getPermissions(), tabsName);
      if (getBaseVersion(view.getName()) == null)
        continue;
      bean.setBaseVersion(getBaseVersion(view.getName()));
      viewBeans.add(bean);
    }
    return viewBeans;
  }

  static public class ViewComparator implements Comparator<ViewBean> {
    public int compare(ViewBean v1, ViewBean v2) throws ClassCastException {
      String name1 = v1.getName();
      String name2 = v2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  public boolean canDelete(List drivers, String viewName) {
    for(Object driver : drivers){
      String views = ((DriveData)driver).getViews() ;
      for(String view: views.split(",")){
        if(viewName.equals(view.trim())) return false ;
      }
    }
    return true ;
  }

  public String getRepository() {
    return getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
  }

  static  public class AddViewActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      if(uiViewList.getViewsBean().size() == 0) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIViewList.msg.access-denied", null, ApplicationMessage.WARNING)) ;
        
        return ;
      }
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_VIEW) ;
      uiViewContainer.removeChildById(UIViewList.ST_EDIT) ;
      uiViewContainer.initPopup(UIViewList.ST_ADD) ;
      UIViewFormTabPane uiViewTabPane =
        uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class) ;
      uiViewTabPane.reset() ;
      UIViewManager uiManager = uiViewList.getAncestorOfType(UIViewManager.class) ;
      uiManager.setRenderedChild(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class DeleteActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList viewList = event.getSource() ;
      viewList.setRenderSibling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID)  ;
      ManageDriveService manageDrive = viewList.getApplicationComponent(ManageDriveService.class) ;
      if(!viewList.canDelete(manageDrive.getAllDrives(), viewName)) {
        UIApplication app = viewList.getAncestorOfType(UIApplication.class) ;
        Object[] args = {viewName} ;
        app.addMessage(new ApplicationMessage("UIViewList.msg.template-in-use", args)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(viewList.getParent()) ;
        return ;
      }
      viewList.getApplicationComponent(ManageViewService.class).removeView(viewName) ;
      viewList.refresh(viewList.getUIPageIterator().getCurrentPage()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(viewList.getParent()) ;
    }
  }

  static  public class EditInfoActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      uiViewList.setRenderSibling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node viewNode = uiViewList.getApplicationComponent(ManageViewService.class).
        getViewByName(viewName, WCMCoreUtils.getSystemSessionProvider()) ;
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_VIEW) ;
      uiViewContainer.removeChildById(UIViewList.ST_ADD) ;
      uiViewContainer.initPopup(UIViewList.ST_EDIT) ;
      UIViewFormTabPane viewTabPane = uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class) ;
      UIViewForm viewForm = viewTabPane.getChild(UIViewForm.class) ;
      viewForm.refresh(true) ;
      viewForm.update(viewNode, false, null) ;
      if(viewForm.getUIFormCheckBoxInput(UIViewForm.FIELD_ENABLEVERSION).isChecked()) {
        viewForm.getUIFormCheckBoxInput(UIViewForm.FIELD_ENABLEVERSION).setEnable(false);
        viewForm.setActions(new String[]{"Save", "Restore", "Cancel", "AddTabForm"}, null) ;
      } else {
        viewForm.getUIFormCheckBoxInput(UIViewForm.FIELD_ENABLEVERSION).setEnable(true);
        viewForm.setActions(new String[]{"Save", "Cancel", "AddTabForm"}, null) ;
      }
      viewForm.setActionInfo(UIViewForm.FIELD_PERMISSION, new String[] {"AddPermission","RemovePermission"}) ;
      viewTabPane.getChild(UITabForm.class).setActions(new String[]{"Save"}, null) ;
      viewTabPane.setSelectedTab(viewForm.getId()) ;
      UIViewManager uiManager = uiViewList.getAncestorOfType(UIViewManager.class) ;
      uiManager.setRenderedChild(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class ViewActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      uiViewList.setRenderSibling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node viewNode = uiViewList.getApplicationComponent(ManageViewService.class).getViewByName(
          viewName, WCMCoreUtils.getSystemSessionProvider()) ;
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_EDIT) ;
      uiViewContainer.removeChildById(UIViewList.ST_ADD) ;
      uiViewContainer.initPopup(UIViewList.ST_VIEW) ;
      UIViewFormTabPane uiViewTabPane =
        uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class);
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      uiViewForm.refresh(false) ;
      uiViewForm.update(viewNode, true, null) ;
      uiViewForm.setActionInfo(UIViewForm.FIELD_PERMISSION, null) ;
      uiViewForm.setActions(new String[]{"Close"}, null) ;
      uiViewTabPane.getChild(UITabForm.class).setActions(new String[]{"BackViewForm"}, null);
      uiViewTabPane.setRenderedChild(UIViewForm.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  public class ViewBean {
    private String id;
    private String name ;
    private String permissions ;
    private String tabList ;
    private String baseVersion =  "";

    public ViewBean(String n, String per, List tabs) {
      id = n;
      name = n ;
      permissions = per ;
      StringBuilder str = new StringBuilder() ;
      for(int i = 0; i < tabs.size(); i++) {
        str.append(" [ ").append(tabs.get(i)).append(" ]");
      }
      tabList = str.toString() ;
    }

    public String getBaseVersion() { return baseVersion; }
    public void setBaseVersion(String s) { baseVersion = s;
    }
    public String getName() {
      ResourceBundle res = RequestContext.getCurrentInstance().getApplicationResourceBundle();
      String label = null;
      try {
        label = res.getString("Views.label." + name);
      } catch (MissingResourceException e) {
        label = name;
      }
      return label;
    }
    public void setName(String s) { name = s; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String s) { permissions = s; }

    public String getTabList() { return tabList; }
    public void setTabList(String ls) { tabList = ls; }
    
    public String getId() { return id; }
  }
}
