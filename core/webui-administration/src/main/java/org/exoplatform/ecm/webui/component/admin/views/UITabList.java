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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
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
  
  private static String[] TAB_BEAN_FIELD = {"tabName", "buttons"} ;
  private static String[] TAB_ACTION = {"Edit","Delete"} ;
  public static String TAB_LIST = "ECMTabList" ;
  
  private String viewName;
  
  public UITabList() throws Exception {
    getUIPageIterator().setId("TabListPageIterator") ;
    configure("tabName", TAB_BEAN_FIELD, TAB_ACTION) ;
  }
  
  public String[] getActions() { return new String[] {"AddTab"} ; }

  @Override
  public void refresh(int currentPage) throws Exception {
    ManageViewService viewService = WCMCoreUtils.getService(ManageViewService.class);
    Node viewNode = viewService.getViewByName(viewName, WCMCoreUtils.getUserSessionProvider());
    List<Tab> tabList = new ArrayList<Tab>();
    NodeIterator nodeIter = viewNode.getNodes();
    while(nodeIter.hasNext()) {
      Node tabNode = nodeIter.nextNode();
      Tab tab = new Tab();
      tab.setTabName(tabNode.getName());
      tab.setButtons(getLocalizationButtons(tabNode.getProperty("exo:buttons").getValue().getString()));
      tabList.add(tab);
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
  
  public Tab getTabByName(String tabName) throws Exception {
    ManageViewService viewService = WCMCoreUtils.getService(ManageViewService.class);
    Node viewNode = viewService.getViewByName(viewName, WCMCoreUtils.getUserSessionProvider());
    Tab tab = new Tab();
    tab.setTabName(tabName);
    tab.setButtons(viewNode.getNode(tabName).getProperty("exo:buttons").getValue().getString());
    return tab;
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
      UIViewContainer uiContainer = uiTabList.getAncestorOfType(UIViewContainer.class);
      UITabForm uiTabForm = uiContainer.createUIComponent(UITabForm.class, null, null);
      uiTabForm.setViewName(uiTabList.getViewName());
      uiContainer.initPopup(UITabList.TAPFORM_POPUP, uiTabForm, 760, 0);
      UIViewFormTabPane uiTabPane = uiTabList.getParent();
      uiTabPane.setSelectedTab(uiTabPane.getSelectedTabId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UITabList> {
    public void execute(Event<UITabList> event) throws Exception {
      UITabList uiTabList = event.getSource();
      ManageViewService manageViewService = WCMCoreUtils.getService(ManageViewService.class);
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID);
      Node viewNode = manageViewService.getViewByName(uiTabList.getViewName(), WCMCoreUtils.getUserSessionProvider());
      viewNode.getNode(tabName).remove();
      viewNode.save();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTabList.getParent());
    }
  }
  
  static  public class EditActionListener extends EventListener<UITabList> {
    public void execute(Event<UITabList> event) throws Exception {
      UITabList uiTabList = event.getSource();
      UIViewContainer uiContainer = uiTabList.getAncestorOfType(UIViewContainer.class);
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID);
      Tab tab = uiTabList.getTabByName(tabName);
      UITabForm uiTabForm = uiContainer.createUIComponent(UITabForm.class, null, null);
      uiTabForm.setViewName(uiTabList.getViewName());
      uiTabForm.update(tab, false);
      uiContainer.initPopup(UITabList.TAPFORM_POPUP, uiTabForm, 760, 0);
      UIViewFormTabPane uiTabPane = uiTabList.getParent();
      uiTabPane.setSelectedTab(uiTabPane.getSelectedTabId());
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
  
  private String getLocalizationButtons(String buttons) {
    StringBuilder localizationButtons = new StringBuilder();
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    if(buttons.contains(";")) {
      String[] arrButtons = buttons.split(";");
      for(int i = 0; i < arrButtons.length; i++) {
        try {
          localizationButtons.append(res.getString("UITabForm.label." + arrButtons[i].trim()));
        } catch(MissingResourceException mre) {
          localizationButtons.append(arrButtons[i]);
        }
        if(i < arrButtons.length - 1) {
          localizationButtons.append(", ");
        }
      }
    }
    return localizationButtons.toString();
  }
}
