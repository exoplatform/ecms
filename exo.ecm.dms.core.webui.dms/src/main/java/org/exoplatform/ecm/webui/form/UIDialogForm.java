/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.form;

import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.field.UIFormActionField;
import org.exoplatform.ecm.webui.form.field.UIFormCalendarField;
import org.exoplatform.ecm.webui.form.field.UIFormCheckBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormHiddenField;
import org.exoplatform.ecm.webui.form.field.UIFormRadioBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormSelectBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormTextAreaField;
import org.exoplatform.ecm.webui.form.field.UIFormTextField;
import org.exoplatform.ecm.webui.form.field.UIFormUploadField;
import org.exoplatform.ecm.webui.form.field.UIFormWYSIWYGField;
import org.exoplatform.ecm.webui.form.field.UIMixinField;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.fckconfig.FCKConfigService;
import org.exoplatform.services.ecm.fckconfig.FCKEditorContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.exoplatform.webui.form.wysiwyg.FCKEditorConfig;
import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008  
 */
@SuppressWarnings("unused")
public class UIDialogForm extends UIForm {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("webui.form.UIDialogForm");
  
  private final String REPOSITORY = "repository";
  protected final static String CANCEL_ACTION = "Cancel".intern();
  protected final static String SAVE_ACTION = "Save".intern();
  protected static final  String[]  ACTIONS = { SAVE_ACTION, CANCEL_ACTION };  

  protected Map<String, Map<String,String>> componentSelectors = new HashMap<String, Map<String,String>>();
  protected Map<String, String> fieldNames = new HashMap<String, String>();
  protected Map<String, String> propertiesName = new HashMap<String, String>();
  protected String contentType; 
  protected boolean isAddNew = true;
  protected boolean isRemovePreference;
  protected boolean isRemoveActionField;
  protected boolean isShowingComponent;
  protected boolean isUpdateSelect;
  protected Map<String, JcrInputProperty> properties = new HashMap<String, JcrInputProperty>();
  protected String repositoryName;
  protected JCRResourceResolver resourceResolver;
  private String childPath;
  private boolean isNotEditNode;
  private boolean dataRemoved_;

  private boolean isNTFile; 
  private boolean isOnchange;
  private boolean isResetForm;
  private boolean isResetMultiField;
  protected String nodePath;
  protected String i18nNodePath = null;
  private List<String> postScriptInterceptor = new ArrayList<String>();  
  private List<String> prevScriptInterceptor = new ArrayList<String>();

  private List<String> listTaxonomy = new ArrayList<String>();
  private String storedPath;

  protected String workspaceName;
  protected boolean isReference;
  
  final static private String TAXONOMIES_ALIAS = "exoTaxonomiesPath" ;

  private String SEPARATOR_VALUE = "::";

  public UIDialogForm() { }

  public boolean isEditing() { return !isAddNew;}
  public boolean isAddNew() { return isAddNew;}
  public void addNew(boolean b) { this.isAddNew = b; }  
  
  private boolean isKeepinglock = false;
  
  public boolean isKeepinglock() {
    return isKeepinglock;
  }

  public void setIsKeepinglock(boolean isKeepinglock) {
    this.isKeepinglock = isKeepinglock;
  }

  public void releaseLock() throws Exception {
    if (isKeepinglock()) {
      Node currentNode = getNode();
      if (currentNode.isLocked()) {
        try {
          if(currentNode.holdsLock()) {
            String lockToken = LockUtil.getLockTokenOfUser(currentNode);        
            if(lockToken != null) {
              currentNode.getSession().addLockToken(LockUtil.getLockToken(currentNode));
            }
            currentNode.unlock();   
            currentNode.removeMixin(Utils.MIX_LOCKABLE);
            currentNode.save();
            //remove lock from Cache
            LockUtil.removeLock(currentNode);
          }
        } catch(LockException le) {
          LOG.error("Fails when unlock node that is editing", le);
        }
      }
    }
    setIsKeepinglock(false);
  }
  
  public List<String> getListTaxonomy() {
    return listTaxonomy;
  }
  
  public void setListTaxonomy(List<String> listTaxonomy) {
    this.listTaxonomy = listTaxonomy;
  }


  public void setStoredLocation(String repository, String workspace, String storedPath) {
    this.repositoryName = repository;
    setWorkspace(workspace);
    setStoredPath(storedPath);
  }

  protected String getCategoryLabel(String resource) {
    String[] taxonomyPathSplit = resource.split("/");
    StringBuilder buildlabel;
    StringBuilder buildPathlabel = new StringBuilder();
    for (int i = 0; i < taxonomyPathSplit.length; i++) {
      buildlabel = new StringBuilder("eXoTaxonomies");
      for (int j = 0; j <= i; j++) {
        buildlabel.append(".").append(taxonomyPathSplit[j]);
      }
      try {
        buildPathlabel.append(Utils.getResourceBundle(buildlabel.append(".label").toString())).append("/");
      } catch (MissingResourceException me) {
        buildPathlabel.append(taxonomyPathSplit[i]).append("/");
      }
    }
    return buildPathlabel.substring(0, buildPathlabel.length() - 1);
  }
  
  public void seti18nNodePath(String nodePath) { i18nNodePath = nodePath; }

  public String geti18nNodePath() { return i18nNodePath; }
  
