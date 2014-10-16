/*
 * Copyright (C) 2003-2008 eXo Platform SAS. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormHiddenInput;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 events = {
  @EventConfig(listeners = UISearchForm.SearchActionListener.class) })
public class UISearchForm extends UIForm {

  /** The template path. */
  private String             templatePath;

  /** The resource resolver. */
  private ResourceResolver   resourceResolver;

  /** The Constant KEYWORD_INPUT. */
  public static final String KEYWORD_INPUT                   = "keywordInput";

  /** The Constant SEARCH_OPTION. */
  public static final String SEARCH_OPTION                   = "searchOption";
  
  /** The Constant DOCUMENT_CHECKING. */
  public static final String DOCUMENT_CHECKING               = "documentCheckBox";

  /** The Constant PAGE_CHECKING. */
  public static final String PAGE_CHECKING                   = "pageCheckBox";

  /** The Constant PORTALS_SELECTOR. */
  public static final String PORTALS_SELECTOR                = "portalSelector";

  /** The Constant ALL_OPTION. */
  public static final String ALL_OPTION                      = "all";

  /** The Constant SORT_FIELD_HIDDEN_INPUT */
  public static final String SORT_FIELD_HIDDEN_INPUT = "sortHiddenInputField";

  /** The Constant ORDER_TYPE_HIDDEN_INPUT */
  public static final String ORDER_TYPE_HIDDEN_INPUT = "orderTypeHiddenInputField";

  /** The Constant MESSAGE_NOT_CHECKED_TYPE_SEARCH. */
  public static final String MESSAGE_NOT_CHECKED_TYPE_SEARCH = "UISearchForm.message.not-checked";

  /** The Constant MESSAGE_NOT_SUPPORT_KEYWORD. */
  public static final String MESSAGE_NOT_SUPPORT_KEYWORD     = "UISearchForm.message.keyword-not-support";

  /** The Constant MESSAGE_NOT_EMPTY_KEYWORD. */
  public static final String MESSAGE_NOT_EMPTY_KEYWORD       = "UISearchForm.message.keyword-not-empty";

  /**
   * Instantiates a new uI search form.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public UISearchForm() throws Exception {
    UIFormStringInput uiKeywordInput = new UIFormStringInput(KEYWORD_INPUT, KEYWORD_INPUT, null);
    UIFormSelectBox uiPortalSelectBox = new UIFormSelectBox(PORTALS_SELECTOR,
                                                            PORTALS_SELECTOR,
                                                            getPortalList());
    List<SelectItemOption<String>> searchOptionList = new ArrayList<SelectItemOption<String>>();
    SelectItemOption<String> pageOption = new SelectItemOption<String>(PAGE_CHECKING, PAGE_CHECKING);
    SelectItemOption<String> docOption = new SelectItemOption<String>(DOCUMENT_CHECKING, DOCUMENT_CHECKING);
    searchOptionList.add(docOption);
    searchOptionList.add(pageOption);
    UIFormRadioBoxInput searchOptionRadioBox = new UIFormRadioBoxInput(SEARCH_OPTION, SEARCH_OPTION, searchOptionList);
    searchOptionRadioBox.setDefaultValue(DOCUMENT_CHECKING);
    searchOptionRadioBox.setValue(DOCUMENT_CHECKING);
    
    addUIFormInput(uiKeywordInput);
    addUIFormInput(uiPortalSelectBox);
    addUIFormInput(searchOptionRadioBox);

    // Hidden fields for storing order criteria from user
    addUIFormInput(new UIFormHiddenInput(SORT_FIELD_HIDDEN_INPUT, SORT_FIELD_HIDDEN_INPUT, null));
    addUIFormInput(new UIFormHiddenInput(ORDER_TYPE_HIDDEN_INPUT, ORDER_TYPE_HIDDEN_INPUT, null));
  }

  /**
   * Inits the.
   *
   * @param templatePath the template path
   * @param resourceResolver the resource resolver
   */
  public void init(String templatePath, ResourceResolver resourceResolver) {
    this.templatePath = templatePath;
    this.resourceResolver = resourceResolver;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#getTemplate()
   */
  public String getTemplate() {
    return templatePath;
  }

  public void setKeyword() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    String keyword = portalRequestContext.getRequestParameter("keyword");
    if (keyword != null && keyword.length() > 0) {
      getUIStringInput(UISearchForm.KEYWORD_INPUT).setValue(keyword);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return resourceResolver;
  }

  /**
   * Gets the portal list.
   *
   * @return the portal list
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private List getPortalList() throws Exception {
    List<SelectItemOption<String>> portals = new ArrayList<SelectItemOption<String>>();
    DataStorage service = getApplicationComponent(DataStorage.class);
    Query<PortalConfig> query = new Query<PortalConfig>(null, null, null, null, PortalConfig.class);
    List<PortalConfig> list = service.find(query).getAll();
    portals.add(new SelectItemOption<String>(ALL_OPTION, ALL_OPTION));
    for (PortalConfig portalConfig : list) {
      portals.add(new SelectItemOption<String>(portalConfig.getName(), portalConfig.getName()));
    }
    return portals;
  }

  /**
   * The listener interface for receiving searchAction events. The class that is
   * interested in processing a searchAction event implements this interface,
   * and the object created with that class is registered with a component using
   * the component's <code>addSearchActionListener<code> method. When
   * the searchAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class SearchActionListener extends EventListener<UISearchForm> {

    /*
     * (non-Javadoc)
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
     * .event.Event)
     */
    @SuppressWarnings({ "deprecation" })
    public void execute(Event<UISearchForm> event) throws Exception {
      UISearchForm uiSearchForm = event.getSource();
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
      UIApplication uiApp = uiSearchForm.getAncestorOfType(UIApplication.class);
      SiteSearchService siteSearchService = uiSearchForm.getApplicationComponent(SiteSearchService.class);
      UISearchPageLayout uiSearchPageContainer = uiSearchForm.getParent();
      UISearchResult uiSearchResult = uiSearchPageContainer.getChild(UISearchResult.class);
      UIFormStringInput uiKeywordInput = uiSearchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT);
      UIFormSelectBox uiPortalSelectBox = uiSearchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR);
      String keyword = uiKeywordInput.getValue();
      UIFormRadioBoxInput uiSearchOptionRadioBox = uiSearchForm.getUIFormRadioBoxInput(SEARCH_OPTION);
      String searchOption = uiSearchOptionRadioBox.getValue();
      String sortField = ((UIFormHiddenInput)uiSearchForm.getUIInput(UISearchForm.SORT_FIELD_HIDDEN_INPUT)).getValue();
      String orderType = ((UIFormHiddenInput)uiSearchForm.getUIInput(UISearchForm.ORDER_TYPE_HIDDEN_INPUT)).getValue();
      boolean pageChecked = false;
      boolean documentChecked = false;
      if (PAGE_CHECKING.equals(searchOption)) {
        pageChecked = true;
      } else if (DOCUMENT_CHECKING.equals(searchOption)) {
        documentChecked = true;
      }
      
      uiSearchResult.clearResult();
      uiSearchResult.setKeyword(keyword);
      if (keyword == null || keyword.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage(MESSAGE_NOT_EMPTY_KEYWORD,
                                                null,
                                                ApplicationMessage.WARNING));
        portletRequestContext.addUIComponentToUpdateByAjax(uiSearchResult);
        return;
      }
      if (!pageChecked && !documentChecked) {
        uiApp.addMessage(new ApplicationMessage(MESSAGE_NOT_CHECKED_TYPE_SEARCH,
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      }
      String resultType = null;
      if (pageChecked) {
        resultType = UIWCMSearchPortlet.SEARCH_PAGE_MODE;
      } else {
        resultType = UIWCMSearchPortlet.SEARCH_CONTENT_MODE;
      }
      String newKey = event.getRequestContext().getRequestParameter(OBJECTID);
      if (newKey != null) {
        keyword = newKey;
        uiKeywordInput.setValue(newKey);
      }
      keyword = Normalizer.normalize(keyword, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
      //keyword = keyword.replaceAll("'","''");
      uiSearchResult.setResultType(resultType);
      String selectedPortal = uiPortalSelectBox.getValue();
      QueryCriteria queryCriteria = new QueryCriteria();

      List<String> documentNodeTypes = new ArrayList<String>();
      if (documentChecked) {
        TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
        documentNodeTypes = templateService.getAllDocumentNodeTypes();
        selectedPortal = Util.getPortalRequestContext().getPortalOwner();
      } else {
        documentNodeTypes.add("gtn:language");
        documentNodeTypes.add("exo:pageMetadata");
        queryCriteria.setFulltextSearchProperty(new String[] {"exo:metaKeywords", "exo:metaDescription", "gtn:name"});
      }

      queryCriteria.setContentTypes(documentNodeTypes.toArray(new String[documentNodeTypes.size()]));

      queryCriteria.setSiteName(selectedPortal);
      queryCriteria.setKeyword(
               org.exoplatform.services.cms.impl.Utils.escapeIllegalCharacterInQuery(keyword));
      if (documentChecked) {
        queryCriteria.setSearchDocument(true);
        queryCriteria.setSearchWebContent(true);
      } else {
        queryCriteria.setSearchDocument(false);
        queryCriteria.setSearchWebContent(false);
      }
      queryCriteria.setSearchWebpage(pageChecked);
      queryCriteria.setLiveMode(WCMComposer.MODE_LIVE.equals(Utils.getCurrentMode()));
      int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE,
                                                                      null));
      queryCriteria.setPageMode(portletPreferences.getValue(UIWCMSearchPortlet.PAGE_MODE, null));
      queryCriteria.setSortBy(sortField);
      queryCriteria.setOrderBy(orderType);
      try {
        AbstractPageList<ResultNode> pageList = null;
        if (pageChecked) {
          pageList = siteSearchService.searchPageContents(WCMCoreUtils.getSystemSessionProvider(),
                                                          queryCriteria,
                                                          itemsPerPage,
                                                          false);
        } else {
          pageList = siteSearchService.searchSiteContents(WCMCoreUtils.getUserSessionProvider(),
                                                         queryCriteria,
                                                         itemsPerPage,
                                                         false);          
        }        
        
        uiSearchResult.setPageList(pageList);
        float timeSearch = pageList.getQueryTime() / 1000;
        uiSearchResult.setSearchTime(timeSearch);        
        if (pageList.getAvailable() <= 0) {
          String suggestion = pageList.getSpellSuggestion();
          uiSearchResult.setSuggestion(suggestion);
          uiSearchForm.setSubmitAction(suggestion);
        }
        portletRequestContext.addUIComponentToUpdateByAjax(uiSearchResult);
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage(MESSAGE_NOT_SUPPORT_KEYWORD,
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      }
      portletRequestContext.addUIComponentToUpdateByAjax(uiSearchPageContainer);
    }
  }
  
  public UIFormRadioBoxInput getUIFormRadioBoxInput(String name)
  {
     return findComponentById(name);
  }
  
}
