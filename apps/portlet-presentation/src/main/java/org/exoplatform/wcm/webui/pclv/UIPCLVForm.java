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
package org.exoplatform.wcm.webui.pclv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.images.RESTImagesRendererService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.webui.paginator.UICustomizeablePaginator;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * ngoc.tran@exoplatform.com Jun 23, 2009
 */
@SuppressWarnings("deprecation")
@ComponentConfigs( {
    @ComponentConfig(lifecycle = UIFormLifecycle.class,
                     events = @EventConfig(listeners = UIPCLVForm.RefreshActionListener.class)),
    @ComponentConfig(type = UICustomizeablePaginator.class,
                     events = @EventConfig(listeners = UICustomizeablePaginator.ShowPageActionListener.class)) })
public class UIPCLVForm extends UIForm {

  /** The template path. */
  private String                   templatePath;

  /** The resource resolver. */
  private ResourceResolver         resourceResolver;

  /** The ui paginator. */
  private UICustomizeablePaginator uiPaginator;

  /** The content column. */
  private String                   contentColumn;

  /** The show link. */
  private boolean                  showLink;

  /** The show header. */
  private boolean                  showHeader;

  /** The show readmore. */
  private boolean                  showReadmore;

  /** The header. */
  private String                   header;

  /** The date formatter. */
  private DateFormat               dateFormatter = null;

  /** Auto detection. */
  private String                   autoDetection;

  /** Show RSS link. */
  private String                   showRSSLink;

  /** The rss link. */
  private String                   rssLink;

  /**
   * Gets the rss link.
   *
   * @return the rss link
   */
  public String getRssLink() {
    return rssLink;
  }

  /**
   * Sets the rss link.
   *
   * @param rssLink the new rss link
   */
  public void setRssLink(String rssLink) {
    this.rssLink = rssLink;
  }

  /**
   * Instantiates a new uIPCLV form.
   */
  public UIPCLVForm() {
  }