  public void addActionField(String name,String label,String[] arguments) throws Exception {
    UIFormActionField formActionField = new UIFormActionField(name,label,arguments);    
    if(formActionField.useSelector()) {
      componentSelectors.put(name, formActionField.getSelectorInfo()); 
    }
    String jcrPath = formActionField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);    
    Node node = getNode();
    if(formActionField.isReference()) isReference = true; 
    else isReference = false;  
    UIComponent uiInput;
    if(formActionField.isMultiValues()) {      
      uiInput = findComponentById(name);
      if (uiInput == null) uiInput = addMultiValuesInput(UIFormStringInput.class,name,label);
      ((UIFormMultiValueInputSet)uiInput).setEditable(formActionField.isEditable());
      if (node == null) {
        String defaultValue = formActionField.getDefaultValue();
        if (defaultValue != null) {
          if (UIFormMultiValueInputSet.class.isInstance(uiInput)) {
            String[] arrDefaultValues = defaultValue.split(",");
            List<String> lstValues = new ArrayList<String>();
            for (String itemDefaultValues : arrDefaultValues) {
              if (!lstValues.contains(itemDefaultValues.trim())) lstValues.add(itemDefaultValues.trim());
            }
            ((UIFormMultiValueInputSet) uiInput).setValue(lstValues);
          }
        }
        renderField(name);
        return;
      }
    } else {
      uiInput = findComponentById(name);
      if(uiInput == null) {
        uiInput = formActionField.createUIFormInput();            
        addUIFormInput((UIFormInput)uiInput);
      }    
      ((UIFormStringInput)uiInput).setEditable(formActionField.isEditable());
    }
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    if(node != null && !isShowingComponent && !isRemovePreference && !isRemoveActionField) {
      if(jcrPath.equals("/node") && (!formActionField.isEditable() || formActionField.isEditableIfNull())) {
        ((UIFormStringInput)uiInput).setValue(node.getName());
        ((UIFormStringInput)uiInput).setEditable(false);
      } else if(node.hasProperty(propertyName) && !isUpdateSelect) {
        String relPath = "";
        String itemRelPath = "";
        if (node.getProperty(propertyName).getDefinition().isMultiple()) {
          StringBuffer buffer = new StringBuffer();
          Value[] values = node.getProperty(propertyName).getValues();
          if (UIFormStringInput.class.isInstance(uiInput)) {
            for (Value value : values) {
              buffer.append(value).append(",");
            }
            if (buffer.toString().endsWith(","))
              buffer.deleteCharAt(buffer.length() - 1);
            ((UIFormStringInput) uiInput).setValue(buffer.toString());
          }
          if (UIFormMultiValueInputSet.class.isInstance(uiInput)) {
            List<String> lstValues = new ArrayList<String>();
            for (Value value : values) {
              lstValues.add(value.getString());
            }
            ((UIFormMultiValueInputSet) uiInput).setValue(lstValues);
          }
        } else {
          String value = node.getProperty(propertyName).getValue().getString();
          if (node.getProperty(propertyName).getDefinition().getRequiredType() == PropertyType.REFERENCE)
            value = getNodePathByUUID(value);
          ((UIFormStringInput) uiInput).setValue(value);
        }
      }
    }
    Node childNode = getChildNode();
    if(isNotEditNode && !isShowingComponent && !isRemovePreference && !isRemoveActionField) {
      if(childNode != null) {
        ((UIFormInput)uiInput).setValue(propertyName);
      } else if(childNode == null && jcrPath.equals("/node") && node != null) {
        ((UIFormInput)uiInput).setValue(node.getName());
      } else {
        ((UIFormInput)uiInput).setValue(null);
      }
    }
    renderField(name);
  }
  public void addActionField(String name, String[] arguments) throws Exception { 
    addActionField(name,null,arguments);
  }

  public void addCalendarField(String name, String label, String[] arguments) throws Exception {
    UIFormCalendarField calendarField = new UIFormCalendarField(name,label,arguments);    
    String jcrPath = calendarField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    if(calendarField.isMultiValues()) {
      renderMultiValuesInput(UIFormDateTimeInput.class,name,label);      
      return;
    }
    UIFormDateTimeInput uiDateTime = findComponentById(name);
    if (uiDateTime == null) uiDateTime = calendarField.createUIFormInput();
    uiDateTime.setDisplayTime(calendarField.isDisplayTime());
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node  node = getNode();
    uiDateTime.setCalendar(uiDateTime.getCalendar());
    if(node != null && node.hasProperty(propertyName) && !isShowingComponent && !isRemovePreference) {
      if (findComponentById(name) == null)
        uiDateTime.setCalendar(node.getProperty(propertyName).getDate());
    } 
    Node childNode = getChildNode();
    if(isNotEditNode && !isShowingComponent && !isRemovePreference) {
      if(childNode != null) {        
        if(childNode.hasProperty(propertyName)) {
          if(childNode.getProperty(propertyName).getDefinition().isMultiple()) {
            Value[] values = childNode.getProperty(propertyName).getValues();
            for(Value value : values) {
              uiDateTime.setCalendar(value.getDate());
            }
          } else {
            uiDateTime.setCalendar(childNode.getProperty(propertyName).getValue().getDate());
          }
        }
      } else if(childNode == null && jcrPath.equals("/node") && node != null) {
        uiDateTime.setCalendar(node.getProperty(propertyName).getDate());
      } else {
        uiDateTime.setCalendar(new GregorianCalendar());
      }
    }
    if (findComponentById(name) == null) addUIFormInput(uiDateTime);
    if(calendarField.isVisible()) renderField(name);
  }
  
  public void addCalendarField(String name, String[] arguments) throws Exception {
    addCalendarField(name,null,arguments);
  }

  public void addHiddenField(String name, String[] arguments) throws Exception {
    UIFormHiddenField formHiddenField = new UIFormHiddenField(name,null,arguments);
    String jcrPath = formHiddenField.getJcrPath();
    JcrInputProperty inputProperty = formHiddenField.createJcrInputProperty();
    setInputProperty(name, inputProperty);
  }

  public void addInterceptor(String scriptPath, String type) {
    if(scriptPath.length() > 0 && type.length() > 0){
      if(type.equals("prev")){
        prevScriptInterceptor.add(scriptPath);
      } else if(type.equals("post")){
        postScriptInterceptor.add(scriptPath);
      }
    } 
  }

  public void addMixinField(String name,String label,String[] arguments) throws Exception {
    UIMixinField mixinField = new UIMixinField(name,label,arguments);
    String jcrPath = mixinField.getJcrPath();
    String nodetype = mixinField.getNodeType();
    String mixintype = mixinField.getMixinTypes();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    if (nodetype != null || mixintype != null) {
      inputProperty.setType(JcrInputProperty.NODE);
      if(nodetype != null) inputProperty.setNodetype(nodetype);
      if(mixintype != null) inputProperty.setMixintype(mixintype);
    }
    setInputProperty(name, inputProperty);
    Node node = getNode();
    if(node != null && mixinField.isVisibleIfNotNull()) {
      UIFormStringInput uiMixin = findComponentById(name);
      if(uiMixin == null) {
        uiMixin = mixinField.createUIFormInput();        
        addUIFormInput(uiMixin);
      }
      uiMixin.setValue(node.getName());
      uiMixin.setEditable(false);
      renderField(name); 
    }
  }    

  public void addMixinField(String name, String[] arguments) throws Exception {
    addMixinField(name,null,arguments);
  }  

  @SuppressWarnings("unchecked")
  public void addCheckBoxField(String name, String lable, String[] arguments) throws Exception{
    UIFormCheckBoxField formCheckBoxField =  new UIFormCheckBoxField(name, lable, arguments);
    String jcrPath = formCheckBoxField.getJcrPath();
    String defaultValue = formCheckBoxField.getDefaultValue();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    UIFormCheckBoxInput uiCheckBoxInput = findComponentById(name);
    
    if (formCheckBoxField.validateType != null) {
      String validateType = formCheckBoxField.validateType;
      String[] validatorList = null;
      if (validateType.indexOf(',') > -1) validatorList = validateType.split(",");
      else validatorList = new String[] {validateType};
      for (String validator : validatorList) {
        uiCheckBoxInput.addValidator(DialogFormUtil.getValidator(validator.trim()));
      }            
    }
    
    if(uiCheckBoxInput == null){
      uiCheckBoxInput = new UIFormCheckBoxInput(name, name, null);
      Node  node = getNode();
      if(node != null && node.hasProperty(propertyName)) {
        uiCheckBoxInput.setChecked(node.getProperty(propertyName).getBoolean());
        uiCheckBoxInput.setValue(uiCheckBoxInput.isChecked());
      } else {
        if(defaultValue != null) {
          uiCheckBoxInput.setChecked(Boolean.valueOf(defaultValue));
          uiCheckBoxInput.setValue(defaultValue);
        }
      }
    }
    if(formCheckBoxField.isOnchange()){
      uiCheckBoxInput.setOnChange("Onchange");
      uiCheckBoxInput.setValue(uiCheckBoxInput.getValue());
    }
    addUIFormInput(uiCheckBoxInput);
    renderField(name);
  }
  
  public void addCheckBoxField(String name, String[] arguments) throws Exception{
    addCheckBoxField(name, null, arguments);
  }
  
  public void addRadioBoxField(String name, String label, String[] arguments) throws Exception{
    UIFormRadioBoxField formRadioBoxField = new UIFormRadioBoxField(name, label, arguments);
    String jcrPath = formRadioBoxField.getJcrPath();
    String options = formRadioBoxField.getOptions();
    String defaultValue = formRadioBoxField.getDefaultValue();
    List<SelectItemOption<String>> optionsList = new ArrayList<SelectItemOption<String>>();
    UIFormRadioBoxInput uiRadioBox = findComponentById(name);
    if(uiRadioBox == null){
      String value = defaultValue.trim().substring(1, defaultValue.length()-1).split(",")[1];
      uiRadioBox = new UIFormRadioBoxInput(name, value, null);
      if(options != null && options.length() > 0){
        String[] array = options.split(";");
        for(int i = 0; i < array.length; i++) {
          String[] arrayChild = array[i].trim().substring(1, array[i].length()-1).split(",");
          List<String> listValue = new ArrayList<String>();
          for(int j=0; j<arrayChild.length; j++) {
            listValue.add(arrayChild[j].trim());
          }
          optionsList.add(new SelectItemOption<String>(listValue.get(0), listValue.get(1)));
        }
        uiRadioBox.setOptions(optionsList);        
      } else {
        uiRadioBox.setOptions(optionsList);
      }
      if(defaultValue != null) uiRadioBox.setDefaultValue(defaultValue);
    }
    uiRadioBox.setValue(uiRadioBox.getValue());
    addUIComponentInput(uiRadioBox);
    renderField(name);
  }
  
  public void addRadioBoxField(String name, String[] arguments) throws Exception{
    addRadioBoxField(name, null, arguments);
  }
  
  public void addSelectBoxField(String name, String label, String[] arguments) throws Exception {
    UIFormSelectBoxField formSelectBoxField = new UIFormSelectBoxField(name,label,arguments);
    String jcrPath = formSelectBoxField.getJcrPath();
    String editable = formSelectBoxField.getEditable();
    String onchange = formSelectBoxField.getOnchange();
    String defaultValue = formSelectBoxField.getDefaultValue();
    String options = formSelectBoxField.getOptions();
    String script = formSelectBoxField.getGroovyScript();
    if (editable == null) formSelectBoxField.setEditable("true");
    List<SelectItemOption<String>> optionsList = new ArrayList<SelectItemOption<String>>();
    UIFormSelectBox uiSelectBox = findComponentById(name);
    if(uiSelectBox == null || isResetForm) {
      uiSelectBox = new UIFormSelectBox(name, name, null);
      if (script != null) {
        try {
          String[] scriptParams = formSelectBoxField.getScriptParams();
          if(scriptParams != null && scriptParams.length > 0 && "repository".equals(scriptParams[0])) scriptParams[0] = repositoryName;
          executeScript(script, uiSelectBox, scriptParams, true);
        } catch (Exception e) {
          LOG.error("An unexpected error occurs", e);
          uiSelectBox.setOptions(optionsList);
        }      
      } else if (options != null && options.length() >0) {
        String[] array = options.split(",");        
        RequestContext context = RequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        String optionLabel;
        for(int i = 0; i < array.length; i++) {
          List<String> listValue = new ArrayList<String>();
          String[] arrayChild = array[i].trim().split(SEPARATOR_VALUE);
          for (int j = 0; j < arrayChild.length; j++) {
            if (!arrayChild[j].trim().equals("")) {
              listValue.add(arrayChild[j].trim());
            }
          }
          try {
            String tagName = listValue.get(0).replaceAll(" ", "-");
            optionLabel = res.getString(tagName);
          } catch (MissingResourceException e) {
            optionLabel = listValue.get(0);
          }          
          if (listValue.size() > 1) {
            optionsList.add(new SelectItemOption<String>(optionLabel, listValue.get(1))); 
          } else { 
            optionsList.add(new SelectItemOption<String>(optionLabel, listValue.get(0))); 
          }
        }
        uiSelectBox.setOptions(optionsList);
      } else {
        uiSelectBox.setOptions(optionsList);
      }      
      if(defaultValue != null) uiSelectBox.setValue(defaultValue);
    }
    propertiesName.put(name, getPropertyName(jcrPath));
    fieldNames.put(getPropertyName(jcrPath), name);
    if (formSelectBoxField.validateType != null) {
      String validateType = formSelectBoxField.validateType;
      String[] validatorList = null;
      if (validateType.indexOf(',') > -1) validatorList = validateType.split(",");
      else validatorList = new String[] {validateType};
      for (String validator : validatorList) {
        uiSelectBox.addValidator(DialogFormUtil.getValidator(validator.trim()));
      }              
    }
    String[] arrNodes = jcrPath.split("/");
    Node childNode = null;
    Node node = getNode();
    String propertyName = getPropertyName(jcrPath);
    if(node != null && arrNodes.length == 4) childNode = node.getNode(arrNodes[2]);    
    if(formSelectBoxField.isMultiValues()) {
      if (formSelectBoxField.getSize() != null && StringUtils.isAlphanumeric(formSelectBoxField.getSize())) {
        uiSelectBox.setSize(Integer.parseInt(formSelectBoxField.getSize()));
      }
      uiSelectBox.setMultiple(true);      
      StringBuffer buffer = new StringBuffer();
      if(childNode != null) {      
        List<String> valueList = new ArrayList<String>();      
        Value[] values = childNode.getProperty(propertyName).getValues();
        for(Value value : values) {
          buffer.append(value.getString()).append(",");
        }
        uiSelectBox.setSelectedValues(StringUtils.split(buffer.toString(), ","));
      } else {
        if(node != null && node.hasProperty(propertyName)) {
          List<String> valueList = new ArrayList<String>();
          if(node.getProperty(propertyName).getDefinition().isMultiple() && (!onchange.equals("true") || !isOnchange)) {
            Value[] values = node.getProperty(propertyName).getValues();
            for(Value value : values) {
              buffer.append(value.getString()).append(",");
            }          
          } else if(onchange.equals("true") && isOnchange) {
            if (uiSelectBox.isMultiple()) {
              String[] values = uiSelectBox.getSelectedValues();
              for (String value : values) {
                buffer.append(value).append(",");
              }
            } else {
              String values = uiSelectBox.getValue();
              buffer.append(values).append(",");
            }
          } else {
            Value[] values = node.getProperty(propertyName).getValues();          
            for(Value value : values) {
              buffer.append(value.getString()).append(",");
            }                
          }        
          uiSelectBox.setSelectedValues(StringUtils.split(buffer.toString(), ","));
        }
      }      
    } else {
      if(childNode != null) {
        uiSelectBox.setValue(childNode.getProperty(propertyName).getValue().getString());
      } else {
        if(node != null && node.hasProperty(propertyName)) {
          if(node.getProperty(propertyName).getDefinition().isMultiple()) {
            if (findComponentById(name) == null)
              uiSelectBox.setValue(node.getProperty(propertyName).getValues().toString());
          } else if(formSelectBoxField.isOnchange() && isOnchange) {
            uiSelectBox.setValue(uiSelectBox.getValue());
          } else {
              if (findComponentById(name) == null)
                 uiSelectBox.setValue(node.getProperty(propertyName).getValue().getString());      
          }
        }
      }  
    }    
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    uiSelectBox.setEditable(formSelectBoxField.isEditable());
    addUIFormInput(uiSelectBox);
    if(isNotEditNode) {      
      Node child = getChildNode();
      if(child != null && child.hasProperty(propertyName)) {
        if(child.getProperty(propertyName).getDefinition().isMultiple()) {
          Value[] values = child.getProperty(propertyName).getValues();
          List<String> listValues = new ArrayList<String>();
          for(Value value : values) {
            listValues.add(value.getString());
          }
          uiSelectBox.setSelectedValues(listValues.toArray(new String[listValues.size()]));
        } else {
          uiSelectBox.setValue(DialogFormUtil.getPropertyValueAsString(child,propertyName));
        }
      }
    }
    if(formSelectBoxField.isOnchange()) uiSelectBox.setOnChange("Onchange");
    if (findComponentById(name) == null) addUIFormInput(uiSelectBox);
    renderField(name);   
  }    

  public void addSelectBoxField(String name, String[] arguments) throws Exception {
    addSelectBoxField(name,null,arguments);
  }

  public void addTextAreaField(String name, String label, String[] arguments) throws Exception {
    UIFormTextAreaField formTextAreaField = new UIFormTextAreaField(name,label,arguments);            
    if(formTextAreaField.useSelector()) {
      componentSelectors.put(name, formTextAreaField.getSelectorInfo());
    }    
    String jcrPath = formTextAreaField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    if(formTextAreaField.isMultiValues()) {
      renderMultiValuesInput(UIFormDateTimeInput.class,name,label);      
      return;
    }
    UIFormTextAreaInput uiTextArea = findComponentById(name);    
    if(uiTextArea == null) {
      uiTextArea = formTextAreaField.createUIFormInput();  
      if(formTextAreaField.getRowSize() != null){
        uiTextArea.setRows(Integer.parseInt(formTextAreaField.getRowSize()));
      } else {
        uiTextArea.setRows(UIFormTextAreaField.DEFAULT_ROW);
      }
      if(formTextAreaField.getColSize() != null){
        uiTextArea.setColumns(Integer.parseInt(formTextAreaField.getColSize()));
      } else {
        uiTextArea.setColumns(UIFormTextAreaField.DEFAULT_COL);
      }
      addUIFormInput(uiTextArea);
    }    
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();
    if(node != null && !isShowingComponent && !isRemovePreference) {
      String value = null;
      if(node.hasProperty(propertyName)) {
        value = node.getProperty(propertyName).getValue().getString();
      } else if(node.isNodeType("nt:file")) {
        Node jcrContentNode = node.getNode("jcr:content");
        if(jcrContentNode.hasProperty(propertyName)) {
          if(jcrContentNode.getProperty(propertyName).getDefinition().isMultiple()) {
            Value[] values = jcrContentNode.getProperty(propertyName).getValues();
            for(Value v : values) {
              value = value + v.getString();
            }
          } else {
            value = jcrContentNode.getProperty(propertyName).getValue().getString();
          }
        }
      }
      uiTextArea.setValue(value);
    } 
    if(isNotEditNode && !isShowingComponent && !isRemovePreference) {
      Node childNode = getChildNode();
      if(node != null && node.hasNode("jcr:content") && childNode != null) {
        Node jcrContentNode = node.getNode("jcr:content");
        uiTextArea.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
      } else {
        if(childNode != null) {
          uiTextArea.setValue(propertyName);
        } else if(childNode == null && jcrPath.equals("/node") && node != null) {
          uiTextArea.setValue(node.getName());
        } else {
          uiTextArea.setValue(null);
        }
      }
    }    
    //set default value for textarea if no value was set by above code
    if(uiTextArea.getValue() == null) {
      if(formTextAreaField.getDefaultValue() != null)
        uiTextArea.setValue(formTextAreaField.getDefaultValue());
      else
        uiTextArea.setValue("");
    }            
    uiTextArea.setEditable(formTextAreaField.isEditable());
    renderField(name);
  }
  public void addTextAreaField(String name, String[] arguments) throws Exception {
    addTextAreaField(name,null,arguments);
  }
  
  public String getPathTaxonomy() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig(repositoryName).getSystemWorkspace();
    String userName = Util.getPortalRequestContext().getRemoteUser();
    Session session;
    if (userName != null)
      session = SessionProviderFactory.createSessionProvider().getSession(workspace, getRepository());
    else
      session = SessionProviderFactory.createAnonimProvider().getSession(workspace, getRepository());
    return ((Node)session.getItem(nodeHierarchyCreator.getJcrPath(TAXONOMIES_ALIAS))).getPath();
  }

  public void addTextField(String name, String label, String[] arguments) throws Exception {
    UIFormTextField formTextField = new UIFormTextField(name,label,arguments);
    String jcrPath = formTextField.getJcrPath();
    String mixintype = formTextField.getMixinTypes();
    String nodetype = formTextField.getNodeType();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    String propertyName = getPropertyName(jcrPath);
    if(mixintype != null) inputProperty.setMixintype(mixintype);
    if(jcrPath.equals("/node") && nodetype != null ) inputProperty.setNodetype(nodetype);
    properties.put(name, inputProperty);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();
    Node childNode = getChildNode();
    if(!isReference) {
      if(formTextField.isReference()) isReference = true; 
      else isReference = false;
    } 
    if(formTextField.isMultiValues()) {
      UIFormMultiValueInputSet uiMulti;
      if(node == null &&childNode == null) {
        uiMulti = findComponentById(name);
        if(uiMulti == null) {
          uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
          uiMulti.setId(name);
          uiMulti.setName(name);
          uiMulti.setType(UIFormStringInput.class);
          uiMulti.setEditable(formTextField.isEditable());
          if (formTextField.validateType != null) {
            String validateType = formTextField.validateType;
            String[] validatorList = null;
            if (validateType.indexOf(',') > -1) validatorList = validateType.split(",");
            else validatorList = new String[] {validateType};
            for (String validator : validatorList) {
              uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()));
            }              
          }
          List<String> valueList = new ArrayList<String>();
          List<UIComponent> listChildren = uiMulti.getChildren();
          if (listChildren.size() == 0) {
            valueList.add(formTextField.getDefaultValue());
          } else {
            for (UIComponent component : listChildren) {
              UIFormStringInput uiStringInput = (UIFormStringInput)component;
              if(uiStringInput.getValue() != null) {
                valueList.add(uiStringInput.getValue().trim());            
              } else{
                valueList.add(formTextField.getDefaultValue());
              }
            }
          }
          uiMulti.setValue(valueList);
          addUIFormInput(uiMulti);
        } 
      } else {
        uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
        uiMulti.setId(name);
        uiMulti.setName(name);
        uiMulti.setType(UIFormStringInput.class);
        uiMulti.setEditable(formTextField.isEditable());
        if (formTextField.validateType != null) {
          String validateType = formTextField.validateType;
          String[] validatorList = null;
          if (validateType.indexOf(',') > -1) validatorList = validateType.split(",");
          else validatorList = new String[] {validateType};
          for (String validator : validatorList) {
            uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()));
          }              
        }                
        addUIFormInput(uiMulti);
      }
      List<String> valueList = new ArrayList<String>();
      if(childNode != null) {
        if(childNode.hasProperty(propertyName)) {
          Value[] values = childNode.getProperty(propertyName).getValues();
          for(Value value : values) {
            valueList.add(value.getString());
          }
          uiMulti.setEditable(formTextField.isEditable());
          uiMulti.setValue(valueList);
        }
      }
      if(node != null && !isShowingComponent && !isRemovePreference) {
        String propertyPath = jcrPath.substring("/node/".length());
        if(node.hasProperty(propertyPath)) {
          Value[] values = node.getProperty(propertyPath).getValues();
          for(Value vl : values) {
            if (vl != null) {
              valueList.add(vl.getString());
            }
          }
        }
        uiMulti.setValue(valueList);        
      }
      if(isResetMultiField) {
        uiMulti.setValue(new ArrayList<Value>());
      }
      uiMulti.setEditable(formTextField.isEditable());
      renderField(name);
      return;
    } 
    UIFormStringInput uiInput = findComponentById(name);
    if(uiInput == null) {
      uiInput = formTextField.createUIFormInput();
      addUIFormInput(uiInput);      
    }
    uiInput.setEditable(formTextField.isEditable());
    if(uiInput.getValue() == null) uiInput.setValue(formTextField.getDefaultValue());       
    else uiInput.setEditable(true);
    if(node != null && !isShowingComponent && !isRemovePreference) {
      if(jcrPath.equals("/node") && (!formTextField.isEditable() || formTextField.isEditableIfNull())) {
        if(i18nNodePath != null) {
          uiInput.setValue(i18nNodePath.substring(i18nNodePath.lastIndexOf("/") + 1));
        } else {
          String nameValue =  node.getPath().substring(node.getPath().lastIndexOf("/") + 1);
          uiInput.setValue(Text.unescapeIllegalJcrChars(nameValue));
        }
        uiInput.setEditable(false);
      } else if(node.hasProperty(propertyName)) {
        uiInput.setValue(node.getProperty(propertyName).getValue().getString());
      } 
    }
    if(isNotEditNode && !isShowingComponent && !isRemovePreference) {
      if(childNode != null && childNode.hasProperty(propertyName)) {
        if(childNode.hasProperty(propertyName)) {
          uiInput.setValue(childNode.getProperty(propertyName).getValue().getString());
        } 
      } else if(childNode == null && jcrPath.equals("/node") && node != null) {
        uiInput.setValue(node.getName());
      } else if(i18nNodePath != null && jcrPath.equals("/node")) {
        uiInput.setValue(i18nNodePath.substring(i18nNodePath.lastIndexOf("/") + 1));
      } else {
        uiInput.setValue(formTextField.getDefaultValue());
      }
    }
    renderField(name);
  }
  public void addTextField(String name, String[] arguments) throws Exception {
    addTextField(name,null,arguments);
  }

  public void addUploadField(String name,String label,String[] arguments) throws Exception {
    UIFormUploadField formUploadField = new UIFormUploadField(name,label,arguments);
    String jcrPath = formUploadField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    setInputProperty(name, inputProperty);
    setMultiPart(true);
    if(formUploadField.isMultiValues()) {
      renderMultiValuesInput(UIFormUploadInput.class,name,label);      
      return;
    }    
    UIFormUploadInput uiInputUpload = findComponentById(name);
    if(uiInputUpload == null) {
      uiInputUpload = formUploadField.createUIFormInput();      
      addUIFormInput(uiInputUpload);
    }
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    renderField(name);
  }

  public void addUploadField(String name, String[] arguments) throws Exception {
    addUploadField(name,null,arguments);
  }

  public void addWYSIWYGField(String name, String label, String[] arguments) throws Exception {
    UIFormWYSIWYGField formWYSIWYGField = new UIFormWYSIWYGField(name,label,arguments);
    String jcrPath = formWYSIWYGField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);       
    setInputProperty(name, inputProperty);
    if(formWYSIWYGField.isMultiValues()) {
      //TODO need add FCKEditorConfig for the service
      renderMultiValuesInput(UIFormWYSIWYGInput.class,name,label);      
      return;
    }            
    UIFormWYSIWYGInput wysiwyg = findComponentById(name);
    if(wysiwyg == null) {
      wysiwyg = formWYSIWYGField.createUIFormInput();      
    }                 
    /**
     * Broadcast some info about current node by FCKEditorConfig Object
     * FCKConfigService used to allow add custom config for fckeditor from service
     * */
    FCKEditorConfig config = new FCKEditorConfig();
    FCKEditorContext editorContext = new FCKEditorContext();
    if(repositoryName != null) {        
      config.put("repositoryName",repositoryName);
      editorContext.setRepository(repositoryName);
    }
    if(workspaceName != null) {
      config.put("workspaceName",workspaceName);
      editorContext.setWorkspace(workspaceName);
    }
    if(nodePath != null) {
      config.put("jcrPath",nodePath);
      editorContext.setCurrentNodePath(nodePath);
    }else {
      config.put("jcrPath",storedPath);
      editorContext.setCurrentNodePath(storedPath);                                  
    }
    FCKConfigService fckConfigService = getApplicationComponent(FCKConfigService.class);
    editorContext.setPortalName(Util.getUIPortal().getName());
    editorContext.setSkinName(Util.getUIPortalApplication().getSkin());
    fckConfigService.processFCKEditorConfig(config,editorContext);      
    wysiwyg.setFCKConfig(config);    
    addUIFormInput(wysiwyg);
    if(wysiwyg.getValue() == null) wysiwyg.setValue(formWYSIWYGField.getDefaultValue());
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();

    if(!isShowingComponent && !isRemovePreference) {
      if(node != null && (node.isNodeType("nt:file") || isNTFile)) {
        Node jcrContentNode = node.getNode("jcr:content");
        wysiwyg.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
      } else {
        if(node != null && node.hasProperty(propertyName)) {
          wysiwyg.setValue(node.getProperty(propertyName).getValue().getString());
        }
      }
    }
    if(isNotEditNode && !isShowingComponent && !isRemovePreference) {
      Node childNode = getChildNode();
      if(node != null && node.hasNode("jcr:content") && childNode != null) {
        Node jcrContentNode = node.getNode("jcr:content");
        wysiwyg.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
      } else {
        if(childNode != null) {
          wysiwyg.setValue(propertyName);
        } else if(childNode == null && jcrPath.equals("/node") && node != null) {
          wysiwyg.setValue(node.getName());
        } else {
          wysiwyg.setValue(null);
        }
      }
    }
    renderField(name);
  }

  public void addWYSIWYGField(String name, String[] arguments) throws Exception {
    addWYSIWYGField(name,null,arguments);
  }
  public Node getChildNode() throws Exception { 
    if(childPath == null) return null;
    return (Node) getSession().getItem(childPath); 
  }

  public String getContentType() { return contentType; };

  public Map<String, JcrInputProperty> getInputProperties() { return properties; }

  public JcrInputProperty getInputProperty(String name) { return properties.get(name); }
  public JCRResourceResolver getJCRResourceResolver() { return resourceResolver; }

  public Node getNode() throws Exception { 
    if(nodePath == null) return null;
    return (Node) getSession().getItem(nodePath); 
  }

  public String getPropertyName(String jcrPath) { 
    return jcrPath.substring(jcrPath.lastIndexOf("/") + 1); 
  } 

  public String getSelectBoxFieldValue(String name) {
    UIFormSelectBox uiSelectBox = findComponentById(name);
    if (uiSelectBox != null) return uiSelectBox.getValue();
    return null;
  }

  public List<String> getSelectedBoxFieldValue(String name) {
    UIFormSelectBox uiSelectBox = findComponentById(name);
    if (uiSelectBox != null) return Arrays.asList(uiSelectBox.getSelectedValues());
    return null;
  }

  @Deprecated
  public Session getSesssion() throws Exception {
    return getSession();
  }
  
  public Session getSession() throws Exception {
    return SessionProviderFactory.createSessionProvider().getSession(workspaceName, getRepository());
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try {      
      return templateService.getTemplatePathByUser(true, contentType, userName, repositoryName);
    } catch (Exception e) {
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { contentType };
      uiApp.addMessage(new ApplicationMessage("UIDialogForm.msg.not-support-contenttype", arg, ApplicationMessage.ERROR));
      return null;
    } 
  }  

  public boolean isResetForm() { return isResetForm; }

  public void onchange(Event<?> event) throws Exception {
  }

  @Override
  public void processAction(WebuiRequestContext context) throws Exception {
    String action = context.getRequestParameter(UIForm.ACTION);
    if (SAVE_ACTION.equalsIgnoreCase(action)) {
      try {
        if (executePreSaveEventInterceptor()) {
          super.processAction(context);
          String nodePath_ = (String) context.getAttribute("nodePath");
          if (nodePath_ != null) {
            executePostSaveEventInterceptor(nodePath_);
          }
        }
      } finally {
        prevScriptInterceptor.clear();
        postScriptInterceptor.clear();
      }
    } else {
      super.processAction(context);
    }
  }

  //update by quangld
  public void removeComponent(String name) {
    if (!properties.isEmpty() && properties.containsKey(name)) {
      properties.remove(name);
      String jcrPath = propertiesName.get(name);
      propertiesName.remove(name);
      fieldNames.remove(jcrPath);
      removeChildById(name); 
    }
  }

  private String getResourceBundle(WebuiRequestContext context, String key) {
    try {
      ResourceBundle rs = context.getApplicationResourceBundle();
      return rs.getString(key);
    } catch(MissingResourceException e) {
      if (LOG.isDebugEnabled()) {
        LOG.warn("Missing resource " + key);
        
      }
      key = key.contains(".") ? key.substring(key.lastIndexOf(".") + 1) : key;
      return key;
    }
  }
  public void renderField(String name) throws Exception {
    UIComponent uiInput = findComponentById(name);
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    Writer w = context.getWriter();
    if(componentSelectors.get(name) != null && name.equals(componentSelectors.get(name).get("returnField"))) {
      w.write("<table style=\"width: auto;\">");
      w.write("<tr>");
      w.write("<td>");
      uiInput.processRender(context);
      w.write("</td>");
    } else {
      uiInput.processRender(context);
    }
    if(componentSelectors.get(name) != null) {
      Map<String,String> fieldPropertiesMap = componentSelectors.get(name);
      String fieldName = fieldPropertiesMap.get("returnField");
      String iconClass = "Add16x16Icon";
      if(fieldPropertiesMap.get("selectorIcon") != null) {
        iconClass = fieldPropertiesMap.get("selectorIcon");
      }
      ResourceBundle rs = context.getApplicationResourceBundle();
      String showComponent = getResourceBundle(context, getId().concat(".title.ShowComponent"));
      String removeReference = getResourceBundle(context, getId().concat(".title.removeReference"));
      if(name.equals(fieldName)) {
        w.write("<td class=\"MultiValueContainerShow\">");
        w.write("<a style=\"cursor:pointer;\" title=\"" + showComponent + "\""
            + "onclick=\"javascript:eXo.webui.UIForm.submitEvent('" 
            + "" + getId() +"','ShowComponent','&objectId="+ fieldName +"' )\"><img class='ActionIcon "+ iconClass +"' src=\"/eXoResources/skin/DefaultSkin/background/Blank.gif\" /></a>");
        /* Not need Remove action if uiInput is UIFormMultiValueInputSet */
        if (!UIFormMultiValueInputSet.class.isInstance(uiInput))
          w.write("<a style=\"cursor:pointer;\" title=\"" + removeReference + "\""
              + "onclick=\"javascript:eXo.webui.UIForm.submitEvent('" 
              + "" + getId() +"','RemoveReference','&objectId="+ fieldName +"' )\"><img class='ActionIcon Remove16x16Icon' src=\"/eXoResources/skin/DefaultSkin/background/Blank.gif\" /></a>");
        w.write("</td>");
        w.write("</tr>");
        w.write("</table>");
      } 
    }
  }
  
  public String getImage(Node node, String nodeTypeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class);
    Node imageNode = node.getNode(nodeTypeName);    
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream();
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(node.getName());
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }
  
  public boolean dataRemoved() { return dataRemoved_; }
  
  public void setDataRemoved(boolean dataRemoved) { dataRemoved_ = dataRemoved; }

  public void resetProperties() { properties.clear(); }
  
  public void resetInterceptors(){
    this.prevScriptInterceptor.clear();
    this.postScriptInterceptor.clear();
  }

  public void setChildPath(String childPath) { this.childPath = childPath; }

  public void setContentType(String type) { this.contentType = type; }

  public void setInputProperty(String name, JcrInputProperty value) { properties.put(name, value); }

  public void setIsNotEditNode(boolean isNotEditNode) { this.isNotEditNode = isNotEditNode; }

  public void setIsNTFile(boolean isNTFile) { this.isNTFile = isNTFile; }

  public void setIsOnchange(boolean isOnchange) { this.isOnchange = isOnchange; }

  public void setIsResetForm(boolean isResetForm) { this.isResetForm = isResetForm; }    

  public void setIsResetMultiField(boolean isResetMultiField) { 
    this.isResetMultiField = isResetMultiField; 
  }

  public void setIsUpdateSelect(boolean isUpdateSelect) { this.isUpdateSelect = isUpdateSelect; }

  public void setJCRResourceResolver(JCRResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  public void setNodePath(String nodePath) { this.nodePath = nodePath; }
  
  public String getNodePath() { return nodePath; }

  public void setRepositoryName(String repositoryName){ this.repositoryName = repositoryName; }      

  public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
  
  public String getStoredPath() { return storedPath; }

  public void setWorkspace(String workspace) { this.workspaceName = workspace; }  

  private void executePostSaveEventInterceptor(String nodePath_) throws Exception {
    if (postScriptInterceptor.size() > 0) {
      String path = nodePath_ + "&workspaceName=" + this.workspaceName + "&repository="
          + this.repositoryName;
        for (String interceptor : postScriptInterceptor) {
          this.executeScript(interceptor, path, null, true);
        }
    }
  }

  private boolean executePreSaveEventInterceptor() throws Exception {
    if (!prevScriptInterceptor.isEmpty()) {
      Map<String, JcrInputProperty> maps = DialogFormUtil.prepareMap(this.getChildren(),
          getInputProperties());
      for (String interceptor : prevScriptInterceptor) {
        if(!executeScript(interceptor, maps, null, false)){
          return false;
        }
      }
    }
    return true;
  }

  private boolean executeScript(String script, Object o, String[] params, boolean printException)
      throws Exception {
    ScriptService scriptService = getApplicationComponent(ScriptService.class);
    try {
      CmsScript dialogScript = scriptService.getScript(script, repositoryName);
      if (params != null) {
        if (params.length > 0 && REPOSITORY.equals(params[0]))
          params = new String[] { repositoryName };
        dialogScript.setParams(params);
      }
      dialogScript.execute(o);
      return true;
    } catch (Exception e) {
      if(printException){
        LOG.warn("An unexpected error occurs", e);
      } else {
        UIApplication uiApp = getAncestorOfType(UIApplication.class);
        if (e instanceof DialogFormException) {
          for (ApplicationMessage message : ((DialogFormException) e).getMessages()) {
            uiApp.addMessage(message);
          }
        } else {
          JCRExceptionManager.process(uiApp, e);
        }
      }
    }
    return false;
  }
  
  private String getNodePathByUUID(String uuid) throws Exception{
    String[] workspaces = getRepository().getWorkspaceNames();
    Node node = null;
    for(String ws : workspaces) {
      try{
        node = SessionProviderFactory.createSystemProvider().getSession(ws, getRepository()).getNodeByUUID(uuid);
        return ws + ":" + node.getPath();
      } catch(ItemNotFoundException e) {
        // do nothing
      }      
    }
    LOG.error("No node with uuid ='" + uuid + "' can be found");
    return null;
  }  

  private ManageableRepository getRepository() throws Exception{         
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class);      
    return repositoryService.getRepository(repositoryName);
  } 

  
  private void renderMultiValuesInput(Class type, String name,String label) throws Exception{
    addMultiValuesInput(type, name, label);
    renderField(name);
  }

  private UIFormMultiValueInputSet addMultiValuesInput(Class type, String name,String label) throws Exception{
    UIFormMultiValueInputSet uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
    uiMulti.setId(name);
    uiMulti.setName(name);
    uiMulti.setType(type);    
    addUIFormInput(uiMulti);
    if(label != null) uiMulti.setLabel(label);
    return uiMulti;
  }

  static  public class OnchangeActionListener extends EventListener<UIDialogForm> {
    public void execute(Event<UIDialogForm> event) throws Exception {      
      event.getSource().isOnchange = true;
      event.getSource().onchange(event);
    }
  }

  public boolean isOnchange() {
    return isOnchange;
  }
  
  public void processRenderAction() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    Writer writer = context.getWriter();
    writer.append("<div class=\"UIAction\">");    
    writer.append("<table class=\"ActionContainer\">");
    writer.append("<tr>");
    writer.append("<td>");    
    String[] listAction = getActions();
    ResourceBundle res = context.getApplicationResourceBundle();
    String actionLabel;
    String link;
    for (String action : listAction) {
      try {
        actionLabel = res.getString(getName() + ".action." + action);
      } catch (MissingResourceException e) {
        actionLabel = action;  
      }
      link = event(action);
      writer.append("<div onclick=\"").append(link).append("\" class=\"ActionButton LightBlueStyle\">");
      writer.append("<div class=\"ButtonLeft\">");
      writer.append("<div class=\"ButtonRight\">");
      writer.append("<div class=\"ButtonMiddle\">");
      writer.append("<a href=\"javascript:void(0);\">").append(actionLabel).append("</a>");
      writer.append("</div>");
      writer.append("</div>");
      writer.append("</div>");
      writer.append("</div>");      
    }    
    writer.append("</td>");
    writer.append("</tr>");
    writer.append("</table>");
    writer.append("</div>");
  }
}