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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : phamtuan
 * phamtuanchip@yahoo.de September 13, 2006 10:07:15 AM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIPropertyForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPropertyForm.ChangeTypeActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPropertyForm.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPropertyForm.RemoveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPropertyForm.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPropertyForm.ResetActionListener.class)
    }
)
public class UIPropertyForm extends UIForm {

  final static public String FIELD_PROPERTY = "name";
  final static public String FIELD_TYPE = "type";
  final static public String FIELD_VALUE = "value";
  final static public String FIELD_NAMESPACE = "namespace";
  final static public String FIELD_MULTIPLE = "multiple";
  final static private String FALSE = "false";
  final static private String TRUE = "true";
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIPropertyForm");
  private String repositoryName_;
  private String propertyName_;
  private boolean isAddNew_ = true;
  private boolean isMultiple_ = false;

  public UIPropertyForm() throws Exception {
    setMultiPart(true);
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_STRING,
        Integer.toString(PropertyType.STRING)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_BINARY,
        Integer.toString(PropertyType.BINARY)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_BOOLEAN,
        Integer.toString(PropertyType.BOOLEAN)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_DATE,
        Integer.toString(PropertyType.DATE)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_DOUBLE,
        Integer.toString(PropertyType.DOUBLE)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_LONG,
        Integer.toString(PropertyType.LONG)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_NAME,
        Integer.toString(PropertyType.NAME)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_PATH,
        Integer.toString(PropertyType.PATH)));
    options.add(new SelectItemOption<String>(PropertyType.TYPENAME_REFERENCE,
        Integer.toString(PropertyType.REFERENCE)));
    List<SelectItemOption<String>> nsOptions = new ArrayList<SelectItemOption<String>>();
    addUIFormInput(new UIFormSelectBox(FIELD_NAMESPACE,FIELD_NAMESPACE, nsOptions));
    addUIFormInput(new UIFormStringInput(FIELD_PROPERTY, FIELD_PROPERTY, null).addValidator(MandatoryValidator.class)
                                                                              .addValidator(ECMNameValidator.class));
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(FIELD_TYPE, FIELD_TYPE, options);
    uiSelectBox.setOnChange("ChangeType");
    addUIFormInput(uiSelectBox);
    List<SelectItemOption<String>> multipleOpt = new ArrayList<SelectItemOption<String>>();
    multipleOpt.add(new SelectItemOption<String>(TRUE, TRUE));
    multipleOpt.add(new SelectItemOption<String>(FALSE, FALSE));
    UIFormSelectBox uiMultiSelectBox = new UIFormSelectBox(FIELD_MULTIPLE,FIELD_MULTIPLE, multipleOpt);
    uiMultiSelectBox.setOnChange("ChangeType");
    addUIFormInput(uiMultiSelectBox);
    initValueField();
    setActions(new String[]{"Save", "Reset", "Cancel"});
  }

  public List<SelectItemOption<String>> getNamespaces() throws Exception {
    List<SelectItemOption<String>> namespaceOptions = new ArrayList<SelectItemOption<String>>();
    String[] namespaces = getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                          .getNamespaceRegistry()
                                                                          .getPrefixes();
    for(String namespace : namespaces){
      namespaceOptions.add(new SelectItemOption<String>(namespace, namespace));
    }
    return namespaceOptions;
  }

  public void refresh() throws Exception {
    reset();
    isAddNew_ = true;
    getUIFormSelectBox(FIELD_TYPE).setValue(Integer.toString(PropertyType.STRING));
    removeChildById(FIELD_VALUE);
    getUIStringInput(FIELD_PROPERTY).setEditable(isAddNew_);
    getUIFormSelectBox(FIELD_TYPE).setEnable(isAddNew_);
    getUIFormSelectBox(FIELD_MULTIPLE).setEnable(isAddNew_);
    getUIFormSelectBox(FIELD_NAMESPACE).setEnable(isAddNew_);
    initValueField();
  }

  public void setRepositoryName(String repositoryName) { repositoryName_ = repositoryName; }

  private void initValueField() throws Exception {
    UIFormMultiValueInputSet uiFormMValue =
      createUIComponent(UIFormMultiValueInputSet.class, null, null);
    uiFormMValue.addValidator(MandatoryValidator.class);
    uiFormMValue.setId(FIELD_VALUE);
    uiFormMValue.setName(FIELD_VALUE);
    uiFormMValue.setType(UIFormStringInput.class);
    addUIFormInput(uiFormMValue);
  }

  private Value createValue(Object value, int type, ValueFactory valueFactory) throws Exception {
    switch (type) {
    case 2:  return valueFactory.createValue((InputStream)value);
    case 3:  return valueFactory.createValue(Long.parseLong(value.toString()));
    case 4:  return valueFactory.createValue(Double.parseDouble(value.toString()));
    case 5:  return valueFactory.createValue((GregorianCalendar)value);
    case 6:  return valueFactory.createValue(Boolean.parseBoolean(value.toString()));
    default: return valueFactory.createValue(value.toString(), type);
    }
  }

  private Value[] createValues(List valueList, int type, ValueFactory valueFactory) throws Exception {
    Value[] values = new Value[valueList.size()];
    for(int i = 0; i < valueList.size(); i++) {
      values[i] = createValue(valueList.get(i), type, valueFactory);
    }
    return values;
  }

  protected void lockForm(boolean isLock) {
    if(isLock) setActions(new String[]{});
    else setActions(new String[]{"Save", "Reset", "Cancel"});
    getUIStringInput(FIELD_PROPERTY).setEditable(!isLock);
    getUIFormSelectBox(FIELD_TYPE).setEnable(!isLock);
    getUIFormSelectBox(FIELD_NAMESPACE).setEnable(!isLock);
  }

  private Node getCurrentNode() throws Exception {
    UIPropertiesManager uiManager = getParent();
    return uiManager.getCurrentNode();
  }

  @SuppressWarnings("unchecked")
  public void loadForm(String propertyName) throws Exception {
    Node currentNode = getCurrentNode();
    propertyName_ = propertyName;
    isAddNew_ = false;
    String[] propertyInfo = propertyName.split(":");
    if(propertyInfo.length == 1){
      getUIFormSelectBox(FIELD_NAMESPACE).setDisabled(true).setValue("");
      getUIStringInput(FIELD_PROPERTY).setEditable(false).setValue(propertyInfo[0]);
    } else{
      getUIFormSelectBox(FIELD_NAMESPACE).setDisabled(true).setValue(propertyInfo[0]);
      getUIStringInput(FIELD_PROPERTY).setEditable(false).setValue(propertyInfo[1]);
    }
    Property property = currentNode.getProperty(propertyName);
    isMultiple_ = property.getDefinition().isMultiple();
    if (property.getType() == 0){
      getUIFormSelectBox(FIELD_TYPE).setDisabled(true).setValue("1");
    } else {
      getUIFormSelectBox(FIELD_TYPE).setDisabled(true).setValue(Integer.toString(property.getType()));
    }
    getUIFormSelectBox(FIELD_MULTIPLE).setDisabled(true).setValue(Boolean.toString(isMultiple_));
    if(isMultiple_) {
      UIFormMultiValueInputSet multiValueInputSet = getUIInput(FIELD_VALUE);
      List listValue = new ArrayList();
      for(Value value : property.getValues()) {
        switch (property.getType()) {
          case 2:  break;
          case 3:  {
            listValue.add(Long.toString(value.getLong()));
            break;
          }
          case 4:  {
            listValue.add(Double.toString(value.getDouble()));
            break;
          }
          case 5:  {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            listValue.add(dateFormat.format(value.getDate().getTime()));
            break;
          }
          case 6: {
            listValue.add(Boolean.toString(value.getBoolean()));
            break;
          }
          default: {
            listValue.add(value.getString());
            break;
          }
        }
      }
      changeMultipleType(multiValueInputSet, property.getType());
      multiValueInputSet.setValue(listValue);
    } else {
      Value value = property.getValue();
      changeSingleType(property.getType());
      switch (property.getType()) {
        case 2:  break;
        case 3:  {
          UIFormStringInput uiForm = getUIStringInput(FIELD_VALUE);
          uiForm.setValue(Long.toString(value.getLong()));
          break;
        }
        case 4:  {
          UIFormStringInput uiForm = getUIStringInput(FIELD_VALUE);
          uiForm.setValue(Double.toString(value.getDouble()));
          break;
        }
        case 5:  {
          UIFormDateTimeInput uiFormDateTimeInput = getUIFormDateTimeInput(FIELD_VALUE);
          SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
          uiFormDateTimeInput.setValue(dateFormat.format(value.getDate().getTime()));
          break;
        }
        case 6: {
          UIFormCheckBoxInput uiFormCheckBoxInput = getUIFormCheckBoxInput(FIELD_VALUE);
          uiFormCheckBoxInput.setValue(Boolean.toString(value.getBoolean()));
          break;
        }
        default: {
          UIFormStringInput uiForm = getUIStringInput(FIELD_VALUE);
          uiForm.setValue(value.getString());
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Object processValue(int type) throws Exception {
    Object value = null;
    UIComponent uiChild = getChildById(FIELD_VALUE);
    if(type == 6) {
      UIFormCheckBoxInput checkbox = (UIFormCheckBoxInput)uiChild;
      value = checkbox.isChecked();
    } else if(type == 5) {
        UIFormDateTimeInput dateInput = (UIFormDateTimeInput)uiChild;
        value = dateInput.getCalendar();
    } else if(type == 2) {
        UIFormUploadInput binaryInput = (UIFormUploadInput)uiChild;
        if(binaryInput.getUploadDataAsStream() != null) {
          value = binaryInput.getUploadDataAsStream();
        }
    } else {
      UIFormStringInput uiStringInput = (UIFormStringInput)uiChild;
      value = uiStringInput.getValue();
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  private List processValues(int type) throws Exception {
    UIFormMultiValueInputSet multiValueInputSet = getUIInput(FIELD_VALUE);
    List valueList = new ArrayList();
    if(type == 6) {
      for(UIComponent child : multiValueInputSet.getChildren()) {
        UIFormCheckBoxInput checkbox = (UIFormCheckBoxInput)child;
        valueList.add(checkbox.isChecked());
      }
    } else if(type == 5) {
      for(UIComponent child : multiValueInputSet.getChildren()) {
        UIFormDateTimeInput dateInput = (UIFormDateTimeInput)child;
        valueList.add(dateInput.getCalendar());
      }
    } else if(type == 2) {
      for(UIComponent child : multiValueInputSet.getChildren()) {
        UIFormUploadInput binaryInput = (UIFormUploadInput)child;
        if(binaryInput.getUploadDataAsStream() != null) {
          InputStream content = binaryInput.getUploadDataAsStream();
          valueList.add(content);
        }
      }
    } else {
      valueList = multiValueInputSet.getValue();
    }
    return valueList;
  }

  private void changeMultipleType(UIFormMultiValueInputSet uiFormMultiValue, int type) {
    if(PropertyType.BINARY == type) {
      uiFormMultiValue.setType(UIFormUploadInput.class);
    } else if(PropertyType.BOOLEAN == type) {
      uiFormMultiValue.setType(UIFormCheckBoxInput.class);
    } else if(PropertyType.DATE == type) {
      uiFormMultiValue.setType(UIFormDateTimeInput.class);
    } else {
      uiFormMultiValue.setType(UIFormStringInput.class);
    }
  }

  @SuppressWarnings("unchecked")
  private void changeSingleType(int type) {
    removeChildById(FIELD_VALUE);
    if(PropertyType.BINARY == type) {
      UIFormUploadInput uiUploadInput = new UIFormUploadInput(FIELD_VALUE, FIELD_VALUE);
      uiUploadInput.setAutoUpload(true);
      addUIFormInput(uiUploadInput);
    } else if(PropertyType.BOOLEAN == type) {
      addUIFormInput(new UIFormCheckBoxInput(FIELD_VALUE, FIELD_VALUE, null));
    } else if(PropertyType.DATE == type) {
      addUIFormInput(new UIFormDateTimeInput(FIELD_VALUE, FIELD_VALUE, null));
    } else {
      addUIFormInput(new UIFormStringInput(FIELD_VALUE, FIELD_VALUE, null));
    }
  }

  static public class ChangeTypeActionListener extends EventListener {
    public void execute(Event event) throws Exception {
      UIPropertyForm uiForm = (UIPropertyForm) event.getSource();
      int type = Integer.parseInt(uiForm.getUIFormSelectBox(FIELD_TYPE).getValue());
      uiForm.removeChildById(FIELD_VALUE);
      boolean isMultiple = Boolean.parseBoolean(uiForm.getUIFormSelectBox(FIELD_MULTIPLE).getValue());
      if(isMultiple) {
        UIFormMultiValueInputSet uiFormMultiValue =
          uiForm.createUIComponent(UIFormMultiValueInputSet.class, null, null);
        uiFormMultiValue.setId(FIELD_VALUE);
        uiFormMultiValue.setName(FIELD_VALUE);
        uiForm.changeMultipleType(uiFormMultiValue, type);
        uiForm.addUIFormInput(uiFormMultiValue);
      } else {
        uiForm.changeSingleType(type);
      }
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  @SuppressWarnings("unchecked")
  static public class SaveActionListener extends EventListener<UIPropertyForm> {
    public void execute(Event<UIPropertyForm> event) throws Exception {
      UIPropertyForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      Node currentNode = uiForm.getCurrentNode();
      if(currentNode.isLocked()) {
        String lockToken = LockUtil.getLockToken(currentNode);
        if(lockToken != null) currentNode.getSession().addLockToken(lockToken);
      }
      if(!currentNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIPropertyForm.msg.node-checkedin", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      boolean isMultiple = false;
      NodeType nodeType = currentNode.getPrimaryNodeType();
      String name = "";
      int type;
      if(uiForm.isAddNew_) {
        if(!nodeType.isNodeType(Utils.NT_UNSTRUCTURED)) {
          uiApp.addMessage(new ApplicationMessage("UIPropertyForm.msg.can-not-add", null,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
          uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
          return;
        }
        String namespace = uiForm.getUIFormSelectBox(FIELD_NAMESPACE).getValue();
        name = namespace + ":" + uiForm.getUIStringInput(FIELD_PROPERTY).getValue();
        if(currentNode.hasProperty(name)) {
          Object[] args = { name };
          uiApp.addMessage(new ApplicationMessage("UIPropertyForm.msg.propertyName-exist", args,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
          uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
          return;
        }
        type = Integer.parseInt(uiForm.getUIFormSelectBox(FIELD_TYPE).getValue());
        isMultiple = Boolean.parseBoolean(uiForm.getUIFormSelectBox(FIELD_MULTIPLE).getValue());
      } else {
        name = uiForm.propertyName_;
        type = currentNode.getProperty(name).getType();
        if (type == 0) type = 1;
        isMultiple = currentNode.getProperty(name).getDefinition().isMultiple();
      }
      try {
        if(isMultiple) {
          Value[] values = {};
          List valueList = uiForm.processValues(type);
          values = uiForm.createValues(valueList, type, currentNode.getSession().getValueFactory());
          // if(currentNode.hasProperty(name)) {
          currentNode.setProperty(name, values);
          //}
        } else {
          Object objValue = uiForm.processValue(type);
          Value value = uiForm.createValue(objValue, type, currentNode.getSession().getValueFactory());
          //  if(currentNode.hasProperty(name)) {
          //setProperty already checks whether the property exists if not it will create a new one as in the description
          currentNode.setProperty(name, value);
          //  }
        }
        currentNode.save();
        currentNode.getSession().save();
      } catch(NullPointerException ne) {
        LOG.error("Unexpected error", ne);
        uiApp.addMessage(new ApplicationMessage("UIPropertyForm.msg.propertyValu-null", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UIPropertyForm.msg.number-format-exception", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      uiForm.refresh();
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiPropertiesManager.setRenderedChild(UIPropertyTab.class);
      return;
    }
  }

  static public class ResetActionListener extends EventListener<UIPropertyForm> {
    public void execute(Event<UIPropertyForm> event) throws Exception {
      UIPropertyForm uiForm = event.getSource();
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiForm.refresh();
      uiForm.isAddNew_ = true;
      uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class CancelActionListener extends EventListener<UIPropertyForm> {
    public void execute(Event<UIPropertyForm> event) throws Exception {
      UIPropertyForm uiForm = event.getSource();
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiPropertiesManager.setRenderedChild(UIPropertyTab.class);
      uiForm.refresh();
      uiForm.isAddNew_ = true;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class AddActionListener extends EventListener<UIPropertyForm> {
    public void execute(Event<UIPropertyForm> event) throws Exception {
      UIPropertyForm uiForm = event.getSource();
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class RemoveActionListener extends EventListener<UIPropertyForm> {
    public void execute(Event<UIPropertyForm> event) throws Exception {
      UIPropertyForm uiForm = event.getSource();
      UIPropertiesManager uiPropertiesManager = uiForm.getAncestorOfType(UIPropertiesManager.class);
      uiPropertiesManager.setRenderedChild(UIPropertyForm.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
}
