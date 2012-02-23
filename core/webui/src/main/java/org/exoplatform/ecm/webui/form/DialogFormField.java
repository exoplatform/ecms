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

import java.util.HashMap;

import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
public abstract class DialogFormField {

  protected final String SEPARATOR = "=";
  protected final String JCR_PATH = "jcrPath" + SEPARATOR;
  protected final String EDITABLE = "editable" + SEPARATOR;
  protected final String ONCHANGE = "onchange" + SEPARATOR;
  protected final String OPTIONS = "options" + SEPARATOR;
  protected final String TYPE = "type" + SEPARATOR ;
  protected final String VISIBLE = "visible" + SEPARATOR;
  protected final String NODETYPE = "nodetype" + SEPARATOR;
  protected final String MIXINTYPE = "mixintype" + SEPARATOR;
  protected final String MIMETYPE = "mimetype" + SEPARATOR; 
  protected final String VALIDATE = "validate" + SEPARATOR;
  protected final String SELECTOR_ACTION = "selectorAction" + SEPARATOR;
  protected final String SELECTOR_CLASS = "selectorClass" + SEPARATOR;
  protected final String SELECTOR_ICON = "selectorIcon" + SEPARATOR;
  protected final String SELECTOR_PARAMS = "selectorParams" + SEPARATOR;
  protected final String WORKSPACE_FIELD = "workspaceField" + SEPARATOR;
  protected final String SCRIPT = "script" + SEPARATOR;
  protected final String SCRIPT_PARAMS = "scriptParams" + SEPARATOR;
  protected final String MULTI_VALUES = "multiValues" + SEPARATOR;
  protected final String REFERENCE = "reference" + SEPARATOR;
  protected final String REPOSITORY = "repository";
  protected final String DEFAULT_VALUES = "defaultValues" + SEPARATOR ;
  protected final String ROW_SIZE = "rows" + SEPARATOR ;
  protected final String COL_SIZE = "columns" + SEPARATOR ;
  protected final String SIZE = "size" + SEPARATOR ;
  protected final String CHANGE_IN_JCR_PATH_PARAM = "changeInJcrPathParam" + SEPARATOR;
  protected final String FILL_JCR_DATA_OF_FILE = "fillJcrDataOfFile" + SEPARATOR;

  protected String editable;
  protected String defaultValue;
  protected String rowSize;
  protected String colSize;
  protected String jcrPath;
  protected String selectorAction;
  protected String selectorClass;
  protected String workspaceField;
  protected String selectorIcon;
  protected String multiValues;
  protected String reference;
  protected String validateType;
  protected String selectorParams;
  protected String name;
  protected String label;
  protected String options;
  protected String visible;
  protected String nodeType;
  protected String mixinTypes;
  protected String mimeTypes;
  protected String onchange;
  protected String groovyScript;
  protected String[] scriptParams;
  protected String type;
  protected String size;
  protected String changeInJcrPathParam;
  protected String fillJcrDataOfFile = "true";

  public DialogFormField(String name, String label, String[] arguments) {
    HashMap<String,String> parsedArguments = parseArguments(arguments) ;
    this.editable = parsedArguments.get(EDITABLE);
    this.defaultValue = parsedArguments.get(DEFAULT_VALUES);
    this.rowSize = parsedArguments.get(ROW_SIZE);
    this.colSize = parsedArguments.get(COL_SIZE);
    this.jcrPath = parsedArguments.get(JCR_PATH);
    this.selectorAction = parsedArguments.get(SELECTOR_ACTION);
    this.selectorClass = parsedArguments.get(SELECTOR_CLASS);
    this.workspaceField = parsedArguments.get(WORKSPACE_FIELD);
    this.selectorIcon = parsedArguments.get(SELECTOR_ICON);
    this.multiValues = parsedArguments.get(MULTI_VALUES);
    this.reference = parsedArguments.get(REFERENCE);
    this.validateType = parsedArguments.get(VALIDATE);
    this.selectorParams = parsedArguments.get(SELECTOR_PARAMS) ;
    this.options = parsedArguments.get(OPTIONS);
    this.visible = parsedArguments.get(VISIBLE);
    this.nodeType = parsedArguments.get(NODETYPE);
    this.mixinTypes = parsedArguments.get(MIXINTYPE);
    this.mimeTypes = parsedArguments.get(MIMETYPE);
    this.onchange = parsedArguments.get(ONCHANGE);
    this.groovyScript = parsedArguments.get(SCRIPT);
    this.type = parsedArguments.get(TYPE);
    this.size = parsedArguments.get(SIZE);
    this.changeInJcrPathParam = parsedArguments.get(CHANGE_IN_JCR_PATH_PARAM);
    this.fillJcrDataOfFile = parsedArguments.get(FILL_JCR_DATA_OF_FILE);
    String scriptParam = parsedArguments.get(SCRIPT_PARAMS);
    if(scriptParam != null) {
      scriptParams = scriptParam.split(",");
    }
    this.name = name;
    this.label = label;
  }

