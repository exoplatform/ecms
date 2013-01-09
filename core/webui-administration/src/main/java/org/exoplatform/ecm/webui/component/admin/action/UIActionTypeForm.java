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
package org.exoplatform.ecm.webui.component.admin.action;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionPlugin;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 * phamtuanchip@yahoo.de September 20, 2006 04:27:15 PM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIActionTypeForm.SaveActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.ChangeTypeActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.CancelActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.AddActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.RemoveActionListener.class)
    }
)
public class UIActionTypeForm extends UIForm {

  final static public String FIELD_EXECUTEACTION = "executeAction" ;
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_VARIABLES = "variables" ;
  public static final String ACTION_TYPE = "exo:scriptAction";

  public UIFormMultiValueInputSet uiFormMultiValue = null ;

  public UIActionTypeForm() throws Exception {
    
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
        addValidator(MandatoryValidator.class)) ;
    UIFormSelectBox actionExecutables = new UIFormSelectBox(FIELD_EXECUTEACTION,FIELD_EXECUTEACTION,
        new ArrayList<SelectItemOption<String>>());
    addUIFormInput(actionExecutables) ;
    setActions( new String[]{"Save", "Cancel"}) ;
  }

  private List<SelectItemOption<String>> getActionTypesValues() throws Exception {
    ActionServiceContainer actionServiceContainer =
      getApplicationComponent(ActionServiceContainer.class) ;
    List <String> actionsTypes= (List <String>) actionServiceContainer.getActionPluginNames();
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    List<String> lstOpt = new ArrayList<String>();
    for(int i =0 ; i< actionsTypes.size() ; i++){
      if(!lstOpt.contains(actionsTypes.get(i))) {
        options.add(new SelectItemOption<String>(actionsTypes.get(i),actionsTypes.get(i))) ;
        lstOpt.add(actionsTypes.get(i));
      }
    }
    return options ;
  }

  private void initMultiValuesField() throws Exception {
    if( uiFormMultiValue != null ) removeChildById(FIELD_VARIABLES);
    uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class, null, null) ;
    uiFormMultiValue.setId(FIELD_VARIABLES) ;
    uiFormMultiValue.setName(FIELD_VARIABLES) ;
    uiFormMultiValue.setType(UIFormStringInput.class) ;
    List<String> list = new ArrayList<String>() ;
    list.add("");
    uiFormMultiValue.setValue(list) ;
    addUIFormInput(uiFormMultiValue) ;
  }

  private List<SelectItemOption<String>> getExecutableOptions(String actionTypeName) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    ActionServiceContainer actionServiceContainer =
      getApplicationComponent(ActionServiceContainer.class) ;
    ActionPlugin actionPlugin = actionServiceContainer.getActionPluginForActionType(actionTypeName) ;
    List<String> executables = (List<String>)actionPlugin.getActionExecutables();
    for(String actionExec : executables) {
      options.add(new SelectItemOption<String>(actionExec, actionExec)) ;
    }
    return options ;
  }

  public void refresh() throws Exception{
    reset() ;
    List<SelectItemOption<String>> actionOptions = getActionTypesValues() ;
    String actionTypeName = actionOptions.get(0).getValue() ;
    
    
    getUIStringInput(FIELD_NAME).setValue("") ;
    List<SelectItemOption<String>> executableOptions = getExecutableOptions(actionTypeName) ;
    getUIFormSelectBox(FIELD_EXECUTEACTION).setOptions(executableOptions) ;
    initMultiValuesField() ;
  }

  static public class ChangeTypeActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }

  @SuppressWarnings("unused")
  static public class SaveActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource() ;
      PortletRequestContext context = (PortletRequestContext) event.getRequestContext() ;
      PortletPreferences preferences = context.getRequest().getPreferences() ;
      String repository = preferences.getValue(Utils.REPOSITORY, "") ;
      UIActionManager uiActionManager = uiForm.getAncestorOfType(UIActionManager.class) ;
      ActionServiceContainer actionServiceContainer =
        uiForm.getApplicationComponent(ActionServiceContainer.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String actionName = uiForm.getUIStringInput(FIELD_NAME).getValue();
      Object[] args = {actionName} ;
      String[] arrFilterChar = {"&", "$", "^", "(", ")", "@", "]", "[", "*", "%", "!", "+"} ;
      String[] arrActionNames = actionName.split(":") ;
      for(String filterChar : arrFilterChar) {
        if(actionName.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.fileName-invalid", null,
              ApplicationMessage.WARNING)) ;
          return ;
        }
      }
      if(!actionName.startsWith("exo:")) {
        uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-name-invalid", args,
                                                 ApplicationMessage.WARNING)) ;
        return ;
      }
      List<String> variables = new ArrayList<String>();
      List values = uiForm.uiFormMultiValue.getValue();
      if(values != null && values.size() > 0) {
        for(Object value : values) {
          variables.add((String)value) ;
        }
      }
      for(NodeType nodeType : actionServiceContainer.getCreatedActionTypes(repository)) {
        if(actionName.equals(nodeType.getName())) {
          uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-exist", null)) ;          
          return ;
        }
      }
      try {
        String execute = uiForm.getUIFormSelectBox(FIELD_EXECUTEACTION).getValue() ;
        actionServiceContainer.createActionType(actionName, ACTION_TYPE, execute, variables,
                                                false, repository);
        uiActionManager.refresh() ;
        uiForm.refresh() ;
        uiActionManager.removeChild(UIPopupWindow.class) ;
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-type-create-error", args,
                                                ApplicationMessage.WARNING)) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionManager) ;
    }
  }

  static public class CancelActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      uiForm.reset() ;
      UIActionManager uiActionManager = uiForm.getAncestorOfType(UIActionManager.class) ;
      uiActionManager.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionManager) ;
    }
  }

  static public class AddActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }

  static public class RemoveActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }
}