  /**
   * Inits the.
   *
   * @param templatePath the template path
   * @param resourceResolver the resource resolver
   * @param dataPageList the data page list
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public void init(String templatePath, ResourceResolver resourceResolver, PageList dataPageList) throws Exception {

    PortletPreferences portletPreferences = getPortletPreferences();
    String paginatorTemplatePath = portletPreferences.getValue(UIPCLVPortlet.PAGINATOR_TEMPlATE_PATH,
                                                               null);
    if (dataPageList == null)
      return;
    this.templatePath = templatePath;
    this.resourceResolver = resourceResolver;
    uiPaginator = addChild(UICustomizeablePaginator.class, null, null);
    uiPaginator.setTemplatePath(paginatorTemplatePath);
    uiPaginator.setResourceResolver(resourceResolver);
    uiPaginator.setPageList(dataPageList);
    Locale locale = Util.getPortalRequestContext().getLocale();
    dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,
                                                         SimpleDateFormat.MEDIUM,
                                                         locale);
  }

  /**
   * Show refresh button.
   *
   * @return true, if successful
   */
  public boolean showRefreshButton() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String isShow = portletPreferences.getValue(UIPCLVPortlet.SHOW_REFRESH_BUTTON, null);
    return (isShow != null) ? Boolean.parseBoolean(isShow) : false;
  }

  /**
   * Show rss link.
   *
   * @return true, if successful
   */
  public boolean showRSSLink() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String isShow = portletPreferences.getValue(UIPCLVPortlet.SHOW_RSS_LINK, null);
    return (isShow != null) ? Boolean.parseBoolean(isShow) : false;
  }

  /**
   * Show rss link.
   *
   * @return true, if successful
   */
  public boolean showReadMore() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String isShow = portletPreferences.getValue(UIPCLVPortlet.SHOW_READMORE, null);
    return (isShow != null) ? Boolean.parseBoolean(isShow) : false;
  }

  /**
   * Checks if is show field.
   *
   * @param field the field
   * @return true, if is show field
   */
  public boolean isShowField(String field) {
    PortletPreferences portletPreferences = getPortletPreferences();
    String showAble = portletPreferences.getValue(field, null);
    return (showAble != null) ? Boolean.parseBoolean(showAble) : false;
  }

  /**
   * Show paginator.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean showPaginator() throws Exception {
    PortletPreferences portletPreferences = getPortletPreferences();
    String itemsPerPage = portletPreferences.getValue(UIPCLVPortlet.ITEMS_PER_PAGE, null);
    UIPCLVContainer container = getAncestorOfType(UIPCLVContainer.class);
    List<Node> nodes = container.getListNode();
    if (nodes == null)
      return false;
    int count = 0;
    for (Node node : nodes) {
      if (node != null) {
        count++;
      }
    }
    if (count > Integer.parseInt(itemsPerPage)) {
      return true;
    }
    return false;
  }

  /**
   * Gets the datetime fommatter.
   *
   * @return the datetime fommatter
   */
  public DateFormat getDatetimeFommatter() {
    return dateFormatter;
  }

  /**
   * Sets the date time format.
   *
   * @param format the new date time format
   */
  public void setDateTimeFormat(String format) {
    ((SimpleDateFormat) dateFormatter).applyPattern(format);
  }

  /**
   * Gets the uI page iterator.
   *
   * @return the uI page iterator
   */
  public UIPageIterator getUIPageIterator() {
    return uiPaginator;
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
    String title = null;
    if (node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        } catch (Exception e) {
          title = null;
        }
      }
    }
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getValue().getString();
    }
    if (title == null)
      title = node.getName();

    return title;
  }

  /**
   * Gets the summary.
   *
   * @param node the node
   * @return the summary
   * @throws Exception the exception
   */
  public String getSummary(Node node) throws Exception {
    if (node.hasProperty("exo:summary")) {
      return node.getProperty("exo:summary").getValue().getString();
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
   * Gets the illustrative image.
   *
   * @param node the node
   * @return the illustrative image
   * @throws Exception the exception
   */
  public String getIllustrativeImage(Node node) throws Exception {
    WebSchemaConfigService schemaConfigService = getApplicationComponent(WebSchemaConfigService.class);
    WebContentSchemaHandler contentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    Node illustrativeImage = null;
    RESTImagesRendererService imagesRendererService = getApplicationComponent(RESTImagesRendererService.class);
    String uri = null;
    try {
      illustrativeImage = contentSchemaHandler.getIllustrationImage(node);
      uri = imagesRendererService.generateImageURI(illustrativeImage, null);
    } catch (Exception e) {
      // You shouldn't throw popup message, because some exception often rise
      // here.
    }
    return uri;
  }

  /**
   * Generate link.
   *
   * @param node the node
   * @return the string
   * @throws Exception the exception
   */
  public String generateLink(Node node) throws Exception {
    PortletRequestContext portletRequestContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    PortletPreferences portletPreferences = portletRequest.getPreferences();
    String workspace = portletPreferences.getValue(UIPCLVPortlet.WORKSPACE, null);
    String preferenceTargetPage = portletPreferences.getValue(UIPCLVPortlet.PREFERENCE_TARGET_PAGE,
                                                              "");
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    
    String nodeName = null;
    if (node.getName().equals("jcr:frozenNode")) {
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace,
                                                                         manageableRepository);
      Node realNode = session.getNodeByUUID(uuid);
      if (realNode != null) {
        nodeName = realNode.getName();
      }
    } else {
      nodeName = node.getName();
    }
    Node newNode = ((UIPCLVContainer) getParent()).getCategoryNode().getNode(nodeName);
    String path = newNode.getPath();

    String itemPath = path.substring(path.lastIndexOf(((UIPCLVContainer) getParent()).getTaxonomyTreeName()));
    
    NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL,
                                                         Util.getPortalRequestContext()
                                                             .getPortalOwner(),
                                                         preferenceTargetPage);
    nodeURL.setResource(resource).setQueryParameterValue("path", itemPath);
    String link = nodeURL.toString();
    
    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    link = friendlyService.getFriendlyUri(link);

    return link;
  }

  /**
   * Gets the portlet preferences.
   *
   * @return the portlet preferences
   */
  private PortletPreferences getPortletPreferences() {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = context.getRequest().getPreferences();
    return portletPreferences;
  }

  /**
   * Gets the template path.
   *
   * @return the template path
   */
  public String getTemplatePath() {
    return templatePath;
  }

  /**
   * Sets the template path.
   *
   * @param templatePath the new template path
   */
  public void setTemplatePath(String templatePath) {
    this.templatePath = templatePath;
  }

  /**
   * Gets the resource resolver.
   *
   * @return the resource resolver
   */
  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  /**
   * Sets the resource resolver.
   *
   * @param resourceResolver the new resource resolver
   */
  public void setResourceResolver(ResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  /**
   * Gets the ui paginator.
   *
   * @return the ui paginator
   */
  public UICustomizeablePaginator getUiPaginator() {
    return uiPaginator;
  }

  /**
   * Sets the ui paginator.
   *
   * @param uiPaginator the new ui paginator
   */
  public void setUiPaginator(UICustomizeablePaginator uiPaginator) {
    this.uiPaginator = uiPaginator;
  }

  /**
   * Gets the content column.
   *
   * @return the content column
   */
  public String getContentColumn() {
    return contentColumn;
  }

  /**
   * Sets the content column.
   *
   * @param contentColumn the new content column
   */
  public void setContentColumn(String contentColumn) {
    this.contentColumn = contentColumn;
  }

  /**
   * Checks if is show link.
   *
   * @return true, if is show link
   */
  public boolean isShowLink() {
    return showLink;
  }

  /**
   * Sets the show link.
   *
   * @param showLink the new show link
   */
  public void setShowLink(boolean showLink) {
    this.showLink = showLink;
  }

  /**
   * Checks if is show header.
   *
   * @return true, if is show header
   */
  public boolean isShowHeader() {
    return showHeader;
  }

  /**
   * Sets the show header.
   *
   * @param showHeader the new show header
   */
  public void setShowHeader(boolean showHeader) {
    this.showHeader = showHeader;
  }

  /**
   * Checks if is show readmore.
   *
   * @return true, if is show readmore
   */
  public boolean isShowReadmore() {
    return showReadmore;
  }

  /**
   * Sets the show readmore.
   *
   * @param showReadmore the new show readmore
   */
  public void setShowReadmore(boolean showReadmore) {
    this.showReadmore = showReadmore;
  }

  /**
   * Gets the header.
   *
   * @return the header
   */
  public String getHeader() {
    return header;
  }

  /**
   * Sets the header.
   *
   * @param header the new header
   */
  public void setHeader(String header) {
    this.header = header;
  }

  /**
   * Gets the date formatter.
   *
   * @return the date formatter
   */
  public DateFormat getDateFormatter() {
    return dateFormatter;
  }

  /**
   * Sets the date formatter.
   *
   * @param dateFormatter the new date formatter
   */
  public void setDateFormatter(DateFormat dateFormatter) {
    this.dateFormatter = dateFormatter;
  }

  /**
   * Gets the auto detection.
   *
   * @return the auto detection
   */
  public String getAutoDetection() {
    return autoDetection;
  }

  /**
   * Sets the auto detection.
   *
   * @param autoDetection the new auto detection
   */
  public void setAutoDetection(String autoDetection) {
    this.autoDetection = autoDetection;
  }

  /**
   * Gets the show rss link.
   *
   * @return the show rss link
   */
  public String getShowRSSLink() {
    return showRSSLink;
  }

  /**
   * Sets the show rss link.
   *
   * @param showRSSLink the new show rss link
   */
  public void setShowRSSLink(String showRSSLink) {
    this.showRSSLink = showRSSLink;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#getTemplate()
   */
  public String getTemplate() {
    return templatePath;
  }

  /**
   * The listener interface for receiving refreshAction events. The class that
   * is interested in processing a refreshAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addRefreshActionListener<code> method. When
   * the refreshAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class RefreshActionListener extends EventListener<UIPCLVForm> {

    /*
     * (non-Javadoc)
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
     * .event.Event)
     */
    public void execute(Event<UIPCLVForm> event) throws Exception {
      UIPCLVForm contentListPresentation = event.getSource();
      UIPCLVContainer container = contentListPresentation.getParent();
      container.onRefresh(event);
    }
  }
}
