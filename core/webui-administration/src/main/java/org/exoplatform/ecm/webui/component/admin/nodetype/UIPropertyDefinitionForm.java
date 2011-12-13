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
package org.exoplatform.ecm.webui.component.admin.nodetype;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 21, 2006
 * 3:36:17 PM
 */
@ComponentConfig(
    template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl"
)
public class UIPropertyDefinitionForm extends UIFormInputSetWithAction {

  final static public String NAMESPACE = "propertyNamespace";
  final static public String DEFINITION_NAME = "propertyname";
  final static public String REQUIRED_TYPE = "requiredType";
  final static public String MULTIPLE = "multiple";
  final static public String MANDATORY = "mandatory";
  final static public String AUTOCREATED = "autoCreated";
  final static public String PROTECTED = "protected";
  final static public String PARENTVERSION = "parentversion";
  final static public String CONSTRAINTS = "constraints";
  final static public String VALUE_CONSTRAINTS = "valueconstraints";
  final static public String TRUE = "true";
  final static public String FALSE = "false";
  final static public String ACTION_UPDATE_PROPERTY = "UpdateProperty";
  final static public String ACTION_CANCEL_PROPERTY = "CancelProperty";

  private String requiredValue_;

  public UIPropertyDefinitionForm(String name) throws Exception {
    super(name);
    setComponentConfig(getClass(), null);
    List<SelectItemOption<String>> autoListItem = new ArrayList<SelectItemOption<String>>();
    autoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    autoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> mandoListItem = new ArrayList<SelectItemOption<String>>();
    mandoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    mandoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> multiListItem = new ArrayList<SelectItemOption<String>>();
    multiListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    multiListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> protectedItem = new ArrayList<SelectItemOption<String>>();
    protectedItem.add(new SelectItemOption<String>(FALSE, FALSE));
    protectedItem.add(new SelectItemOption<String>(TRUE, TRUE));

    addUIFormInput(new UIFormSelectBox(NAMESPACE, NAMESPACE, getNamespaces()));
    addUIFormInput(new UIFormStringInput(DEFINITION_NAME, DEFINITION_NAME, null));
    UIFormSelectBox uiRequired = new UIFormSelectBox(REQUIRED_TYPE, REQUIRED_TYPE, getRequiredTypes());
    uiRequired.setOnChange("ChangeRequiredType");
    addUIFormInput(uiRequired);
    addUIFormInput(new UIFormSelectBox(AUTOCREATED, AUTOCREATED, autoListItem));
    addUIFormInput(new UIFormSelectBox(MANDATORY, MANDATORY, mandoListItem));
    addUIFormInput(new UIFormSelectBox(PARENTVERSION, PARENTVERSION, getParentVersions()));
    addUIFormInput(new UIFormSelectBox(PROTECTED, PROTECTED, protectedItem));
    addUIFormInput(new UIFormSelectBox(MULTIPLE, MULTIPLE, multiListItem));
    addUIFormInput(new UIFormStringInput(VALUE_CONSTRAINTS, VALUE_CONSTRAINTS, null));
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
  }

  private List<SelectItemOption<String>> getParentVersions() {
    List<SelectItemOption<String>> versionItem = new ArrayList<SelectItemOption<String>>();
    versionItem.add(new SelectItemOption<String>("COPY", "1"));
    versionItem.add(new SelectItemOption<String>("VERSION", "2"));
    versionItem.add(new SelectItemOption<String>("INITIALIZE", "3"));
    versionItem.add(new SelectItemOption<String>("COMPUTE", "4"));
    versionItem.add(new SelectItemOption<String>("IGNORE", "5"));
    versionItem.add(new SelectItemOption<String>("ABORT", "6"));
    return versionItem;
  }

  private List<SelectItemOption<String>> getRequiredTypes() {
    List<SelectItemOption<String>> requireType = new ArrayList<SelectItemOption<String>>();
    requireType.add(new SelectItemOption<String>("STRING", "1"));
    requireType.add(new SelectItemOption<String>("BINARY", "2"));
    requireType.add(new SelectItemOption<String>("LONG", "3"));
    requireType.add(new SelectItemOption<String>("DOUBLE", "4"));
    requireType.add(new SelectItemOption<String>("DATE", "5"));
    requireType.add(new SelectItemOption<String>("BOOLEAN", "6"));
    requireType.add(new SelectItemOption<String>("NAME", "7"));
    requireType.add(new SelectItemOption<String>("PATH", "8"));
    requireType.add(new SelectItemOption<String>("REFERENCE", "9"));
    requireType.add(new SelectItemOption<String>("UNDEFINED", "0"));
    return requireType;
  }

