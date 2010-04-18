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
package org.exoplatform.ecm.webui.component.admin.repository;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Aug 8, 2007  
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl")
public class UIWizardStep2 extends UIFormInputSetWithAction {
  final static public String FIELD_CONTAINER = "container" ;
  final static public String FIELD_SOURCENAME = "sourceName" ;  
  final static public String FIELD_DBTYPE = "dbType" ;
  final static public String FIELD_ISMULTI = "isMulti" ;
  final static public String FIELD_STORETYPE = "storeType" ;
  final static public String FIELD_MAXBUFFER = "maxBuffer" ;
  final static public String FIELD_SWAPPATH = "swapPath" ;
  final static public String FIELD_STOREPATH = "storePath" ;  
  final static public String FIELD_FILTER = "filterType" ;  
  final static public String FIELD_EXTERNAL_STORE = "externalStore" ;

  public UIWizardStep2(String name) throws Exception {
    super(name);
    addChild(new UIFormStringInput(FIELD_CONTAINER, FIELD_CONTAINER, null)) ;
    setActionInfo(FIELD_CONTAINER, new String[]{"SelectContainer"}) ;
    addChild(new UIFormStringInput(FIELD_SOURCENAME, FIELD_SOURCENAME, null)) ;
    addChild(new UIFormSelectBox(FIELD_DBTYPE, FIELD_DBTYPE, getDbType())) ;
    addChild(new UIFormCheckBoxInput<Boolean>(FIELD_ISMULTI, FIELD_ISMULTI, null)) ;
    addChild(new UIFormStringInput(FIELD_MAXBUFFER, FIELD_MAXBUFFER, null)) ;
    addChild(new UIFormStringInput(FIELD_SWAPPATH, FIELD_SWAPPATH, null)) ;
    UIFormCheckBoxInput<Boolean> externalStore = new UIFormCheckBoxInput<Boolean>(FIELD_EXTERNAL_STORE,FIELD_EXTERNAL_STORE, null) ;
    externalStore.setOnChange("ChangeTypeStore") ;
    addChild(externalStore) ;
    addChild(new UIFormStringInput(FIELD_STORETYPE, FIELD_STORETYPE, null).setRendered(false)) ;
    setActionInfo(FIELD_STORETYPE, new String[]{"SelectStore"}) ;
    addChild(new UIFormSelectBox(FIELD_FILTER, FIELD_FILTER, getFilterType()).setRendered(false)) ;
    addChild(new UIFormStringInput(FIELD_STOREPATH, FIELD_STOREPATH, null).setRendered(false)) ;
  }
  public List<SelectItemOption<String>> getFilterType() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("Binary", "Binary")) ;
    return options ;
  }
  private List<SelectItemOption<String>> getDbType() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    for(String dataType : DBConstants.DB_DIALECTS) {
      options.add(new SelectItemOption<String>(dataType, dataType)) ;
    }
    return options ;
  }
  protected void resetFields() {
    reset() ;
    setFieldIsMulti(false) ;
    setFieldDBType(getDbType()) ;
  }
  protected void fillFields(String containerType, String sourceName, String selectedDBType, boolean isMutil, 
      String storeType, String storePath, String filter, String maxBuffer, String swapPath){
    setFieldContainer(containerType) ;
    setFieldSourceName(sourceName) ;
    setFieldDBType(selectedDBType) ;
    setFieldIsMulti(isMutil) ;
    setFieldStoreType(storeType) ;
    setFieldStorePath(storePath) ;
    setFieldFilter(filter);
    setFieldMaxBuffer(maxBuffer) ;
    setFieldSwapPath(swapPath) ;
  }
  protected void setFieldContainer(String container) {
    getUIStringInput(FIELD_CONTAINER).setValue(container) ;
  }
  protected void setFieldSourceName(String source) {
    getUIStringInput(FIELD_SOURCENAME).setValue(source) ;
  }
  protected void setFieldDBType(String dbType) {
    getUIFormSelectBox(FIELD_DBTYPE).setValue(dbType) ;
  }
  protected void setFieldDBType(List<SelectItemOption<String>> options) {
    getUIFormSelectBox(FIELD_DBTYPE).setOptions(options) ;
  }
  protected void setFieldIsMulti(boolean isMulti) {
    getUIFormCheckBoxInput(FIELD_ISMULTI).setChecked(isMulti) ;
  }
  protected void setFieldStoreType(String storeType) {
    getUIStringInput(FIELD_STORETYPE).setValue(storeType) ;
  }
  protected void setFieldStorePath(String storePath) {
    getUIStringInput(FIELD_STOREPATH).setValue(storePath) ;
  }
  protected void setFieldFilter(String filter) {
    getUIStringInput(FIELD_FILTER).setValue(filter) ;
  }
  protected void setFieldMaxBuffer(String maxBuffer) {
    getUIStringInput(FIELD_MAXBUFFER).setValue(maxBuffer) ;
  }
  protected void setFieldSwapPath(String swappath) {
    getUIStringInput(FIELD_SWAPPATH).setValue(swappath) ;
  }

  protected void lockFields(boolean isLock) {
    boolean isEdiable =!isLock ;
    if(isLock){
      setActionInfo(FIELD_CONTAINER, null) ;
      setActionInfo(FIELD_STORETYPE, null) ;
    } else {
      setActionInfo(FIELD_CONTAINER, new String[]{"SelectContainer"}) ;
      setActionInfo(FIELD_STORETYPE, new String[]{"SelectStore"}) ;
    }
    getUIStringInput(FIELD_CONTAINER).setEditable(isEdiable) ;
    getUIStringInput(FIELD_SOURCENAME).setEditable(isEdiable) ;
    getUIFormSelectBox(FIELD_DBTYPE).setEnable(isEdiable) ;
    getUIFormCheckBoxInput(FIELD_ISMULTI).setEnable(isEdiable) ;
    getUIStringInput(FIELD_STORETYPE).setEditable(isEdiable) ;
    getUIStringInput(FIELD_STOREPATH).setEditable(isEdiable) ;
    getUIStringInput(FIELD_FILTER).setEnable(isEdiable) ;
    getUIStringInput(FIELD_MAXBUFFER).setEditable(isEdiable) ;
    getUIStringInput(FIELD_SWAPPATH).setEditable(isEdiable) ;
  }
}
