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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Oct 2, 2006
 * 1:55:22 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIJCRAdvancedSearch.SaveActionListener.class),
      @EventConfig(listeners = UIJCRAdvancedSearch.SearchActionListener.class),
      @EventConfig(listeners = UIJCRAdvancedSearch.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(phase=Phase.DECODE, listeners = UIJCRAdvancedSearch.ChangeOptionActionListener.class)
    }
)
public class UIJCRAdvancedSearch extends UIForm implements UIPopupComponent {
  public static final String FIELD_NAME = "name" ;
  public static final String FIELD_QUERY = "query" ;
  public static final String FIELD_SELECT_BOX = "selectBox" ;

  private static final String ROOT_SQL_QUERY = "select * from nt:base order by exo:dateCreated DESC" ;
  private static final String SQL_QUERY = "select * from nt:base where jcr:path like '$0/%' order by exo:dateCreated DESC" ;
  private static final String ROOT_XPATH_QUERY = "//* order by @exo:dateCreated descending" ;
  private static final String XPATH_QUERY = "/jcr:root$0//* order by @exo:dateCreated descending" ;
  private static final String CHANGE_OPTION = "ChangeOption" ;
  private boolean isEdit_ = false ;
  private String queryPath_ ;
  private String queryStatement_;
  private String queryLanguage_;

