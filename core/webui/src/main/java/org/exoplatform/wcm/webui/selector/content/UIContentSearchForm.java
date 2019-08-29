/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.selector.content;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.QueryCriteria.DATE_RANGE_SELECTED;
import org.exoplatform.services.wcm.search.QueryCriteria.DatetimeRange;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 *          dzungdev@gmail.com
 * Jan 5, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/selector/content/UIContentSearchForm.gtmpl",
    events = {
      @EventConfig(listeners = UIContentSearchForm.SearchWebContentActionListener.class),
      @EventConfig(listeners = UIContentSearchForm.AddMetadataTypeActionListener.class),
      @EventConfig(listeners = UIContentSearchForm.AddNodeTypeActionListener.class)
    }
)

public class UIContentSearchForm extends UIForm {

  public static final String LOCATION = "location";
  public static final String SEARCH_BY_NAME = "name";
  public static final String SEARCH_BY_CONTENT = "content";
  public static final String RADIO_NAME = "WcmRadio";
  final static public String TIME_OPTION = "timeOpt";
  final static public String PROPERTY = "property";
  final static public String CONTAIN = "contain";
  final static public String START_TIME = "startTime";
  final static public String END_TIME = "endTime";
  final static public String DOC_TYPE = "docType";
  final static public String CATEGORY = "category";
  final static public String CREATED_DATE = "CREATED";
  final static public String MODIFIED_DATE = "MODIFIED";
  final static public String EXACTLY_PROPERTY = "exactlyPro";
  final static public String CONTAIN_PROPERTY = "containPro";
  final static public String NOT_CONTAIN_PROPERTY = "notContainPro";
  final static public String DATE_PROPERTY = "datePro";
  final static public String NODETYPE_PROPERTY = "nodetypePro";
  final static public String CHECKED_RADIO_ID = "checkedRadioId";

  private String checkedRadioId;

  public UIContentSearchForm() throws Exception {
  }

  public void init() throws Exception {
    List<SelectItemOption<String>> portalNameOptions = new ArrayList<SelectItemOption<String>>();
    List<String> portalNames = getPortalNames();
    for(String portalName: portalNames) {
      portalNameOptions.add(new SelectItemOption<String>(portalName, portalName));
    }
    UIFormSelectBox portalNameSelectBox = new UIFormSelectBox(LOCATION, LOCATION, portalNameOptions);
    portalNameSelectBox.setDefaultValue(portalNames.get(0));
    addChild(portalNameSelectBox);

    addUIFormInput(new UIFormStringInput(SEARCH_BY_NAME,SEARCH_BY_NAME,null));
    addUIFormInput(new UIFormStringInput(SEARCH_BY_CONTENT, SEARCH_BY_CONTENT, null));

    addUIFormInput(new UIFormStringInput(PROPERTY, PROPERTY, null).setDisabled(true));
    addUIFormInput(new UIFormStringInput(CONTAIN, CONTAIN, null));

    List<SelectItemOption<String>> dateOptions = new ArrayList<SelectItemOption<String>>();
    dateOptions.add(new SelectItemOption<String>(CREATED_DATE,CREATED_DATE));
    dateOptions.add(new SelectItemOption<String>(MODIFIED_DATE,MODIFIED_DATE));
    addUIFormInput(new UIFormSelectBox(TIME_OPTION,TIME_OPTION, dateOptions));
    UIFormDateTimeInput startTime = new UIFormDateTimeInput(START_TIME, START_TIME, null, true);
    addUIFormInput(startTime);
    UIFormDateTimeInput endTime = new UIFormDateTimeInput(END_TIME, END_TIME, null, true);
    addUIFormInput(endTime);
    addUIFormInput(new UIFormStringInput(DOC_TYPE, DOC_TYPE, null).setDisabled(true));

    setActions(new String[] {"SearchWebContent"} );
  }
  
  private List<String> getPortalNames() throws Exception {
    List<String> portalNames = new ArrayList<String>();
    String currentPortalName = Util.getPortalRequestContext().getPortalOwner();
    WCMConfigurationService configService = getApplicationComponent(WCMConfigurationService.class);
    String sharedPortalName = configService.getSharedPortalName();
    portalNames.add(currentPortalName);
    portalNames.add(sharedPortalName);
    return portalNames;
  }

