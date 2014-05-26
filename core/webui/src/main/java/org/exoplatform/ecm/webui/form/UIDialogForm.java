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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.core.fckconfig.FCKConfigService;
import org.exoplatform.ecm.webui.core.fckconfig.FCKEditorContext;
import org.exoplatform.ecm.webui.form.field.UIFormActionField;
import org.exoplatform.ecm.webui.form.field.UIFormCalendarField;
import org.exoplatform.ecm.webui.form.field.UIFormCheckBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormHiddenField;
import org.exoplatform.ecm.webui.form.field.UIFormRadioBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormRichtextField;
import org.exoplatform.ecm.webui.form.field.UIFormSelectBoxField;
import org.exoplatform.ecm.webui.form.field.UIFormTextAreaField;
import org.exoplatform.ecm.webui.form.field.UIFormTextField;
import org.exoplatform.ecm.webui.form.field.UIFormUploadField;
import org.exoplatform.ecm.webui.form.field.UIFormWYSIWYGField;
import org.exoplatform.ecm.webui.form.field.UIMixinField;
import org.exoplatform.ecm.webui.form.validator.UploadFileMimeTypesValidator;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.input.UIUploadInput;
import org.exoplatform.webui.form.wysiwyg.FCKEditorConfig;
import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */

@ComponentConfigs( {
  @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset",
                   events = {
    @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }) })
