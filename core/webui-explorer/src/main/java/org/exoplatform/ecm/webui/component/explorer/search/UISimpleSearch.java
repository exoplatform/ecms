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
import java.util.Calendar;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
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
    template =  "app:/groovy/webui/component/explorer/search/UISimpleSearch.gtmpl",
    events = {
      @EventConfig(listeners = UISimpleSearch.CancelActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UISimpleSearch.SearchActionListener.class),
      @EventConfig(listeners = UISimpleSearch.SaveActionListener.class),
      @EventConfig(listeners = UISimpleSearch.MoreConstraintsActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UISimpleSearch.RemoveConstraintActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UISimpleSearch.AddActionListener.class),
      @EventConfig(listeners = UISimpleSearch.CompareExactlyActionListener.class),
      @EventConfig(listeners = UISimpleSearch.AddMetadataTypeActionListener.class),
      @EventConfig(listeners = UISimpleSearch.AddNodeTypeActionListener.class),
      @EventConfig(listeners = UISimpleSearch.AddCategoryActionListener.class)
    }
)
public class UISimpleSearch extends UIForm {

  public static final String CONSTRAINTS_FORM = "ConstraintsForm";
  public static final String INPUT_SEARCH = "input";
  public static final String CONSTRAINTS = "constraints";
  public static final String NODE_PATH = "nodePath";
  public static final String FIRST_OPERATOR = "firstOperator";
  public static final String OR = "or";
  public static final String AND = "and";

  private static final Log LOG  = ExoLogger.getLogger(UISimpleSearch.class.getName());
  private List<String> constraints_ = new ArrayList<String>();
  private List<String> virtualConstraints_ = new ArrayList<String>();
  private List<String> categoryPathList = new ArrayList<String>();

  public List<String> getCategoryPathList() { return categoryPathList; }
  public void setCategoryPathList(List<String> categoryPathListItem) {
    categoryPathList = categoryPathListItem;
  }

  private static final String ROOT_XPATH_QUERY = "//*";

  private static final String XPATH_QUERY      = "/jcr:root$0//*";

  private static final String ROOT_SQL_QUERY   = "SELECT * FROM nt:base WHERE jcr:path LIKE '/%' ";

  private static final String LINK_REQUIREMENT = "AND ( (jcr:primaryType like 'exo:symlink' or " +
                                                        "jcr:primaryType like 'exo:taxonomyLink') ";

  private static final String SQL_QUERY        = "SELECT * FROM nt:base WHERE jcr:path LIKE '$0/%' ";

  private String              _OR  = "Or";

  private String              _AND = "And";

  public UISimpleSearch() throws Exception {
    addUIFormInput(new UIFormInputInfo(NODE_PATH, NODE_PATH, null));
    addUIFormInput(new UIFormStringInput(INPUT_SEARCH, INPUT_SEARCH, null));
    List<SelectItemOption<String>> operators = new ArrayList<SelectItemOption<String>>();
    RequestContext context = RequestContext.getCurrentInstance();
    try {
    ResourceBundle res = context.getApplicationResourceBundle();
      _AND = res.getString("UIConstraintForm.label.and");
      _OR = res.getString("UIConstraintForm.label.or");
    }catch (MissingResourceException e) {
      // There is no resource found, just use the default resource-bundle in English as first value of _OR & _AND
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not get resource bundle for UISimpleSearch label: " + e.getMessage());
      }
    }
    operators.add(new SelectItemOption<String>(_AND, AND));
    operators.add(new SelectItemOption<String>(_OR, OR));
    addUIFormInput(new UIFormSelectBox(FIRST_OPERATOR, FIRST_OPERATOR, operators));
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction("moreConstraints");
    uiInputAct.addUIFormInput(new UIFormInputInfo(CONSTRAINTS, CONSTRAINTS, null));
    addUIComponentInput(uiInputAct);
    UIConstraintsForm uiConstraintsForm = new UIConstraintsForm(CONSTRAINTS_FORM);
    uiConstraintsForm.setRendered(false);
    addChild(uiConstraintsForm);

