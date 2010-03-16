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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 26, 2006  
 * 4:29:08 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UISimpleSearch.CancelActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UISimpleSearch.SearchActionListener.class),
      @EventConfig(listeners = UISimpleSearch.SaveActionListener.class),
      @EventConfig(listeners = UISimpleSearch.MoreConstraintsActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UISimpleSearch.RemoveConstraintActionListener.class, phase=Phase.DECODE)
    }    
)
public class UISimpleSearch extends UIForm {

  final static public String INPUT_SEARCH = "input";
  final static public String CONSTRAINTS = "constraints";
  final static public String NODE_PATH = "nodePath";
  final static public String FIRST_OPERATOR = "firstOperator";
  final static public String OR = "or".intern();
  final static public String AND = "and".intern();
  private static final Log LOG  = ExoLogger.getLogger("explorer.UISimpleSearch");
  private List<String> constraints_ = new ArrayList<String>();
  private List<String> virtualConstraints_ = new ArrayList<String>();
  private List<String> categoryPathList = new ArrayList<String>();
  
  public List<String> getCategoryPathList() { return categoryPathList; }
  public void setCategoryPathList(List<String> categoryPathListItem) {
    categoryPathList = categoryPathListItem; 
  }
  
  private static final String ROOT_XPATH_QUERY = "//*";
  private static final String XPATH_QUERY = "/jcr:root$0//*";
  private static final String ROOT_SQL_QUERY = "SELECT * FROM nt:base WHERE jcr:path LIKE '/%' ";
  private static final String SQL_QUERY = "SELECT * FROM nt:base WHERE jcr:path LIKE '$0/%' ";
  
  public UISimpleSearch() throws Exception {
    addUIFormInput(new UIFormInputInfo(NODE_PATH, NODE_PATH, null));
    addUIFormInput(new UIFormStringInput(INPUT_SEARCH, INPUT_SEARCH, null));
    List<SelectItemOption<String>> operators = new ArrayList<SelectItemOption<String>>();
    operators.add(new SelectItemOption<String>(AND, AND));
    operators.add(new SelectItemOption<String>(OR, OR));
    addUIFormInput(new UIFormSelectBox(FIRST_OPERATOR, FIRST_OPERATOR, operators));
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction("moreConstraints");
    uiInputAct.addUIFormInput(new UIFormInputInfo(CONSTRAINTS, CONSTRAINTS, null));
    addUIComponentInput(uiInputAct);
    setActions(new String[] {"MoreConstraints", "Search", "Save", "Cancel"});
  }
  
  public List<String> getConstraints() { return constraints_; }
  
  public void updateAdvanceConstraint(String constraint, String operator, String virtualDateQuery) { 
    if(constraint.length() > 0) {
      if(constraints_.size() == 0) {
        constraints_.add("(" + constraint + " )");
        if(virtualDateQuery != null) virtualConstraints_.add("(" + virtualDateQuery + " )");
        else virtualConstraints_.add("(" + constraint + " )");
      } else {
        constraints_.add(" "+operator.toLowerCase()+" (" + constraint + " ) ");
        if(virtualDateQuery != null) virtualConstraints_.add(" "+operator.toLowerCase()+" (" + virtualDateQuery + " ) ");
        else virtualConstraints_.add(" "+operator.toLowerCase()+" (" + constraint + " ) ");
      }
    }
    UIFormInputSetWithAction inputInfor = getChildById("moreConstraints");
    inputInfor.setIsDeleteOnly(true);
    inputInfor.setListInfoField(CONSTRAINTS, virtualConstraints_);
    String[] actionInfor = {"RemoveConstraint"};
    inputInfor.setActionInfo(CONSTRAINTS, actionInfor);
  }
  