@SuppressWarnings("unused")
public class UIDialogForm extends UIForm {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIDialogForm.class.getName());

  private final String REPOSITORY = "repository";
  protected final static String CANCEL_ACTION = "Cancel";
  protected final static String SAVE_ACTION = "Save";
  protected static final String SAVE_AND_CLOSE = "SaveAndClose";
  protected static final  String[]  ACTIONS = { SAVE_ACTION, CANCEL_ACTION };
  private static final String WYSIWYG_MULTI_ID = "WYSIWYGRichTextMultipleInputset";

  protected Map<String, Map<String,String>> componentSelectors = new HashMap<String, Map<String,String>>();
  protected Map<String, String> fieldNames = new HashMap<String, String>();
  protected Map<String, String> propertiesName = new HashMap<String, String>();
  protected Map<String, String[]> uiMultiValueParam = new HashMap<String, String[]>();
  protected String contentType;
  protected boolean isAddNew = true;
  protected boolean isRemovePreference;
  protected boolean isRemoveActionField;
  protected boolean isShowingComponent;
  protected boolean isUpdateSelect;
  protected Map<String, JcrInputProperty> properties = new HashMap<String, JcrInputProperty>();
  protected Map<String, String> options = new HashMap<String, String>();
  protected String repositoryName;
  protected JCRResourceResolver resourceResolver;
  private String childPath;
  private boolean isNotEditNode;
  private boolean dataRemoved_ = false;;

  private boolean isNTFile;
  private boolean isOnchange;
  private boolean isResetForm;
  private boolean isResetMultiField;
  protected String nodePath;
  protected String i18nNodePath = null;
  private List<String> postScriptInterceptor = new ArrayList<String>();
  private List<String> prevScriptInterceptor = new ArrayList<String>();

  private List<String> listTaxonomy = new ArrayList<String>();
  private List<String> removedNodes = new ArrayList<String>();
  private String storedPath;

  protected String workspaceName;
  protected boolean isReference;
  protected boolean isShowActionsOnTop_ = false;
  private List<String> removedBinary ;
  /** Selected Tab id */
  private String selectedTab;

  private String SEPARATOR_VALUE = "::";

  public UIDialogForm() {
    removedBinary = new ArrayList<String>();
  }

  public boolean isEditing() { return !isAddNew;}
  public boolean isAddNew() { return isAddNew;}
  public void addNew(boolean b) { this.isAddNew = b; }

  private boolean isKeepinglock = false;

  public void setSelectedTab(String selectedTab) {
    this.selectedTab = selectedTab;
  }

  public String getSelectedTab() {
    return selectedTab;
  }
  public boolean isKeepinglock() {
    return isKeepinglock;
  }

  public void setIsKeepinglock(boolean isKeepinglock) {
    this.isKeepinglock = isKeepinglock;
  }

  public boolean isShowActionsOnTop() { return isShowActionsOnTop_; }

  public void setShowActionsOnTop(boolean isShowActionsOnTop) {
    this.isShowActionsOnTop_ = isShowActionsOnTop;
  }

  public void releaseLock() throws Exception {
    if (isKeepinglock()) {
      Node currentNode = getNode();
      if ((currentNode!=null) && currentNode.isLocked()) {
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
          if (LOG.isErrorEnabled()) {
            LOG.error("Fails when unlock node that is editing", le);
          }
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

  public void setStoredLocation(String workspace, String storedPath) {
    try {
    this.repositoryName = getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                          .getConfiguration()
                                                                          .getName();
    } catch (RepositoryException ex) {
      this.repositoryName = null;
    }
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

  @SuppressWarnings("unchecked")
  public void addActionField(String name,String label,String[] arguments) throws Exception {
    UIFormActionField formActionField = new UIFormActionField(name,label,arguments);
    if(formActionField.useSelector()) {
      componentSelectors.put(name, formActionField.getSelectorInfo());
    }
    String jcrPath = formActionField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formActionField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    Node node = getNode();
    UIComponent uiInput;
    boolean isFirstTimeRender = false;
    if(formActionField.isMultiValues()) {
      uiInput = findComponentById(name);
      if (uiInput == null) {
        isFirstTimeRender = true;
        uiInput = addMultiValuesInput(UIFormStringInput.class,name,label);
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
      }
      ((UIFormMultiValueInputSet)uiInput).setEditable(formActionField.isEditable());
      if (node == null) {
        renderField(name);
        return;
      }
    } else {
      uiInput = findComponentById(name);
      if(uiInput == null) {
        isFirstTimeRender = true;
        uiInput = formActionField.createUIFormInput();
        addUIFormInput((UIFormInput<?>)uiInput);
      }
      ((UIFormStringInput)uiInput).setReadOnly(!formActionField.isEditable());
    }
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);

    if (node != null && !isShowingComponent && !isRemovePreference && !isRemoveActionField) {
      if (jcrPath.equals("/node")
          && (!formActionField.isEditable() || formActionField.isEditableIfNull())) {
        ((UIFormStringInput) uiInput).setDisabled(true);
      }
    }

    if (node != null && !isShowingComponent && !isRemovePreference && !isRemoveActionField
        && isFirstTimeRender) {
      if(jcrPath.equals("/node") && (!formActionField.isEditable() || formActionField.isEditableIfNull())) {
        ((UIFormStringInput)uiInput).setValue(node.getName());
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
        ((UIFormInput<String>)uiInput).setValue(propertyName);
      } else if(childNode == null && jcrPath.equals("/node") && node != null) {
        ((UIFormInput<String>)uiInput).setValue(node.getName());
      } else {
        ((UIFormInput<?>)uiInput).setValue(null);
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
    inputProperty.setChangeInJcrPathParam(calendarField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    if(calendarField.isMultiValues()) {
      renderMultiValuesInput(UIFormDateTimeInput.class,name,label);
      return;
    }
    boolean isFirstTimeRender = false;
    UIFormDateTimeInput uiDateTime = findComponentById(name);
    if (uiDateTime == null) {
      isFirstTimeRender = true;
      uiDateTime = calendarField.createUIFormInput();
      if (calendarField.validateType != null) {
        DialogFormUtil.addValidators(uiDateTime, calendarField.validateType);
      }
      if (isAddNew && uiDateTime.getCalendar() == null) {
        uiDateTime.setCalendar(new GregorianCalendar());
      }
    }
    uiDateTime.setDisplayTime(calendarField.isDisplayTime());
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node  node = getNode();
    uiDateTime.setCalendar(uiDateTime.getCalendar());
    if(node != null && node.hasProperty(propertyName) && !isShowingComponent && !isRemovePreference) {
      if (isFirstTimeRender)
        uiDateTime.setCalendar(node.getProperty(propertyName).getDate());
    }
    Node childNode = getChildNode();
    if(isNotEditNode && !isShowingComponent && !isRemovePreference) {
      if(childNode != null) {
        if(childNode.hasProperty(propertyName)) {
          if(childNode.getProperty(propertyName).getDefinition().isMultiple()) {
            Value[] values = childNode.getProperty(propertyName).getValues();
            for(Value value : values) {
              if (uiDateTime.getDefaultValue() == null) {
                uiDateTime.setCalendar(value.getDate());
                uiDateTime.setDefaultValue(uiDateTime.getValue());
              }
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
    }else{
      if((node != null) && node.hasNode("jcr:content") && (childNode == null)) {
        Node jcrContentNode = node.getNode("jcr:content");
        if(jcrContentNode.hasProperty(propertyName)) {
          if(jcrContentNode.getProperty(propertyName).getDefinition().isMultiple()) {
            Value[] values = jcrContentNode.getProperty(propertyName).getValues();
            for(Value value : values) {
              if (uiDateTime.getDefaultValue() == null) {
                uiDateTime.setCalendar(value.getDate());
                uiDateTime.setDefaultValue(uiDateTime.getValue());
              }
            }
          }else{
            uiDateTime.setCalendar(jcrContentNode.getProperty(propertyName).getValue().getDate());
          }
        }
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
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formHiddenField.getChangeInJcrPathParam());
    inputProperty.setValue(formHiddenField.getDefaultValue());
    if(formHiddenField.getMixinTypes() != null) inputProperty.setMixintype(formHiddenField.getMixinTypes());
    if(formHiddenField.getNodeType() != null ) inputProperty.setNodetype(formHiddenField.getNodeType());
    setInputProperty(name, inputProperty);
  }

  public void addInterceptor(String scriptPath, String type) {
    if(scriptPath.length() > 0 && type.length() > 0){
      if(type.equals("prev") && !prevScriptInterceptor.contains(scriptPath)){
        prevScriptInterceptor.add(scriptPath);
      } else if(type.equals("post") && !postScriptInterceptor.contains(scriptPath)){
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
    inputProperty.setChangeInJcrPathParam(mixinField.getChangeInJcrPathParam());
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
        uiMixin.setValue(node.getName());
        addUIFormInput(uiMixin);
      } else
        uiMixin.setValue(node.getName());
      uiMixin.setReadOnly(true);
      renderField(name);
    }
  }

  public void addMixinField(String name, String[] arguments) throws Exception {
    addMixinField(name,null,arguments);
  }

  public void addCheckBoxField(String name, String lable, String[] arguments) throws Exception{
    UIFormCheckBoxField formCheckBoxField =  new UIFormCheckBoxField(name, lable, arguments);
    String jcrPath = formCheckBoxField.getJcrPath();
    String defaultValue = formCheckBoxField.getDefaultValue();
    if (defaultValue == null) defaultValue = "false";
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formCheckBoxField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();
    Node childNode = getChildNode();
    UICheckBoxInput uiCheckBoxInput = findComponentById(name);
    boolean isFirstTimeRender = false;
    if(uiCheckBoxInput == null || isResetForm ){
      isFirstTimeRender = true;
        uiCheckBoxInput = new UICheckBoxInput(name, name, null);
        if (defaultValue != null) {
          uiCheckBoxInput.setValue(Boolean.parseBoolean(defaultValue));
          uiCheckBoxInput.setChecked(Boolean.parseBoolean(defaultValue));
        }
    }

    if (node != null && node.hasProperty(propertyName) && isFirstTimeRender) {
      uiCheckBoxInput.setValue(Boolean.parseBoolean(node.getProperty(propertyName).getValue().toString()));
      uiCheckBoxInput.setChecked(node.getProperty(propertyName).getValue().getBoolean());
    }else if( childNode != null && childNode.hasProperty(propertyName) && isFirstTimeRender){
      uiCheckBoxInput.setValue(Boolean.parseBoolean(childNode.getProperty(propertyName).getValue().toString()));
      uiCheckBoxInput.setChecked(childNode.getProperty(propertyName).getValue().getBoolean());
    }

    if (formCheckBoxField.validateType != null) {
      DialogFormUtil.addValidators(uiCheckBoxInput, formCheckBoxField.validateType);
    }

    if(formCheckBoxField.isOnchange()){
      uiCheckBoxInput.setOnChange("Onchange");
      uiCheckBoxInput.setValue(uiCheckBoxInput.getValue());
    }
    removeChildById(name);
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
    boolean isFirstTimeRender = false;
    if(uiRadioBox == null){
      isFirstTimeRender = true;
      uiRadioBox = new UIFormRadioBoxInput(name, defaultValue, null);
      if(options != null && options.length() > 0){
        String[] array = options.split(";");
        for(int i = 0; i < array.length; i++) {
          String[] arrayChild = array[i].trim().split(",");
          for(int j=0; j<arrayChild.length; j++) {
            optionsList.add(new SelectItemOption<String>(arrayChild[j], arrayChild[j]));
          }
        }
        uiRadioBox.setOptions(optionsList);
      } else {
        uiRadioBox.setOptions(optionsList);
      }
      if(defaultValue != null) uiRadioBox.setDefaultValue(defaultValue);
    }
    addUIFormInput(uiRadioBox);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formRadioBoxField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    Node node = getNode();
    Node childNode = getChildNode();
    if(childNode != null) {
      if(childNode.hasProperty(propertyName) && isFirstTimeRender) {
        uiRadioBox.setValue(childNode.getProperty(propertyName).getValue().getString());
      }
    } else {
      if(node != null && node.hasProperty(propertyName) && isFirstTimeRender) {
        uiRadioBox.setValue(node.getProperty(propertyName).getString());
      }
    }
    if(isNotEditNode) {
      Node child = getChildNode();
      if(child != null && child.hasProperty(propertyName) && isFirstTimeRender) {
        uiRadioBox.setValue(DialogFormUtil.getPropertyValueAsString(child,propertyName));
      }
    }
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
    boolean isFirstTimeRender = false;
    if (uiSelectBox == null || isResetForm) {
      isFirstTimeRender = true;
      uiSelectBox = new UIFormSelectBox(name, name, null);
      if (script != null) {
        try {
          String[] scriptParams = formSelectBoxField.getScriptParams();
          executeScript(script, uiSelectBox, scriptParams, true);
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs", e);
          }
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
      DialogFormUtil.addValidators(uiSelectBox, formSelectBoxField.validateType);
    }
    String[] arrNodes = jcrPath.split("/");
    Node childNode = null;
    Node node = getNode();
    String propertyName = getPropertyName(jcrPath);
    if(node != null && arrNodes.length == 4) childNode = node.getNode(arrNodes[2]);
    if (formSelectBoxField.isMultiValues()) {
      if (formSelectBoxField.getSize() != null
          && StringUtils.isAlphanumeric(formSelectBoxField.getSize())) {
        uiSelectBox.setSize(Integer.parseInt(formSelectBoxField.getSize()));
      }
      uiSelectBox.setMultiple(true);
      StringBuffer buffer = new StringBuffer();
      if((childNode != null) && isFirstTimeRender && childNode.hasProperty(propertyName)) {
        List<String> valueList = new ArrayList<String>();
        Value[] values = childNode.getProperty(propertyName).getValues();
        for(Value value : values) {
          buffer.append(value.getString()).append(",");
        }
        uiSelectBox.setSelectedValues(StringUtils.split(buffer.toString(), ","));
      } else {
        if(node != null && isFirstTimeRender && node.hasProperty(propertyName)) {
          List<String> valueList = new ArrayList<String>();
          if (node.getProperty(propertyName).getDefinition().isMultiple()
              && (!"true".equals(onchange) || !isOnchange)) {
            Value[] values = node.getProperty(propertyName).getValues();
            for(Value value : values) {
              buffer.append(value.getString()).append(",");
            }
          } else if("true".equals(onchange) && isOnchange) {
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
      if ((childNode != null) && isFirstTimeRender && childNode.hasProperty(propertyName)) {
        uiSelectBox.setValue(childNode.getProperty(propertyName).getValue().getString());
      } else {
        if (node != null && node.hasProperty(propertyName)) {
          if (node.getProperty(propertyName).getDefinition().isMultiple()) {
            if (findComponentById(name) == null)
              uiSelectBox.setValue(node.getProperty(propertyName).getValues().toString());
          } else if (formSelectBoxField.isOnchange() && isOnchange) {
            uiSelectBox.setValue(uiSelectBox.getValue());
          } else {
            if (findComponentById(name) == null)
              uiSelectBox.setValue(node.getProperty(propertyName).getValue().getString());
          }
        }
      }
    }
    uiSelectBox.setReadOnly(!formSelectBoxField.isEditable());
//    addUIFormInput(uiSelectBox);
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

    StringBuilder newValues = new StringBuilder();
    int count = 0;
    for (String v : ((UIFormSelectBox)findComponentById(name)).getSelectedValues()) {
      if (count++ > 0) newValues.append(",");
      newValues.append(v);
    }
    String newValue = newValues.toString();
    JcrInputProperty inputProperty = properties.get(name);
    if (inputProperty== null) {
      inputProperty = new JcrInputProperty();
      inputProperty.setJcrPath(jcrPath);
      inputProperty.setChangeInJcrPathParam(formSelectBoxField.getChangeInJcrPathParam());
      setInputProperty(name, inputProperty);
    } else {
      if (inputProperty.getValue() != null) {
      String oldValue = inputProperty.getValue().toString();
      if ((oldValue != null) && (!oldValue.equals(newValue))) {
        Iterator<String> componentSelector = componentSelectors.keySet().iterator();
        Map<String, String> obj = null;
        while (componentSelector.hasNext()) {
          String componentName = componentSelector.next();
          obj = (Map<String, String>) componentSelectors.get(componentName);
          Set<String> set = obj.keySet();
          for (String key : set) {
            if (name.equals(obj.get(key))) {
              UIComponent uiInput = findComponentById(componentName);
              ((UIFormStringInput) uiInput).reset();
            }
          }
        }
      }
      }
    }
    inputProperty.setValue(newValue);
    if (isUpdateSelect && newValue != null) {
      String[] values1 = newValue.split(",");
      uiSelectBox.setSelectedValues(values1);
    }
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
    inputProperty.setChangeInJcrPathParam(formTextAreaField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String option = formTextAreaField.getOptions();
    setInputOption(name, option);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();
    Node childNode = getChildNode();
    boolean isFirstTimeRender = false;
    if(formTextAreaField.isMultiValues()) {
      UIFormMultiValueInputSet uiMulti;
      if(node == null && childNode == null) {
        uiMulti = findComponentById(name);
        if(uiMulti == null) {
          isFirstTimeRender = true;
          uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
          uiMulti.setId(name);
          uiMulti.setName(name);
          uiMulti.setType(UIFormTextAreaInput.class);
          uiMulti.setEditable(formTextAreaField.isEditable());
          if (formTextAreaField.validateType != null) {
            String validateType = formTextAreaField.validateType;
            String[] validatorList = null;
            if (validateType.indexOf(',') > -1) validatorList = validateType.split(",");
            else validatorList = new String[] {validateType};
            for (String validator : validatorList) {
              Object[] params;
              String s_param=null;
              int p_begin, p_end;
              p_begin = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_BEGIN);
              p_end   = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_END);
              if (p_begin>=0 && p_end > p_begin) {
                s_param = validator.substring(p_begin, p_end);
                params = s_param.split(DialogFormUtil.VALIDATOR_PARAM_SEPERATOR);
                uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()), params) ;
              }else {
                uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim())) ;
              }
            }
          }
          List<String> valueList = new ArrayList<String>();
          List<UIComponent> listChildren = uiMulti.getChildren();
          if (listChildren.size() == 0) {
            valueList.add(formTextAreaField.getDefaultValue());
          } else {
            for (UIComponent component : listChildren) {
              UIFormTextAreaInput uiTextAreaInput = (UIFormTextAreaInput)component;
              if(uiTextAreaInput.getValue() != null) {
                valueList.add(uiTextAreaInput.getValue().trim());
              } else{
                valueList.add(formTextAreaField.getDefaultValue());
              }
            }
          }
          uiMulti.setValue(valueList);
          addUIFormInput(uiMulti);
        }
      } else {
        uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
        isFirstTimeRender = true;
        uiMulti.setId(name);
        uiMulti.setName(name);
        uiMulti.setType(UIFormTextAreaInput.class);
        uiMulti.setEditable(formTextAreaField.isEditable());
        if (formTextAreaField.validateType != null) {
          String validateType = formTextAreaField.validateType;
          String[] validatorList = null;
          if (validateType.indexOf(',') > -1)
            validatorList = validateType.split(",");
          else
            validatorList = new String[] { validateType };
          for (String validator : validatorList) {
            Object[] params;
            String s_param = null;
            int p_begin, p_end;
            p_begin = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_BEGIN);
            p_end = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_END);
            if (p_begin >= 0 && p_end > p_begin) {
              s_param = validator.substring(p_begin, p_end);
              params = s_param.split(DialogFormUtil.VALIDATOR_PARAM_SEPERATOR);
              uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()), params);
            } else {
              uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()));
            }
          }
        }
        addUIFormInput(uiMulti);
      }
      List<String> valueList = new ArrayList<String>();
      boolean valueListIsSet = false;
      if((node != null) && node.hasNode("jcr:content") && (childNode == null)) {
        Node jcrContentNode = node.getNode("jcr:content");
        if(jcrContentNode.hasProperty(propertyName)) {
          Value[] values = jcrContentNode.getProperty(propertyName).getValues();
          for(Value value : values) {
            valueList.add(value.getString());
          }
          uiMulti.setEditable(formTextAreaField.isEditable());
          uiMulti.setValue(valueList);
          valueListIsSet = true;
        }
      } else {
        if(childNode != null) {
          if(childNode.hasProperty(propertyName)) {
            Value[] values = childNode.getProperty(propertyName).getValues();
            for(Value value : values) {
              valueList.add(value.getString());
            }
            uiMulti.setEditable(formTextAreaField.isEditable());
            uiMulti.setValue(valueList);
            valueListIsSet = true;
          }
        }
      }
      if (!valueListIsSet && node != null && !isShowingComponent && !isRemovePreference
          && isFirstTimeRender) {
        String propertyPath = jcrPath.substring("/node/".length());
        if (node.hasProperty(propertyPath)) {
          Value[] values = node.getProperty(propertyPath).getValues();
          // if the node type is mix:referenceable, its values will contain the UUIDs of the reference nodes
          // we need to get the paths of the reference nodes instead of its UUIDs to display onto screen
          if (node.getProperty(propertyPath).getType() == PropertyType.REFERENCE) {
            for (Value vl : values) {
              if (vl != null) {
                String strUUID = vl.getString();
                try {
                  String strReferenceableNodePath = node.getSession().getNodeByUUID(strUUID).getPath();

                  //if the referenceable node is not ROOT, remove the "/" character at head
                  if (strReferenceableNodePath.length() > 1){
                    strReferenceableNodePath = strReferenceableNodePath.substring(1);
                  }

                  valueList.add(strReferenceableNodePath);
                } catch (ItemNotFoundException infEx) {
                  valueList.add(formTextAreaField.getDefaultValue());
                } catch (RepositoryException repoEx) {
                  valueList.add(formTextAreaField.getDefaultValue());
                }
              }
            }
          } else {
            for (Value vl : values) {
              if (vl != null) {
                valueList.add(vl.getString());
              }
            }
          }
        }
        uiMulti.setValue(valueList);
      }
      if(isResetMultiField) {
        uiMulti.setValue(new ArrayList<Value>());
      }
      uiMulti.setEditable(formTextAreaField.isEditable());
      renderField(name);
      return;
    }
    UIFormTextAreaInput uiTextArea = findComponentById(name);
    if(uiTextArea == null) {
      isFirstTimeRender = true;
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
    if (node != null && !isShowingComponent && !isRemovePreference && isFirstTimeRender) {
      StringBuffer value = new StringBuffer();
      if (node.hasProperty(propertyName)) {
        value.append(node.getProperty(propertyName).getValue().getString());
        uiTextArea.setValue(value.toString());
      } else if (node.isNodeType("nt:file")) {
        Node jcrContentNode = node.getNode("jcr:content");
        if (jcrContentNode.hasProperty(propertyName)) {
          if (jcrContentNode.getProperty(propertyName).getDefinition().isMultiple()) {
            Value[] values = jcrContentNode.getProperty(propertyName).getValues();
            for (Value v : values) {
              value.append(v.getString());
            }
            uiTextArea.setValue(value.toString());
          } else {
            uiTextArea.setValue(jcrContentNode.getProperty(propertyName).getValue().getString());
          }
        }
      }
    }
    if (isNotEditNode && !isShowingComponent && !isRemovePreference && isFirstTimeRender) {
      if (node != null && node.hasNode("jcr:content") && childNode != null) {
        Node jcrContentNode = node.getNode("jcr:content");
        uiTextArea.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
      } else {
        if (childNode != null) {
          uiTextArea.setValue(propertyName);
        } else if (childNode == null && jcrPath.equals("/node") && node != null) {
          uiTextArea.setValue(node.getName());
        } else {
          uiTextArea.setValue(null);
        }
      }
    }
    //set default value for textarea if no value was set by above code
    if (uiTextArea.getValue() == null) {
      if (formTextAreaField.getDefaultValue() != null)
        uiTextArea.setValue(formTextAreaField.getDefaultValue());
      else
        uiTextArea.setValue("");
    }
    uiTextArea.setReadOnly(!formTextAreaField.isEditable());
    renderField(name);
  }
  public void addTextAreaField(String name, String[] arguments) throws Exception {
    addTextAreaField(name,null,arguments);
  }

  public String getPathTaxonomy() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    String userName = Util.getPortalRequestContext().getRemoteUser();
    Session session;
    if (userName != null)
      session = WCMCoreUtils.getUserSessionProvider().getSession(workspace, getRepository());
    else
      session = WCMCoreUtils.createAnonimProvider().getSession(workspace, getRepository());
    return ((Node)session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH))).getPath();
  }

  @SuppressWarnings("unchecked")
  public void addTextField(String name, String label, String[] arguments) throws Exception {
    UIFormTextField formTextField = new UIFormTextField(name,label,arguments);
    String jcrPath = formTextField.getJcrPath();
    String mixintype = formTextField.getMixinTypes();
    String nodetype = formTextField.getNodeType();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formTextField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String option = formTextField.getOptions();
    setInputOption(name, option);
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
    boolean isFirstTimeRender = false;
    if(formTextField.isMultiValues()) {
      UIFormMultiValueInputSet uiMulti;
      if(node == null &&childNode == null) {
        uiMulti = findComponentById(name);
        if(uiMulti == null) {
          isFirstTimeRender = true;
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
              Object[] params;
              String s_param=null;
              int p_begin, p_end;
              p_begin = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_BEGIN);
              p_end   = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_END);
              if (p_begin>=0 && p_end > p_begin) {
                s_param = validator.substring(p_begin, p_end);
                params = s_param.split(DialogFormUtil.VALIDATOR_PARAM_SEPERATOR);
                uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()), params) ;
              }else {
                uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim())) ;
              }
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
        isFirstTimeRender = true;
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
            Object[] params;
            String s_param=null;
            int p_begin, p_end;
            p_begin = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_BEGIN);
            p_end   = validator.indexOf(DialogFormUtil.VALIDATOR_PARAM_END);
            if (p_begin>=0 && p_end > p_begin) {
              s_param = validator.substring(p_begin, p_end);
              params = s_param.split(DialogFormUtil.VALIDATOR_PARAM_SEPERATOR);
              uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim()), params) ;
            }else {
              uiMulti.addValidator(DialogFormUtil.getValidator(validator.trim())) ;
            }
          }
        }
        if (getChildById(name) == null)
          addUIFormInput(uiMulti);
      }
      List<String> valueList = new ArrayList<String>();
      boolean valueListIsSet = false;
      if((node != null) && node.hasNode("jcr:content") && (childNode == null)) {
        Node jcrContentNode = node.getNode("jcr:content");
        if(jcrContentNode.hasProperty(propertyName)) {
          Value[] values = jcrContentNode.getProperty(propertyName).getValues();
          for(Value value : values) {
            valueList.add(value.getString());
          }
          uiMulti.setEditable(formTextField.isEditable());
          uiMulti.setValue(valueList);
          valueListIsSet = true;
        }
      } else {
        if(childNode != null) {
          if(childNode.hasProperty(propertyName)) {
            Value[] values = childNode.getProperty(propertyName).getValues();
            for(Value value : values) {
              valueList.add(value.getString());
            }
            uiMulti.setEditable(formTextField.isEditable());
            uiMulti.setValue(valueList);
            valueListIsSet = true;
          }
        }
      }
      if (!valueListIsSet && node != null && !isShowingComponent && !isRemovePreference
          && isFirstTimeRender) {
        String propertyPath = jcrPath.substring("/node/".length());
        if (node.hasProperty(propertyPath)) {
          Value[] values = node.getProperty(propertyPath).getValues();
          // if the node type is mix:referenceable, its values will contain the UUIDs of the reference nodes
          // we need to get the paths of the reference nodes instead of its UUIDs to display onto screen
          if (node.getProperty(propertyPath).getType() == PropertyType.REFERENCE) {
            for (Value vl : values) {
              if (vl != null) {
                String strUUID = vl.getString();
                try {
                  String strReferenceableNodePath = node.getSession().getNodeByUUID(strUUID).getPath();

                  //if the referenceable node is not ROOT, remove the "/" character at head
                  if (strReferenceableNodePath.length() > 1){
                    strReferenceableNodePath = strReferenceableNodePath.substring(1);
                  }

                  valueList.add(strReferenceableNodePath);
                } catch (ItemNotFoundException infEx) {
                  valueList.add(formTextField.getDefaultValue());
                } catch (RepositoryException repoEx) {
                  valueList.add(formTextField.getDefaultValue());
                }
              }
            }
          } else {
            for (Value vl : values) {
              if (vl != null) {
                valueList.add(vl.getString());
              }
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
      isFirstTimeRender = true;
      uiInput = formTextField.createUIFormInput();
      addUIFormInput(uiInput);
    }
    uiInput.setReadOnly(!formTextField.isEditable());
    if(uiInput.getValue() == null) uiInput.setValue(formTextField.getDefaultValue());
    else uiInput.setReadOnly(false);

    if(node != null && !isShowingComponent && !isRemovePreference) {
      if(jcrPath.equals("/node") && (!formTextField.isEditable() || formTextField.isEditableIfNull())) {
        uiInput.setDisabled(true);
      }
    }
    if(node != null && !isShowingComponent && !isRemovePreference && isFirstTimeRender) {
      if(jcrPath.equals("/node") && (!formTextField.isEditable() || formTextField.isEditableIfNull())) {
        String value = uiInput.getValue();
        if(i18nNodePath != null) {
          uiInput.setValue(i18nNodePath.substring(i18nNodePath.lastIndexOf("/") + 1));
        } else {
          String nameValue =  node.getPath().substring(node.getPath().lastIndexOf("/") + 1);
          uiInput.setValue(nameValue);
        }
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
    String mimeTypes = formUploadField.getMimeTypes();
    String jcrPath = formUploadField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(formUploadField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String option = formUploadField.getOptions();
    setInputOption(name, option);
    setMultiPart(true);
    String propertyName = getPropertyName(jcrPath);
    properties.put(name, inputProperty);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);
    Node node = getNode();
    if(formUploadField.isMultiValues()) {
      UIFormMultiValueInputSet multiValueField = getChildById(name);
      if (multiValueField == null) {
        String propertyPath = jcrPath.substring("/node/".length());
        if (node != null && node.hasNode(propertyPath)) {
          multiValueField = createUIComponent(UIFormUploadMultiValueInputSet.class, null, null);
          multiValueField.setId(name);
          multiValueField.setName(name);
          multiValueField.setType(UIFormUploadInputNoUploadButton.class);
          addUIFormInput(multiValueField);
          NodeIterator nodeIter = node.getNode(propertyPath).getNodes();
          int count = 0;
          while (nodeIter.hasNext()) {
            Node childNode = nodeIter.nextNode();
            if (!childNode.isNodeType(NodetypeConstant.NT_FILE)) continue;
            UIFormInputBase<?> uiInput = multiValueField.createUIFormInput(count++);
            ((UIFormUploadInputNoUploadButton)uiInput).setFileName(childNode.getName());
            Value value = childNode.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_DATA).getValue();
            ((UIFormUploadInputNoUploadButton)uiInput).setByteValue(
                                                                    IOUtil.getStreamContentAsBytes(value.getStream()));
          }
          if(label != null) multiValueField.setLabel(label);
          multiValueField.setType(UIFormUploadInputNoRemoveButton.class);
          renderField(name);
          return;
        }
        multiValueField = renderMultiValuesInput(UIFormUploadInputNoRemoveButton.class,name,label);
        if (mimeTypes != null) {
          multiValueField.addValidator(UploadFileMimeTypesValidator.class, mimeTypes);
        }
        return;
      }
    } else {
      UIUploadInput uiInputUpload = findComponentById(name);
      if(uiInputUpload == null) {
        uiInputUpload = formUploadField.createUIFormInput();
        if (mimeTypes != null) {
            uiInputUpload.addValidator(UploadFileMimeTypesValidator.class, mimeTypes);
        }
        addUIFormInput(uiInputUpload);
      }
    }
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
    inputProperty.setChangeInJcrPathParam(formWYSIWYGField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String option = formWYSIWYGField.getOptions();
    setInputOption(name, option);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);

    List<UIFormInputBase<String>> wysiwygList = getUIFormInputList(name, formWYSIWYGField, false);
    if(formWYSIWYGField.isMultiValues()) {
      UIFormMultiValueInputSet uiMulti = findComponentById(name);
      if (uiMulti == null) {
        uiMulti = createUIComponent(UIFormMultiValueInputSet.class, WYSIWYG_MULTI_ID, null);

        this.uiMultiValueParam.put(name, arguments);
        uiMulti.setId(name);
        uiMulti.setName(name);
        uiMulti.setType(UIFormWYSIWYGInput.class);
        for (int i = 0; i < wysiwygList.size(); i++) {
          uiMulti.addChild(wysiwygList.get(i));
          wysiwygList.get(i).setId(name + i);
          wysiwygList.get(i).setName(name + i);
        }
        addUIFormInput(uiMulti);
        if(label != null) uiMulti.setLabel(label);
      }
    } else {
      if (wysiwygList.size() > 0)
        addUIFormInput(wysiwygList.get(0));
    }
    renderField(name);
  }

  @SuppressWarnings("unchecked")
  private List<UIFormInputBase<String>> getUIFormInputList(String name,
                                                           DialogFormField formField,
                                                           boolean isCreateNew) throws Exception {
    String jcrPath = formField.getJcrPath();
    String propertyName = getPropertyName(jcrPath);
    List<UIFormInputBase<String>> ret = new ArrayList<UIFormInputBase<String>>();

    UIFormInputBase<String> formInput = formField.isMultiValues() ? null : (UIFormInputBase<String>)findComponentById(name);

    boolean isFirstTimeRender = false;
    if(formInput == null) {
      isFirstTimeRender = true;
      formInput = formField.createUIFormInput();
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
    if (formInput instanceof UIFormWYSIWYGInput)
      ((UIFormWYSIWYGInput)formInput).setFCKConfig(config);
    if(formInput.getValue() == null) formInput.setValue(formField.getDefaultValue());
    Node node = getNode();

    if (isCreateNew) {
      ret.add(formInput);
      return ret;
    }
    if (!formField.isMultiValues() && isFirstTimeRender) {
      if(!isShowingComponent && !isRemovePreference) {
        if(node != null && (node.isNodeType("nt:file") || isNTFile) && formField.isFillJcrDataFile()) {
          Node jcrContentNode = node.getNode("jcr:content");
          formInput.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
        } else {
          if(node != null && node.hasProperty(propertyName)) {
            formInput.setValue(node.getProperty(propertyName).getValue().getString());
          }
        }
      }
      if(isNotEditNode && !isShowingComponent && !isRemovePreference && isFirstTimeRender) {
        Node childNode = getChildNode();
        if(node != null && node.hasNode("jcr:content") && childNode != null && formField.isFillJcrDataFile()) {
          Node jcrContentNode = node.getNode("jcr:content");
          formInput.setValue(jcrContentNode.getProperty("jcr:data").getValue().getString());
        } else {
          if(childNode != null) {
            formInput.setValue(propertyName);
          } else if(childNode == null && jcrPath.equals("/node") && node != null) {
            formInput.setValue(node.getName());
          } else {
            formInput.setValue(null);
          }
        }
      }
      ret.add(formInput);
      return ret;
    }
    Value[] values = null;

    if(!isShowingComponent && !isRemovePreference && isFirstTimeRender) {
      if(node != null && node.hasProperty(propertyName)) {
        values = node.getProperty(propertyName).getValues();
      }
    }
    if(isNotEditNode && !isShowingComponent && !isRemovePreference && isFirstTimeRender) {
      Node childNode = getChildNode();
      if(childNode != null) {
        values = new Value[] {node.getSession().getValueFactory().createValue(propertyName)};
      } else if(childNode == null && jcrPath.equals("/node") && node != null) {
        values = new Value[] {node.getSession().getValueFactory().createValue(node.getName())};
      } else {
        values = new Value[] {node.getSession().getValueFactory().createValue("")};
      }
    }
    if (values != null && isFirstTimeRender) {
      for (Value v : values) {
        UIFormInputBase<String> uiFormInput = formField.createUIFormInput();
        if (uiFormInput instanceof UIFormWYSIWYGInput)
          ((UIFormWYSIWYGInput)uiFormInput).setFCKConfig((FCKEditorConfig)config.clone());
        if(v == null || v.getString() == null)
          uiFormInput.setValue(formField.getDefaultValue());
        else uiFormInput.setValue(v.getString());
        ret.add(uiFormInput);
      }
    } else {
      ret.add(formInput);
    }
    return ret;
  }

  public void addWYSIWYGField(String name, String[] arguments) throws Exception {
    addWYSIWYGField(name,null,arguments);
  }

  public void addRichtextField(String name, String label, String[] arguments) throws Exception {
    UIFormRichtextField richtextField = new UIFormRichtextField(name,label,arguments);
    String jcrPath = richtextField.getJcrPath();
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    inputProperty.setChangeInJcrPathParam(richtextField.getChangeInJcrPathParam());
    setInputProperty(name, inputProperty);
    String option = richtextField.getOptions();
    setInputOption(name, option);
    String propertyName = getPropertyName(jcrPath);
    propertiesName.put(name, propertyName);
    fieldNames.put(propertyName, name);

    List<UIFormInputBase<String>> richtextList = getUIFormInputList(name, richtextField, false);
    if(richtextField.isMultiValues()) {
      UIFormMultiValueInputSet uiMulti = findComponentById(name);
      if (uiMulti == null) {
        uiMulti = createUIComponent(UIFormMultiValueInputSet.class, WYSIWYG_MULTI_ID, null);

        this.uiMultiValueParam.put(name, arguments);
        uiMulti.setId(name);
        uiMulti.setName(name);
        uiMulti.setType(UIFormRichtextInput.class);
        for (int i = 0; i < richtextList.size(); i++) {
          uiMulti.addChild(richtextList.get(i));
          richtextList.get(i).setId(name + i);
          richtextList.get(i).setName(name + i);
        }
        addUIFormInput(uiMulti);
        if(label != null) uiMulti.setLabel(label);
      }
    } else {
      if (getChildById(name) == null && richtextList.size() > 0)
        addUIFormInput(richtextList.get(0));
    }
    renderField(name);
  }

  public void addRichtextField(String name, String[] arguments) throws Exception {
    addRichtextField(name,null,arguments);
  }

  public Node getChildNode() throws Exception {
    if(childPath == null) return null;
    return (Node) getSession().getItem(childPath);
  }

  public String getContentType() { return contentType; };

  public Map<String, JcrInputProperty> getInputProperties() { return properties; }

  public Map<String, String> getInputOptions() { return options; }

  public JcrInputProperty getInputProperty(String name) { return properties.get(name); }
  public String getInputOption(String name) { return options.get(name); }
  public JCRResourceResolver getJCRResourceResolver() { return resourceResolver; }

  public Node getNode() {
    if(nodePath == null) return null;
    try {
      return (Node) getSession().getItem(nodePath);
    } catch (Exception e) {
      return null;
    }
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

  public Session getSession() throws Exception {
    return WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, getRepository());
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try {
      return templateService.getTemplatePathByUser(true, contentType, userName);
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
    boolean clearInterceptor = false;
    if (SAVE_ACTION.equalsIgnoreCase(action) || SAVE_AND_CLOSE.equalsIgnoreCase(action)) {
      try {
        if (executePreSaveEventInterceptor()) {
          super.processAction(context);
          String nodePath_ = (String) context.getAttribute("nodePath");
          if (nodePath_ != null) {
            executePostSaveEventInterceptor(nodePath_);
            clearInterceptor = true;
          }
        } else {
          context.setProcessRender(true);
          super.processAction(context);
        }
      } finally {
        if (clearInterceptor) {
          prevScriptInterceptor.clear();
          postScriptInterceptor.clear();
        }
      }
    } else {
      super.processAction(context);
    }
  }

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
      if (LOG.isWarnEnabled()) {
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
      uiInput.processRender(context);
    } else {
      uiInput.processRender(context);
    }
    if(componentSelectors.get(name) != null) {
      Map<String,String> fieldPropertiesMap = componentSelectors.get(name);
      String fieldName = fieldPropertiesMap.get("returnField");
      String iconClass = "uiIconPlus";
      if(fieldPropertiesMap.get("selectorIcon") != null) {
        iconClass = fieldPropertiesMap.get("selectorIcon");
      }
      ResourceBundle rs = context.getApplicationResourceBundle();
      String showComponent = getResourceBundle(context, getId().concat(".title.ShowComponent"));
      String removeReference = getResourceBundle(context, getId().concat(".title.removeReference"));
      if (name.equals(fieldName)) {
        w.write("<a rel=\"tooltip\" data-placement=\"bottom\" class=\"actionIcon\" title=\"" + showComponent + "\""
            + "onclick=\"javascript:eXo.webui.UIForm.submitEvent('" + "" + getId()
            + "','ShowComponent','&objectId=" + fieldName + "' )\"><i"
            + " class='"
            + iconClass + " uiIconLightGray'></i></a>");
        /* No need Remove action if uiInput is UIFormMultiValueInputSet */
        if (!UIFormMultiValueInputSet.class.isInstance(uiInput))
          w.write("<a rel=\"tooltip\" data-placement=\"bottom\" class=\"actionIcon\" title=\""
              + removeReference
              + "\""
              + "onclick=\"javascript:eXo.webui.UIForm.submitEvent('"
              + ""
              + getId()
              + "','RemoveReference','&objectId="
              + fieldName
              + "' )\"><i"
              +" class='uiIconTrash uiIconLightGray'></i>"
              + "</a>");
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

  public String getImage(InputStream input, String nodeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class);
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(nodeName);
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }
  @Deprecated
  /**
   * Deprecated method, should used removeData(String path) next time
   */
  public boolean dataRemoved() { return dataRemoved_; }
  @Deprecated
  public void setDataRemoved(boolean dataRemoved) { dataRemoved_ = dataRemoved; }

  /**
   * Mark a uploaded field as removed.
   *
   * @param       path: of property content binarydata for uploading
   */
  public void removeData(String path) {
    if (!removedBinary.contains(path)) {
      removedBinary.add(path);
    }
  }
  /**
   * Checking the binary field is removed or not
   *
   * @param       path: of property content binarydata for uploading
   * @return    : True if the uploaded field is removed from UI
   */
  public boolean isDataRemoved(String path) {
    return removedBinary.contains(path);
  }
  public void clearDataRemovedList() {
    removedBinary.clear();
  }
  public void resetProperties() { properties.clear(); }

  public void resetInterceptors(){
    this.prevScriptInterceptor.clear();
    this.postScriptInterceptor.clear();
  }

  public void setChildPath(String childPath) { this.childPath = childPath; }

  public void setContentType(String type) { this.contentType = type; }

  public void setInputProperty(String name, JcrInputProperty value) { properties.put(name, value); }

  public void setInputOption(String name, String value) { options.put(name, value); }

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

  public String getLastModifiedDate() throws Exception {
    return getLastModifiedDate(getNode());
  }

  public String getLastModifiedDate(Node node) {
    String d = StringUtils.EMPTY;
      try {
        if (node.hasProperty("exo:dateModified")) {
          Locale locale = Util.getPortalRequestContext().getLocale();
          DateFormat dateFormater = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, locale);
          Calendar calendar = node.getProperty("exo:dateModified").getValue().getDate();
          d = dateFormater.format(calendar.getTime());
        }
      } catch (ValueFormatException e) { d = StringUtils.EMPTY;
      } catch (IllegalStateException e) { d = StringUtils.EMPTY;
      } catch (PathNotFoundException e) { d = StringUtils.EMPTY;
      } catch (RepositoryException e) { d = StringUtils.EMPTY;
      }
    return d;
  }

  protected List<String> getRemovedNodes() { return removedNodes; }

  public void addRemovedNode(String path) { removedNodes.add(path); }

  public void clearRemovedNode() { removedNodes = new ArrayList<String>(); }

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
          getInputProperties(), getInputOptions());
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
      CmsScript dialogScript = scriptService.getScript(script);
      if (params != null) {
        dialogScript.setParams(params);
      }
      dialogScript.execute(o);
      return true;
    } catch (Exception e) {
      if(printException){
        if (LOG.isWarnEnabled()) {
          LOG.warn("An unexpected error occurs", e);
        }
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
        node = WCMCoreUtils.getSystemSessionProvider().getSession(ws, getRepository()).getNodeByUUID(uuid);
        return ws + ":" + node.getPath();
      } catch(ItemNotFoundException e) {
        continue;
      }
    }
    if (LOG.isErrorEnabled()) {
      LOG.error("No node with uuid ='" + uuid + "' can be found");
    }
    return null;
  }

  private ManageableRepository getRepository() throws Exception{
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class);
    return repositoryService.getCurrentRepository();
  }

  private UIFormMultiValueInputSet renderMultiValuesInput(Class<? extends UIFormInputBase<?>> type, String name,String label) throws Exception{
    UIFormMultiValueInputSet ret = addMultiValuesInput(type, name, label);
    renderField(name);
    return ret;
  }

  private UIFormMultiValueInputSet addMultiValuesInput(Class<? extends UIFormInputBase<?>> type, String name,String label) throws Exception{
    UIFormMultiValueInputSet uiMulti = null;
    if (UIUploadInput.class.isAssignableFrom(type)) {
      uiMulti = createUIComponent(UIFormUploadMultiValueInputSet.class, null, null);
    }
    else {
      uiMulti = createUIComponent(UIFormMultiValueInputSet.class, null, null);
    }
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
    writer.append("<div class=\"uiAction uiActionBorder\">");
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
      writer.append("<button type=\"button\" class=\"btn\" onclick =\"")
            .append(link)
            .append("\">")
            .append(actionLabel)
            .append("</button>");
    }
    writer.append("</div>");
  }

  public Node getNodeByType(String nodeType) throws Exception {
    if (this.getNode() == null) return null;
    NodeIterator nodeIter = this.getNode().getNodes();
    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (node.isNodeType(nodeType))
        return node;
    }
    return null;
  }

  static public class AddActionListener extends EventListener<UIFormMultiValueInputSet> {
    public void execute(Event<UIFormMultiValueInputSet> event) throws Exception {
      UIFormMultiValueInputSet uiSet = event.getSource();
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      if (uiSet.getId().equals(id)) {
        // get max id
        List<UIComponent> children = uiSet.getChildren();
        if (children.size() > 0) {
          UIFormInputBase<?> uiInput = (UIFormInputBase<?>) children.get(children.size() - 1);
          String index = uiInput.getId();
          int maxIndex = Integer.parseInt(index.replaceAll(id, ""));

          UIDialogForm uiDialogForm = uiSet.getAncestorOfType(UIDialogForm.class);
          String[] arguments = uiDialogForm.uiMultiValueParam.get(uiSet.getName());
          UIFormInputBase<?> newUIInput = null;
          if (uiInput instanceof UIFormWYSIWYGInput) {
            newUIInput = uiDialogForm.getUIFormInputList(uiSet.getName(),
                                                         new UIFormWYSIWYGField(uiSet.getName(),
                                                                                null,
                                                                                arguments),
                                                         true).get(0);
          } else {
            newUIInput = uiDialogForm.getUIFormInputList(uiSet.getName(),
                                                         new UIFormRichtextField(uiSet.getName(),
                                                                                 null,
                                                                                 arguments),
                                                         true).get(0);
          }

          uiSet.addChild(newUIInput);
          newUIInput.setId(uiSet.getName() + (maxIndex + 1));
          newUIInput.setName(uiSet.getName() + (maxIndex + 1));
        }
      }
    }
  }

}