    setActions(new String[] {"MoreConstraints", "Search", "Save", "Cancel"});
  }

  public List<String> getConstraints() { return constraints_; }

  public void updateAdvanceConstraint(String constraint, String operator, String virtualDateQuery) {
    if (constraint.length() > 0) {
      if (constraints_.size() == 0) {
        constraints_.add("(" + constraint + " )");
        if (virtualDateQuery != null)
          virtualConstraints_.add("(" + virtualDateQuery + " )");
        else
          virtualConstraints_.add("(" + constraint + " )");
      } else {
        constraints_.add(" " + operator.toLowerCase() + " (" + constraint + " ) ");
        if (virtualDateQuery != null)
          virtualConstraints_.add(" " + operator.toLowerCase() + " (" + virtualDateQuery + " ) ");
        else
          virtualConstraints_.add(" " + operator.toLowerCase() + " (" + constraint + " ) ");
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
    String escapedText = org.exoplatform.services.cms.impl.Utils.escapeIllegalCharacterInQuery(text);
    if(text != null && constraints_.size() == 0) {
      if ("/".equals(currentNode.getPath())) {
        statement.append(ROOT_XPATH_QUERY);
      } else {
        statement.append(StringUtils.replace(XPATH_QUERY, "$0", currentNode.getPath()));
      }
      statement.append("[(jcr:contains(.,'").append(escapedText.replaceAll("'", "''")).append("'))]");
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
        statement.append("[(jcr:contains(.,'")
                 .append(escapedText.replaceAll("'", "''"))
                 .append("')) ")
                 .append(operator)
                 .append(" (");
      }
      for(String constraint : constraints_) {
        if (!constraint.contains("exo:category"))
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
    String escapedText = org.exoplatform.services.cms.impl.Utils.escapeIllegalCharacterInQuery(text);
    if(text != null && constraints_.size() == 0) {//no constraint
      if ("/".equals(currentNode.getPath())) {
        statement.append(ROOT_SQL_QUERY);
      } else {
        statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
      }
      statement.append(LINK_REQUIREMENT).append(" OR ( CONTAINS(*,'").
                append(escapedText.replaceAll("'", "''")).append("') ) )");
    } else if(constraints_.size() > 0) {//constraint != null
      //get constraint statement
      StringBuilder tmpStatement = new StringBuilder();
      String operator = getUIFormSelectBox(FIRST_OPERATOR).getValue();
      for(String constraint : constraints_) {
        if (!constraint.contains("exo:category")) {
          tmpStatement.append(constraint);
        }
      }
      String str = tmpStatement.toString().trim();
      if (str.toLowerCase().startsWith("or")) str = str.substring("or".length());
      if (str.toLowerCase().startsWith("and")) str = str.substring("and".length());
      String constraintsStatement = str.length() == 0 ? str : operator + " ( " + str + " ) ";
      //get statement
      if(text == null) {
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_SQL_QUERY);
        } else {
          statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
        }
        if (constraintsStatement.length() > 0)
          statement.append(constraintsStatement);
      } else {
        if ("/".equals(currentNode.getPath())) {
          statement.append(ROOT_SQL_QUERY);
        } else {
          statement.append(StringUtils.replace(SQL_QUERY, "$0", currentNode.getPath()));
        }
        statement.append(LINK_REQUIREMENT).append("OR ( CONTAINS(*,'").
                  append(escapedText.replaceAll("'", "''")).append("') ");
        if (constraintsStatement.length() > 0)
          statement.append(constraintsStatement);
        statement.append(" ) )");
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
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.value-save-null", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch);
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
          newFirstVirtualConstraint = uiSimpleSearch.virtualConstraints_.get(0)
                                                                        .substring(3,
                                                                                   uiSimpleSearch.virtualConstraints_.get(0)
                                                                                                                     .length());
          uiSimpleSearch.constraints_.set(0, newFirstConstraint);
          uiSimpleSearch.virtualConstraints_.set(0, newFirstVirtualConstraint);
        } else if(uiSimpleSearch.constraints_.get(0).trim().startsWith(AND)) {
          newFirstConstraint = uiSimpleSearch.constraints_.get(0).substring(4, uiSimpleSearch.constraints_.get(0).length());
          newFirstVirtualConstraint = uiSimpleSearch.virtualConstraints_.get(0)
                                                                        .substring(4,
                                                                                   uiSimpleSearch.virtualConstraints_.get(0)
                                                                                                                     .length());
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
      UIECMSearch uiECMSearch = uiSimpleSearch.getAncestorOfType(UIECMSearch.class);
      UISearchResult uiSearchResult = uiECMSearch.getChild(UISearchResult.class);
      UIApplication uiApp = uiSimpleSearch.getAncestorOfType(UIApplication.class);
      if(text == null && uiSimpleSearch.constraints_.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.value-null", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch);
        return;
      }
      uiSearchResult.setCategoryPathList(uiSimpleSearch.getCategoryPathList());

      Preference pref = uiExplorer.getPreference();
      String queryType = pref.getQueryType();
      String statement;
      //List<String> searchCategoryPathList = uiSimpleSearch.getCategoryPathList();
      if (queryType.equals(Preference.XPATH_QUERY)) {
        statement = uiSimpleSearch.getQueryStatement() + " order by @exo:dateCreated descending";
      } else {
        statement = uiSimpleSearch.getSQLStatement() + " order by exo:dateCreated DESC";
      }
      long startTime = System.currentTimeMillis();
      try {
        uiSearchResult.setQuery(statement, currentNode.getSession().getWorkspace().getName(),
                                queryType.equals(Preference.XPATH_QUERY) ? Query.XPATH : Query.SQL,
                                IdentityConstants.SYSTEM.equals(currentNode.getSession().getUserID()), text);
        uiSearchResult.updateGrid();
      } catch (RepositoryException reEx) {
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.inputSearch-invalid", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch);
        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        uiApp.addMessage(new ApplicationMessage("UISimpleSearch.msg.query-invalid", null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch);
        return;
      }
      long time = System.currentTimeMillis() - startTime;
      uiSearchResult.setSearchTime(time);
      uiECMSearch.setSelectedTab(uiSearchResult.getId());
      uiSimpleSearch.getUIFormInputInfo(UISimpleSearch.NODE_PATH).setValue(currentNode.getPath());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiECMSearch);
    }
  }

  static  public class MoreConstraintsActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISimpleSearch uiSimpleSearch = event.getSource();
      UIConstraintsForm uiConstraintsForm = uiSimpleSearch.getChild(UIConstraintsForm.class);
      if(uiConstraintsForm.isRendered()) uiConstraintsForm.setRendered(false);
      else uiConstraintsForm.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSimpleSearch);
    }
  }

  static public class AddActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UIConstraintsForm uiForm = event.getSource().getChild(UIConstraintsForm.class);
      boolean isExactly = uiForm.getUICheckBoxInput(UIConstraintsForm.EXACTLY_PROPERTY).isChecked() ;
      boolean isContain = uiForm.getUICheckBoxInput(UIConstraintsForm.CONTAIN_PROPERTY).isChecked() ;
      boolean isNotContain = uiForm.getUICheckBoxInput(UIConstraintsForm.NOT_CONTAIN_PROPERTY).isChecked() ;
      boolean isDateTime = uiForm.getUICheckBoxInput(UIConstraintsForm.DATE_PROPERTY).isChecked() ;
      boolean isNodeType = uiForm.getUICheckBoxInput(UIConstraintsForm.NODETYPE_PROPERTY).isChecked() ;
      boolean isCategory = uiForm.getUICheckBoxInput(UIConstraintsForm.CATEGORY_PROPERTY).isChecked() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      if (!isExactly && !isContain && !isNotContain && !isDateTime && !isNodeType && !isCategory) {
        uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.must-choose-one",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        return;
      }
      if (isExactly) {
        String property = uiForm.getUIStringInput(UIConstraintsForm.PROPERTY1).getValue();
        if (property == null || property.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-required",
                                                  null,
                                                  ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return;
        }
        String value = uiForm.getUIStringInput(UIConstraintsForm.CONTAIN_EXACTLY).getValue() ;
        if (value == null || value.trim().length() < 0) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.exactly-require",
                                                  null,
                                                  ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return;
        }
        uiForm.addConstraint(0) ;
      }
      if(isContain) {
        String property = uiForm.getUIStringInput(UIConstraintsForm.PROPERTY2).getValue() ;
        if(property == null || property.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-required", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        String value = uiForm.getUIStringInput(UIConstraintsForm.CONTAIN).getValue() ;
        if(value == null || value.trim().length() < 0) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.value-required", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        uiForm.addConstraint(1) ;
      }
      if(isNotContain) {
        String property = uiForm.getUIStringInput(UIConstraintsForm.PROPERTY3).getValue() ;
        if(property == null || property.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-required", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        String value = uiForm.getUIStringInput(UIConstraintsForm.NOT_CONTAIN).getValue() ;
        if(value == null || value.trim().length() < 0) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.value-required", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        uiForm.addConstraint(2) ;
      }
      if(isDateTime) {
        String fromDate = uiForm.getUIFormDateTimeInput(UIConstraintsForm.START_TIME).getValue() ;
        String toDate = uiForm.getUIFormDateTimeInput(UIConstraintsForm.END_TIME).getValue() ;
        if(fromDate == null || fromDate.trim().length() == 0) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.fromDate-required", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        if(!fromDate.matches(UIConstraintsForm.DATETIME_REGEX) || !uiForm.isValidDateTime(fromDate)) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.fromDate-invalid", null,
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        Calendar bfDate = uiForm.getUIFormDateTimeInput(UIConstraintsForm.START_TIME).getCalendar() ;
        if(toDate != null && toDate.trim().length() >0) {
          Calendar afDate = uiForm.getUIFormDateTimeInput(UIConstraintsForm.END_TIME).getCalendar();
          if(!toDate.matches(UIConstraintsForm.DATETIME_REGEX) || !uiForm.isValidDateTime(toDate)) {
            uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.toDate-invalid", null,
                                                    ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
            return ;
          }
          if(bfDate.compareTo(afDate) == 1) {
            uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.date-invalid", null,
                                                    ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
            return ;
          }
        }
        uiForm.addConstraint(3);
      }
      if(isNodeType) {
        String property = uiForm.getUIStringInput(UIConstraintsForm.DOC_TYPE).getValue() ;
        if(property == null || property.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-required", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return ;
        }
        uiForm.addConstraint(4) ;
      }
      if (isCategory) {
        String category = uiForm.getUIStringInput(UIConstraintsForm.CATEGORY_TYPE).getValue();
        if (category == null || category.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-required",
              null, ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
          return;
        }
        uiForm.addConstraint(5);
      }

      uiForm.resetConstraintForm() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }

  static public class AddMetadataTypeActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISearchContainer uiContainer = event.getSource().getAncestorOfType(UISearchContainer.class);
      String type = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String popupId = UIConstraintsForm.PROPERTY1;
      if(type.equals("1")) popupId = UIConstraintsForm.PROPERTY2 ;
      else if(type.equals("2")) popupId = UIConstraintsForm.PROPERTY3 ;
      uiContainer.initMetadataPopup(popupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }

  static public class AddNodeTypeActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISearchContainer uiContainer = event.getSource().getAncestorOfType(UISearchContainer.class);
      uiContainer.initNodeTypePopup() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }

  static public class CompareExactlyActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UIConstraintsForm uiConstraintForm = event.getSource().getChild(UIConstraintsForm.class);
      String property = uiConstraintForm.getUIStringInput(UIConstraintsForm.PROPERTY1).getValue() ;
      UIJCRExplorer uiExplorer = uiConstraintForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiConstraintForm.getAncestorOfType(UIApplication.class) ;
      if(property == null || property.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIConstraintsForm.msg.properties-null", null,
                                                 ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiConstraintForm);
        return ;
      }
      String currentPath = uiExplorer.getCurrentNode().getPath() ;
      StringBuffer statement = new StringBuffer("select * from nt:base where ");
      if (!currentPath.equals("/")) {
        statement.append("jcr:path like '").append(currentPath).append("/%' AND ");
      }
      statement.append(property).append(" is not null");
      QueryManager queryManager = uiExplorer.getTargetSession().getWorkspace().getQueryManager() ;
      Query query = queryManager.createQuery(statement.toString(), Query.SQL) ;
      QueryResult result = query.execute() ;
      if(result == null || result.getNodes().getSize() == 0) {
        uiApp.addMessage(new ApplicationMessage("UICompareExactlyForm.msg.not-result-found", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiConstraintForm);
        return ;
      }
      UISearchContainer uiContainer = uiConstraintForm.getAncestorOfType(UISearchContainer.class);
      UICompareExactlyForm uiCompareExactlyForm =
        uiContainer.createUIComponent(UICompareExactlyForm.class, null, null) ;
      UIPopupContainer uiPopup = uiContainer.getChild(UIPopupContainer.class);
      uiPopup.getChild(UIPopupWindow.class).setId("ExactlyFormPopup") ;
      uiPopup.getChild(UIPopupWindow.class).setShowMask(true);
      uiCompareExactlyForm.init(property, result) ;
      uiPopup.activate(uiCompareExactlyForm, 600, 500) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }

  static public class AddCategoryActionListener extends EventListener<UISimpleSearch> {
    public void execute(Event<UISimpleSearch> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class);
      uiSearchContainer.initCategoryPopup();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer) ;
    }
  }
}