  @SuppressWarnings("unchecked")
  public abstract <T extends UIFormInputBase> T createUIFormInput() throws Exception;

  public JcrInputProperty createJcrInputProperty (){
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(jcrPath);
    return inputProperty;
  }

  public String getEditable() { return editable; }
  public void setEditable(String editable) { this.editable = editable; }

  public String getDefaultValue() { return defaultValue; }
  public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

  public String getRowSize() { return rowSize; }
  public void setRowSize(String rowSize) { this.rowSize = rowSize; }

  public String getColSize() { return colSize; }
  public void setColSize(String colSize) { this.colSize = colSize; }

  public String getJcrPath() { return jcrPath; }
  public void setJcrPath(String jcrPath) { this.jcrPath = jcrPath; }

  public String getSelectorAction() { return selectorAction; }
  public void setSelectorAction(String selectorAction) {
    this.selectorAction = selectorAction;
  }

  public String getSelectorClass() { return selectorClass; }
  public void setSelectorClass(String selectorClass) {
    this.selectorClass = selectorClass;
  }

  public String getWorkspaceField() { return workspaceField; }
  public void setWorkspaceField(String workspaceField) {
    this.workspaceField = workspaceField;
  }

  public String getSelectorIcon() {  return selectorIcon; }
  public void setSelectorIcon(String selectorIcon) {
    this.selectorIcon = selectorIcon;
  }

  public String getMultiValues() { return multiValues; }
  public void setMultiValues(String multiValues) {
    this.multiValues = multiValues;
  }

  public String getReference() { return reference; }
  public void setReferenceType(String reference) {
    this.reference = reference;
  }

  public String getValidateType() { return validateType; }
  public void setValidateType(String validateType) {
    this.validateType = validateType;
  }

  public String[] getSelectorParams() {
    if(selectorParams != null) {
      return selectorParams.split(",");
    }
    return null;
  }