  public UIJCRAdvancedSearch() throws Exception  {
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null));
    List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>() ;
    ls.add(new SelectItemOption<String>("SQL", "sql")) ;
    ls.add(new SelectItemOption<String>("xPath", "xpath")) ;
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(FIELD_SELECT_BOX, FIELD_SELECT_BOX, ls) ;
    uiSelectBox.setOnChange("Change") ;
    addUIFormInput(uiSelectBox) ;
    addUIFormInput(new UIFormTextAreaInput(FIELD_QUERY, FIELD_QUERY, null)) ;
    setActions(new String[]{"Search", "Save", "Cancel"}) ;
  }

  public void update(Query query) throws Exception {
    if(query == null) {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
      Node selectedNode = uiExplorer.getCurrentNode() ;
      String path = selectedNode.getPath() ;
      String queryText = StringUtils.replace(SQL_QUERY, "$0", path) ;
      if ("/".equals(path)) queryText = ROOT_SQL_QUERY  ;
      getUIStringInput(FIELD_NAME).setValue(null) ;
      getUIStringInput(FIELD_NAME).setEditable(true) ;
      getUIFormSelectBox(FIELD_SELECT_BOX).setOnChange(CHANGE_OPTION) ;
      getUIFormSelectBox(FIELD_SELECT_BOX).setValue("sql") ;
      getUIFormTextAreaInput(FIELD_QUERY).setValue(queryText) ;
    } else {
      String storedPath = query.getStoredQueryPath() ;
      queryPath_ = storedPath ;
      storedPath = storedPath.substring(storedPath.lastIndexOf("/") + 1, storedPath.length()) ;
      getUIStringInput(FIELD_NAME).setValue(storedPath) ;
      getUIStringInput(FIELD_NAME).setEditable(false) ;
      getUIFormSelectBox(FIELD_SELECT_BOX).setOnChange(CHANGE_OPTION) ;
      getUIFormSelectBox(FIELD_SELECT_BOX).setValue(query.getLanguage()) ;
      getUIFormTextAreaInput(FIELD_QUERY).setValue(query.getStatement()) ;
    }
  }

  public void setQuery(Query query) {
    if (query != null) {
      queryLanguage_ = query.getLanguage();
      queryStatement_ = query.getStatement();
    } else {
      queryLanguage_ = null;
      queryStatement_ = null;
    }
  }

  public void setIsEdit(boolean isEdit) { isEdit_ = isEdit ; }
  public boolean isEdit() { return isEdit_ ; }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}

  static  public class CancelActionListener extends EventListener<UIJCRAdvancedSearch> {
    public void execute(Event<UIJCRAdvancedSearch> event) throws Exception {
      UIJCRAdvancedSearch uiJAdvancedSearch = event.getSource() ;
      if(uiJAdvancedSearch.isEdit_) {
        UIPopupWindow uiPopup = uiJAdvancedSearch.getParent() ;
        uiPopup.setShow(false) ;  
        uiPopup.setRendered(false) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiJAdvancedSearch.getParent()) ;
      } else {
        uiJAdvancedSearch.getAncestorOfType(UIPopupContainer.class).deActivate() ;
      }
    }
  }

  static public class SearchActionListener extends EventListener<UIJCRAdvancedSearch> {
    public void execute(Event<UIJCRAdvancedSearch> event) throws Exception {
      UIJCRAdvancedSearch uiForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      StringBuffer queryS = new StringBuffer();
      queryS.append(uiForm.getUIFormTextAreaInput(FIELD_QUERY).getValue());
      String searchType = uiForm.getUIFormSelectBox(FIELD_SELECT_BOX).getValue() ;
      UIECMSearch uiSearch = uiForm.getParent() ;
      long startTime = System.currentTimeMillis();
      try {
        if(queryS.toString().toLowerCase().indexOf("order by") < 0) {
          if(searchType.equals("sql")) {
            queryS.append(" order by exo:dateCreated DESC");
          } else if(searchType.equals("xpath")) {
            queryS.append(" order by @exo:dateCreated descending");
          }
        }
        UISearchResult uiSearchResult = uiSearch.getChild(UISearchResult.class) ;
        uiSearchResult.setQuery(queryS.toString(), uiExplorer.getTargetSession().getWorkspace().getName(), searchType, 
                                IdentityConstants.SYSTEM.equals(uiExplorer.getTargetSession().getUserID()), null);
        uiSearchResult.updateGrid() ;
        long time = System.currentTimeMillis() - startTime;
        uiSearchResult.setSearchTime(time);
        uiSearch.setSelectedTab(UIECMSearch.ADVANCED_RESULT) ;
      } catch (Exception e){
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.invalid-queryStatement", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        return ;
      }
    }
  }

  static  public class ChangeOptionActionListener extends EventListener<UIJCRAdvancedSearch> {
    public void execute(Event<UIJCRAdvancedSearch> event) throws Exception {
      UIJCRAdvancedSearch uiForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      String  currentPath = uiExplorer.getCurrentNode().getPath() ;
      String queryText = "" ;
      String searchType = uiForm.getUIFormSelectBox(FIELD_SELECT_BOX).getValue() ;
      if(searchType.equals(Query.SQL)){
        if ("/".equals(currentPath)) queryText = ROOT_SQL_QUERY ;
        else queryText = StringUtils.replace(SQL_QUERY, "$0", currentPath) ;
        uiForm.getUIFormTextAreaInput(FIELD_QUERY).setValue(queryText) ;
      } else {
        if ("/".equals(currentPath)) queryText = ROOT_XPATH_QUERY ;
        else queryText = StringUtils.replace(XPATH_QUERY, "$0", currentPath) ;
        uiForm.getUIFormTextAreaInput(FIELD_QUERY).setValue(queryText) ;
      }
      if(uiForm.isEdit_ && uiForm.queryLanguage_ != null) {
        if(searchType.equals(uiForm.queryLanguage_)) {
          uiForm.getUIFormTextAreaInput(FIELD_QUERY).setValue(uiForm.queryStatement_) ;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
    }
  }

  static  public class SaveActionListener extends EventListener<UIJCRAdvancedSearch> {
    public void execute(Event<UIJCRAdvancedSearch> event) throws Exception {
      UIJCRAdvancedSearch uiForm = event.getSource() ;
      String statement = uiForm.getUIFormTextAreaInput(FIELD_QUERY).getValue() ;
      String queryLang = uiForm.getUIFormSelectBox(FIELD_SELECT_BOX).getValue() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;

      if(statement == null || statement.trim().length() ==0) {
        uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.value-save-null", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        return ;
      }

      if(!uiForm.isEdit_) {
        QueryService queryService = uiForm.getApplicationComponent(QueryService.class) ;
        String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
        if(name == null || name.trim().length() == 0) {
          uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.query-name-null", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        String[] arrFilterChar = { "&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", "'",
            "#", ";", "}", "{", "/", "|", "\\", "\"" };
        for(String filterChar : arrFilterChar) {
          if(name.indexOf(filterChar) > -1) {
            uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.name-invalid", null,
                                                    ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
            return ;
          }
        }
        String userName = Util.getPortalRequestContext().getRemoteUser() ;
        try {
          queryService.addQuery(name, statement, queryLang, userName) ;
        } catch(Exception e){
          uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.save_unSuccessful", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        UIECMSearch uiSearch = uiForm.getParent() ;
        uiSearch.getChild(UISavedQuery.class).updateGrid(1);
        uiForm.update(null) ;
        uiSearch.setSelectedTab(uiSearch.getChild(UISavedQuery.class).getId()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSearch) ;
      } else {
        UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
        QueryManager queryManager = uiExplorer.getTargetSession().getWorkspace().getQueryManager() ;
        try {
          queryManager.createQuery(statement, queryLang) ;
        } catch(Exception e) {
          uiApp.addMessage(new ApplicationMessage("UIJCRAdvancedSearch.msg.save_unSuccessful", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        ManageableRepository repository =
          uiForm.getApplicationComponent(RepositoryService.class).getRepository(uiExplorer.getRepositoryName()) ;
        Session session = repository.getSystemSession(repository.getConfiguration().getDefaultWorkspaceName()) ;
        Node queryNode = (Node) session.getItem(uiForm.queryPath_) ;
        queryNode.setProperty("jcr:language", queryLang) ;
        queryNode.setProperty("jcr:statement", statement) ;
        queryNode.save() ;
        session.save() ;
        session.logout() ;
        UISavedQuery uiSavedQuery = uiForm.getAncestorOfType(UISavedQuery.class) ;
        uiSavedQuery.updateGrid(1);
        uiSavedQuery.removeChildById(UISavedQuery.EDIT_FORM) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSavedQuery.getParent()) ;
      }
    }
  }
}