  public List<SelectItemOption<String>> getNamespaces() throws Exception {
    List<SelectItemOption<String>> namespacesOptions = new ArrayList<SelectItemOption<String>>();
    String[] namespaces = getApplicationComponent(RepositoryService.class)
                          .getCurrentRepository().getNamespaceRegistry().getPrefixes();
    for(int i = 0; i < namespaces.length; i ++){
      namespacesOptions.add(new SelectItemOption<String>(namespaces[i], namespaces[i]));
    }
    return namespacesOptions;
  }

  public void refresh() throws Exception {
    List<SelectItemOption<String>> autoListItem = new ArrayList<SelectItemOption<String>>();
    autoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    autoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> mandoListItem = new ArrayList<SelectItemOption<String>>();
    mandoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    mandoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> multiListItem = new ArrayList<SelectItemOption<String>>();
    multiListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    multiListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> protectedItem = new ArrayList<SelectItemOption<String>>();
    protectedItem.add(new SelectItemOption<String>(FALSE, FALSE));
    protectedItem.add(new SelectItemOption<String>(TRUE, TRUE));

    getUIFormSelectBox(NAMESPACE).setOptions(getNamespaces()).setDisabled(false);
    getUIStringInput(DEFINITION_NAME).setEditable(true).setValue(null);
    getUIFormSelectBox(REQUIRED_TYPE).setOptions(getRequiredTypes()).setDisabled(false);
    getUIFormSelectBox(AUTOCREATED).setOptions(autoListItem).setDisabled(false);
    getUIFormSelectBox(MANDATORY).setOptions(mandoListItem).setDisabled(false);
    getUIFormSelectBox(PARENTVERSION).setOptions(getParentVersions()).setDisabled(false);
    getUIFormSelectBox(PROTECTED).setOptions(protectedItem).setDisabled(false);
    getUIFormSelectBox(MULTIPLE).setOptions(multiListItem).setDisabled(false);
    getUIStringInput(VALUE_CONSTRAINTS).setEditable(true).setValue(null);
    UINodeTypeForm uiForm = getParent();
    UIFormInputSetWithAction uiPropertyTab = uiForm.getChildById(UINodeTypeForm.PROPERTY_DEFINITION);
    uiForm.setActionInTab(uiPropertyTab);
  }

  private void setRequiredValue(String requiredValue) { requiredValue_ = requiredValue; }

  private String getRequiredValue() { return requiredValue_ == null ? "1" : requiredValue_; }

  public void update(NodeType nodeType, String propertyName) throws Exception {
    if(propertyName != null) {
      PropertyDefinition[] propertyDefinitions = nodeType.getPropertyDefinitions();
      for(int i = 0; i < propertyDefinitions.length; i++) {
        String name = propertyDefinitions[i].getName();
        if(name.equals(propertyName)) {
          if (propertyName.indexOf(":") > -1) {
            getUIStringInput(DEFINITION_NAME).
            setValue(propertyName.substring(propertyName.indexOf(":") + 1));
            getUIFormSelectBox(NAMESPACE).
            setValue(propertyName.substring(0, propertyName.indexOf(":")));
          } else {
            getUIStringInput(DEFINITION_NAME).setValue(propertyName);
            getUIFormSelectBox(NAMESPACE).setValue("");
          }
          String requiredType = Integer.toString(propertyDefinitions[i].getRequiredType());
          getUIFormSelectBox(REQUIRED_TYPE).setValue(requiredType);
          getUIFormSelectBox(MULTIPLE).
          setValue(String.valueOf(propertyDefinitions[i].isMultiple()));
          getUIFormSelectBox(MANDATORY).
          setValue(String.valueOf(propertyDefinitions[i].isMandatory()));
          getUIFormSelectBox(AUTOCREATED).
          setValue(String.valueOf(propertyDefinitions[i].isAutoCreated()));
          getUIFormSelectBox(PROTECTED).
          setValue(String.valueOf(propertyDefinitions[i].isProtected()));
          String[] cons = propertyDefinitions[i].getValueConstraints();
          StringBuilder conValues = new StringBuilder();
          if(cons != null && cons.length > 0) {
            for(int j = 0; j < cons.length; j ++) {
              if(conValues.length() > 0) conValues.append(", ");
              conValues.append(cons[j]);
            }
          }
          getUIStringInput(VALUE_CONSTRAINTS).setValue(conValues.toString());
          String parentVersion = Integer.toString(propertyDefinitions[i].getOnParentVersion());
          getUIFormSelectBox(PARENTVERSION).setValue(parentVersion);
          break;
        }
      }
    }
    getUIFormSelectBox(NAMESPACE).setDisabled(true);
    getUIStringInput(DEFINITION_NAME).setEditable(false);
    getUIFormSelectBox(REQUIRED_TYPE).setDisabled(true);
    getUIFormSelectBox(AUTOCREATED).setDisabled(true);
    getUIFormSelectBox(MANDATORY).setDisabled(true);
    getUIFormSelectBox(PARENTVERSION).setDisabled(true);
    getUIFormSelectBox(PROTECTED).setDisabled(true);
    getUIFormSelectBox(MULTIPLE).setDisabled(true);
    getUIStringInput(VALUE_CONSTRAINTS).setEditable(false);
  }

