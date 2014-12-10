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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;

@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/search/UIConstraintsForm.gtmpl"
)
public class UIConstraintsForm extends UIFormInputSetWithAction implements UISelectable{

  final static public String OPERATOR = "operator" ;
  final static public String TIME_OPTION = "timeOpt" ;
  final static public String PROPERTY1 = "property1" ;
  final static public String PROPERTY2 = "property2" ;
  final static public String PROPERTY3 = "property3" ;
  final static public String CONTAIN_EXACTLY = "containExactly" ;
  final static public String CONTAIN = "contain" ;
  final static public String NOT_CONTAIN = "notContain" ;
  final static public String START_TIME = "startTime" ;
  final static public String END_TIME = "endTime" ;
  final static public String DOC_TYPE = "docType" ;
  final static public String CATEGORY_TYPE = "categoryType" ;
  final static public String AND_OPERATION = "and" ;
  final static public String OR_OPERATION = "or" ;
  final static public String CREATED_DATE = "CREATED" ;
  final static public String MODIFIED_DATE = "MODIFIED" ;
  final static public String EXACTLY_PROPERTY = "exactlyPro" ;
  final static public String CONTAIN_PROPERTY = "containPro" ;
  final static public String NOT_CONTAIN_PROPERTY = "notContainPro" ;
  final static public String DATE_PROPERTY = "datePro" ;
  final static public String NODETYPE_PROPERTY = "nodetypePro" ;
  final static public String CATEGORY_PROPERTY = "categoryPro" ;
  final static public String SPLIT_REGEX = "/|\\s+|:" ;
  final static public String DATETIME_REGEX =
    "^(\\d{1,2}\\/\\d{1,2}\\/\\d{1,4})\\s*(\\s+\\d{1,2}:\\d{1,2}:\\d{1,2})?$" ;

  private String              virtualDateQuery_;

  private String              _CREATED_DATE;

  private String              _MODIFIED_DATE;

  private String              _AND_OPERATION;

  private String              _OR_OPERATION;

  public UIConstraintsForm(String name) throws Exception {
    super(name);
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    _AND_OPERATION = res.getString("UIConstraintForm.label.and");
    _OR_OPERATION = res.getString("UIConstraintForm.label.or");
    _CREATED_DATE = res.getString("UIConstraintForm.label.created");
    _MODIFIED_DATE = res.getString("UIConstraintForm.label.modified");
    setActions(new String[] {"Add", "Cancel"}, null) ;
    List<SelectItemOption<String>> typeOperation = new ArrayList<SelectItemOption<String>>() ;
    typeOperation.add(new SelectItemOption<String>(_AND_OPERATION, AND_OPERATION));
    typeOperation.add(new SelectItemOption<String>(_OR_OPERATION, OR_OPERATION));
    addUIFormInput(new UIFormSelectBox(OPERATOR, OPERATOR, typeOperation)) ;

    addUIFormInput(new UICheckBoxInput(EXACTLY_PROPERTY, EXACTLY_PROPERTY, null)) ;
    addUIFormInput(new UIFormStringInput(PROPERTY1, PROPERTY1, null)) ;
    addUIFormInput(new UIFormStringInput(CONTAIN_EXACTLY, CONTAIN_EXACTLY, null)) ;

    addUIFormInput(new UICheckBoxInput(CONTAIN_PROPERTY, CONTAIN_PROPERTY, null)) ;
    addUIFormInput(new UIFormStringInput(PROPERTY2, PROPERTY2, null)) ;
    addUIFormInput(new UIFormStringInput(CONTAIN, CONTAIN, null)) ;

    addUIFormInput(new UICheckBoxInput(NOT_CONTAIN_PROPERTY, NOT_CONTAIN_PROPERTY, null)) ;
    addUIFormInput(new UIFormStringInput(PROPERTY3, PROPERTY3, null)) ;
    addUIFormInput(new UIFormStringInput(NOT_CONTAIN, NOT_CONTAIN, null)) ;


    addUIFormInput(new UICheckBoxInput(DATE_PROPERTY, DATE_PROPERTY, null)) ;
    List<SelectItemOption<String>> dateOperation = new ArrayList<SelectItemOption<String>>() ;
    dateOperation.add(new SelectItemOption<String>(_CREATED_DATE, CREATED_DATE));
    dateOperation.add(new SelectItemOption<String>(_MODIFIED_DATE, MODIFIED_DATE));
    addUIFormInput(new UIFormSelectBox(TIME_OPTION, TIME_OPTION, dateOperation)) ;
    UIFormDateTimeInput uiFromDate = new UIFormDateTimeInput(START_TIME, START_TIME, null) ;
    uiFromDate.addValidator(DateTimeValidator.class);
    uiFromDate.setDisplayTime(true) ;
    addUIFormInput(uiFromDate) ;
    UIFormDateTimeInput uiToDate = new UIFormDateTimeInput(END_TIME, END_TIME, null) ;
    uiToDate.addValidator(DateTimeValidator.class);
    uiToDate.setDisplayTime(true) ;
    addUIFormInput(uiToDate) ;
    addUIFormInput(new UICheckBoxInput(NODETYPE_PROPERTY, NODETYPE_PROPERTY, null)) ;
    addUIFormInput(new UIFormStringInput(DOC_TYPE, DOC_TYPE, null)) ;
    addUIFormInput(new UICheckBoxInput(CATEGORY_PROPERTY, CATEGORY_PROPERTY, null)) ;
    addUIFormInput(new UIFormStringInput(CATEGORY_TYPE, CATEGORY_TYPE, null)) ;
  }

