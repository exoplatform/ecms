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

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 5:31:04 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UIViewFormTabPane.SaveActionListener.class),
      @EventConfig(listeners = UIViewFormTabPane.ResetActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.EditTabActionListener.class),
      @EventConfig(listeners = UIViewFormTabPane.DeleteTabActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.RestoreActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.CloseActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewFormTabPane.AddTabFormActionListener.class),
      @EventConfig(listeners = UIViewFormTabPane.BackViewFormActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewForm.AddPermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewForm.RemovePermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIViewForm.ChangeVersionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITabForm.AddTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UIViewFormTabPane extends UIFormTabPane {

  final static public String POPUP_PERMISSION = "PopupViewPermission" ;

  private UIViewForm uiViewForm ;
  private UITabForm uiTabForm ;

  public UIViewFormTabPane() throws Exception {
    super("UIViewFormTabPane") ;

    uiViewForm = new UIViewForm("UIViewForm") ;
    addUIComponentInput(uiViewForm) ;

    uiTabForm = new UITabForm("UITabForm") ;
    addUIComponentInput(uiTabForm) ;
    uiTabForm.setRendered(false);
    setSelectedTab(uiViewForm.getId()) ;
    setActions(new String[]{}) ;
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIViewForm.label." + id) ;
    } catch (MissingResourceException ex) {
      return id ;
    }
  }

  static  public class SaveActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewTabPane.uiViewForm.save() ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiTabForm.refresh(true) ;
      uiViewTabPane.uiViewForm.refresh(true) ;
      uiViewTabPane.removeChildById(POPUP_PERMISSION) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class CloseActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiTabForm.refresh(true) ;
      uiViewTabPane.uiViewForm.refresh(true) ;
      uiViewTabPane.removeChildById(POPUP_PERMISSION) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static public class AddTabFormActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiTabForm.refresh(true);
      uiViewTabPane.uiTabForm.setRendered(true);
      uiViewTabPane.uiViewForm.setRendered(false);
      uiViewTabPane.setSelectedTab(uiViewTabPane.uiTabForm.getId());
      ((UIFormStringInput) uiViewTabPane.uiTabForm.
          getChildById(UITabForm.FIELD_NAME)).addValidator(MandatoryValidator.class);
      uiViewTabPane.uiViewForm.setViewName(((UIFormStringInput)
          uiViewTabPane.uiViewForm.getChildById(UIViewForm.FIELD_NAME)).getValue());
      uiViewTabPane.uiViewForm.setPermission(((UIFormStringInput)
          uiViewTabPane.uiViewForm.getChildById(UIViewForm.FIELD_PERMISSION)).getValue());
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer);
    }
  }

  static public class BackViewFormActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      UITabForm uiTabForm = uiViewTabPane.getChild(UITabForm.class) ;
      String tabName = uiViewForm.getTabList();
      if (tabName != null && tabName.length() != 0) {
        if (tabName.indexOf(",") > 0) {
          tabName = tabName.substring(0, tabName.indexOf(","));
        }
      }
      ((UIFormStringInput)uiTabForm.getChildById(UITabForm.FIELD_NAME)).getValidators().clear();
      ((UIFormStringInput)uiViewForm.getChildById(UIViewForm.FIELD_NAME)).setValue(uiViewForm.getViewName());
      ((UIFormStringInput)uiViewForm.getChildById(UIViewForm.FIELD_PERMISSION)).setValue(uiViewForm.getPermission());
      uiTabForm.setRendered(false);
      uiViewForm.setRendered(true);
      uiViewForm.update(null, false, null);
      uiViewTabPane.setSelectedTab(uiViewForm.getId());
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer);
    }
  }

  static  public class ResetActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      uiViewTabPane.uiTabForm.refresh(true) ;
      if(uiViewTabPane.getSelectedTabId().equalsIgnoreCase(uiViewForm.getId())) {
        uiViewTabPane.uiViewForm.revertVersion() ;
        uiViewTabPane.uiViewForm.refresh(true) ;
        uiViewTabPane.setSelectedTab(uiViewForm.getId()) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }

  static  public class EditTabActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UITabForm uiTabForm = uiViewTabPane.getChild(UITabForm.class) ;
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiTabForm.setRendered(true);
      ((UIFormStringInput)uiTabForm.getChildById(UITabForm.FIELD_NAME)).addValidator(MandatoryValidator.class);
      uiViewTabPane.uiViewForm.setViewName(((UIFormStringInput) uiViewTabPane.
          uiViewForm.getChildById(UIViewForm.FIELD_NAME)).getValue());
      uiViewTabPane.uiViewForm.setPermission(((UIFormStringInput) uiViewTabPane.
          uiViewForm.getChildById(UIViewForm.FIELD_PERMISSION)).getValue());
      uiViewTabPane.setSelectedTab(uiTabForm.getId()) ;
      uiViewTabPane.uiViewForm.editTab(tabName);
      uiViewTabPane.uiViewForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }

  static  public class DeleteTabActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      uiViewTabPane.setSelectedTab(uiViewForm.getId()) ;
      uiViewTabPane.uiViewForm.deleteTab(tabName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }

  static  public class RestoreActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiViewForm.changeVersion() ;
      UIViewContainer uiContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      UIViewList uiViewList = uiContainer.findFirstComponentOfType(UIViewList.class) ;
      uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
      uiViewTabPane.uiTabForm.refresh(true) ;
      uiViewTabPane.uiViewForm.refresh(true) ;
      uiViewTabPane.removeChildById(POPUP_PERMISSION) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
}
