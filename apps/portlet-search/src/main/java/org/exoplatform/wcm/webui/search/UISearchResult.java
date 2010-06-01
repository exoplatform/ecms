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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.search.WCMPaginatedQueryResult;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.paginator.UICustomizeablePaginator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@SuppressWarnings("deprecation")
@ComponentConfigs( {
	@ComponentConfig(
		lifecycle = Lifecycle.class),
	@ComponentConfig(
		type = UICustomizeablePaginator.class, 
		events = @EventConfig(listeners = UICustomizeablePaginator.ShowPageActionListener.class)) 
})
public class UISearchResult extends UIContainer {

	/** The template path. */
	private String										templatePath;

	/** The resource resolver. */
	private ResourceResolver					resourceResolver;

	/** The ui paginator. */
	private UICustomizeablePaginator	uiPaginator;

	/** The keyword. */
	private String										keyword;

	/** The result type. */
	private String										resultType;

	/** The suggestion. */
	private String										suggestion;

	/** The suggestion. */
	private String										suggestionURL;

	/** The date formatter. */
	private SimpleDateFormat					dateFormatter			= new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);

	/** The search time. */
	private float											searchTime;

	/** The Constant PARAMETER_REGX. */
	public final static String				PARAMETER_REGX		= "(portal=.*)&(keyword=.*)";

	/** The Constant RESULT_NOT_FOUND. */
	public final static String				RESULT_NOT_FOUND	= "UISearchResult.msg.result-not-found";

  /**
	 * Inits the.
	 * 
	 * @param templatePath the template path
	 * @param resourceResolver the resource resolver
	 * 
	 * @throws Exception the exception
	 */
	public void init(String templatePath, ResourceResolver resourceResolver) throws Exception {
		PortletRequestContext portletRequestContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
		PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
		String paginatorTemplatePath = portletPreferences.getValue(	UIWCMSearchPortlet.SEARCH_PAGINATOR_TEMPLATE_PATH,
																																null);
		this.templatePath = templatePath;
		this.resourceResolver = resourceResolver;
		uiPaginator = addChild(UICustomizeablePaginator.class, null, null);
		uiPaginator.setTemplatePath(paginatorTemplatePath);
		uiPaginator.setResourceResolver(resourceResolver);
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
		if (resultType == null || resultType.length() == 0) {
			resultType = "DocumentAndPage";
		}
		PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
		String portal = portalRequestContext.getRequestParameter("portal");
		String keyword = portalRequestContext.getRequestParameter("keyword");
		if (portal != null && keyword != null) {
			UISearchPageLayout uiSearchPageContainer = getAncestorOfType(UISearchPageLayout.class);
			UISearchForm searchForm = uiSearchPageContainer.getChild(UISearchForm.class);
			//searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).setSelectedValues(new String[] {portal});
			//searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).setValue(keyword);
			if (searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).getValue() != null) {
			  portal = searchForm.getUIFormSelectBox(UISearchForm.PORTALS_SELECTOR).getValue();
			  portal = portal.equals(UISearchForm.ALL_OPTION)?Util.getPortalRequestContext().getPortalOwner():portal;
			} 
			if (searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).getValue() != null)
			  keyword = searchForm.getUIStringInput(UISearchForm.KEYWORD_INPUT).getValue();			
									
			SiteSearchService siteSearchService = getApplicationComponent(SiteSearchService.class);
			QueryCriteria queryCriteria = new QueryCriteria();			
			
	    boolean isSearchDocument  = searchForm.getUIFormCheckBoxInput(UISearchForm.DOCUMENT_CHECKING).isChecked();	   
	    boolean isWebPage  = searchForm.getUIFormCheckBoxInput(UISearchForm.PAGE_CHECKING).isChecked();
			
	    String repository = portletPreferences.getValue(UIWCMSearchPortlet.REPOSITORY, null);                                                   
	    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
	    List<String> documentNodeTypes = templateService.getAllDocumentNodeTypes(repository);

	    queryCriteria.setContentTypes(documentNodeTypes.toArray(new String[documentNodeTypes.size()]));
			queryCriteria.setSiteName(portal);
			queryCriteria.setKeyword(keyword.toLowerCase());			
			queryCriteria.setSearchWebpage(isWebPage);
			queryCriteria.setSearchDocument(isSearchDocument);
			queryCriteria.setSearchWebContent(isSearchDocument);
			
			if (Boolean.parseBoolean(Utils.getCurrentMode())) {
        queryCriteria.setLiveMode(true);
      } else {
        queryCriteria.setLiveMode(false);
      }
			int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE, null));
			try {
				WCMPaginatedQueryResult paginatedQueryResult = siteSearchService.searchSiteContents(
				                                                                                    Utils.getSessionProvider(),
				                                                                                    queryCriteria,
																																														itemsPerPage, false);
				setSearchTime(paginatedQueryResult.getQueryTimeInSecond());
				setSuggestion(paginatedQueryResult.getSpellSuggestion());
				String suggestionURL = Util.getPortalRequestContext().getRequestURI();
				suggestionURL += "?portal=" + portal + "&keyword=" + getSuggestion();
				setSuggestionURL(suggestionURL);
				setPageList(paginatedQueryResult);
			} catch (Exception e) {
				UIApplication uiApp = getAncestorOfType(UIApplication.class);
				uiApp.addMessage(new ApplicationMessage(UISearchForm.MESSAGE_NOT_SUPPORT_KEYWORD, null, ApplicationMessage.WARNING));
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
	 * 
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
	 * 
	 * @return the title
	 * 
	 * @throws Exception the exception
	 */
	public String getTitle(Node node) throws Exception {
		return node.hasProperty("exo:title") ? node.getProperty("exo:title").getValue().getString()
																				: node.getName();
	}

	/**
	 * Gets the uRL.
	 * 
	 * @param node the node
	 * 
	 * @return the uRL
	 * 
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
	 * 
	 * @return the published node uri
	 */
	public String getPublishedNodeURI(String navNodeURI) {
		PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
		PortletRequest portletRequest = getPortletRequest();
		String accessMode = null;
		if (portalRequestContext.getAccessPath() == PortalRequestContext.PUBLIC_ACCESS) {
			accessMode = "public";
		} else {
			accessMode = "private";
		}

		String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":" + String.format("%s", portletRequest.getServerPort());
		if (navNodeURI.startsWith(baseURI))
			return navNodeURI;
		return baseURI + portalRequestContext.getRequestContextPath() + "/" + accessMode + navNodeURI;
	}

	/**
	 * Gets the uRL.
	 * 
	 * @param node the node
	 * 
	 * @return the uRL
	 * 
	 * @throws Exception the exception
	 */
	public String getURL(Node node) throws Exception {
		String link = null;
		PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
		PortletRequest portletRequest = getPortletRequest();
		String portalURI = portalRequestContext.getPortalURI();
		PortletPreferences portletPreferences = portletRequest.getPreferences();
		String repository = portletPreferences.getValue(UIWCMSearchPortlet.REPOSITORY, null);
		String workspace = portletPreferences.getValue(UIWCMSearchPortlet.WORKSPACE, null);
		String baseURI = portletRequest.getScheme() + "://"	+ portletRequest.getServerName() + ":" + String.format("%s", portletRequest.getServerPort());
		String basePath = portletPreferences.getValue(UIWCMSearchPortlet.BASE_PATH, null);
		
    link = baseURI + portalURI + basePath + "/" + repository + "/" + workspace;		
    if (node.isNodeType("nt:frozenNode")){
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      Node originalNode = node.getSession().getNodeByUUID(uuid);      
      link += originalNode.getPath() + "?version=" + node.getParent().getName();
    } else {
      link += node.getPath();
    }
		
		return link;
	}

	private PortletRequest getPortletRequest() {
	  PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    return portletRequestContext.getRequest();
	}
	
	/**
	 * Gets the created date.
	 * 
	 * @param node the node
	 * 
	 * @return the created date
	 * 
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
	 * Checks if is show paginator.
	 * 
	 * @return true, if is show paginator
	 * 
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
}