  private String getContainQueryString(String property, String type, boolean isContain) {
    String value = getUIStringInput(type).getValue() ;
    if(value == null) return "" ;
    if(value.trim().length() > 0) {
      if(isContain) return " jcr:contains(@" + property.trim() + ", '"+ value.trim() + "')" ;
      return " fn:not(jcr:contains(@" + property.trim() + ", '"+ value.trim() + "'))" ;
    }
    return "" ;
  }

  private String getContainSQLQueryString(String property, String type, boolean isContain) {
    String value = getUIStringInput(type).getValue();
    if(value == null) return "";
    if(value.trim().length() > 0) {
      if(isContain) return " CONTAINS(" + property.trim() + ", '"+ value.trim() + "')";
      return " NOT CONTAINS(" + property.trim() + ", '"+ value.trim() + "')";
    }
    return "";
  }

  private String getDateTimeQueryString(String beforeDate, String afterDate, String type) {
    Calendar bfDate = getUIFormDateTimeInput(START_TIME).getCalendar() ;
    if(bfDate==null) return "";
    if (afterDate != null && afterDate.trim().length() > 0) {
      Calendar afDate = getUIFormDateTimeInput(END_TIME).getCalendar();
      if (type.equals(CREATED_DATE)) {
        virtualDateQuery_ = "(documents created from '" + beforeDate
            + "') and (documents created to '" + afterDate + "')";
        return "@exo:dateCreated >= xs:dateTime('" + ISO8601.format(bfDate)
            + "') and @exo:dateCreated <= xs:dateTime('" + ISO8601.format(afDate) + "')";
      } else if (type.equals(MODIFIED_DATE)) {
        virtualDateQuery_ = "(documents modified from '" + beforeDate
            + "') and (documents modified to '" + afterDate + "')";
        return "@exo:dateModified >= xs:dateTime('" + ISO8601.format(bfDate)
            + "') and @exo:dateModified <= xs:dateTime('" + ISO8601.format(afDate) + "')";
      }
    } else {
      if(type.equals(CREATED_DATE)) {
        virtualDateQuery_ = "(documents created from '"+beforeDate+"')" ;
        return "@exo:dateCreated >= xs:dateTime('"+ISO8601.format(bfDate)+"')" ;
      } else if(type.equals(MODIFIED_DATE)) {
        virtualDateQuery_ = "(documents modified from '"+beforeDate+"')" ;
        return "@exo:dateModified >= xs:dateTime('"+ISO8601.format(bfDate)+"')" ;
      }
    }
    return "" ;
  }

