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

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.form.validator.NodeTypeNameValidator;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.scripts.impl.ScriptServiceImpl;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

import com.ibm.icu.text.Transliterator;

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

  final static public String FIELD_SCRIPT = "script" ;
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_VARIABLES = "variables" ;
  public static final String ACTION_TYPE = "exo:scriptAction";
  
  private String actionName_;
  
  private boolean isUpdate = false;

  public UIFormMultiValueInputSet uiFormMultiValue = null ;

  public UIActionTypeForm() throws Exception {
    
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
        addValidator(MandatoryValidator.class).addValidator(NodeTypeNameValidator.class));
    UIFormSelectBox actionExecutables = new UIFormSelectBox(FIELD_SCRIPT,FIELD_SCRIPT,
        new ArrayList<SelectItemOption<String>>());
    addUIFormInput(actionExecutables) ;
    setActions( new String[]{"Save", "Cancel"}) ;
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

  private List<SelectItemOption<String>> getScriptOptions() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    ScriptServiceImpl scriptService = WCMCoreUtils.getService(ScriptServiceImpl.class);
    List<Node> scriptOptions = scriptService.getECMActionScripts(WCMCoreUtils.getSystemSessionProvider());
    String baseScriptPath = scriptService.getBaseScriptPath();
    for(Node script : scriptOptions) {
      SelectItemOption<String> itemOption =
          new SelectItemOption<String>(script.getName(), StringUtils.substringAfter(script.getPath(), baseScriptPath + "/"));
      options.add(itemOption);
    }
    return options ;
  }

  public void refresh() throws Exception{
    reset() ;
    getUIStringInput(FIELD_NAME).setValue("") ;
    List<SelectItemOption<String>> scriptOptions = getScriptOptions() ;
    getUIFormSelectBox(FIELD_SCRIPT).setOptions(scriptOptions);
    initMultiValuesField() ;
  }
  
  public void update(String actionName, String actionLabel) throws Exception {
    isUpdate = true;
    ScriptServiceImpl scriptService = WCMCoreUtils.getService(ScriptServiceImpl.class);
    NodeTypeManager ntManager = WCMCoreUtils.getRepository().getNodeTypeManager();
    NodeType nodeType = ntManager.getNodeType(actionName);
    actionName_ = actionName;
    String resourceName = scriptService.getResourceNameByNodeType(nodeType);
    getUIStringInput(FIELD_NAME).setValue(actionLabel);
    getUIFormSelectBox(FIELD_SCRIPT).setOptions(getScriptOptions()).setValue(resourceName);
    List<String> valueList = new ArrayList<String>();
    PropertyDefinition[] proDefs = nodeType.getPropertyDefinitions();
    for(PropertyDefinition pro : proDefs) {
      //Check if require type is STRING
      if(pro.isProtected() || pro.isAutoCreated() || pro.isMultiple() || pro.isMandatory()) continue;
      if(pro.getRequiredType() == 1 && pro.getOnParentVersion() == OnParentVersionAction.COPY) {
        valueList.add(pro.getName());
      }
    }
    initMultiValuesField() ;
    uiFormMultiValue.setValue(valueList);
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
      String repository = WCMCoreUtils.getRepository().getConfiguration().getName() ;
      UIActionManager uiActionManager = uiForm.getAncestorOfType(UIActionManager.class) ;
      ActionServiceContainer actionServiceContainer =
        uiForm.getApplicationComponent(ActionServiceContainer.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String actionLabel = uiForm.getUIStringInput(FIELD_NAME).getValue();
      String actionName = "exo:" + cleanString(actionLabel);

      if(uiForm.isUpdate) actionName = uiForm.actionName_;

      List<String> variables = new ArrayList<String>();
      List values = uiForm.uiFormMultiValue.getValue();
      if(values != null && values.size() > 0) {
        for(Object value : values) {
          variables.add((String)value) ;
        }
      }
      if(!uiForm.isUpdate) {
        for(NodeType nodeType : actionServiceContainer.getCreatedActionTypes(repository)) {
          if(actionName.equals(nodeType.getName())) {
            uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-exist", null, ApplicationMessage.WARNING)) ;          
            return ;
          }
        }
      }
      try {
        String script = uiForm.getUIFormSelectBox(FIELD_SCRIPT).getValue() ;
        actionServiceContainer.createActionType(actionName, ACTION_TYPE, script, actionLabel, variables,
                                                false, uiForm.isUpdate);
        uiActionManager.refresh() ;
        uiForm.refresh() ;
        uiActionManager.removeChild(UIPopupWindow.class) ;
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-type-create-error", new Object[] {actionLabel},
                                                ApplicationMessage.WARNING)) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionManager) ;
    }
    private String cleanString(String input){
      Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
      input = accentsconverter.transliterate(input);
      return input.trim();
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
