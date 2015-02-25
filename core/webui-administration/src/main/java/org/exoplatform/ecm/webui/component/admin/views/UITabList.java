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
import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Feb 18, 2013
 * 12:15:50 PM  
 */

@ComponentConfig(
        template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
        events = {
            @EventConfig(listeners = UITabList.DeleteActionListener.class, confirm = "UITabList.msg.confirm-delete"),
            @EventConfig(listeners = UITabList.EditActionListener.class),
            @EventConfig(listeners = UITabList.AddTabActionListener.class)
        }
    )
public class UITabList extends UIPagingGrid {
  
  final static public String   TAPFORM_POPUP         = "TabForm_Popup";
  
  public static String[] TAB_BEAN_FIELD = {"tabName", "localizeButtons"} ;
  public static String TAB_LIST = "ECMTabList" ;
  private String[] actions_ = new String[] {"AddTab"};
  private boolean isView_ = false;
  
  private String viewName;
  
  public UITabList() throws Exception {
    getUIPageIterator().setId("TabListPageIterator") ;
    configure("tabName", UITabList.TAB_BEAN_FIELD, new String[] {"Edit","Delete"}) ;
  }
  
  public String[] getActions() { 
    return actions_; 
  }
  
  public void setActions(String[] actions) {
    actions_ = actions;
  }
  
  public boolean isView() {
    return isView_;
  }
  
  public void view(boolean isView) {
    isView_ = isView;
  }  

  @Override
  public void refresh(int currentPage) throws Exception {
    List<Tab> tabList = new ArrayList<Tab>();
    UITabContainer uiTabContainer = getParent();
    UIViewFormTabPane uiTabPane = uiTabContainer.getParent();
    UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
    if(isView_) {
      ManageViewService viewService = WCMCoreUtils.getService(ManageViewService.class);
      Node viewNode = viewService.getViewByName(viewName, WCMCoreUtils.getUserSessionProvider());
      NodeIterator nodeIter = viewNode.getNodes();
      while(nodeIter.hasNext()) {
        Node tabNode = nodeIter.nextNode();
        Tab tab = new Tab();
        tab.setTabName(tabNode.getName());
        tab.setButtons(tabNode.getProperty("exo:buttons").getValue().getString());
        tab.setLocalizeButtons(uiViewForm.getLocalizationButtons(tabNode.getProperty("exo:buttons").getValue().getString()));
        tabList.add(tab);
      }
    } else {
      tabList = uiViewForm.getTabs();
    }
    Collections.sort(tabList, new TabComparator());
    ListAccess<Tab> tabBeanList = new ListAccessImpl<Tab>(Tab.class, tabList);
    getUIPageIterator().setPageList(new LazyPageList<Tab>(tabBeanList, getUIPageIterator().getItemsPerPage()));
    getUIPageIterator().setTotalItems(tabList.size());
    if (currentPage > getUIPageIterator().getAvailablePage()) {
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    } else {
      getUIPageIterator().setCurrentPage(currentPage);
    }
  }
  
  public String getViewName() {
    return viewName;
  }
  
  public void setViewName(String name) {
    viewName = name;
  }
  
  static  public class AddTabActionListener extends EventListener<UITabList> {
    public void execute(Event<UITabList> event) throws Exception {
      UITabList uiTabList = event.getSource();
      UITabContainer uiContainer = uiTabList.getParent();
      UITabForm uiTabForm = uiContainer.createUIComponent(UITabForm.class, null, null);
      uiContainer.initPopup(UITabList.TAPFORM_POPUP, uiTabForm, 760, 0);
      UIViewFormTabPane uiTabPane = uiContainer.getParent();
      uiTabPane.setSelectedTab(uiContainer.getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UITabList> {
    public void execute(Event<UITabList> event) throws Exception {
      UITabList uiTabList = event.getSource();
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID);
      UITabContainer uiTabContainer = uiTabList.getParent();
      UIViewFormTabPane uiTabPane = uiTabContainer.getParent();
      UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
      uiViewForm.getTabMap().remove(tabName);
      uiTabList.refresh(uiTabList.getUIPageIterator().getCurrentPage());
      uiTabPane.setSelectedTab(uiTabList.getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTabList.getParent());
    }
  }
  
  static  public class EditActionListener extends EventListener<UITabList> {
    public void execute(Event<UITabList> event) throws Exception {
      UITabList uiTabList = event.getSource();
      UITabContainer uiContainer = uiTabList.getParent();
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIViewFormTabPane uiTabPane = uiContainer.getParent();
      UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
      Tab tab = uiViewForm.getTabMap().get(tabName);
      UITabForm uiTabForm = uiContainer.createUIComponent(UITabForm.class, null, null);
      uiTabForm.update(tab, false);
      uiContainer.initPopup(UITabList.TAPFORM_POPUP, uiTabForm, 760, 0);
      uiTabPane.setSelectedTab(uiTabList.getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }  

  static public class TabComparator implements Comparator<Tab> {
    public int compare(Tab o1, Tab o2) throws ClassCastException {
      String name1 = o1.getTabName();
      String name2 = o2.getTabName();
      return name1.compareToIgnoreCase(name2);
    }
  }
}