  private String getDateTimeSQLQueryString(String beforeDate, String afterDate, String type) {
    Calendar bfDate = getUIFormDateTimeInput(START_TIME).getCalendar();
    if(bfDate==null) return "";
    if (afterDate != null && afterDate.trim().length() > 0) {
      Calendar afDate = getUIFormDateTimeInput(END_TIME).getCalendar();
      if (type.equals(CREATED_DATE)) {
        virtualDateQuery_ = "(documents created from '" + beforeDate
            + "') and (documents created to '" + afterDate + "')";
        return "exo:dateCreated >= TIMESTAMP '" + ISO8601.format(bfDate)
            + "' and exo:dateCreated <= TIMESTAMP '" + ISO8601.format(afDate) + "'";
      } else if (type.equals(MODIFIED_DATE)) {
        virtualDateQuery_ = "(documents modified from '" + beforeDate
            + "') and (documents modified to '" + afterDate + "')";
        return "exo:dateModified >= TIMESTAMP '" + ISO8601.format(bfDate)
            + "' and exo:dateModified <= TIMESTAMP '" + ISO8601.format(afDate) + "'";
      }
    } else {
      if(type.equals(CREATED_DATE)) {
        virtualDateQuery_ = "(documents created from '"+beforeDate+"')";
        return "exo:dateCreated >= TIMESTAMP '"+ISO8601.format(bfDate)+"'";
      } else if(type.equals(MODIFIED_DATE)) {
        virtualDateQuery_ = "(documents modified from '"+beforeDate+"')";
        return "exo:dateModified >= TIMESTAMP '"+ISO8601.format(bfDate)+"'";
      }
    }
    return "" ;
  }

  private String getNodeTypeQueryString(String nodeTypes) {
    StringBuffer advanceQuery = new StringBuffer();
    String[] arrNodeTypes = {};
    if (nodeTypes.indexOf(",") > -1)
      arrNodeTypes = nodeTypes.split(",");
    if (arrNodeTypes.length > 0) {
      for (String nodeType : arrNodeTypes) {
        if (advanceQuery.length() == 0)
          advanceQuery.append("@jcr:primaryType = '").append(nodeType).append("'");
        else
          advanceQuery.append(" ")
                      .append(OR_OPERATION)
                      .append(" ")
                      .append("@jcr:primaryType = '")
                      .append(nodeType)
                      .append("'");
      }
    } else {
      advanceQuery.append("@jcr:primaryType = '").append(nodeTypes).append("'");
    }
    return advanceQuery.toString();
  }

  private String getNodeTypeSQLQueryString(String nodeTypes) {
    StringBuffer advanceQuery = new StringBuffer();
    String[] arrNodeTypes = {};
    if (nodeTypes.indexOf(",") > -1)
      arrNodeTypes = nodeTypes.split(",");
    if (arrNodeTypes.length > 0) {
      for (String nodeType : arrNodeTypes) {
        if (advanceQuery.length() == 0)
          advanceQuery.append("jcr:primaryType = '").append(nodeType).append("'");
        else
          advanceQuery.append(" ")
                      .append(OR_OPERATION)
                      .append(" ")
                      .append("jcr:primaryType = '")
                      .append(nodeType)
                      .append("'");
      }
    } else {
      advanceQuery.append("jcr:primaryType = '").append(nodeTypes).append("'");
    }
    return advanceQuery.toString();
  }

  /**
   * Create query string for category
   * @param categoryPath
   * @return
   */
  private String getCategoryQueryString(String categoryPath) {
    if (categoryPath == null || categoryPath.length() == 0) return "";
    return ("@exo:category = '" + categoryPath + "'");
  }

  private String getCategorySQLQueryString(String categoryPath) {
    if (categoryPath == null || categoryPath.length() == 0) return "";
    return ("exo:category = '" + categoryPath + "'");
  }

