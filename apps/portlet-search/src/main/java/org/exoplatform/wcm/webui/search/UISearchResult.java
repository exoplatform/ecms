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
package org.exoplatform.wcm.webui.search;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.commons.utils.ISO8601;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.paginator.UICustomizeablePaginator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.form.UIFormHiddenInput;
import org.exoplatform.webui.form.UIFormRadioBoxInput;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@SuppressWarnings("deprecation")
@ComponentConfigs( {
    @ComponentConfig(lifecycle = Lifecycle.class),
    @ComponentConfig(type = UICustomizeablePaginator.class,
                     events = @EventConfig(listeners = UICustomizeablePaginator.ShowPageActionListener.class)) })
public class UISearchResult extends UIContainer {

  /** The template path. */
  private String                   templatePath;

  /** The resource resolver. */
  private ResourceResolver         resourceResolver;

  /** The ui paginator. */
  private UICustomizeablePaginator uiPaginator;

  /** The keyword. */
  private String                   keyword;

  /** The result type. */
  private String                   resultType;

  /** The suggestion. */
  private String                   suggestion;

  /** The suggestion. */
  private String                   suggestionURL;
  
  /** The PageMode */
  private String                    pageMode;

  /** The date formatter. */
  private SimpleDateFormat         dateFormatter    = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);

  /** The search time. */
  private float                    searchTime;
  
  /** The search result in "More" mode */
  private List<ResultNode> moreListResult;
  
  /** The page that already queried (used only in "More" mode */
  private Set<Integer> morePageSet;

  /** The Constant PARAMETER_REGX. */
  public final static String       PARAMETER_REGX   = "(portal=.*)&(keyword=.*)";

  /** The Constant RESULT_NOT_FOUND. */
  public final static String       RESULT_NOT_FOUND = "UISearchResult.msg.result-not-found";

  /**
   * Inits the.
   *
   * @param templatePath the template path
   * @param resourceResolver the resource resolver
   * @throws Exception the exception
   */
  public void init(String templatePath, ResourceResolver resourceResolver) throws Exception {
    PortletRequestContext portletRequestContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
    String paginatorTemplatePath = portletPreferences.getValue(UIWCMSearchPortlet.SEARCH_PAGINATOR_TEMPLATE_PATH,
                                                               null);
    this.pageMode = portletPreferences.getValue(UIWCMSearchPortlet.PAGE_MODE, SiteSearchService.PAGE_MODE_NONE);
    this.templatePath = templatePath;
    this.resourceResolver = resourceResolver;
    uiPaginator = addChild(UICustomizeablePaginator.class, null, null);
    uiPaginator.setTemplatePath(paginatorTemplatePath);
    uiPaginator.setResourceResolver(resourceResolver);
    uiPaginator.setPageMode(pageMode);
    clearResult();
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui
   * .application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    PortletRequestContext porletRequestContext = (PortletRequestContext) context;
    PortletPreferences portletPreferences = porletRequestContext.getRequest().getPreferences();
    if (resultType == null || resultType.trim().length() == 0) {
      resultType = "Document";
    }
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    String portal = portalRequestContext.getRequestParameter("portal");
    String keyword = portalRequestContext.getRequestParameter("keyword");
    if ((portal != null) && (keyword != null) && (keyword.length() > 0)) {
      UISearchPageLayout uiSearchPageContainer = getAncestorOfType(UISearchPageLayout.class);
      UISearchForm searchForm = uiSearchPageContainer.getChild(UISearchForm.class);
      // searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).setSelectedValues(new
      // String[] {portal});
      searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).setValue(keyword);
      if (searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).getValue() != null) {
        portal = searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).getValue();
      }
      if (searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).getValue() != null) {
        keyword = searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).getValue();
      }
      setKeyword(keyword);
      keyword = Normalizer.normalize(keyword, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

      SiteSearchService siteSearchService = getApplicationComponent(SiteSearchService.class);
      QueryCriteria queryCriteria = new QueryCriteria();

      UIFormRadioBoxInput searchOption = searchForm.getUIFormRadioBoxInput(UISearchForm.SEARCH_OPTION); 
      boolean isSearchDocument = (searchOption.getValue().equals(UISearchForm.DOCUMENT_CHECKING));
      boolean isWebPage = (searchOption.getValue().equals(UISearchForm.PAGE_CHECKING));

      List<String> documentNodeTypes = new ArrayList<String>();
      if (isSearchDocument) {
        TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
        documentNodeTypes = templateService.getAllDocumentNodeTypes();
        portal = Util.getPortalRequestContext().getPortalOwner();
        resultType = "Document";
      } else {
        documentNodeTypes.add("gtn:language");
        documentNodeTypes.add("exo:pageMetadata");
        queryCriteria.setFulltextSearchProperty(new String[] {"exo:metaKeywords", "exo:metaDescription", "gtn:name"});
        resultType = "Page";
      }
      
      String pageMode = portletPreferences.getValue(UIWCMSearchPortlet.PAGE_MODE, SiteSearchService.PAGE_MODE_NONE);
      
      queryCriteria.setContentTypes(documentNodeTypes.toArray(new String[documentNodeTypes.size()]));
      queryCriteria.setSiteName(portal);
      queryCriteria.setKeyword(
             org.exoplatform.services.cms.impl.Utils.escapeIllegalCharacterInQuery(keyword).toLowerCase());
      queryCriteria.setSearchWebpage(isWebPage);
      queryCriteria.setSearchDocument(isSearchDocument);
      queryCriteria.setSearchWebContent(isSearchDocument);
      queryCriteria.setPageMode(pageMode);

      queryCriteria.setLiveMode(WCMComposer.MODE_LIVE.equals(Utils.getCurrentMode()));
      queryCriteria.setSortBy(this.getSortField());
      queryCriteria.setOrderBy(this.getOrderType());
      int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE,
                                                                      null));
      try {
        AbstractPageList<ResultNode> pageList = null;
        if (isWebPage) {
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
        
        setSearchTime(pageList.getQueryTime() / 1000);
        setSuggestion(pageList.getSpellSuggestion());
        if (pageList.getAvailable() <= 0) {
          String suggestion = pageList.getSpellSuggestion();
          setSuggestionURL(suggestion);
          searchForm.setSubmitAction(suggestion);
        }
        setPageList(pageList);        
      } catch (Exception e) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage(UISearchForm.MESSAGE_NOT_SUPPORT_KEYWORD,
                                                null,
                                                ApplicationMessage.WARNING));
      }
    }
    super.processRender(context);
  }

  /**
   * Sets the page list.
   *
   * @param dataPageList the new page list
   */
  @SuppressWarnings("unchecked")
  public void setPageList(PageList dataPageList) {
    uiPaginator.setPageList(dataPageList);
    moreListResult = new ArrayList<ResultNode>();
    morePageSet = new HashSet<Integer>();    
  }

  /**
   * Gets the total item.
   *
   * @return the total item
   */
  public int getTotalItem() {
    return uiPaginator.getPageList().getAvailable();
  }

  /**
   * Gets the items per page.
   *
   * @return the items per page
   */
  public int getItemsPerPage() {
    return uiPaginator.getPageList().getPageSize();
  }

  /**
   * Gets the current page.
   *
   * @return the current page
   */
  public int getCurrentPage() {
    return uiPaginator.getCurrentPage();
  }

  /**
   * Gets the page mode
   * @return the page mode
   */
  public String getPageMode() {
    return pageMode;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.portal.webui.portal.UIPortalComponent#getTemplate()
   */
  public String getTemplate() {
    return templatePath;
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
   * Gets the current page data.
   *
   * @return the current page data
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public List getCurrentPageData() throws Exception {
    return uiPaginator.getCurrentPageData();
  }

  /**
   * Gets the title.
   *
   * @param node the node
   * @return the title
   * @throws Exception the exception
   */
  public String getTitle(Node node) throws Exception {
    if (UIWCMSearchPortlet.SEARCH_CONTENT_MODE.equals(this.getResultType())) {
      return org.exoplatform.ecm.webui.utils.Utils.getTitle(node);
    } else {
      Session session = node.getSession();
      Node mopLink = (Node) session.getItem(node.getPath() + "/mop:link");
      if (mopLink != null && mopLink.hasProperty("mop:page")) {
        String mopPageLink = mopLink.getProperty("mop:page").getValue().getString();
        Node mopPage = (Node) session.getItem(mopPageLink);
        if (mopPage != null && mopPage.hasProperty("gtn:name")) {
          return mopPage.getProperty("gtn:name").getValue().getString();
        } else {
          return node.getName().replaceFirst("mop:", "");
        }
      } else {
        return node.getName().replaceFirst("mop:", "");
      }
      
    }
  }

  /**
   * Gets the uRL.
   *
   * @param node the node
   * @return the uRL
   * @throws Exception the exception
   */
  public List<String> getURLs(Node node) throws Exception {
    List<String> urls = new ArrayList<String>();
    if (!node.hasProperty("publication:navigationNodeURIs")) {
      urls.add(getURL(node));
    } else {
      for (Value value : node.getProperty("publication:navigationNodeURIs").getValues()) {
        urls.add(value.getString());
      }
    }
    return urls;
  }

  /**
   * Gets the published node uri.
   *
   * @param navNodeURI the nav node uri
   * @return the published node uri
   */
  public String getPublishedNodeURI(String navNodeURI) {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    PortletRequest portletRequest = getPortletRequest();

    StringBuffer baseURI = new StringBuffer();
    baseURI.append(portletRequest.getScheme()).append("://").append(portletRequest.getServerName());
    if (portletRequest.getServerPort() != 80) {
      baseURI.append(":").append(String.format("%s", portletRequest.getServerPort()));
    }
    if (navNodeURI.startsWith(baseURI.toString()))
      return navNodeURI;
    
    NodeURL nodeURL = portalRequestContext.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(portalRequestContext.getSiteType(),
                                                         portalRequestContext.getSiteName(),
                                                         navNodeURI);
    nodeURL.setResource(resource);
    return baseURI + nodeURL.toString();
  }

  /**
   * Gets the uRL.
   *
   * @param node the node
   * @return the uRL
   * @throws Exception the exception
   */
  public String getURL(Node node) throws Exception {
    PortletRequest portletRequest = getPortletRequest();
    PortletPreferences portletPreferences = portletRequest.getPreferences();
    String repository = WCMCoreUtils.getRepository().getConfiguration().getName();
    String workspace = portletPreferences.getValue(UIWCMSearchPortlet.WORKSPACE, null);
    String basePath = portletPreferences.getValue(UIWCMSearchPortlet.BASE_PATH, null);
    String detailParameterName = portletPreferences.getValue(UIWCMSearchPortlet.DETAIL_PARAMETER_NAME, null);    

    StringBuffer path = new StringBuffer();
    path.append("/").append(repository).append("/").append(workspace);
    NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);   
    NavigationResource resource = new NavigationResource(SiteType.PORTAL,
                                                         Util.getPortalRequestContext()
                                                             .getPortalOwner(), basePath);
    nodeURL.setResource(resource);
    if (node.isNodeType("nt:frozenNode")) {
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      Node originalNode = node.getSession().getNodeByUUID(uuid);
      path.append(originalNode.getPath());      
      nodeURL.setQueryParameterValue("version", node.getParent().getName());
    } else {
      path.append(node.getPath());
    }

    nodeURL.setQueryParameterValue(detailParameterName, path.toString());
    nodeURL.setSchemeUse(true);
    return nodeURL.toString();
  }

  private PortletRequest getPortletRequest() {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    return portletRequestContext.getRequest();
  }

  /**
   * Gets the created date.
   *
   * @param node the node
   * @return the created date
   * @throws Exception the exception
   */
  public String getCreatedDate(Node node) throws Exception {
    if (node.hasProperty("exo:dateCreated")) {
      Calendar calendar = node.getProperty("exo:dateCreated").getValue().getDate();
      return dateFormatter.format(calendar.getTime());
    }
    return null;
  }

  /**
   * Gets the mofified date of search result node.
   *
   * @param node the node
   * @return the mofified date
   * @throws Exception the exception
   */
  private String getModifiedDate(Node node) throws Exception {
    Calendar calendar = node.hasProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE) ?
            node.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate() :
            node.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
    DateFormat simpleDateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.SHORT);
    return simpleDateFormat.format(calendar.getTime());
  }

  /**
   * Checks if is show paginator.
   *
   * @return true, if is show paginator
   * @throws Exception the exception
   */
  public boolean isShowPaginator() throws Exception {
    PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest()
                                                                                                              .getPreferences();
    String itemsPerPage = portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE, null);
    int totalItems = uiPaginator.getTotalItems();
    if (totalItems > Integer.parseInt(itemsPerPage)) {
      return true;
    }
    return false;
  }

  /**
   * Gets the search time.
   *
   * @return the search time
   */
  public float getSearchTime() {
    return searchTime;
  }

  /**
   * Sets the search time.
   *
   * @param searchTime the new search time
   */
  public void setSearchTime(float searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * Gets the suggestion.
   *
   * @return the suggestion
   */
  public String getSuggestion() {
    return suggestion;
  }

  /**
   * Sets the suggestion.
   *
   * @param suggestion the suggestion
   */
  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  /**
   * Gets the suggestion URL.
   *
   * @return the suggestion URL
   */
  public String getSuggestionURL() {
    return suggestionURL;
  }

  /**
   * Sets the suggestion URL.
   *
   * @param suggestionURL the suggestion url
   */
  public void setSuggestionURL(String suggestionURL) {
    this.suggestionURL = suggestionURL;
  }

  /**
   * Gets the keyword.
   *
   * @return the keyword
   */
  public String getKeyword() {
    return this.keyword;
  }

  /**
   * Sets the keyword.
   *
   * @param keyword the new keyword
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * Gets the result type.
   *
   * @return the result type
   */
  public String getResultType() {
    return this.resultType;
  }

  /**
   * Sets the result type.
   *
   * @param resultType the new result type
   */
  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  /**
   * Gets the number of page.
   *
   * @return the number of page
   */
  public int getNumberOfPage() {
    return uiPaginator.getPageList().getAvailablePage();
  }
  
   /**
    * Clears the displayed result list
    */
  @SuppressWarnings("unchecked")
  public void clearResult() {
    moreListResult = new ArrayList<ResultNode>();
    morePageSet = new HashSet<Integer>();
    PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest()
                                                                                                              .getPreferences();
    String itemsPerPage = portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE, null);
    setPageList(new ObjectPageList(new ArrayList<ResultNode>(), Integer.parseInt(itemsPerPage)));
  }
  
  /**
   * Gets the real node list to display
   * 
   * @return the real node list
   */
  public List<ResultNode> getRealCurrentPageData() throws Exception {
    int currentPage = getCurrentPage();
    if (SiteSearchService.PAGE_MODE_MORE.equals(pageMode)) {
      if (!morePageSet.contains(currentPage)) {
        morePageSet.add(currentPage);
        moreListResult.addAll(getCurrentPageData());
      }
    }
    return SiteSearchService.PAGE_MODE_MORE.equals(pageMode) ? moreListResult : getCurrentPageData();
  }

  /**
   * Get string used to describe search result node.
   *
   * @param resultNode ResultNode
   * @return result node description
   * @throws Exception
   */
  private String getDetail(ResultNode resultNode) throws Exception {
    Node realNode = org.exoplatform.wcm.webui.Utils.getRealNode(resultNode.getNode());
    String resultType = this.getResultType();
    if (UIWCMSearchPortlet.SEARCH_CONTENT_MODE.equals(resultType)) {
      return WCMCoreUtils.getService(LivePortalManagerService.class).getLivePortalByChild(realNode).getName()
            .concat(org.exoplatform.services.cms.impl.Utils.fileSize(realNode))
            .concat(" - ")
            .concat(getModifiedDate(realNode));
    } else {
      return StringUtils.substringBefore(StringUtils.substringAfter(realNode.getPath(),
              SiteSearchService.PATH_PORTAL_SITES.concat("/mop:")),"/")
              .concat(" - ")
              .concat(resultNode.getUserNavigationURI());
    }
  }

  /**
   * Get resource bundle from given key.
   *
   * @param key Key
   * @return
   */
  private String getLabel(String key) {
    try {
      ResourceBundle rs = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
      return rs.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * Get Order Type ("asc" or "desc") from user criteria.
   *
   * @return order type
   * @throws Exception
   */
  private String getOrderType() throws Exception {
    UISearchForm uiSearchForm = this.getParent().findFirstComponentOfType(UISearchForm.class);
    String orderType = ((UIFormHiddenInput)uiSearchForm.getUIInput(UISearchForm.ORDER_TYPE_HIDDEN_INPUT)).getValue();
    return StringUtils.isEmpty(orderType) ? "asc" : orderType;
  }

  /**
   * Get Sort Field from user criteria.
   *
   * @return sort field used to sort result
   */
  private String getSortField() {
    UISearchForm uiSearchForm = this.getParent().findFirstComponentOfType(UISearchForm.class);
    String sortField = ((UIFormHiddenInput)uiSearchForm.getUIInput(UISearchForm.SORT_FIELD_HIDDEN_INPUT)).getValue();
    return StringUtils.isEmpty(sortField) ? "relevancy" : sortField;
  }
}
