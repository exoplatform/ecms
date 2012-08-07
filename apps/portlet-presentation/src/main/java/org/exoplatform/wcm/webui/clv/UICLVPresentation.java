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
package org.exoplatform.wcm.webui.clv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.portlet.PortletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.images.RESTImagesRendererService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.paginator.UICustomizeablePaginator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wcm.webui.reader.ContentReader;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 21, 2008
 */
/**
 * The Class UICLVPresentation.
 */
@SuppressWarnings("deprecation")
@ComponentConfigs({
  @ComponentConfig(
    lifecycle = Lifecycle.class, 
    events = {
      @EventConfig(listeners = UICLVPresentation.RefreshActionListener.class),
      @EventConfig(listeners = UICLVPresentation.DeleteContentActionListener.class)      
    }
  ),
  @ComponentConfig(
    type = UICustomizeablePaginator.class, 
    events = @EventConfig(listeners = UICustomizeablePaginator.ShowPageActionListener.class)
  ) 
})

public class UICLVPresentation extends UIContainer {

	private static final String defaultScvParam = "content-id";
	
  /** The template path. */
  private String                   templatePath;

  /** The resource resolver. */
  private ResourceResolver         resourceResolver;

  /** The ui paginator. */
  private UICustomizeablePaginator uiPaginator;

  /** The date formatter. */
  private DateFormat               dateFormatter = null;
  
  /** Generic TagStyles configurable in ECM Administration */
  private Map<String, String> tagStyles = null;

  
  /**
   * Instantiates a new uICLV presentation.
   */
  public UICLVPresentation() {
  }

  /**
   * Inits the.
   * 
   * @param resourceResolver the resource resolver
   * @param dataPageList the data page list
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public void init(ResourceResolver resourceResolver, PageList dataPageList) throws Exception {

    String paginatorTemplatePath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_PAGINATOR_TEMPLATE);
    this.templatePath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_DISPLAY_TEMPLATE);

    this.resourceResolver = resourceResolver;
    uiPaginator = addChild(UICustomizeablePaginator.class, null, null);
    uiPaginator.setTemplatePath(paginatorTemplatePath);
    uiPaginator.setResourceResolver(resourceResolver);
    uiPaginator.setPageList(dataPageList);
    Locale locale = Util.getPortalRequestContext().getLocale();
    dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, locale);
  }
  
  public List<CategoryBean> getCategories() throws Exception {
    String fullPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
    return getCategories(fullPath, "exo:taxonomy", 0);

  }

  public List<CategoryBean> getCategories(String primaryType) throws Exception {
  	String fullPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
  	return getCategories(fullPath, primaryType, 0);
  	
  }
  
  public List<CategoryBean> getCategories(boolean withChildren) throws Exception {
  	String fullPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
  	return getCategories(fullPath, "exo:taxonomy", 0, withChildren);
  }

  
  public List<CategoryBean> getCategories(String fullPath, String primaryType, int depth) throws Exception {
  	return getCategories(fullPath,  primaryType,  depth, true);
  }
  
  public List<CategoryBean> getCategories(String fullPath, String primaryType, int depth, boolean withChildren) throws Exception {
    if (fullPath==null || fullPath.length()==0) {
    	return null;
    }
    WCMComposer wcmComposer = getApplicationComponent(WCMComposer.class);
    HashMap<String, String> filters = new HashMap<String, String>();
    filters.put(WCMComposer.FILTER_MODE, Utils.getCurrentMode());
    
    String orderType = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_TYPE);
    String orderBy = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_BY);
//    orderType = "ASC";
//    orderBy = "jcr:path";
    filters.put(WCMComposer.FILTER_ORDER_BY, orderBy);
    filters.put(WCMComposer.FILTER_ORDER_TYPE, orderType);
    filters.put(WCMComposer.FILTER_LANGUAGE, Util.getPortalRequestContext().getLocale().getLanguage());
//    filters.put(WCMComposer.FILTER_RECURSIVE, "true");
    filters.put(WCMComposer.FILTER_PRIMARY_TYPE, primaryType);

    String clvBy = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_CLV_BY);
    
    /* Allows us to know the current selected node */
	  String paramPath = Util.getPortalRequestContext().getRequestParameter(clvBy);

