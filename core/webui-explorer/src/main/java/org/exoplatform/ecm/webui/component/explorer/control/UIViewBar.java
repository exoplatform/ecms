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
package org.exoplatform.ecm.webui.component.explorer.control;

import java.util.List;

import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.*;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Aug 2, 2006
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/control/UIViewBar.gtmpl",
    events = {
      @EventConfig(listeners = UIViewBar.ShowSideBarActionListener.class),
      @EventConfig(listeners = UIViewBar.PreferencesActionListener.class),
      @EventConfig(listeners = UIViewBar.BackActionListener.class),
      @EventConfig(listeners = UIViewBar.RefreshSessionActionListener.class),
      @EventConfig(listeners = UIViewBar.ChangeViewActionListener.class)
    }
)

@Deprecated
public class UIViewBar extends UIForm {
  final static private String FIELD_SELECT_VIEW = "views" ;

  public UIViewBar() throws Exception {
    UIFormSelectBox selectView  = new UIFormSelectBox(FIELD_SELECT_VIEW, null, null) ;
    selectView.setOnChange("ChangeView") ;
    addChild(selectView) ;
  }

  public boolean isDirectlyDrive() {
    boolean returnboolean = false;
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String usecase =  portletPref.getValue("usecase", "").trim();
    if (usecase.equals("jailed") || usecase.equals("personal") || usecase.equals("social")) {
      returnboolean = true;
    }
    return returnboolean;
  }

  public void setViewOptions(List<SelectItemOption<String>> viewOptions) {
    getUIFormSelectBox(FIELD_SELECT_VIEW).setOptions(viewOptions) ;
    getUIFormSelectBox(FIELD_SELECT_VIEW).setValue(viewOptions.get(0).getValue()) ;
  }

  public boolean isShowSideBar() throws Exception {
    UIJCRExplorer uiExplorer =  getAncestorOfType(UIJCRExplorer.class);
    return uiExplorer.getPreference().isShowSideBar();
  }

  static public class BackActionListener extends EventListener<UIViewBar> {
    public void execute(Event<UIViewBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      UISearchResult simpleSearchResult = uiJCRExplorer.findComponentById(UIDocumentWorkspace.SIMPLE_SEARCH_RESULT);
      if(simpleSearchResult != null) simpleSearchResult.setRendered(false);
      uiJCRExplorer.setRenderSibling(UIDrivesBrowserContainer.class);
    }
  }

  static public class ShowSideBarActionListener extends EventListener<UIViewBar> {
    public void execute(Event<UIViewBar> event) throws Exception {
      UIViewBar uiViewBar = event.getSource();
      UIJCRExplorerPortlet explorerPorltet = uiViewBar.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = explorerPorltet.findFirstComponentOfType(UIJCRExplorer.class);
      UIJcrExplorerContainer uiJcrExplorerContainer= explorerPorltet.getChild(UIJcrExplorerContainer.class);
      Preference pref = uiExplorer.getPreference();
      pref.setShowSideBar(true);
      uiExplorer.refreshExplorer();
      uiJcrExplorerContainer.setRenderedChild(UIJCRExplorer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
    }
  }

  static public class PreferencesActionListener extends EventListener<UIViewBar> {
    public void execute(Event<UIViewBar> event) throws Exception {
      UIViewBar viewBar = event.getSource();
      UIJCRExplorer uiJCRExplorer = viewBar.getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer popupAction = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIPreferencesForm uiPrefForm = popupAction.activate(UIPreferencesForm.class, 600) ;
      uiPrefForm.update(uiJCRExplorer.getPreference()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }

  static public class RefreshSessionActionListener extends EventListener<UIViewBar> {
    public void execute(Event<UIViewBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiJCRExplorer.getSession().refresh(false) ;
      uiJCRExplorer.refreshExplorer() ;
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      String viewName = event.getSource().getUIFormSelectBox(FIELD_SELECT_VIEW).getValue() ;
      uiActionBar.setTabOptions(viewName) ;
      UIApplication uiApp = uiJCRExplorer.getAncestorOfType(UIApplication.class) ;
      String mess = "UIJCRExplorer.msg.refresh-session-success" ;
      uiApp.addMessage(new ApplicationMessage(mess, null, ApplicationMessage.INFO)) ;
    }
  }

  static public class ChangeViewActionListener extends EventListener<UIViewBar> {
    public void execute(Event<UIViewBar> event) throws Exception {
      UIViewBar uiViewBar = event.getSource();
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      String viewName = uiViewBar.getUIFormSelectBox(FIELD_SELECT_VIEW).getValue() ;
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      uiActionBar.setTabOptions(viewName) ;
    }
  }
}