  private PropertyDefinitionValue getPropertyByName(String propertyName, List<PropertyDefinitionValue> listProperty) {
    for(PropertyDefinitionValue property : listProperty) {
      if(property.getName().equals(propertyName)) return property;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private void setValues(PropertyDefinitionValue property) throws Exception {
    String propertyName = property.getName();
    if (propertyName.indexOf(":") > -1) {
      getUIStringInput(DEFINITION_NAME).
      setValue(propertyName.substring(propertyName.indexOf(":") + 1));
      getUIFormSelectBox(NAMESPACE).setValue(propertyName.substring(0, propertyName.indexOf(":")));
    } else {
      getUIStringInput(DEFINITION_NAME).setValue(propertyName);
      getUIFormSelectBox(NAMESPACE).setValue("");
    }
    String requiredType = Integer.toString(property.getRequiredType());
    getUIFormSelectBox(REQUIRED_TYPE).setValue(requiredType);
    getUIFormSelectBox(MULTIPLE).setValue(String.valueOf(property.isMultiple()));
    getUIFormSelectBox(MANDATORY).setValue(String.valueOf(property.isMandatory()));
    getUIFormSelectBox(AUTOCREATED).setValue(String.valueOf(property.isAutoCreate()));
    getUIFormSelectBox(PROTECTED).setValue(String.valueOf(property.isReadOnly()));
    List<String> cons = property.getValueConstraints();
    StringBuffer valueConstraints = null;
    for (String value : cons) {
      if (valueConstraints == null) {
        valueConstraints = new StringBuffer(value);
      } else {
        valueConstraints.append(",").append(value);
      }
    }
    getUIStringInput(VALUE_CONSTRAINTS).setValue(valueConstraints.toString());
    String parentVersion = Integer.toString(property.getOnVersion());
    getUIFormSelectBox(PARENTVERSION).setValue(parentVersion);
  }

  static public class RemovePropertyActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      String propertyName = event.getRequestContext().getRequestParameter(OBJECTID);
      if(propertyName == null || propertyName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name", null));
        return;
      }
      for(PropertyDefinitionValue property : uiForm.addedPropertiesDef_) {
        if(property.getName().equals(propertyName)) {
          uiForm.addedPropertiesDef_.remove(property);
          break;
        }
      }
      uiForm.setPropertyValue(uiForm.addedPropertiesDef_);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class EditPropertyActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      String propertyName = event.getRequestContext().getRequestParameter(OBJECTID);
      if(propertyName == null || propertyName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name", null));       
        return;
      }
      PropertyDefinitionValue property =
        uiPropertyForm.getPropertyByName(propertyName, uiForm.addedPropertiesDef_);
      uiPropertyForm.setValues(property);
      uiPropertyForm.setRequiredValue(Integer.toString(property.getRequiredType()));
      UIFormInputSetWithAction propertyTab = uiForm.getChildById(UINodeTypeForm.PROPERTY_DEFINITION);
      String[] actionNames = {ACTION_UPDATE_PROPERTY, ACTION_CANCEL_PROPERTY};
      String[] fieldNames = {propertyName, null};
      propertyTab.setActions(actionNames, fieldNames);
      uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  @SuppressWarnings("unchecked")
  static public class UpdatePropertyActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      String propertyName = event.getRequestContext().getRequestParameter(OBJECTID);
      PropertyDefinitionValue propertyInfo =
        uiPropertyForm.getPropertyByName(propertyName, uiForm.addedPropertiesDef_);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String prefix = uiForm.getUIFormSelectBox(NAMESPACE).getValue();
      String name = uiPropertyForm.getUIStringInput(DEFINITION_NAME).getValue();
      if(name == null || name.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name", null));        
        return;
      }
      for (int i = 0; i < name.length(); i++) {
        char c = name.charAt(i);
        if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
          || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c=='[' || c==']' || c==',') {
          continue ;
        }
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name",
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      }
      if (propertyInfo == null) {
        propertyInfo = new PropertyDefinitionValue();
      }
      if (prefix != null && prefix.length() > 0) {
        propertyInfo.setName(prefix + ":" + name);
      } else {
        propertyInfo.setName(name);
      }
      
      String requiredType = uiForm.getUIFormSelectBox(REQUIRED_TYPE).getValue();
      propertyInfo.setRequiredType(Integer.parseInt(requiredType));
      String isMultiple = uiForm.getUIFormSelectBox(MULTIPLE).getValue();
      propertyInfo.setMultiple(Boolean.parseBoolean(isMultiple));
      String isMandatory = uiForm.getUIFormSelectBox(MANDATORY).getValue();
      propertyInfo.setMandatory(Boolean.parseBoolean(isMandatory));
      String autoCreate = uiForm.getUIFormSelectBox(AUTOCREATED).getValue();
      propertyInfo.setAutoCreate(Boolean.parseBoolean(autoCreate));
      String isProtected = uiForm.getUIFormSelectBox(PROTECTED).getValue();
      propertyInfo.setReadOnly(Boolean.parseBoolean(isProtected));

      String onParent = uiForm.getUIFormSelectBox(PARENTVERSION).getValue();
      propertyInfo.setOnVersion(Integer.parseInt(onParent));
      String valueConstraints = uiForm.getUIStringInput(VALUE_CONSTRAINTS).getValue();
      List<String> constraintValues = new ArrayList<String>();
      if(valueConstraints != null) {
        if(valueConstraints.indexOf(",") > -1) {
          String[] arrValues = valueConstraints.split(",");
          for(int i = 0; i < arrValues.length; i++) {
            constraintValues.add(arrValues[i]);
          }
        } else {
          constraintValues.add(valueConstraints);
        }
      }
      propertyInfo.setValueConstraints(constraintValues);
      uiForm.setPropertyValue(uiForm.addedPropertiesDef_);
      uiPropertyForm.refresh();
      UIFormInputSetWithAction propertyTab = uiForm.getChildById(UINodeTypeForm.PROPERTY_DEFINITION);
      propertyTab.setActions(new String[] {UINodeTypeForm.ADD_PROPERTY}, null);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  @SuppressWarnings("unchecked")
  static public class AddPropertyActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String prefix = uiForm.getUIFormSelectBox(NAMESPACE).getValue();
      String propertyName =
        uiForm.getUIStringInput(DEFINITION_NAME).getValue();
      if(propertyName == null || propertyName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name", null));        
        return;
      }

      for(int i = 0; i < propertyName.length(); i ++){
        char c = propertyName.charAt(i);
        if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
          || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c=='[' || c==']' || c==',') {
          continue ;
        }
        uiApp.addMessage(new ApplicationMessage("UIPropertyDefinitionForm.msg.property-name",
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      }
      PropertyDefinitionValue propertyInfo = new PropertyDefinitionValue();
      if (prefix != null && prefix.length() > 0) {
        propertyInfo.setName(prefix + ":" + propertyName);
      } else {
        propertyInfo.setName(propertyName);
      }
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      

      String requiredType = uiForm.getUIFormSelectBox(REQUIRED_TYPE).getValue();
      propertyInfo.setRequiredType(Integer.parseInt(requiredType));
      String multipleValue = uiForm.getUIFormSelectBox(MULTIPLE).getValue();
      propertyInfo.setMultiple(Boolean.parseBoolean(multipleValue));
      String mandatory = uiForm.getUIFormSelectBox(MANDATORY).getValue();
      propertyInfo.setMandatory(Boolean.parseBoolean(mandatory));
      String autoCreate = uiForm.getUIFormSelectBox(AUTOCREATED).getValue();
      propertyInfo.setAutoCreate(Boolean.parseBoolean(autoCreate));
      String isProtected = uiForm.getUIFormSelectBox(PROTECTED).getValue();
      propertyInfo.setReadOnly(Boolean.parseBoolean(isProtected));
      String onParent = uiForm.getUIFormSelectBox(PARENTVERSION).getValue();
      propertyInfo.setOnVersion(Integer.parseInt(onParent));
      String valueConstraints = uiForm.getUIStringInput(VALUE_CONSTRAINTS).getValue();
      List<String> constraintValues = new ArrayList<String>();
      if(valueConstraints != null) {
        if(valueConstraints.indexOf(",") > -1) {
          String[] arrValues = valueConstraints.split(",");
          for(int i = 0; i < arrValues.length; i++) {
            constraintValues.add(arrValues[i].toString());
          }
        } else {
          constraintValues.add(valueConstraints);
        }
      }
      propertyInfo.setValueConstraints(constraintValues);
      uiForm.addedPropertiesDef_.add(propertyInfo);
      uiForm.setPropertyValue(uiForm.addedPropertiesDef_);
      uiPropertyForm.refresh();
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      UIFormInputSetWithAction nodeTypeTab = uiForm.getChildById(UINodeTypeForm.NODETYPE_DEFINITION);
      nodeTypeTab.setIsView(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class ChangeRequiredTypeActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      String value = uiForm.getUIFormSelectBox(REQUIRED_TYPE).getValue();
      uiPropertyForm.setRequiredValue(value);
      if(value.equals("1") || value.equals("9")) {
        uiForm.getUIStringInput(VALUE_CONSTRAINTS).setEditable(true);
      } else {
        uiForm.getUIStringInput(VALUE_CONSTRAINTS).setEditable(false);
      }
      uiForm.getUIStringInput(VALUE_CONSTRAINTS).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class AddConstraintsActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      uiForm.removeChildById("Contraints");
      String values = uiForm.getUIStringInput(VALUE_CONSTRAINTS).getValue();
      UIFormInputSetWithAction constraintTab = null;
      if(uiPropertyForm.getRequiredValue().equals("9")) {
        constraintTab = new UINodeTypeOptionList("Contraints");
        uiForm.addUIComponentInput(constraintTab);
        uiForm.setSelectedTab(constraintTab.getId());
        UINodeTypeOptionList uiOptionList = uiForm.getChild(UINodeTypeOptionList.class);
        uiOptionList.update(values);
      } else if(uiPropertyForm.getRequiredValue().equals("1")) {
        constraintTab = new UIFormInputSetWithAction("Contraints");
        UIFormMultiValueInputSet valuesConstraint =
          uiPropertyForm.createUIComponent(UIFormMultiValueInputSet.class, null, CONSTRAINTS);
        valuesConstraint.setType(UIFormStringInput.class);
        constraintTab.addUIFormInput(valuesConstraint);
        uiForm.addUIComponentInput(constraintTab);
        uiForm.setSelectedTab(constraintTab.getId());
      } else {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        String message = "UIPropertyDefinitionForm.msg.not-supported-value-constraints";
        uiApp.addMessage(new ApplicationMessage(message, null));        
        return;
      }
      uiForm.setSelectedTab("Contraints");
      String[] actionNames = new String[] {"AddValue", "CancelConstraints"};
      constraintTab.setActions(actionNames, null);      
    }
  }

  static public class CancelPropertyActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      uiPropertyForm.refresh();
      UIFormInputSetWithAction childTab = uiForm.getChildById(UINodeTypeForm.PROPERTY_DEFINITION);
      childTab.setActions(new String[] {UINodeTypeForm.ADD_PROPERTY} , null);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class AddActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      uiForm.setSelectedTab("Contraints");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class RemoveActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      uiForm.setSelectedTab("Contraints");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  @SuppressWarnings("unchecked")
  static public class AddValueActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIPropertyDefinitionForm uiPropertyForm = uiForm.getChild(UIPropertyDefinitionForm.class);
      StringBuffer strValues = null;
      if(uiPropertyForm.getRequiredValue().equals("9")) {
        List<String> selectedNodes = new ArrayList<String>();
        List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
        uiForm.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
        int count = 0;
        for(int i = 0; i < listCheckbox.size(); i ++) {
          if(listCheckbox.get(i).isChecked()) {
            selectedNodes.add(listCheckbox.get(i).getName());
            count ++;
          }
        }
        for (int i = 0; i < selectedNodes.size(); i++) {
          if (strValues == null) {
            strValues = new StringBuffer(selectedNodes.get(i));
          } else {
            strValues.append(",").append(selectedNodes.get(i));
          }
        }
      } else if(uiPropertyForm.getRequiredValue().equals("1")) {
        UIFormMultiValueInputSet uiMulti = uiForm.getUIInput(CONSTRAINTS);
        List<String> constraintValues = (List<String>)uiMulti.getValue();
        if(constraintValues == null) constraintValues = new ArrayList<String>();
        for (int i = 0; i < constraintValues.size(); i++) {
          if (strValues == null) {
            strValues = new StringBuffer(constraintValues.get(i));
          } else {
            strValues.append(",").append(constraintValues.get(i));
          }
        }

      }
      uiForm.getUIStringInput(VALUE_CONSTRAINTS).setValue(strValues.toString());
      uiForm.removeChildById("Contraints");
      uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class CancelConstraintsActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      uiForm.removeChildById("Contraints");
      uiForm.setTabRender(UINodeTypeForm.PROPERTY_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
}