  public static class AddMetadataTypeActionListener extends EventListener<UIContentSearchForm> {
    public void execute(Event<UIContentSearchForm> event) throws Exception {
      UIContentSearchForm contentSearchForm = event.getSource();
      UIContentSelector contentSelector = contentSearchForm.getParent();
      contentSearchForm.setCheckedRadioId(event.getRequestContext().getRequestParameter(CHECKED_RADIO_ID));
      UIContainer uiContainer = 
          contentSearchForm.getAncestorOfType(UIContentSelector.class).getAncestorOfType(UIContainer.class);
      contentSelector.initMetadataPopup(uiContainer);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  public static class AddNodeTypeActionListener extends EventListener<UIContentSearchForm> {
    public void execute(Event<UIContentSearchForm> event) throws Exception {
      UIContentSearchForm contentSearchForm = event.getSource();
      UIContentSelector contentSelector = contentSearchForm.getParent();
      contentSearchForm.setCheckedRadioId(event.getRequestContext().getRequestParameter(CHECKED_RADIO_ID));
      UIContainer uiContainer = 
          contentSearchForm.getAncestorOfType(UIContentSelector.class).getAncestorOfType(UIContainer.class);
      contentSelector.initNodeTypePopup(uiContainer);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  private QueryCriteria getInitialQueryCriteria(String siteName) throws Exception {
    QueryCriteria qCriteria = new QueryCriteria();
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    String[] allContentType = templateService.getDocumentTemplates().toArray(new String[0]);
    qCriteria.setContentTypes(allContentType);  //only get the content nodes which have the document template.
    qCriteria.setSearchWebpage(false);
    qCriteria.setSiteName(siteName);
    qCriteria.setLiveMode(false);
    return qCriteria;
  }

  private boolean haveEmptyField(UIApplication uiApp, Event<UIContentSearchForm> event, String field, String messageKey) throws Exception {
      if(field == null || "".equals(field) || (field.toString().trim().length() <= 0)) {
        uiApp.addMessage(new ApplicationMessage(
            messageKey, null, ApplicationMessage.WARNING));
        WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
        requestContext.addUIComponentToUpdateByAjax(this);
        return true;
    }
    return false;
  }

  private AbstractPageList<ResultNode> searchContentByName(String keyword,
      QueryCriteria qCriteria, int pageSize) throws Exception {
    qCriteria.setFulltextSearch(false);
    qCriteria.setKeyword(keyword);

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    SiteSearchService siteSearch = getApplicationComponent(SiteSearchService.class);
    return siteSearch.searchSiteContents(WCMCoreUtils.getUserSessionProvider(), qCriteria, requestContext.getLocale(), pageSize, true);
  }

  private AbstractPageList<ResultNode> searchContentByFulltext(String keyword,
      QueryCriteria qCriteria, int pageSize) throws Exception {
    qCriteria.setFulltextSearch(true);
    qCriteria.setFulltextSearchProperty(new String[] {QueryCriteria.ALL_PROPERTY_SCOPE});
    qCriteria.setKeyword(keyword);

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    SiteSearchService siteSearch = getApplicationComponent(SiteSearchService.class);
    return siteSearch.searchSiteContents(WCMCoreUtils.getUserSessionProvider(), qCriteria, requestContext.getLocale(), pageSize, true);
  }

  private AbstractPageList<ResultNode> searchContentByProperty(String property,
      String keyword, QueryCriteria qCriteria, int pageSize) throws Exception {
    qCriteria.setFulltextSearch(true);
    qCriteria.setFulltextSearchProperty(new String[] {property});
    qCriteria.setKeyword(keyword);

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    SiteSearchService siteSearchService = getApplicationComponent(SiteSearchService.class);
    return siteSearchService.searchSiteContents(WCMCoreUtils.getUserSessionProvider(), qCriteria, requestContext.getLocale(), pageSize, true);
  }

  private AbstractPageList<ResultNode> searchContentByDate(DATE_RANGE_SELECTED dateRangeSelected,
      Calendar fromDate, Calendar endDate, QueryCriteria qCriteria, int pageSize) throws Exception {
    qCriteria.setDateRangeSelected(dateRangeSelected);
    DatetimeRange dateTimeRange = new QueryCriteria.DatetimeRange(fromDate, endDate);
    if(DATE_RANGE_SELECTED.CREATED.equals(dateRangeSelected)) {
      qCriteria.setCreatedDateRange(dateTimeRange);
    } else if(DATE_RANGE_SELECTED.MODIFIDED.equals(dateRangeSelected)) {
      qCriteria.setLastModifiedDateRange(dateTimeRange);
    }
    qCriteria.setFulltextSearch(true);
    qCriteria.setFulltextSearchProperty(null);

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    SiteSearchService siteSearch = getApplicationComponent(SiteSearchService.class);
    return siteSearch.searchSiteContents(WCMCoreUtils.getUserSessionProvider(), qCriteria, requestContext.getLocale(), pageSize, true);
  }

  private AbstractPageList<ResultNode> searchContentByType(String documentType,
      QueryCriteria qCriteria, int pageSize) throws Exception {
    qCriteria.setFulltextSearch(true);
    qCriteria.setFulltextSearchProperty(null);
    qCriteria.setContentTypes(documentType.split(","));

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    SiteSearchService siteSearch = getApplicationComponent(SiteSearchService.class);
    return siteSearch.searchSiteContents(WCMCoreUtils.getUserSessionProvider(), qCriteria, requestContext.getLocale(), pageSize, true);
  }

  static public class SearchWebContentActionListener extends EventListener<UIContentSearchForm> {

    public void execute(Event<UIContentSearchForm> event) throws Exception {
      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      UIContentSearchForm uiWCSearch = event.getSource();
      
      int pageSize = 5;
      String radioValue = event.getRequestContext().getRequestParameter(RADIO_NAME);
      uiWCSearch.setCheckedRadioId(event.getRequestContext().getRequestParameter(CHECKED_RADIO_ID));
      String siteName = uiWCSearch.getUIStringInput(UIContentSearchForm.LOCATION).getValue();
      UIContentSelector uiWCTabSelector = uiWCSearch.getParent();
      UIApplication uiApp = uiWCSearch.getAncestorOfType(UIApplication.class);
      QueryCriteria qCriteria = uiWCSearch.getInitialQueryCriteria(siteName);
      AbstractPageList<ResultNode> pagResult = null;

      ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
      try {

        if (UIContentSearchForm.SEARCH_BY_NAME.equals(radioValue)) {
          String keyword = uiWCSearch.getUIStringInput(radioValue).getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, keyword,"UIContentSearchForm.msg.empty-name"))
            return;
          pagResult = uiWCSearch.searchContentByName(keyword.trim(), qCriteria, pageSize);
        } else if (UIContentSearchForm.SEARCH_BY_CONTENT.equals(radioValue)) {
          String keyword = uiWCSearch.getUIStringInput(radioValue).getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, keyword,"UIContentSearchForm.msg.empty-content"))
            return;
          pagResult = uiWCSearch.searchContentByFulltext(keyword, qCriteria, pageSize);
        } else if (UIContentSearchForm.PROPERTY.equals(radioValue)) {
          String property = uiWCSearch.getUIStringInput(UIContentSearchForm.PROPERTY).getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, property,"UIContentSearchForm.msg.empty-property"))
            return;
          String keyword = uiWCSearch.getUIStringInput(UIContentSearchForm.CONTAIN).getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, keyword,"UIContentSearchForm.msg.empty-property-keyword"))
            return;
          pagResult = uiWCSearch.searchContentByProperty(property,
                                                            keyword,
                                                            qCriteria,
                                                            pageSize);
        } else if (UIContentSearchForm.TIME_OPTION.equals(radioValue)) {
          UIFormDateTimeInput startDateInput = uiWCSearch.getUIFormDateTimeInput(UIContentSearchForm.START_TIME);
          UIFormDateTimeInput endDateInput = uiWCSearch.getUIFormDateTimeInput(UIContentSearchForm.END_TIME);

          //startDateInput cannot be empty
          String strStartDate = startDateInput.getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, strStartDate,"UIContentSearchForm.msg.empty-startDate"))
            return;
          
          //startDateInput must have a valid format
          try {
            DateFormat dateFormat = new SimpleDateFormat(startDateInput.getDatePattern_().trim());
            dateFormat.setLenient(false);
            dateFormat.parse(startDateInput.getValue());
          } catch (ParseException e) {
            uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-format",
                                                    new Object[] 
                                                               { resourceBundle.getString("UIContentSearchForm.title.FromDate") },
                                                    ApplicationMessage.WARNING));
            requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
            return;
          }
          
          Calendar startDate = startDateInput.getCalendar();
          Calendar endDate = null;
          
          if (endDateInput.getValue() == null || endDateInput.getValue().length() == 0) {
            //set endDate value when endDateInput is empty
            if (startDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
              endDate = startDate;
            } else {
              endDate = Calendar.getInstance();
            }
          } else {
            //endDateInput should have a valid format
            try {
              DateFormat dateFormat = new SimpleDateFormat(endDateInput.getDatePattern_().trim());
              dateFormat.setLenient(false);
              dateFormat.parse(endDateInput.getValue());
            } catch (ParseException e) {
              uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-format",
                                                      new Object[] 
                                                                 { resourceBundle.getString("UIContentSearchForm.title.ToDate") },
                                                      ApplicationMessage.WARNING));
              requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
              return;
            }
            
            endDate = endDateInput.getCalendar();
          }
          
          //startDate cannot be after endDate
          if (startDate.getTimeInMillis() > endDate.getTimeInMillis()) {
            uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-date",
                                                    null,
                                                    ApplicationMessage.WARNING));
            requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
            return;
          }
          // startDate cannot be later than today
          if (startDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
            uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-startDate",
                                                    null,
                                                    ApplicationMessage.WARNING));
            requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
            return;
          }
          try {
            String dateRangeSelected = uiWCSearch.getUIStringInput(UIContentSearchForm.TIME_OPTION).getValue();
            if (UIContentSearchForm.CREATED_DATE.equals(dateRangeSelected)) {
              pagResult = uiWCSearch.searchContentByDate(DATE_RANGE_SELECTED.CREATED,
                                                            startDate,
                                                            endDate,
                                                            qCriteria,
                                                            pageSize);
            } else {
              pagResult = uiWCSearch.searchContentByDate(DATE_RANGE_SELECTED.MODIFIDED,
                                                            startDate,
                                                            endDate,
                                                            qCriteria,
                                                            pageSize);
            }
          } catch (IllegalArgumentException iaEx) {
            uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.time-to-late",
                                                    null,
                                                    ApplicationMessage.WARNING));
            requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
            return;
          }
        } else if (UIContentSearchForm.DOC_TYPE.equals(radioValue)) {
          String documentType = uiWCSearch.getUIStringInput(UIContentSearchForm.DOC_TYPE)
                                          .getValue();
          if (uiWCSearch.haveEmptyField(uiApp, event, documentType,"UIContentSearchForm.msg.empty-doctype"))
            return;
          try {
            pagResult = uiWCSearch.searchContentByType(documentType,
                                                                  qCriteria,
                                                                  pageSize);
          } catch (Exception ex) {
            uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-nodeType",
                                                    new Object[] { documentType },
                                                    ApplicationMessage.ERROR));
            requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
            return;
          }
        }
      } catch (InvalidQueryException iqe) {
        uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-keyword",
                                                null,
                                                ApplicationMessage.WARNING));
        requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
        return;
      } catch (RepositoryException re) {
        uiApp.addMessage(new ApplicationMessage("UIContentSearchForm.msg.invalid-keyword",
                                                null,
                                                ApplicationMessage.WARNING));
        requestContext.addUIComponentToUpdateByAjax(uiWCSearch);
        return;
      }
      UIContentSearchResult uiWCSearchResult = uiWCTabSelector.getChild(UIContentSearchResult.class);
      uiWCSearchResult.updateGrid(pagResult);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWCTabSelector);
      uiWCTabSelector.setSelectedTab(uiWCSearchResult.getId());
    }
  }

  public String getCheckedRadioId() {
    return checkedRadioId;
  }

  public void setCheckedRadioId(String checkedRadioId) {
    this.checkedRadioId = checkedRadioId;
  }
}