  void addConstraint(int opt) throws Exception {
    String advanceQuery = "" ;
    String property ;
    virtualDateQuery_ = null ;
    UISimpleSearch uiSimpleSearch = this.getParent();
    UIJCRExplorer uiExplorer = uiSimpleSearch.getAncestorOfType(UIJCRExplorer.class);
    Preference pref = uiExplorer.getPreference();
    String queryType = pref.getQueryType();
    switch (opt) {
      case 0:
        property = getUIStringInput(PROPERTY1).getValue() ;
        String value = getUIStringInput(CONTAIN_EXACTLY).getValue() ;
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = "@" + property + " = '" + value.trim() + "'" ;
        else
          advanceQuery = " CONTAINS(" + property + ", '" + value.trim() + "')" ;
        break;
      case 1:
        property = getUIStringInput(PROPERTY2).getValue() ;
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = getContainQueryString(property, CONTAIN, true);
        else
          advanceQuery = getContainSQLQueryString(property, CONTAIN, true);
        break;
      case 2:
        property = getUIStringInput(PROPERTY3).getValue();
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = getContainQueryString(property, NOT_CONTAIN, false);
        else
          advanceQuery = getContainSQLQueryString(property, NOT_CONTAIN, false);
        break;
      case 3:
        String fromDate = getUIFormDateTimeInput(START_TIME).getValue() ;
        String toDate = getUIFormDateTimeInput(END_TIME).getValue() ;
        String type = getUIFormSelectBox(TIME_OPTION).getValue() ;
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = getDateTimeQueryString(fromDate, toDate, type);
        else
          advanceQuery = getDateTimeSQLQueryString(fromDate, toDate, type);
        break ;
      case 4:
        property = getUIStringInput(DOC_TYPE).getValue();
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = getNodeTypeQueryString(property);
        else
          advanceQuery = getNodeTypeSQLQueryString(property);
        break;
      case 5:
        property = getUIStringInput(CATEGORY_TYPE).getValue();
        if (queryType.equals(Preference.XPATH_QUERY))
          advanceQuery = getCategoryQueryString(property);
        else
          advanceQuery = getCategorySQLQueryString(property);
        String firstOperator = uiSimpleSearch.getUIStringInput(UISimpleSearch.FIRST_OPERATOR).getValue();
        if (!uiSimpleSearch.getCategoryPathList().contains(property) && firstOperator.equals("and"))
          uiSimpleSearch.getCategoryPathList().add(property);
        break;
      default:
        break;
    }
    uiSimpleSearch.updateAdvanceConstraint(advanceQuery,
                                           getUIFormSelectBox(OPERATOR).getValue(),
                                           virtualDateQuery_);
  }

  void resetConstraintForm() {
    reset();
    getUICheckBoxInput(EXACTLY_PROPERTY).setChecked(false);
    getUICheckBoxInput(CONTAIN_PROPERTY).setChecked(false);
    getUICheckBoxInput(NOT_CONTAIN_PROPERTY).setChecked(false);
    getUICheckBoxInput(DATE_PROPERTY).setChecked(false);
    getUICheckBoxInput(NODETYPE_PROPERTY).setChecked(false);
    getUICheckBoxInput(CATEGORY_PROPERTY).setChecked(false);
  }

  boolean isValidDateTime(String dateTime) {
    String[] arr = dateTime.split(SPLIT_REGEX, 7) ;
    int valid = Integer.parseInt(arr[0]) ;
    if(valid < 1 || valid > 12) return false;
    Calendar date = new GregorianCalendar(Integer.parseInt(arr[2]), valid - 1, 1) ;
    if(Integer.parseInt(arr[1]) > date.getActualMaximum(Calendar.DAY_OF_MONTH)) return false;
    if (arr.length > 3
        && (Integer.parseInt(arr[3]) > 23 || Integer.parseInt(arr[4]) > 59 || Integer.parseInt(arr[5]) > 59))
      return false;
    return true;
  }

  /**
   * Set category to text box and closeof choose category popup window
   * @param selectField: name of text field input
   * @param value: value of chosen category
   * @throws Exception
   */
  public void doSelect(String selectField, Object value) throws Exception {
    /* Set value to textbox */
    if (value==null) {
      getUIStringInput(selectField).setValue("");
      getUICheckBoxInput(UIConstraintsForm.CATEGORY_PROPERTY).setChecked(false);
    }else {
      getUIStringInput(selectField).setValue(value.toString());
      /* Set value for checkBox is checked */
      getUICheckBoxInput(UIConstraintsForm.CATEGORY_PROPERTY).setChecked(true);
    }
    UISearchContainer uiSearchContainer = getAncestorOfType(UISearchContainer.class);
    /*
     *  Close popup window when finish choose category
     */
    UIPopupWindow uiPopup = uiSearchContainer.findComponentById(UISearchContainer.CATEGORY_POPUP);
    uiPopup.setRendered(false);
    uiPopup.setShow(false);
  }

  public UIFormDateTimeInput getUIFormDateTimeInput(String name) {
    return findComponentById(name);
  }
}
