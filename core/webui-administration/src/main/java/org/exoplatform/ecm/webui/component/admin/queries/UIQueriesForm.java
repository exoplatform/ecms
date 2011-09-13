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
package org.exoplatform.ecm.webui.component.admin.queries;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
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
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006
 * 11:30:29 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIQueriesForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueriesForm.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueriesForm.ChangeQueryTypeActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueriesForm.AddPermissionActionListener.class)
    }
)
public class UIQueriesForm extends UIForm implements UISelectable {

  final static public String QUERY_NAME = "name" ;
  final static public String QUERY_TYPE = "type" ;
  final static public String STATEMENT = "statement" ;
  final static public String PERMISSIONS = "permissions" ;
  final static public String CACHE_RESULT = "cache" ;
  final static public String[] ACTIONS = {"Save", "Cancel"} ;
  final static public String SQL_QUERY = "select * from exo:article where jcr:path like '/Documents/Live/%'" ;
  final static public String XPATH_QUERY = "/jcr:root/Documents/Live//element(*, exo:article)" ;
  final static public String[] REG_EXPRESSION = {"[", "]", ":", "&"} ;

  private boolean isAddNew_ = false ;

  public UIQueriesForm() throws Exception {
    addUIFormInput(new UIFormStringInput(QUERY_NAME, QUERY_NAME, null).
      addValidator(MandatoryValidator.class).
        addValidator(ECMNameValidator.class)) ;
    List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>() ;
    ls.add(new SelectItemOption<String>("xPath", "xpath")) ;
    ls.add(new SelectItemOption<String>("SQL", "sql")) ;
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(QUERY_TYPE, QUERY_TYPE, ls) ;
    uiSelectBox.setOnChange("ChangeQueryType") ;
    addUIFormInput(uiSelectBox) ;
    UIFormTextAreaInput uiStatement = new UIFormTextAreaInput(STATEMENT, STATEMENT, null) ;
    uiStatement.setValue(XPATH_QUERY) ;
    uiStatement.addValidator(MandatoryValidator.class) ;
    addUIFormInput(uiStatement) ;
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(CACHE_RESULT, CACHE_RESULT, null)) ;
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction("PermissionButton") ;
    uiInputAct.addUIFormInput(new UIFormStringInput(PERMISSIONS, PERMISSIONS, null).setEditable(false)
                                                                                   .addValidator(MandatoryValidator.class));
    uiInputAct.setActionInfo(PERMISSIONS, new String[] {"AddPermission"}) ;
    addUIComponentInput(uiInputAct) ;
  }

  public String[] getActions() { return ACTIONS ; }

  public void doSelect(String selectField, Object value) {
    getUIStringInput(selectField).setValue(value.toString());
    UIQueriesManager uiManager = getAncestorOfType(UIQueriesManager.class);
    UIPopupWindow uiPopup = uiManager.getChildById("PermissionPopup");
    uiPopup.setShowMask(true);
    uiManager.removeChildById("PermissionPopup");
  }

  public void setIsAddNew(boolean isAddNew) { isAddNew_ = isAddNew ; }

  public void update(String queryName)throws Exception{
    isAddNew_ = false ;
    QueryService queryService = getApplicationComponent(QueryService.class) ;
    if(queryName == null) {
      isAddNew_ = true ;
      reset() ;
      return ;
    }
    Node query = queryService.getSharedQuery(queryName, WCMCoreUtils.getSystemSessionProvider());
    getUIStringInput(QUERY_NAME).setValue(queryName) ;
    getUIStringInput(QUERY_NAME).setEditable(false) ;
    if(query.hasProperty("exo:cachedResult")) {
      getUIFormCheckBoxInput(CACHE_RESULT).setChecked(query.getProperty("exo:cachedResult").getBoolean()) ;
    } else {
      getUIFormCheckBoxInput(CACHE_RESULT).setChecked(false) ;
    }
    if(query.hasProperty("jcr:statement")) {
      getUIFormTextAreaInput(STATEMENT).setValue(query.getProperty("jcr:statement").getString()) ;
    }
    if(query.hasProperty("jcr:language")) {
      getUIFormSelectBox(QUERY_TYPE).setValue(query.getProperty("jcr:language").getString()) ;
    }
    if(query.hasProperty("exo:accessPermissions")) {
      Value[] values = query.getProperty("exo:accessPermissions").getValues() ;
      StringBuilder strValues = new StringBuilder() ;
      for(int i = 0; i < values.length; i ++) {
        if(strValues.length() > 0) strValues = strValues.append(",") ;
        strValues = strValues.append(values[i].getString()) ;
      }
      getUIStringInput(PERMISSIONS).setValue(strValues.toString()) ;
    }
  }

  static public class CancelActionListener extends EventListener<UIQueriesForm> {
    public void execute(Event<UIQueriesForm> event) throws Exception {
      UIQueriesForm uiForm = event.getSource() ;
      UIQueriesManager uiManager = uiForm.getAncestorOfType(UIQueriesManager.class) ;
      uiManager.removeChildById(UIQueriesList.ST_ADD) ;
      uiManager.removeChildById(UIQueriesList.ST_EDIT) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class SaveActionListener extends EventListener<UIQueriesForm> {
    public void execute(Event<UIQueriesForm> event) throws Exception {
      UIQueriesForm uiForm = event.getSource() ;
      QueryService queryService = uiForm.getApplicationComponent(QueryService.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String queryName = uiForm.getUIStringInput(QUERY_NAME).getValue().trim();
      if(uiForm.isAddNew_) {
        for(Node queryNode : queryService.getSharedQueries(WCMCoreUtils.getSystemSessionProvider())) {
          if(queryNode.getName().equals(queryName)) {
            uiApp.addMessage(new ApplicationMessage("UIQueriesForm.msg.name-existing", null,
                ApplicationMessage.WARNING)) ;            
            return ;
          }
        }
      }
      if(!Utils.isNameValid(queryName, REG_EXPRESSION)) {
        uiApp.addMessage(new ApplicationMessage("UIQueriesForm.msg.name-invalid", null,
                                                ApplicationMessage.WARNING)) ;        
        return ;
      }
      String statement = uiForm.getUIFormTextAreaInput(STATEMENT).getValue() ;
      UIFormInputSetWithAction permField = uiForm.getChildById("PermissionButton") ;
      String permissions = permField.getUIStringInput(PERMISSIONS).getValue() ;
      if((permissions == null)||(permissions.trim().length() == 0)) {
        uiApp.addMessage(new ApplicationMessage("UIQueriesForm.msg.permission-require", null,
                                                ApplicationMessage.WARNING)) ;        
        return ;
      }
      String queryType = uiForm.getUIFormSelectBox(QUERY_TYPE).getValue() ;
      boolean cacheResult = uiForm.getUIFormCheckBoxInput(CACHE_RESULT).isChecked() ;
      try {
        if(permissions.indexOf(",") > -1) {
          queryService.addSharedQuery(queryName,
                                      statement,
                                      queryType,
                                      permissions.split(","),
                                      cacheResult);
        } else {
          queryService.addSharedQuery(queryName,
                                      statement,
                                      queryType,
                                      new String[] { permissions },
                                      cacheResult);
        }
      } catch(InvalidQueryException qe) {
        uiApp.addMessage(new ApplicationMessage("UIQueriesForm.msg.invalid-query", null,
                                                ApplicationMessage.WARNING)) ;        
        return ;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UIQueriesForm.msg.access-denied", null,
                  ApplicationMessage.WARNING)) ;        
        return ;
      }
      UIQueriesManager uiManager = uiForm.getAncestorOfType(UIQueriesManager.class) ;
      uiManager.getChild(UIQueriesList.class).refresh(1);
      uiManager.removeChildById(UIQueriesList.ST_ADD) ;
      uiManager.removeChildById(UIQueriesList.ST_EDIT) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class ChangeQueryTypeActionListener extends EventListener<UIQueriesForm> {
    public void execute(Event<UIQueriesForm> event) throws Exception {
      UIQueriesForm uiForm= event.getSource() ;
      String queryType = uiForm.getUIFormSelectBox(QUERY_TYPE).getValue() ;
      if(queryType.equals(Query.XPATH)) {
        uiForm.getUIFormTextAreaInput(STATEMENT).setValue(XPATH_QUERY) ;
      } else {
        uiForm.getUIFormTextAreaInput(STATEMENT).setValue(SQL_QUERY) ;
      }
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIQueriesForm> {
    public void execute(Event<UIQueriesForm> event) throws Exception {
      UIQueriesManager uiManager = event.getSource().getAncestorOfType(UIQueriesManager.class) ;
      String membership = event.getSource().getUIStringInput(PERMISSIONS).getValue() ;
      uiManager.initPermissionPopup(membership) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
