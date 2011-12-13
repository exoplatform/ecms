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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 22, 2006
 * 11:50:10 AM
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl")
public class UIChildNodeDefinitionForm extends UIFormInputSetWithAction {

  final static public String NAMESPACE = "childNamespace";
  final static public String CHILD_NAME = "childNodename";
  final static public String REQUIRED_PRIMARY_TYPE = "requiredPrimaryType";
  final static public String MULTIPLE = "childMultiple";
  final static public String MANDATORY = "childMandatory";
  final static public String AUTOCREATED = "childAutocreated";
  final static public String PROTECTED = "childProtected";
  final static public String PARENTVERSION = "childParentversion";
  final static public String DEFAULT_PRIMARY_TYPE = "defaultPrimaryType";
  final static public String SAME_NAME = "sameNameSiblings";
  final static public String TRUE = "true";
  final static public String FALSE = "false";
  final static public String ACTION_UPDATE_CHILD = "UpdateChild";
  final static public String ACTION_CANCEL_CHILD = "CancelChild";

  public UIChildNodeDefinitionForm(String name) throws Exception {
    super(name);
    setComponentConfig(getClass(), null);
    List<SelectItemOption<String>> autoListItem = new ArrayList<SelectItemOption<String>>();
    autoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    autoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> mandoListItem = new ArrayList<SelectItemOption<String>>();
    mandoListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    mandoListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> sameNameListItem = new ArrayList<SelectItemOption<String>>();
    sameNameListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    sameNameListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> protectedItem = new ArrayList<SelectItemOption<String>>();
    protectedItem.add(new SelectItemOption<String>(FALSE, FALSE));
    protectedItem.add(new SelectItemOption<String>(TRUE, TRUE));

    addUIFormInput(new UIFormSelectBox(NAMESPACE, NAMESPACE, getNamespaces()));
    addUIFormInput(new UIFormStringInput(CHILD_NAME, CHILD_NAME, null));
    addUIFormInput(new UIFormStringInput(DEFAULT_PRIMARY_TYPE, DEFAULT_PRIMARY_TYPE, null));
    addUIFormInput(new UIFormSelectBox(AUTOCREATED, AUTOCREATED, autoListItem));
    addUIFormInput(new UIFormSelectBox(MANDATORY, MANDATORY, mandoListItem));
    addUIFormInput(new UIFormSelectBox(PARENTVERSION, PARENTVERSION, getParentVersions()));
    addUIFormInput(new UIFormSelectBox(PROTECTED, PROTECTED, protectedItem));
    addUIFormInput(new UIFormSelectBox(SAME_NAME, SAME_NAME, sameNameListItem));
    addUIFormInput(new UIFormStringInput(REQUIRED_PRIMARY_TYPE, REQUIRED_PRIMARY_TYPE, null));
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

  private List<SelectItemOption<String>> getNamespaces() throws Exception {
    List<SelectItemOption<String>> namespacesOptions = new ArrayList<SelectItemOption<String>>();
    String[] namespaces = getApplicationComponent(RepositoryService.class)
                          .getCurrentRepository().getNamespaceRegistry().getPrefixes();
    for( int i = 0; i < namespaces.length; i ++){
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

    List<SelectItemOption<String>> sameNameListItem = new ArrayList<SelectItemOption<String>>();
    sameNameListItem.add(new SelectItemOption<String>(FALSE, FALSE));
    sameNameListItem.add(new SelectItemOption<String>(TRUE, TRUE));

    List<SelectItemOption<String>> protectedItem = new ArrayList<SelectItemOption<String>>();
    protectedItem.add(new SelectItemOption<String>(FALSE, FALSE));
    protectedItem.add(new SelectItemOption<String>(TRUE, TRUE));

    getUIFormSelectBox(NAMESPACE).setOptions(getNamespaces()).setDisabled(false);
    getUIStringInput(CHILD_NAME).setEditable(true).setValue(null);
    getUIStringInput(DEFAULT_PRIMARY_TYPE).setEditable(true).setValue(null);
    getUIFormSelectBox(AUTOCREATED).setOptions(autoListItem).setDisabled(false);
    getUIFormSelectBox(MANDATORY).setOptions(mandoListItem).setDisabled(false);
    getUIFormSelectBox(PARENTVERSION).setOptions(getParentVersions()).setDisabled(false);
    getUIFormSelectBox(PROTECTED).setOptions(protectedItem).setDisabled(false);
    getUIFormSelectBox(SAME_NAME).setOptions(sameNameListItem).setDisabled(false);
    getUIStringInput(REQUIRED_PRIMARY_TYPE).setEditable(true).setValue(null);
    UINodeTypeForm uiForm = getParent();
    UIFormInputSetWithAction uiChildTab = uiForm.getChildById(UINodeTypeForm.CHILDNODE_DEFINITION);
    uiForm.setActionInTab(uiChildTab);
  }

  public void update(NodeType nodeType, String childNodeName) throws Exception {
    if(childNodeName != null) {
      NodeDefinition[] nodeDefinitions = nodeType.getChildNodeDefinitions();
      for(int i = 0; i < nodeDefinitions.length; i++) {
        String name = nodeDefinitions[i].getName();
        if(name.equals(childNodeName)) {
          if(childNodeName.indexOf(":") > -1) {
            String[] arr = childNodeName.split(":");
            getUIFormSelectBox(NAMESPACE).setValue(arr[0].trim());
            getUIStringInput(CHILD_NAME).setValue(arr[1].trim());
          } else {
            getUIFormSelectBox(NAMESPACE).setValue("");
            getUIStringInput(CHILD_NAME).setValue(childNodeName);
          }
          NodeType defaultNodeType = nodeDefinitions[i].getDefaultPrimaryType();
          if(defaultNodeType != null) {
            getUIStringInput(DEFAULT_PRIMARY_TYPE).setValue(defaultNodeType.getName());
          }
          String sameName = String.valueOf(nodeDefinitions[i].allowsSameNameSiblings());
          getUIFormSelectBox(SAME_NAME).setValue(sameName);
          getUIFormSelectBox(MANDATORY).setValue(String.valueOf(nodeDefinitions[i].isMandatory()));
          getUIFormSelectBox(AUTOCREATED).setValue(String.valueOf(nodeDefinitions[i].isAutoCreated()));
          getUIFormSelectBox(PROTECTED).setValue(String.valueOf(nodeDefinitions[i].isProtected()));
          String parentVersion = Integer.toString(nodeDefinitions[i].getOnParentVersion());
          getUIFormSelectBox(PARENTVERSION).setValue(parentVersion);
          StringBuilder requiredType = new StringBuilder();
          NodeType[] requiredPrimaryType = nodeDefinitions[i].getRequiredPrimaryTypes();
          for(int j = 0; j < requiredPrimaryType.length; j ++){
            if(requiredType.length() > 0) requiredType.append(" , ");
            requiredType.append(requiredPrimaryType[j].getName());
          }
          getUIStringInput(REQUIRED_PRIMARY_TYPE).setValue(requiredType.toString());
          break;
        }
      }
    }
    getUIFormSelectBox(NAMESPACE).setDisabled(true);
    getUIStringInput(CHILD_NAME).setEditable(false);
    getUIStringInput(DEFAULT_PRIMARY_TYPE).setEditable(false);
    getUIFormSelectBox(AUTOCREATED).setDisabled(true);
    getUIFormSelectBox(MANDATORY).setDisabled(true);
    getUIFormSelectBox(PARENTVERSION).setDisabled(true);
    getUIFormSelectBox(PROTECTED).setDisabled(true);
    getUIFormSelectBox(SAME_NAME).setDisabled(true);
    getUIStringInput(REQUIRED_PRIMARY_TYPE).setEditable(false);
  }

  @SuppressWarnings("unchecked")
  private void setValues(NodeDefinitionValue node) {
    String childNodeName = node.getName();
    if(childNodeName.indexOf(":") > -1) {
      String[] arr = childNodeName.split(":");
      getUIFormSelectBox(NAMESPACE).setValue(arr[0].trim());
      getUIStringInput(CHILD_NAME).setValue(arr[1].trim());
    } else {
      getUIFormSelectBox(NAMESPACE).setValue("");
      getUIStringInput(CHILD_NAME).setValue(childNodeName);
    }
    getUIStringInput(DEFAULT_PRIMARY_TYPE).setValue(node.getDefaultNodeTypeName());
    getUIFormSelectBox(SAME_NAME).setValue(String.valueOf(node.isSameNameSiblings()));
    getUIFormSelectBox(MANDATORY).setValue(String.valueOf(node.isMandatory()));
    getUIFormSelectBox(AUTOCREATED).setValue(String.valueOf(node.isAutoCreate()));
    getUIFormSelectBox(PROTECTED).setValue(String.valueOf(node.isReadOnly()));
    String parentVersion = Integer.toString(node.getOnVersion());
    getUIFormSelectBox(PARENTVERSION).setValue(parentVersion);
    List<String> requiredPrimaryType = node.getRequiredNodeTypeNames();
    StringBuilder listRequired = new StringBuilder();
    for(int j = 0; j < requiredPrimaryType.size(); j ++){
      if(listRequired.length() < 1) {
        listRequired.append(requiredPrimaryType.get(j));
      } else {
        listRequired.append(",").append(requiredPrimaryType.get(j));
      }
    }
    getUIStringInput(REQUIRED_PRIMARY_TYPE).setValue(listRequired.toString());
  }

  private NodeDefinitionValue getChildNodeByName(String nodeName,
                                                 List<NodeDefinitionValue> listNode) {
    for(NodeDefinitionValue node : listNode) {
      if(node.getName().equals(nodeName)) return node;
    }
    return null;
  }

  static public class RemoveChildNodeActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      String nodeName = event.getRequestContext().getRequestParameter(OBJECTID);
      for(NodeDefinitionValue node : uiForm.addedChildDef_) {
        if(node.getName().equals(nodeName)) {
          uiForm.addedChildDef_.remove(node);
          break;
        }
      }
      uiForm.setChildValue(uiForm.addedChildDef_);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class EditChildNodeActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIChildNodeDefinitionForm uiChildNodeForm = uiForm.getChild(UIChildNodeDefinitionForm.class);
      String nodeName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if(nodeName == null || nodeName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        uiApp.addMessage(new ApplicationMessage("UIChildNodeDefinitionForm.msg.child-name", null));
        return;
      }
      for(int i = 0; i < nodeName.length(); i ++){
        char c = nodeName.charAt(i);
        if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
          || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c=='[' || c==']' || c==',') {
          continue ;
        }
        uiApp.addMessage(new ApplicationMessage(
            "UIChildNodeDefinitionForm.msg.child-invalid", null,
            ApplicationMessage.WARNING));
        return;
      }
      NodeDefinitionValue nodeDefValue =
        uiChildNodeForm.getChildNodeByName(nodeName, uiForm.addedChildDef_);
      uiChildNodeForm.setValues(nodeDefValue);
      UIFormInputSetWithAction childTab = uiForm.getChildById(UINodeTypeForm.CHILDNODE_DEFINITION);
      String[] actionNames = {ACTION_UPDATE_CHILD, ACTION_CANCEL_CHILD};
      String[] fieldNames = {nodeName, null};
      childTab.setActions(actionNames , fieldNames);
      uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class UpdateChildActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIChildNodeDefinitionForm uiChildNodeForm = uiForm.getChild(UIChildNodeDefinitionForm.class);
      String nodeName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      NodeDefinitionValue nodeTypeValue =
        uiChildNodeForm.getChildNodeByName(nodeName, uiForm.addedChildDef_);
      ApplicationMessage message;
      String prefix = uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.NAMESPACE).getValue();
      String childNodeName =
        uiForm.getUIStringInput(UIChildNodeDefinitionForm.CHILD_NAME).getValue();
      if(childNodeName == null || childNodeName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        uiApp.addMessage(new ApplicationMessage("UIChildNodeDefinitionForm.msg.child-name", null));
        return;
      }
      for(int i = 0; i < childNodeName.length(); i ++){
        char c = childNodeName.charAt(i);
        if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
          || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c=='[' || c==']' || c==',') {
          continue ;
        }
        uiApp.addMessage(new ApplicationMessage("UIChildNodeDefinitionForm.msg.child-invalid",
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      }
      if(prefix != null && prefix.length() > 0) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append(":").append(childNodeName);
        childNodeName = sb.toString();
      }
      if (nodeTypeValue == null) {
        nodeTypeValue = new NodeDefinitionValue();
      }
      nodeTypeValue.setName(childNodeName);
      String defaultType =
        uiForm.getUIStringInput(UIChildNodeDefinitionForm.DEFAULT_PRIMARY_TYPE).getValue();
      if(defaultType == null || defaultType.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        message = new ApplicationMessage("UIChildNodeDefinitionForm.msg.defaultPrimaryType", null);
        uiApp.addMessage(message);
        return;
      }
      nodeTypeValue.setDefaultNodeTypeName(defaultType);
      String isSameName =
        uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.SAME_NAME).getValue();
      nodeTypeValue.setSameNameSiblings(Boolean.parseBoolean(isSameName));
      String isMandatory =
        uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.MANDATORY).getValue();
      nodeTypeValue.setMandatory(Boolean.parseBoolean(isMandatory));
      String autoCreate =
        uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.AUTOCREATED).getValue();
      nodeTypeValue.setAutoCreate(Boolean.parseBoolean(autoCreate));
      String isProtected =
        uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.PROTECTED).getValue();
      nodeTypeValue.setReadOnly(Boolean.parseBoolean(isProtected));
      String parentVer =
        uiForm.getUIFormSelectBox(UIChildNodeDefinitionForm.PARENTVERSION).getValue();
      nodeTypeValue.setOnVersion(Integer.parseInt(parentVer));
      String requiredType =
        uiForm.getUIStringInput(UIChildNodeDefinitionForm.REQUIRED_PRIMARY_TYPE).getValue();
      if(requiredType == null || requiredType.length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        message = new ApplicationMessage("UIChildNodeDefinitionForm.msg.requiredPrimaryType", null);
        uiApp.addMessage(message);
        return;
      }
      String[] types = requiredType.split(",");
      List<String> reqList = new ArrayList<String>();
      for(int i = 0; i < types.length; i++) {
        reqList.add(types[i].trim());
      }
      nodeTypeValue.setRequiredNodeTypeNames(reqList);
      uiForm.setChildValue(uiForm.addedChildDef_);
      uiChildNodeForm.refresh();
      UIFormInputSetWithAction childTab = uiForm.getChildById(UINodeTypeForm.CHILDNODE_DEFINITION);
      childTab.setActions(new String[] {UINodeTypeForm.ADD_CHILD}, null);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class AddChildActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      NodeDefinitionValue nodeTypeValue = new NodeDefinitionValue();
      String prefix = uiForm.getUIFormSelectBox(NAMESPACE).getValue();
      ApplicationMessage message;
      String childNodeName = uiForm.getUIStringInput(CHILD_NAME).getValue();
      if(childNodeName == null || childNodeName.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        uiApp.addMessage(new ApplicationMessage("UIChildNodeDefinitionForm.msg.child-name", null));
        return;
      }
      for(int i = 0; i < childNodeName.length(); i ++){
        char c = childNodeName.charAt(i);
        if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
          || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c=='[' || c==']' || c==',') {
          continue ;
        }
        uiApp.addMessage(new ApplicationMessage(
            "UIChildNodeDefinitionForm.msg.child-invalid", null,
            ApplicationMessage.WARNING));
        return;
      }
      if (prefix != null && prefix.length() > 0) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append(":").append(childNodeName);
        childNodeName = sb.toString();
      }
      nodeTypeValue.setName(childNodeName);
      String defaultType =
        uiForm.getUIStringInput(DEFAULT_PRIMARY_TYPE).getValue();
      if(defaultType == null || defaultType.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        message = new ApplicationMessage("UIChildNodeDefinitionForm.msg.defaultPrimaryType", null);
        uiApp.addMessage(message);
        return;
      }
      nodeTypeValue.setDefaultNodeTypeName(defaultType);
      String isSameName = uiForm.getUIFormSelectBox(SAME_NAME).getValue();
      nodeTypeValue.setSameNameSiblings(Boolean.parseBoolean(isSameName));
      String madatory = uiForm.getUIFormSelectBox(MANDATORY).getValue();
      nodeTypeValue.setMandatory(Boolean.parseBoolean(madatory));
      String autoCreate = uiForm.getUIFormSelectBox(AUTOCREATED).getValue();
      nodeTypeValue.setAutoCreate(Boolean.parseBoolean(autoCreate));
      String isProtected = uiForm.getUIFormSelectBox(PROTECTED).getValue();
      nodeTypeValue.setReadOnly(Boolean.parseBoolean(isProtected));
      String parentVer = uiForm.getUIFormSelectBox(PARENTVERSION).getValue();
      nodeTypeValue.setOnVersion(Integer.parseInt(parentVer));
      String requiredType = uiForm.getUIStringInput(REQUIRED_PRIMARY_TYPE).getValue();
      if(requiredType == null || requiredType.trim().length() == 0) {
        uiForm.setTabRender(UINodeTypeForm.CHILDNODE_DEFINITION);
        message = new ApplicationMessage("UIChildNodeDefinitionForm.msg.requiredPrimaryType", null);
        uiApp.addMessage(message);
        return;
      }
      String[] types = requiredType.split(",");
      List<String> reqList = new ArrayList<String>();
      for(int i = 0; i < types.length; i++){
        reqList.add(types[i].trim());
      }
      nodeTypeValue.setRequiredNodeTypeNames(reqList);
      uiForm.addedChildDef_.add(nodeTypeValue);
      uiForm.setChildValue(uiForm.addedChildDef_);
      UIChildNodeDefinitionForm uiChildNodeForm = uiForm.getChild(UIChildNodeDefinitionForm.class);
      uiChildNodeForm.refresh();
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      UIFormInputSetWithAction nodeTypeTab = uiForm.getChildById(UINodeTypeForm.NODETYPE_DEFINITION);
      nodeTypeTab.setIsView(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static  public class AddDefaultTypeActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIFormInputSetWithAction defaultPrimaryTypeTab =
        new UINodeTypeOptionList(UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB);
      uiForm.removeChildTabs(new String[] {UINodeTypeForm.SUPER_TYPE_TAB,
                                           UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB});
      if(defaultPrimaryTypeTab.isRendered()) {
        uiForm.removeChildById(UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB);
      }
      String[] actionNames = {UINodeTypeForm.ADD_TYPE, UINodeTypeForm.ACTION_CANCEL_TAB};
      String[] fieldNames = {DEFAULT_PRIMARY_TYPE,
                                          UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB};
      defaultPrimaryTypeTab.setActions(actionNames, fieldNames);
      uiForm.addUIComponentInput(defaultPrimaryTypeTab);
      uiForm.setTabRender(UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB);
      UINodeTypeOptionList uiOptionList = uiForm.getChild(UINodeTypeOptionList.class);
      String defaultTypeValue = uiForm.getUIStringInput(DEFAULT_PRIMARY_TYPE).getValue();
      uiOptionList.update(defaultTypeValue);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static  public class AddRequiredTypeActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIFormInputSetWithAction requiredPrimaryTypeTab =
        new UINodeTypeOptionList(UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB);
      uiForm.removeChildTabs(new String[] {UINodeTypeForm.SUPER_TYPE_TAB,
                                           UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB});
      if(requiredPrimaryTypeTab.isRendered()) {
        uiForm.removeChildById(UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB);
      }
      String[] actionNames = {UINodeTypeForm.ADD_TYPE, UINodeTypeForm.ACTION_CANCEL_TAB};
      String[] fieldNames = {REQUIRED_PRIMARY_TYPE, UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB};
      requiredPrimaryTypeTab.setActions(actionNames, fieldNames);
      uiForm.addUIComponentInput(requiredPrimaryTypeTab);
      uiForm.setTabRender(UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB);
      UINodeTypeOptionList uiOptionList = uiForm.getChild(UINodeTypeOptionList.class);
      String requiredTypeValue = uiForm.getUIStringInput(REQUIRED_PRIMARY_TYPE).getValue();
      uiOptionList.update(requiredTypeValue);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class CancelChildActionListener extends EventListener<UINodeTypeForm> {
    public void execute(Event<UINodeTypeForm> event) throws Exception {
      UINodeTypeForm uiForm = event.getSource();
      UIChildNodeDefinitionForm uiChildNodeForm = uiForm.getChild(UIChildNodeDefinitionForm.class);
      uiChildNodeForm.refresh();
      UIFormInputSetWithAction childTab = uiForm.getChildById(UINodeTypeForm.CHILDNODE_DEFINITION);
      childTab.setActions(new String[] {UINodeTypeForm.ADD_CHILD}, null);
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
}