    NodeLocation nodeLocation = NodeLocation.getNodeLocationByExpression(fullPath);
    
    List<Node> nodes = wcmComposer.getContents(nodeLocation.getRepository(), nodeLocation.getWorkspace(), nodeLocation.getPath(), filters, WCMCoreUtils.getUserSessionProvider());
    List<CategoryBean> categories = new LinkedList<CategoryBean>();
    for (Node node:nodes) {
    	String title = getTitle(node);
    	String url = getCategoryURL(node);
    	String path = node.getPath();
    	long total = (node.hasProperty("exo:total"))?node.getProperty("exo:total").getValue().getLong():0;
    	boolean isSelected = paramPath!=null&&paramPath.endsWith(path);
    	CategoryBean cat = new CategoryBean(node.getName(), node.getPath(), title, url, isSelected, depth, total);
    	NodeLocation catLocation = NodeLocation.getNodeLocationByNode(node);
      if (withChildren) {
        List<CategoryBean> childs = getCategories(catLocation.toString(), primaryType, depth+1);
        if (childs!=null && childs.size()>0)
          cat.setChilds(childs);
      }
      categories.add(cat);
    }
    return categories;
  }
  
  public String getTagHtmlStyle(long tagCount) throws Exception {
  	for (Entry<String, String> entry : getTagStyles().entrySet()) {
  		if (checkTagRate(tagCount, entry.getKey()))
	  		return entry.getValue();
  	}
  	return "";
  }

  private Map<String ,String> getTagStyles() throws Exception {
  	if (tagStyles==null) {
	    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
	    String workspace = "dms-system";
	    tagStyles = new HashMap<String ,String>() ;
	    for(Node tag : folksonomyService.getAllTagStyle("repository", workspace)) {
	      tagStyles.put(tag.getProperty("exo:styleRange").getValue().getString(),
	      						 tag.getProperty("exo:htmlStyle").getValue().getString());
	    }
  	}
    return tagStyles ;
  }
  
  private boolean checkTagRate(long numOfDocument, String range) throws Exception {
    String[] vals = StringUtils.split(range ,"..") ;    
    int minValue = Integer.parseInt(vals[0]) ;
    int maxValue ;
    if(vals[1].equals("*")) {
      maxValue = Integer.MAX_VALUE ;
    }else {
      maxValue = Integer.parseInt(vals[1]) ;
    }
    if(minValue <=numOfDocument && numOfDocument <maxValue ) return true ;    
    return false ;
  }
  
  
  /**
   * Gets the uRL.
   * 
   * @param node the node
   * @return the uRL
   * @throws Exception the exception
   */
  public String getCategoryURL(Node node) throws Exception {
    String link = null;
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String portalURI = portalRequestContext.getPortalURI();
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node);
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":" + 
                     String.format("%s", portletRequest.getServerPort());
    String basePath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_TARGET_PAGE);
    String clvBy = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_CLV_BY);
    if (clvBy == null || clvBy.length() == 0)
    	clvBy = UICLVPortlet.DEFAULT_SHOW_CLV_BY;
    
    String params =  nodeLocation.getRepository() + ":" + nodeLocation.getWorkspace() +":"+ node.getPath();
    link = baseURI + portalURI + basePath + "?" + clvBy + "=" + Text.escape(params, '%', true, " :");
    
    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    link = friendlyService.getFriendlyUri(link);
    
    return link;
  }

  /**
   * Checks if is show field.
   * 
   * @param field the field
   * @return true, if is show field
   */
  public boolean isShowField(String field) {
    String visible = Utils.getPortletPreference(field);
    return (visible != null) ? Boolean.parseBoolean(visible) : false;
  }

  /**
   * Show paginator.
   * 
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean showPaginator() throws Exception {
    String itemsPerPage = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE);
    int totalItems = uiPaginator.getTotalItems();
    if (totalItems > Integer.parseInt(itemsPerPage)) {
      return true;
    }
    return false;
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
   * @see org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return resourceResolver;
  }

  /**
   * Gets the title.
   * 
   * @param node the node
   * @return the title
   * @throws Exception the exception
   */
  public String getTitle(Node node) throws Exception {
	  String title = null;
	  if (node.hasProperty("exo:title")) {
	  	title = node.getProperty("exo:title").getValue().getString();
	  } else if (node.hasNode("jcr:content")) {
		  Node content = node.getNode("jcr:content");
		  if (content.hasProperty("dc:title")) {
		    try {
		      title = content.getProperty("dc:title").getValues()[0].getString();
		    } catch(Exception ex) {}
		  }
	  }
	  if (title==null) {
	  	if (node.isNodeType("nt:frozenNode")){
	  		String uuid = node.getProperty("jcr:frozenUuid").getString();
	  		Node originalNode = node.getSession().getNodeByUUID(uuid);
	  		title = originalNode.getName();
	  	} else {
	  		title = node.getName();
	  	}
	  	
	  }
	  return ContentReader.getXSSCompatibilityContent(title);
  }

  /**
   * Gets the summary.
   * 
   * @param node the node
   * @return the summary
   * @throws Exception the exception
   */
  public String getSummary(Node node) throws Exception {
	  String desc = null;
	  if (node.hasProperty("exo:summary")) {
		  desc = node.getProperty("exo:summary").getValue().getString();
	  } else if (node.hasNode("jcr:content")) {
		  Node content = node.getNode("jcr:content");
		  if (content.hasProperty("dc:description")) {
		    try {
		      desc = content.getProperty("dc:description").getValues()[0].getString();
		    } catch(Exception ex) {
		      return null;
		    }
		  }
	  }
	  return desc;
  }

  /**
   * Gets the uRL.
   * 
   * @param node the node
   * @return the uRL
   * @throws Exception the exception
   */
  public String getURL(Node node) throws Exception {
    String link = null;
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String portalURI = portalRequestContext.getPortalURI();
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node);
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":" + String.format("%s", portletRequest.getServerPort());
    String basePath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_TARGET_PAGE);
    String scvWith = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_SCV_WITH);
    if (scvWith == null || scvWith.length() == 0)
    	scvWith = UICLVPortlet.DEFAULT_SHOW_SCV_WITH;
    if (node.isNodeType("nt:frozenNode")){
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      Node originalNode = node.getSession().getNodeByUUID(uuid);
      link = baseURI + portalURI + basePath + "?" + scvWith + "=/" + nodeLocation.getRepository() + "/" + 
                       nodeLocation.getWorkspace() + Text.escape(originalNode.getPath(), '%', true, " ");
    } else {
      link = baseURI + portalURI + basePath + "?" + scvWith + "=/" + nodeLocation.getRepository() + "/" + 
                       nodeLocation.getWorkspace() + Text.escape(node.getPath(), '%', true, " ");
    }
    
    String fullPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPathParamValue();
    if (fullPath!=null) {
        String clvBy = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_CLV_BY);
      link += "&"+clvBy+"="+Text.escape(fullPath, '%', true, " :");
    }
    
    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    link = friendlyService.getFriendlyUri(link);
    
    return link;
  }

  /**
   * Gets the webdav url.
   * 
   * @param node the node
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavURL(Node node) throws Exception {
  	PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
  	PortletRequest portletRequest = portletRequestContext.getRequest();
  	NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node);
  	String repository = nodeLocation.getRepository();
  	String workspace = nodeLocation.getWorkspace();
  	String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":" + String.format("%s", portletRequest.getServerPort());
  	
    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    String link = "#";//friendlyService.getFriendlyUri(link);

  	String portalName = PortalContainer.getCurrentPortalContainerName();
  	String restContextName = PortalContainer.getCurrentRestContextName();
  	if (node.isNodeType("nt:frozenNode")){
  		String uuid = node.getProperty("jcr:frozenUuid").getString();
  		Node originalNode = node.getSession().getNodeByUUID(uuid);  
  		link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/" + workspace + originalNode.getPath() + "?version=" + node.getParent().getName();
  	} else {
  		link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/" + workspace + node.getPath();
  	}
  	
  	return friendlyService.getFriendlyUri(link);
	  
  }
  
  /**
   * Gets the author.
   * 
   * @param node the node
   * @return the author
   * @throws Exception the exception
   */
  public String getAuthor(Node node) throws Exception {
    if (node.hasProperty("exo:owner")) {
      String ownerId = node.getProperty("exo:owner").getValue().getString();
      return ownerId;
    }
    return null;
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
   * Gets the modified date.
   * 
   * @param node the node
   * @return the modified date
   * @throws Exception the exception
   */
  public String getModifiedDate(Node node) throws Exception {
    if (node.hasProperty("exo:dateModified")) {
      Calendar calendar = node.getProperty("exo:dateModified").getValue().getDate();
      return dateFormatter.format(calendar.getTime());
    }
    return null;
  }

  /**
   * Gets the content icon.
   * 
   * @param node the node
   * @return the content icon
   */
  public String getContentIcon(Node node) {
  	try {
    	if (node.isNodeType("nt:frozenNode")){
    		String uuid = node.getProperty("jcr:frozenUuid").getString();
    		Node originalNode = node.getSession().getNodeByUUID(uuid);  		
    		return "Icon16x16 default16x16Icon " + org.exoplatform.ecm.webui.utils.Utils.getNodeTypeIcon(originalNode, "16x16Icon");
    	} else {
    		return "Icon16x16 default16x16Icon "+org.exoplatform.ecm.webui.utils.Utils.getNodeTypeIcon(node, "16x16Icon");
    	}
  		
  	} catch (RepositoryException e) {
  	  Utils.createPopupMessage(this, "UIMessageBoard.msg.get-content-icon", null, ApplicationMessage.ERROR);
  	}
    return null;
  }

  public String getHeader() {
  	String header = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_HEADER);
  	UICLVContainer clvContainer = this.getAncestorOfType(UICLVContainer.class);
  	boolean isAutoDetect = Boolean.parseBoolean(Utils.getPortletPreference(UICLVPortlet.PREFERENCE_AUTOMATIC_DETECTION));
  	String contextualFolder = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER);
  	boolean isContextualEnable = UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE.equals(contextualFolder);
    String clvBy = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_CLV_BY);
	  String paramPath = Util.getPortalRequestContext().getRequestParameter(clvBy);

  	if (!isAutoDetect || !clvContainer.isModeByFolder() || paramPath==null || !isContextualEnable) return header;
  	
  	try {
  		
  	  Node folderNode =NodeLocation.getNodeByExpression(this.getAncestorOfType(UICLVPortlet.class).getFolderPath()); 
			if (folderNode.hasProperty(org.exoplatform.ecm.webui.utils.Utils.EXO_TITLE)) {
				String folderTitle = folderNode.getProperty(org.exoplatform.ecm.webui.utils.Utils.EXO_TITLE).getString();
				if (folderTitle != null && folderTitle.length() > 0)
					header = folderTitle;
			} else {
				header = folderNode.getName();
			}
		} catch (RepositoryException repositoryException) {		  
		} catch (Exception e) {
		}
		return header;
  }
  
  public UIPageIterator getUIPageIterator() {
    return uiPaginator;
  }
  
  @SuppressWarnings("unchecked")
  public List getCurrentPageData() throws Exception {
    return uiPaginator.getCurrentPageData();
  }
  
  public void setDateTimeFormat(String format) {
    ((SimpleDateFormat) dateFormatter).applyPattern(format);
  }
  
  public String getEditLink(Node node, boolean isEditable, boolean isNew) {
	  return Utils.getEditLink(node, isEditable, isNew);
  }
  
  public boolean isShowEdit(Node node) {
  	if (Utils.isShowQuickEdit()) {
  		try {
  			Node parent = node.getParent();
  			((ExtendedNode)node).checkPermission(PermissionType.SET_PROPERTY);
  			((ExtendedNode)parent).checkPermission(PermissionType.ADD_NODE);
  		} catch (Exception e) {
  			return false;
  		}
  		return true;
  	} else {
  		return false;
  	}
  }
  
  /**
   * Gets the illustrative image.
   * 
   * @param node the node
   * @return the illustrative image
   */
  public String getIllustrativeImage(Node node) {
    WebSchemaConfigService schemaConfigService = getApplicationComponent(WebSchemaConfigService.class);
    WebContentSchemaHandler contentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    RESTImagesRendererService imagesRendererService = getApplicationComponent(RESTImagesRendererService.class);
    Node illustrativeImage = null;
    String uri = null;
    try{
      illustrativeImage = contentSchemaHandler.getIllustrationImage(node);
      uri = imagesRendererService.generateImageURI(illustrativeImage, null);
    } catch(PathNotFoundException ex) {
      // We don't do anything here because so many documents doesn't have illustration image
    } catch (Exception e) {
      e.printStackTrace();
    }
    return uri;
  }

  public boolean isShowRssLink() {
  	return isShowField(UICLVPortlet.PREFERENCE_SHOW_RSSLINK) 
  				 && 
  				 (this.getAncestorOfType(UICLVPortlet.class).getFolderPathParamValue() != null ||
  					UICLVPortlet.DISPLAY_MODE_AUTOMATIC.equals(Utils.getPortletPreference(UICLVPortlet.PREFERENCE_DISPLAY_MODE)));
  }
  
	/**
	 * Gets the rss link.
	 * 
	 * @return the rss link
	 */
	public String getRssLink() {
		String portal = PortalContainer.getCurrentPortalContainerName();
		String rest = PortalContainer.getCurrentRestContextName();
		String server = Util.getPortalRequestContext().getRequest().getRequestURL().toString();
		int lastIndex = server.indexOf(portal);
		server = server.substring(0, lastIndex-1) + Util.getPortalRequestContext().getPortalURI();
		String fullPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPathParamValue();
		if (fullPath == null || fullPath.length() == 0)
			fullPath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH);
		if (fullPath == null)
		  return "/"+portal+"/"+rest+      
      "&siteName=" + Util.getUIPortal().getOwner() + 
      "&orderBy=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_BY) +
      "&orderType=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_TYPE) +
      "&detailPage=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_TARGET_PAGE) + 
      "&detailParam=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_SCV_WITH);
		String[] repoWsPath = fullPath.split(":");
		return  "/"+portal+"/"+rest+
						"/feed/rss?repository=" + repoWsPath[0] + 
						"&workspace=" + repoWsPath[1] + 
						"&server=" + server + 
						"&siteName=" + Util.getUIPortal().getOwner() + 
						"&folderPath=" + repoWsPath[2] +
						"&orderBy=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_BY) +
						"&orderType=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ORDER_TYPE) + 