  private String getQueryStatement() throws Exception {
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    StringBuilder statement = new StringBuilder(1024);
    String text = getUIStringInput(INPUT_SEARCH).getValue();
    if(text != null && constraints_.size() == 0) {
      if ("/".equals(currentNode.getPath())) {
        statement.append(ROOT_XPATH_QUERY);
      } else {
        statement.append(StringUtils.replace(XPATH_QUERY, "$0", currentNode.getPath()));
      }
      statement.append("[(jcr:contains(.,'").append(text.replaceAll("'", "''")).append("'))]");
    } else if(constraints_.size() > 0) {
      if(text == null) {
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_XPATH_QUERY).append("[(");
        } else {
          statement.append(StringUtils.replace(XPATH_QUERY, "$0", currentNode.getPath())).append("[(");
        } 
      } else {
        String operator = getUIFormSelectBox(FIRST_OPERATOR).getValue();
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_XPATH_QUERY);
        } else {
          statement.append(StringUtils.replace(XPATH_QUERY, "$0", currentNode.getPath()));
        } 
        statement.append("[(jcr:contains(.,'").append(text.replaceAll("'", "''")).append("')) ").append(operator).append(" (");
      }
      for(String constraint : constraints_) {
        statement.append(constraint);
      }
      statement.append(")]");
    }
    return statement.toString();
  }
  
  private String getSQLStatement() throws Exception {
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    StringBuilder statement = new StringBuilder(1024);
    String text = getUIStringInput(INPUT_SEARCH).getValue();
    if(text != null && constraints_.size() == 0) {
      if ("/".equals(currentNode.getPath())) {
        statement.append(ROOT_SQL_QUERY);
      } else {
        statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
      }
      statement.append("AND CONTAINS(*,'").append(text.replaceAll("'", "''")).append("')");
    } else if(constraints_.size() > 0) {
      if(text == null) {
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_SQL_QUERY);
        } else {
          statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
        } 
      } else {        
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_SQL_QUERY);
        } else {
          statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
        } 
        statement.append("AND CONTAINS(*,'").append(text.replaceAll("'", "''")).append("') ");
      }
      String operator = getUIFormSelectBox(FIRST_OPERATOR).getValue();
      statement.append(operator).append(" ");
      for(String constraint : constraints_) {
        statement.append(constraint);
      }
    }
    return statement.toString();
  }
  
  static  public class SaveActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISimpleSearch uiSimpleSearch = event.getSource();
      UIApplication uiApp = uiSimpleSearch.getAncestorOfType(UIApplication.class);
      String text = uiSimpleSearch.getUIStringInput(INPUT_SEARCH).getValue();
      if((text == null) && uiSimpleSearch.constraints_.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.value-save-null", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      UISearchContainer uiSearchContainer = uiSimpleSearch.getParent();
      uiSearchContainer.initSaveQueryPopup(uiSimpleSearch.getQueryStatement(), true, Query.XPATH);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer);
    }
  }
  
  static  public class CancelActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction();
    }
  }
  
  static  public class RemoveConstraintActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISimpleSearch uiSimpleSearch = event.getSource();
      int intIndex = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiSimpleSearch.constraints_.remove(intIndex);
      uiSimpleSearch.virtualConstraints_.remove(intIndex);
      if (uiSimpleSearch.categoryPathList.size() > intIndex) uiSimpleSearch.categoryPathList.remove(intIndex);
      if(uiSimpleSearch.constraints_.size() > 0 && intIndex == 0) {
        String newFirstConstraint = null;
        String newFirstVirtualConstraint = null;
        if(uiSimpleSearch.constraints_.get(0).trim().startsWith(OR)) {
          newFirstConstraint = uiSimpleSearch.constraints_.get(0).substring(3, uiSimpleSearch.constraints_.get(0).length());
          newFirstVirtualConstraint = uiSimpleSearch.virtualConstraints_.get(0).substring(3, uiSimpleSearch.virtualConstraints_.get(0).length());
          uiSimpleSearch.constraints_.set(0, newFirstConstraint);
          uiSimpleSearch.virtualConstraints_.set(0, newFirstVirtualConstraint);
        } else if(uiSimpleSearch.constraints_.get(0).trim().startsWith(AND)) {
          newFirstConstraint = uiSimpleSearch.constraints_.get(0).substring(4, uiSimpleSearch.constraints_.get(0).length());
          newFirstVirtualConstraint = uiSimpleSearch.virtualConstraints_.get(0).substring(4, uiSimpleSearch.virtualConstraints_.get(0).length());
          uiSimpleSearch.constraints_.set(0, newFirstConstraint);
          uiSimpleSearch.virtualConstraints_.set(0, newFirstVirtualConstraint);
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch.getParent());
    }
  }
  
  static public class SearchActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISimpleSearch uiSimpleSearch = event.getSource();
      String text = uiSimpleSearch.getUIStringInput(INPUT_SEARCH).getValue();
      UIJCRExplorer uiExplorer = uiSimpleSearch.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      QueryManager queryManager = currentNode.getSession().getWorkspace().getQueryManager();
      UIECMSearch uiECMSearch = uiSimpleSearch.getAncestorOfType(UIECMSearch.class); 
      UISearchResult uiSearchResult = uiECMSearch.getChild(UISearchResult.class);
      UIApplication uiApp = uiSimpleSearch.getAncestorOfType(UIApplication.class);
      if(text == null && uiSimpleSearch.constraints_.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.value-null", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      uiSearchResult.setCategoryPathList(uiSimpleSearch.getCategoryPathList());
      
      //TODO need review this code. should use validator for text field
      String[] arrFilterChar = {"&", "$", "@", ":","]", "[", "*", "%", "!"};
      if(text != null) {
        for(String filterChar : arrFilterChar) {
          if(text.indexOf(filterChar) > -1) {
            uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.inputSearch-invalid", null, 
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            return;
          }
        }
      }
      Preference pref = uiExplorer.getPreference();
      String queryType = pref.getQueryType();
      String statement;
      List<String> searchCategoryPathList = uiSimpleSearch.getCategoryPathList();
      if (queryType.equals(Preference.XPATH_QUERY)) {
        statement = uiSimpleSearch.getQueryStatement() + " order by @exo:dateCreated descending";
        if ((searchCategoryPathList != null) && (searchCategoryPathList.size() > 0)) {
          for (String searchCategoryPath : searchCategoryPathList) {
            String statementReplace = statement.replaceAll("@exo:category = '" + searchCategoryPath + "'", 
                "@jcr:mixinTypes = 'mix:referenceable'");
            statement = statementReplace;
          }
        }
      } else {
        statement = uiSimpleSearch.getSQLStatement() + " order by exo:dateCreated DESC";
        if ((searchCategoryPathList != null) && (searchCategoryPathList.size() > 0)) {
          for (String searchCategoryPath : searchCategoryPathList) {
            String statementReplace = statement.replaceAll("exo:category = '" + searchCategoryPath + "'", 
                "jcr:mixinTypes = 'mix:referenceable'");
            statement = statementReplace;
          }
        }
      }        
      long startTime = System.currentTimeMillis();
      try {
        Query query;
        if (queryType.equals(Preference.XPATH_QUERY))
          query = queryManager.createQuery(statement, Query.XPATH);
        else
          query = queryManager.createQuery(statement, Query.SQL);
        QueryResult queryResult = query.execute();
        uiSearchResult.clearAll();
        uiSearchResult.setQueryResults(queryResult);
        uiSearchResult.updateGrid(true);
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.query-invalid", null, 
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      long time = System.currentTimeMillis() - startTime;
      uiSearchResult.setSearchTime(time);
      uiECMSearch.setRenderedChild(UISearchResult.class);
      uiSimpleSearch.getUIFormInputInfo(UISimpleSearch.NODE_PATH).setValue(currentNode.getPath());
    }
  }
  
  static  public class MoreConstraintsActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getParent();
      UIConstraintsForm uiConstraintsForm = uiSearchContainer.getChild(UIConstraintsForm.class);
      if(uiConstraintsForm.isRendered()) uiConstraintsForm.setRendered(false);
      else uiConstraintsForm.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer);
    }
  }
}