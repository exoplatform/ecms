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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 5:31:04 PM
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/admin/view/UIViewFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UIViewFormTabPane.SaveActionListener.class),
      @EventConfig(listeners = UIViewFormTabPane.RestoreActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.CloseActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.SelectTabActionListener.class),
      @EventConfig(listeners = UIViewForm.ChangeVersionActionListener.class, phase = Phase.DECODE)
    }
)
public class UIViewFormTabPane extends UITabPane {
  final static public String POPUP_PERMISSION = "PopupViewPermission" ;

  private String selectedTabId = "UITemplateContainer";
  
  private String[] actions_ = new String[] {"Save", "Cancel"};
  private String primaryBtn_ = "Save";
  
  private boolean isUpdate_ = false;

  public String getSelectedTabId()
  {
     return selectedTabId;
  }

  public void setSelectedTab(String renderTabId)
  {
     selectedTabId = renderTabId;
  }

  public void setSelectedTab(int index)
  {
     selectedTabId = getChild(index - 1).getId();
  }
  
  public String[] getActions() {
    if(actions_.length == 1) primaryBtn_ = actions_[0];
    return actions_;
  }
  
  public void setActions(String[] actions) {
    actions_ = actions;
  }
  
  public String getPrimaryButtonAction() {
    return primaryBtn_;
  }
  
  public void setPrimaryButtonAction(String primaryBtn) {
    primaryBtn_ = primaryBtn;
  }  

  public UIViewFormTabPane() throws Exception {
  	UIViewForm uiViewForm = addChild(UIViewForm.class, null, null) ;
  	addChild(UITabList.class, null, null);
  	addChild(UIViewPermissionContainer.class, null, null);
  	setSelectedTab(uiViewForm.getId()) ;
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIViewForm.label." + id) ;
    } catch (MissingResourceException ex) {
      return id ;
    }
  }
  
  public void update(boolean isUpdate) {
    isUpdate_ = isUpdate;
  }
  
  public boolean isUpdate() {
    return isUpdate_;
  }

  static  public class SaveActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewTabPane.getChild(UIViewForm.class).save() ;
      UIPopupWindow uiPopup = null;
      if(uiViewTabPane.isUpdate()) {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_EDIT);
      } else {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_ADD);
      }
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIPopupWindow uiPopup = null;
      if(uiViewTabPane.isUpdate()) {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_EDIT);
      } else {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_ADD);
      }
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class CloseActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.getChild(UITabForm.class).refresh(true) ;
      uiViewTabPane.getChild(UIViewForm.class).refresh(true) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIPopupWindow uiPopup = null;
      if(uiViewTabPane.isUpdate()) {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_EDIT);
      } else {
        uiPopup = uiViewContainer.getChildById(UIViewList.ST_ADD);
      }
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class RestoreActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      UITabForm uiTabForm = uiViewTabPane.getChild(UITabForm.class) ;
      uiViewForm.changeVersion() ;
      UIViewContainer uiContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIViewList uiViewList = uiContainer.findFirstComponentOfType(UIViewList.class) ;
      uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
      uiTabForm.refresh(true) ;
      uiViewForm.refresh(true) ;
      uiViewTabPane.removeChildById(POPUP_PERMISSION) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static public class SelectTabActionListener extends EventListener<UIViewFormTabPane>
  {
    public void execute(Event<UIViewFormTabPane> event) throws Exception
    {
      WebuiRequestContext context = event.getRequestContext();
      String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
      if (renderTab == null)
        return;
      event.getSource().setSelectedTab(renderTab);
      WebuiRequestContext parentContext = (WebuiRequestContext)context.getParentAppRequestContext();
      if (parentContext != null)
      {
        parentContext.setResponseComplete(true);
      }
      else
      {
        context.setResponseComplete(true);
      }
    }
  } 
}