  public String getSelectorParam() { return selectorParams; }
  public void setSelectorParam(String param) { this.selectorParams = param; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getLabel() { return label; }
  public void setLabel(String label) {this.label = label;}

  public String getOptions() { return options; }
  public void setOptions(String options) { this.options = options; }

  public String getVisible() { return visible;}
  public void setVisible(String visible) {this.visible = visible; }

  public String getNodeType() { return nodeType; }
  public void setNodeType(String nodeType) { this.nodeType = nodeType; }

  public String getMixinTypes() { return mixinTypes; }
  public void setMixinTypes(String mixinTypes) { this.mixinTypes = mixinTypes; }
  
  public String getMimeTypes() { return mimeTypes; }
  public void setMimeTypes(String mimeTypes) { this.mimeTypes = mimeTypes; }

  public String getOnchange() { return onchange; }
  public void setOnchange(String onchange) { this.onchange = onchange; }

  public String getGroovyScript() { return groovyScript; }
  public void setGroovyScript(String groovyScript) { this.groovyScript = groovyScript; }

  public String[] getScriptParams() { return scriptParams; }
  public void setScriptParams(String[] scriptParams) { this.scriptParams = scriptParams; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type;}

  public String getSize() { return size; }
  public void setSize(String size) { this.size = size;}
  
  public String getChangeInJcrPathParam() { return changeInJcrPathParam; }
  public void setChangeInJcrPathParam(String value) { this.changeInJcrPathParam = value; }
  
  public String getFillJcrDataFile() { return fillJcrDataOfFile; }
  public void setFillJcrDataFile(String value) { fillJcrDataOfFile = value; }

  public boolean isMultiValues() { return "true".equalsIgnoreCase(multiValues); }
  public boolean isReference() { return "true".equalsIgnoreCase(reference); }
  public boolean isEditable() { return !"false".equalsIgnoreCase(editable); }
  public boolean isEditableIfNull() { return "if-null".equalsIgnoreCase(editable); }
  public boolean isVisibleIfNotNull() { return "if-not-null".equals(visible); }
  public boolean isFillJcrDataFile() { 
    return "true".equals(fillJcrDataOfFile) || fillJcrDataOfFile == null; 
  }

  private HashMap<String,String> parseArguments(String[] arguments) {
    HashMap<String,String> map = new HashMap<String,String>() ;
    for(String argument:arguments) {
      String value = null;
      if(argument.indexOf(SEPARATOR)>0) {
        value = argument.substring(argument.indexOf(SEPARATOR)+1) ;
      }else {
        value = argument;
        map.put(DEFAULT_VALUES,value) ; continue;
      }
      if (argument.startsWith(JCR_PATH)) {
        map.put(JCR_PATH,value); continue;
      } else if (argument.startsWith(EDITABLE)) {
        map.put(EDITABLE,value); continue;
      } else if (argument.startsWith(SELECTOR_ACTION)) {
        map.put(SELECTOR_ACTION,value) ; continue;
      } else if (argument.startsWith(SELECTOR_CLASS)) {
        map.put(SELECTOR_CLASS,value); continue;
      } else if (argument.startsWith(MULTI_VALUES)) {
        map.put(MULTI_VALUES,value); continue;
      } else if (argument.startsWith(SELECTOR_ICON)) {
        map.put(SELECTOR_ICON,value); continue;
      } else if (argument.startsWith(SELECTOR_PARAMS)) {
        map.put(SELECTOR_PARAMS,value); continue;
      }else if (argument.startsWith(WORKSPACE_FIELD)) {
        map.put(WORKSPACE_FIELD,value); continue;
      } else if (argument.startsWith(VALIDATE)) {
        map.put(VALIDATE,value); continue;
      } else if (argument.startsWith(REFERENCE)) {
        map.put(REFERENCE, value); continue;
      } else if(argument.startsWith(DEFAULT_VALUES)) {
        map.put(DEFAULT_VALUES,value); continue;
      } else if(argument.startsWith(ROW_SIZE)) {
        map.put(ROW_SIZE,value); continue;
      } else if(argument.startsWith(COL_SIZE)) {
        map.put(COL_SIZE,value); continue;
      } else if(argument.startsWith(OPTIONS)){
        map.put(OPTIONS,value);  continue;
      } else if(argument.startsWith(SCRIPT)) {
        map.put(SCRIPT,value); continue;
      } else if(argument.startsWith(SCRIPT_PARAMS)){
        map.put(SCRIPT_PARAMS,value); continue;
      } else if(argument.startsWith(VISIBLE)){
        map.put(VISIBLE,value); continue;
      } else if(argument.startsWith(TYPE)){
        map.put(TYPE,value) ; continue;
      } else if(argument.startsWith(ONCHANGE)){
        map.put(ONCHANGE,value); continue;
      } else if (argument.startsWith(MIXINTYPE)) {
        map.put(MIXINTYPE, value); continue;
      } else if (argument.startsWith(MIMETYPE)) {
        map.put(MIMETYPE, value); continue;
      } else if(argument.startsWith(NODETYPE)) {
        map.put(NODETYPE, value) ;
        continue ;
      } else if(argument.startsWith(SIZE)) {
        map.put(SIZE, value) ;
        continue ;
      } else if (argument.startsWith(CHANGE_IN_JCR_PATH_PARAM)) {
        map.put(CHANGE_IN_JCR_PATH_PARAM, value);
        continue;
      } else if (argument.startsWith(FILL_JCR_DATA_OF_FILE)) {
        map.put(FILL_JCR_DATA_OF_FILE, value);
        continue;
      } else {
        map.put(DEFAULT_VALUES,argument);
      }
    }
    return map;
  }
}
