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
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 5:31:04 PM
 */
@ComponentConfigs({
        @ComponentConfig(
                type = UIViewForm.class,
                lifecycle = UIFormLifecycle.class,
                template = "app:/groovy/webui/component/admin/view/UIForm.gtmpl",
                events = {
                        @EventConfig(listeners = UIViewFormTabPane.SaveActionListener.class),
                        @EventConfig(listeners = UIViewFormTabPane.RestoreActionListener.class, phase = Phase.DECODE),
                        @EventConfig(listeners = UIViewFormTabPane.CancelActionListener.class, phase = Phase.DECODE),
                        @EventConfig(listeners = UIViewFormTabPane.CloseActionListener.class, phase = Phase.DECODE),
                        @EventConfig(listeners = UIViewFormTabPane.SelectTabActionListener.class, phase = Phase.DECODE),
                        @EventConfig(listeners = UIViewForm.ChangeVersionActionListener.class, phase = Phase.DECODE)
                }),
        @ComponentConfig(
                template =  "app:/groovy/webui/component/admin/view/UIViewFormTabPane.gtmpl"
                )
                
})
public class UIViewFormTabPane extends UITabPane {
  private static final Log logger = ExoLogger.getLogger(UIViewFormTabPane.class.getName());
  final static public String POPUP_PERMISSION = "PopupViewPermission" ;

  private String selectedTabId = "UITemplateContainer";

  public static final String SAVE_BUTTON    = "Save";
  public static final String CANCEL_BUTTON  = "Cancel";
  public static final String RESTORE_BUTTON = "Restore";
  
  private String[] actions_ = new String[] {SAVE_BUTTON, CANCEL_BUTTON};
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
    UITabList uiTabList = this.findFirstComponentOfType(UITabList.class);
    String viewName = uiTabList.getViewName();
    if(StringUtils.isNotEmpty(viewName) && isUpdate() ) {
      try{
        ManageViewService viewService = WCMCoreUtils.getService(ManageViewService.class);
        Node viewNode = viewService.getViewByName(viewName, WCMCoreUtils.getSystemSessionProvider());
        if (viewNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE))
          actions_ = new String[]{SAVE_BUTTON, CANCEL_BUTTON, RESTORE_BUTTON};
      }catch (Exception ex){
        logger.error("View {0} does not exits", viewName);
      }
    }
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
    addChild(UITabContainer.class, null, null);
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
    getChild(UIViewPermissionContainer.class).update(isUpdate);
  }
  
  public void view(boolean isView) {
    UITabContainer uiContainer = getChild(UITabContainer.class);
    uiContainer.getChild(UITabList.class).view(isView);
    getChild(UIViewPermissionContainer.class).view(isView);
  }  
  
  public boolean isUpdate() {
    return isUpdate_;
  }

  static  public class SaveActionListener extends EventListener<UIViewForm> {
    public void execute(Event<UIViewForm> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource().getParent();
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

  static  public class CancelActionListener extends EventListener<UIViewForm> {
    public void execute(Event<UIViewForm> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource().getParent();
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

  static  public class CloseActionListener extends EventListener<UIViewForm> {
    public void execute(Event<UIViewForm> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource().getParent();
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIPopupWindow uiPopup = uiViewContainer.getChildById(UIViewList.ST_VIEW);;
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class RestoreActionListener extends EventListener<UIViewForm> {
    public void execute(Event<UIViewForm> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource().getParent();
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      uiViewForm.changeVersion() ;
      UIViewContainer uiContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIViewList uiViewList = uiContainer.findFirstComponentOfType(UIViewList.class) ;
      uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
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
  
  public void processDecode(WebuiRequestContext context) throws Exception {
    List<UIFormInputBase> inputs = new ArrayList<UIFormInputBase>();
    this.findComponentOfType(inputs, UIFormInputBase.class);
    String action = context.getRequestParameter(UIForm.ACTION);
    for (UIFormInputBase input : inputs) {
      if (!input.isValid()) {
        continue;
      }
      String inputValue = context.getRequestParameter(input.getId());
      if (inputValue == null || inputValue.trim().length() == 0) {
        inputValue = context.getRequestParameter(input.getName());
      }
      input.decode(inputValue, context);
    }
    Event<UIComponent> event =  this.createEvent(action, Event.Phase.DECODE, context);
    if (event != null) {
      event.broadcast();
    }
  }
  
  public String event(String name) throws Exception {
    StringBuilder b = new StringBuilder();
    b.append("javascript:eXo.webui.UIForm.submitForm('").append("UIViewForm").append("','");
    b.append(name).append("',true)");
    return b.toString();
  }  
}