//						"&title="
//						"&desc="My%20description
						"&detailPage=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_TARGET_PAGE) + 
						"&detailParam=" + Utils.getPortletPreference(UICLVPortlet.PREFERENCE_SHOW_SCV_WITH);
  }  
  
	
	/**
	 * This method will put the mandatory html code to manage QuickEdit mode
	 * 
	 * @param cssClass
	 * @param viewNode
	 * @return
	 * @throws Exception
	 */
	public String addQuickEditDiv(String cssClass, Node viewNode) throws Exception {
		StringBuffer sb = new StringBuffer();
		String contentEditLink = getEditLink(viewNode, true, false);
		String contentDeleteLink = event("DeleteContent", NodeLocation.getExpressionByNode(viewNode));
		String hoverClass = Utils.isShowQuickEdit() ? " ContainerHoverClassInner" : "";		
		PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
		sb.append("<div class=\""+cssClass+"\" onmouseover=\"this.className  = '"+cssClass+" "+hoverClass+"' \" onmouseout=\"this.className = '"+cssClass+"' \">");
		if (Utils.isShowQuickEdit()) {
			sb.append("	<div class=\"EdittingContent\" style=\" z-index: 1\">");
			sb.append("		<div class=\"EdittingToolBar \" >");
			sb.append("			<div class=\"EdittingToolBarL\">");
			sb.append("				<div class=\"EdittingToolBarC clearfix\">");
			if (Utils.isShowDelete(viewNode)) {
			    String strDeleteBundle="Delete";
			    try {
			      strDeleteBundle = portletRequestContext.getApplicationResourceBundle().getString("UICLVPresentation.action.delete");
			    } catch (MissingResourceException e) { }
				sb.append("					<div style=\"float: right\">");
				sb.append("                     <a href=\""+contentDeleteLink+"\" title=\"" + strDeleteBundle + "\"class=\"CloseContentIcon\" >");
				sb.append("						  &nbsp;");
				sb.append("						</a>");  
				sb.append("					</div>");
			} 

			if(isShowEdit(viewNode) && !LockUtil.isLocked(viewNode)){
			   String strEditBundle="Delete";
			   try {
			     strEditBundle = portletRequestContext.getApplicationResourceBundle().getString("UICLVPresentation.action.edit");
			   } catch (MissingResourceException e) { }
				sb.append("					<div style=\"float: right\">");
				sb.append("						<a onclick = 'eXo.ecm.CLV.addURL(this)' href=\""+contentEditLink+"\" title=\"" + strEditBundle + "\" class=\"EditContentIcon\" >");
				sb.append("						  &nbsp;");
				sb.append("						</a>");    
				sb.append("					</div>");
			} else {
				sb.append("					<div style=\"float: right\">");
				sb.append("						<div title=\"lock\" class=\"IconLocked\" >");
				sb.append("						  &nbsp;");
				sb.append("						</div>");    
				sb.append("					</div>");		
			}
			if (viewNode.hasProperty("publication:currentState")) {
			  String state = viewNode.getProperty("publication:currentState").getValue().getString(); 
			  try {
			    state = portletRequestContext.getApplicationResourceBundle().getString("PublicationStates."+state);
			  } catch (MissingResourceException e) { }
			  sb.append("         <div class=\"EdittingCurrentState\" style=\"float: right\">");        
			  sb.append(""+state);
			  sb.append("         </div>");
			}
			sb.append("				</div>");
			sb.append("			</div>");
			sb.append("		</div>");
			sb.append("	</div>");
		}

		return sb.toString();
	}		
	
  /**
   * The listener interface for receiving refreshAction events.
   * The class that is interested in processing a refreshAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRefreshActionListener<code> method. When
   * the refreshAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class RefreshActionListener extends EventListener<UICLVPresentation> {
    /*
     * (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVPresentation> event) throws Exception {
      UICLVPresentation clvPresentation = event.getSource();
      clvPresentation.getAncestorOfType(UICLVContainer.class).onRefresh(event);
    }
  }

  public static class DeleteContentActionListener extends EventListener<UICLVPresentation> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVPresentation> event) throws Exception {
      UICLVPresentation contentListPresentation = event.getSource();
      String itemPath = event.getRequestContext().getRequestParameter(OBJECTID);
      Node node = NodeLocation.getNodeByExpression(itemPath);
      Node parent = node.getParent();
      node.remove();
      parent.getSession().save();
      event.getRequestContext().addUIComponentToUpdateByAjax(contentListPresentation);
      Utils.createPopupMessage(contentListPresentation, "UICLVPresentation.msg.delete-content-successfull", null, ApplicationMessage.INFO);      
    }
  }
  
}
