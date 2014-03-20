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
package org.exoplatform.wcm.webui;

import java.io.InputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.navigation.NavigationUtils;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.core.UIPopupWindow;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import com.ibm.icu.text.Transliterator;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.phamvu@exoplatform.com
 * Oct 23, 2008
 */
public class Utils {

  /** The Quick edit attribute for HTTPSession */
  public static final String TURN_ON_QUICK_EDIT = "turnOnQuickEdit";

  private static final String SQL_PARAM_PATTERN = "\\$\\{([^\\$\\{\\}])+\\}";
  
  private static final String JCR_CONTENT = "jcr:content";

  private static final String JCR_DATA = "jcr:data";

  private static final String JCR_MIMETYPE = "jcr:mimeType";  

  private static final String NT_FILE = "nt:file";

  private static final String NT_UNSTRUCTURED = "nt:unstructured";
  
  private static final String DOCUMENTS_ACTIVITY = "documents";

  /**
   * Checks if is edits the portlet in create page wizard.
   * @return true, if is edits the portlet in create page wizard
   */
  public static boolean isEditPortletInCreatePageWizard() {
    UIPortalApplication portalApplication = Util.getUIPortalApplication();
    UIMaskWorkspace uiMaskWS = portalApplication.getChildById(UIPortalApplication.UI_MASK_WS_ID);
    // show maskworkpace is being in Portal page edit mode
    if (uiMaskWS.getWindowWidth() > 0 && uiMaskWS.getWindowHeight() < 0)
      return true;
    return false;
  }

  /**
   * Checks if is quick editmode.
   *
   * @param container the current container
   * @param popupWindowId the popup window id
   *
   * @return true, if is quick editmode
   */
  public static boolean isQuickEditMode(UIContainer container, String popupWindowId) {
    UIPopupContainer popupContainer = getPopupContainer(container);
    if (popupContainer == null)
      return false;
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    if (popupWindow == null)
      return false;
    return true;
  }

  /**
   * Check if the portlet current mode is view mode or not
   * @param pContext The request context of a portlet
   *
   * @return return true if current portlet mode is view mode; otherwise return false
   */
  public static boolean isPortletViewMode(PortletRequestContext pContext) {
    return PortletMode.VIEW.equals(pContext.getApplicationMode());
  }

  public static boolean isPortalEditMode() {
    return Util.getUIPortalApplication().getModeState() != UIPortalApplication.NORMAL_MODE;
  }

  public static String getRealPortletId(PortletRequestContext portletRequestContext) {
    String portletId = portletRequestContext.getWindowId();
    int modeState = Util.getUIPortalApplication().getModeState();
    switch (modeState) {
    case UIPortalApplication.NORMAL_MODE:
      return portletId;
    case UIPortalApplication.APP_BLOCK_EDIT_MODE:
      return "UIPortlet-" + portletId;
    case UIPortalApplication.APP_VIEW_EDIT_MODE:
      return "EditMode-" + portletId;
    default:
      return null;
    }
  }

  /**
   * Can edit current portal.
   *
   * @param remoteUser the remote user
   * @return true, if successful
   * @throws Exception the exception
   */
  public static boolean canEditCurrentPortal(String remoteUser) throws Exception {
    if (remoteUser == null)
      return false;
    IdentityRegistry identityRegistry = Util.getUIPortalApplication()
                                            .getApplicationComponent(IdentityRegistry.class);
    Identity identity = identityRegistry.getIdentity(remoteUser);
    if (identity == null)
      return false;
    UIPortal uiPortal = Util.getUIPortal();
    // this code only work for single edit permission
    String editPermission = uiPortal.getEditPermission();
    MembershipEntry membershipEntry = MembershipEntry.parse(editPermission);
    return identity.isMemberOf(membershipEntry);
  }

  /**
   * Clean string.
   *
   * @param str the str
   * @return the string
   */
  public static String cleanString(String str) {
    Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
    str = accentsconverter.transliterate(str);
    // the character ? seems to not be changed to d by the transliterate
    // function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
    // delete special character
    for (int i = 0; i < cleanedStr.length(); i++) {
      char c = cleanedStr.charAt(i);
      if (c == ' ') {
        if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
          cleanedStr.deleteCharAt(i--);
        } else {
          c = '-';
          cleanedStr.setCharAt(i, c);
        }
        continue;
      }
      if (i > 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        cleanedStr.deleteCharAt(i--);
        continue;
      }
      if (i > 0 && c == '-' && cleanedStr.charAt(i - 1) == '-')
        cleanedStr.deleteCharAt(i--);
    }
    return cleanedStr.toString().toLowerCase();
  }

  /**
   * Refresh whole portal by AJAX.
   *
   * @param context the portlet request context
   */
  public static void updatePortal(PortletRequestContext context) {
    UIPortalApplication portalApplication = Util.getUIPortalApplication();
    PortalRequestContext portalRequestContext = (PortalRequestContext) context.getParentAppRequestContext();
    UIWorkingWorkspace uiWorkingWS = portalApplication.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
    portalRequestContext.addUIComponentToUpdateByAjax(uiWorkingWS);
    portalRequestContext.ignoreAJAXUpdateOnPortlets(true);
  }

  /**
   * Gets the viewable node by WCMComposer (depends on site mode)
   *
   * @param repository the repository's name
   * @param workspace the workspace's name
   * @param nodeIdentifier the node's path or node's UUID
   * @return the viewable node. Return <code>null</code> if
   *         <code>nodeIdentifier</code> is invalid
   * @see #getViewableNodeByComposer(String repository, String workspace, String
   *      nodeIdentifier, String version) getViewableNodeByComposer()
   */
  public static Node getViewableNodeByComposer(String repository,
                                               String workspace,
                                               String nodeIdentifier) {
    return getViewableNodeByComposer(repository, workspace, nodeIdentifier, null);
  }

  /**
   * Gets the viewable node by WCMComposer (depends on site mode)
   *
   * @param repository the repository's name
   * @param workspace the workspace's name
   * @param nodeIdentifier the node's path or node's UUID
   * @param version the base version (e.g. <code>WCMComposer.BASE_VERSION</code>
   *          )
   * @return the viewable node. Return <code>null</code> if
   *         <code>nodeIdentifier</code> is invalid
   * @see #getViewableNodeByComposer(String repository, String workspace, String
   *      nodeIdentifier) getViewableNodeByComposer()
   * @see WCMComposer
   */
  public static Node getViewableNodeByComposer(String repository,
                                               String workspace,
                                               String nodeIdentifier,
                                               String version) {
    return getViewableNodeByComposer(repository, workspace, nodeIdentifier, version, WCMComposer.VISIBILITY_USER);
  }

  /**
   * Gets the viewable node by WCMComposer (depends on site mode)
   *
   * @param repository the repository's name
   * @param workspace the workspace's name
   * @param nodeIdentifier the node's path or node's UUID
   * @param version the base version (e.g. <code>WCMComposer.BASE_VERSION</code>
   *          )
   * @param cacheVisibility the visibility of cache
   *
   * @return the viewable node. Return <code>null</code> if
   *         <code>nodeIdentifier</code> is invalid
   * @see #getViewableNodeByComposer(String repository, String workspace, String
   *      nodeIdentifier) getViewableNodeByComposer()
   * @see WCMComposer
   */
  public static Node getViewableNodeByComposer(String repository,
                                               String workspace,
                                               String nodeIdentifier,
                                               String version,
                                               String cacheVisibility) {
    try {
      HashMap<String, String> filters = new HashMap<String, String>();
      StringBuffer filterLang = new StringBuffer(Util.getPortalRequestContext()
                                                     .getLocale()
                                                     .getLanguage());
      String country = Util.getPortalRequestContext().getLocale().getCountry();
      if (country != null && country.length() > 0) {
        filterLang.append("_").append(country);
      }
      filters.put(WCMComposer.FILTER_LANGUAGE, filterLang.toString());
      filters.put(WCMComposer.FILTER_MODE, Utils.getCurrentMode());
      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
      PortletMode portletMode = portletRequestContext.getApplicationMode();
      filters.put(WCMComposer.PORTLET_MODE, portletMode.toString());
      if (version != null)
        filters.put(WCMComposer.FILTER_VERSION, version);
      filters.put(WCMComposer.FILTER_VISIBILITY, cacheVisibility);
      return WCMCoreUtils.getService(WCMComposer.class)
                         .getContent(workspace,
                                 Text.escapeIllegalJcrChars(nodeIdentifier),
                                     filters,
                                     WCMCoreUtils.getUserSessionProvider());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the current mode of the site
   *
   * @return the current mode (e.g. <code>WCMComposer.MODE_EDIT</code>)
   * @see WCMComposer
   */
  public static String getCurrentMode() {
    Object isQuickEditable = Util.getPortalRequestContext()
                                 .getRequest()
                                 .getSession()
                                 .getAttribute(TURN_ON_QUICK_EDIT);
    if (isQuickEditable == null)
      return WCMComposer.MODE_LIVE;
    boolean turnOnQuickEdit = Boolean.parseBoolean(isQuickEditable.toString());
    return turnOnQuickEdit ? WCMComposer.MODE_EDIT : WCMComposer.MODE_LIVE;
  }

  /**
   * Check if the current mode is live mode or not
   *
   * @return return true if current mode is WCMComposer.MODE_LIVE; otherwise
   *         false.
   */
  public static boolean isLiveMode() {
    return WCMComposer.MODE_LIVE.equals(getCurrentMode());
  }

  /**
   * Check if the content is draft and current mode of the site is edit mode
   *
   * @param content the content node.
   * @return true, the content is draft and current mode is edit mode, otherwise
   *         return false.
   */
  public static boolean isShowDraft(Node content) {
    if (content == null)
      return false;
    try {
      if (content.isNodeType("nt:frozenNode"))
        return false;
      WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
      String contentState = wcmPublicationService.getContentState(content);
      boolean isDraftContent = false;
      if (PublicationDefaultStates.DRAFT.equals(contentState))
        isDraftContent = true;
      boolean isShowDraft = false;
      if (WCMComposer.MODE_EDIT.equals(getCurrentMode()))
        isShowDraft = true;
      return isDraftContent && isShowDraft;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Check if the current mode of the site is edit mode
   *
   * @return true, if current mode is edit mode
   */
  public static boolean isShowQuickEdit() {
    try {
      boolean isEditMode = false;
      if (WCMComposer.MODE_EDIT.equals(getCurrentMode()))
        isEditMode = true;
      return isEditMode;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Check if the user can delete the current node
   *
   * @return true, if current mode is edit mode
   * @throws RepositoryException
   * @throws AccessControlException
   */
  public static boolean isShowDelete(Node content) throws AccessControlException, RepositoryException {
    boolean isEditMode = false;
    if (WCMComposer.MODE_EDIT.equals(getCurrentMode())) isEditMode = true;
    ((ExtendedNode) content).checkPermission(PermissionType.SET_PROPERTY);
    ((ExtendedNode) content).checkPermission(PermissionType.ADD_NODE);
    ((ExtendedNode) content).checkPermission(PermissionType.REMOVE);
    return isEditMode;
  }

  /**
   * Check if the content is editable and current mode of the site is edit mode
   *
   * @param content the content node
   * @return true if there is no content if the content is editable and current
   *         mode is edit mode
   */
  public static boolean isShowQuickEdit(Node content) {
    if (content == null)
      return true;
    try {
      boolean isEditMode = false;
      if (WCMComposer.MODE_EDIT.equals(getCurrentMode())
          || Util.getUIPortalApplication().getModeState() != UIPortalApplication.NORMAL_MODE)
        isEditMode = true;
      ((ExtendedNode) content).checkPermission(PermissionType.SET_PROPERTY);
      ((ExtendedNode) content).checkPermission(PermissionType.ADD_NODE);
      ((ExtendedNode) content).checkPermission(PermissionType.REMOVE);
      return isEditMode;
    } catch (Exception e) {
      return false;
    }
  }

  public static String getEditLink(Node node, boolean isEditable, boolean isNew) {
    try {
      ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
      String nodeWorkspace = node.getSession().getWorkspace().getName();
      String driveWorkspace = nodeWorkspace;
      List<DriveData> listDrive = manageDriveService.getAllDrives();
      for(DriveData drive : listDrive) {
        if(drive.getWorkspace().equals(nodeWorkspace) && node.getPath().startsWith(drive.getHomePath())) {
          driveWorkspace = drive.getName();
          break;
        }
      }
      String itemPath = driveWorkspace + node.getPath();
      return getEditLink(itemPath, isEditable, isNew);
    } catch (RepositoryException re) {
      return null;
    } catch(Exception e) {
      return null;
    }
  }
  
  public static String getActivityEditLink(Node node) {
    try {
      String itemPath = node.getSession().getWorkspace().getName() + node.getPath();
      return getActivityEditLink(itemPath);
    } catch (RepositoryException e) {
      return null;
    }
  }

  /**
   * Creates a restfull compliant link to the editor for editing a content,
   * adding a content or managing contents. Example : Add Content : isEditable =
   * false, isNew = true, itemPath = the parent folder path Edit Content :
   * isEditable = true, isNew = false, itemPath = the content path Manage
   * Contents = isEditable = false, isNew = false, itemPath = the folder path
   *
   * @param itemPath
   * @param isEditable
   * @param isNew
   * @return
   */
  public static String getEditLink(String itemPath, boolean isEditable, boolean isNew) {
    PortalRequestContext pContext = Util.getPortalRequestContext();
    String backto = pContext.getRequestURI();
    WCMConfigurationService configurationService = Util.getUIPortalApplication()
                                                       .getApplicationComponent(WCMConfigurationService.class);
    String editorPageURI = configurationService.getRuntimeContextParam(
                               isEditable || isNew ? WCMConfigurationService.EDITOR_PAGE_URI :
                                                     WCMConfigurationService.SITE_EXPLORER_URI);
    UserNode editorNode = getEditorNode(editorPageURI);

    if (editorNode == null) {
      return "";
    }

    NodeURL nodeURL = pContext.createURL(NodeURL.TYPE);
    nodeURL.setNode(editorNode).setQueryParameterValue("path", itemPath);
    if (isEditable) {
      nodeURL.setQueryParameterValue("edit", "true");
    }
    if (isNew) {
      nodeURL.setQueryParameterValue("addNew", "true");
    }
    nodeURL.setQueryParameterValue(org.exoplatform.ecm.webui.utils.Utils.URL_BACKTO, backto);

    return nodeURL.toString();
  }
  
  public static String getActivityEditLink(String itemPath) {
    PortalRequestContext pContext = Util.getPortalRequestContext();    
    String siteType = pContext.getSiteKey().getType().getName();
    String backto = pContext.getRequestURI();
    WCMConfigurationService configurationService = Util.getUIPortalApplication()
    		.getApplicationComponent(WCMConfigurationService.class);
    
    String editorPageURI = null;
    if(siteType.equals(PortalConfig.PORTAL_TYPE))
      editorPageURI = configurationService.getRuntimeContextParam(WCMConfigurationService.EDIT_PAGE_URI);
    else if(siteType.equals(PortalConfig.GROUP_TYPE)) {
      StringBuffer sb = new StringBuffer();      
    	editorPageURI = pContext.getSiteName();
    	editorPageURI = editorPageURI.substring(editorPageURI.lastIndexOf("/")+1, editorPageURI.length());
    	sb.append(editorPageURI).append("/").append(DOCUMENTS_ACTIVITY);
    	editorPageURI = sb.toString();
    }
    UserNode editorNode = getEditorNode(editorPageURI, siteType);

    if (editorNode == null) {
      return "";
    }

    NodeURL nodeURL = pContext.createURL(NodeURL.TYPE);
    nodeURL.setNode(editorNode);
    nodeURL.setQueryParameterValue("path", itemPath);
    nodeURL.setQueryParameterValue("edit", "true");   
    nodeURL.setQueryParameterValue(org.exoplatform.ecm.webui.utils.Utils.URL_BACKTO, backto);

    return nodeURL.toString();
  }
  
  private static UserNode getEditorNode(String editorPageURI, String siteType) {
    UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
    List<UserNavigation> allNavs = userPortal.getNavigations();

    for (UserNavigation nav : allNavs) {
      if (nav.getKey().getType().getName().equalsIgnoreCase(siteType)) {
        UserNode userNode = userPortal.resolvePath(nav, null, editorPageURI);
        if (userNode != null) {
          return userNode;
        }
      }
    }
    return null;
  }

  private static UserNode getEditorNode(String editorPageURI) {
    UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
    List<UserNavigation> allNavs = userPortal.getNavigations();

    for (UserNavigation nav : allNavs) {
      if (nav.getKey().getType().equals(SiteType.GROUP)) {
        UserNode userNode = userPortal.resolvePath(nav, null, editorPageURI);
        if (userNode != null) {
          return userNode;
        }
      }
    }
    return null;
  }

  /**
   * Creates the popup window. Each portlet have a <code>UIPopupContainer</code>
   * . <br/>
   * Every <code>UIPopupWindow</code> created by this method is belong to this
   * container.
   *
   * @param container the current container
   * @param component the component which will be display as a popup
   * @param popupWindowId the popup's ID
   * @param width the width of the popup
   * @throws Exception the exception
   */
  public static void createPopupWindow(UIContainer container,
                                       UIComponent component,
                                       String popupWindowId,
                                       int width) throws Exception {
    UIPopupContainer popupContainer = initPopup(container, component, popupWindowId, width);
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(popupContainer);
  }

  /**
   * Creates the popup window. Each portlet have a <code>UIPopupContainer</code>
   * . <br/>
   * Every <code>UIPopupWindow</code> created by this method is belong to this
   * container.
   *
   * @param container the current container
   * @param component the component which will be display as a popup
   * @param popupWindowId the popup's ID
   * @param width the width of the popup
   * @param isShowMask Set as true to create mask layer
   * @throws Exception the exception
   */
  public static void createPopupWindow(UIContainer container,
                                       UIComponent component,
                                       String popupWindowId,
                                       int width, boolean isShowMask) throws Exception {
    UIPopupContainer popupContainer = initPopup(container, component, popupWindowId, width);
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    popupWindow.setShowMask(isShowMask);
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(popupContainer);
  }

  /**
   * Creates the popup window. Each portlet have a <code>UIPopupContainer</code>
   * . <br/>
   * Every <code>UIPopupWindow</code> created by this method is belong to this
   * container.
   *
   * @param container the current container
   * @param component the component which will be display as a popup
   * @param popupWindowId the popup's ID
   * @param width the width of the popup
   * @param top the top of the popup
   * @param left the left of the popup
   * @throws Exception the exception
   */
  public static void createPopupWindow(UIContainer container,
      UIComponent component,
      String popupWindowId,
      int width, int top, int left) throws Exception {
    UIPopupContainer popupContainer = initPopup(container, component, popupWindowId, width);
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    popupWindow.setCoordindate(top, left);
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(popupContainer);
  }
  
  public static void createPopupWindow(UIContainer container,
                                       UIComponent component,
                                       String popupWindowId,
                                       boolean isMiddle,
                                       int width) throws Exception {
    UIPopupContainer popupContainer = initPopup(container, component, popupWindowId, width);
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    popupWindow.setMiddle(isMiddle);
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(popupContainer);
  }

  private static UIPopupContainer initPopup(UIContainer container,
      UIComponent component,
      String popupWindowId,
      int width) throws Exception {
    UIPopupContainer popupContainer = getPopupContainer(container);
    popupContainer.removeChildById(popupWindowId);
    popupContainer.removeChildById("UIPopupWindow");
    UIPopupWindow popupWindow = popupContainer.addChild(UIPopupWindow.class, null, popupWindowId);
    popupWindow.setUIComponent(component);
    popupWindow.setWindowSize(width, 0);
    popupWindow.setShow(true);
    popupWindow.setRendered(true);
    popupWindow.setResizable(true);
    popupWindow.setShowMask(true);
    return popupContainer;
  }


  /**
   * Close popup window.
   *
   * @param container the current container
   * @param popupWindowId the popup's ID
   */
  public static void closePopupWindow(UIContainer container, String popupWindowId) {
    UIPopupContainer popupContainer = getPopupContainer(container);
    popupContainer.removeChildById(popupWindowId);
  }

  /**
   * Update popup window.
   *
   * @param container the container
   * @param component the component which will be replace for the old one in the
   *          same popup
   * @param popupWindowId the popup's ID
   */
  public static void updatePopupWindow(UIContainer container,
                                       UIComponent component,
                                       String popupWindowId) {
    UIPopupContainer popupContainer = getPopupContainer(container);
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    popupWindow.setUIComponent(component);
  }

  /**
   * Gets the popup container.
   *
   * @param container the current container
   * @return the popup container
   */
  public static UIPopupContainer getPopupContainer(UIContainer container) {
    if (container instanceof UIPortletApplication)
      return container.getChild(UIPopupContainer.class);
    UIPortletApplication portletApplication = container.getAncestorOfType(UIPortletApplication.class);
    return portletApplication.getChild(UIPopupContainer.class);
  }

  /**
   * Creates the popup message.
   *
   * @param container the current container
   * @param message the message key
   * @param args the arguments to show in the message
   * @param type the message's type (e.g. <code>ApplicationMessage.INFO</code>)
   * @see ApplicationMessage
   */
  public static void createPopupMessage(UIContainer container,
                                        String message,
                                        Object[] args,
                                        int type) {
    UIApplication application = container.getAncestorOfType(UIApplication.class);
    application.addMessage(new ApplicationMessage(message, args, type));
  }

  /**
   * Get one portlet preference by name
   *
   * @param preferenceName the name of preference
   * @return the portlet preference's value
   */
  public static String getPortletPreference(String preferenceName) {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
    return preferences.getValue(preferenceName, null);
  }

  /**
   * Get all portlet preferences
   *
   * @return all portlet preferences
   */
  public static PortletPreferences getAllPortletPreferences() {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    return portletRequestContext.getRequest().getPreferences();
  }

  /**
   * Check if the node is viewable for the current user or not viewable. <br/>
   * return True if the node is viewable, otherwise will return False
   *
   * @param node: The node to check
   */
  public static boolean isViewable(Node node) {
    try {
      node.refresh(true);
      ((ExtendedNode) node).checkPermission(PermissionType.READ);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Get the real node from frozen node, symlink node return True if the node is
   * viewable, otherwise will return False
   *
   * @param node: The node to check
   */
  public static Node getRealNode(Node node) throws Exception {
    // TODO: Need to add to check symlink node
    if (node.isNodeType("nt:frozenNode")) {
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      return node.getSession().getNodeByUUID(uuid);
    }
    return node;
  }

  public static String getRealNodePath(Node node) throws Exception {
    if (node.isNodeType("nt:frozenNode")) {
      Node realNode = getRealNode(node);
      return Text.escape(realNode.getPath(),'%',true) + "?version=" + node.getParent().getName();
    }
    return Text.escape(node.getPath(),'%',true);
  }

  public static String getWebdavURL(Node node) throws Exception {
    return getWebdavURL(node, true);
  }

  public static String getWebdavURL(Node node, boolean withTimeParam) throws Exception {
    return getWebdavURL(node, withTimeParam, true);
  }

  public static String getWebdavURL(Node node, boolean withTimeParam, boolean isGetRealNodePath) throws Exception {
    NodeLocation location = NodeLocation.getNodeLocationByNode(getRealNode(node));
    String repository = location.getRepository();
    String workspace = location.getWorkspace();
    String currentProtal = PortalContainer.getCurrentRestContextName();
    String portalName = PortalContainer.getCurrentPortalContainerName();

    String originalNodePath = isGetRealNodePath ? getRealNodePath(node) : Text.escape(node.getPath(),'%',true);
    StringBuffer imagePath = new StringBuffer();
    imagePath.append("/")
             .append(portalName)
             .append("/")
             .append(currentProtal)
             .append("/jcr/")
             .append(repository)
             .append("/")
             .append(workspace)
             .append(originalNodePath);
    if (withTimeParam) {
      if (imagePath.indexOf("?") > 0) {
        imagePath.append("&time=");
      } else {
        imagePath.append("?time=");
      }
      imagePath.append(System.currentTimeMillis());
    }
    return imagePath.toString();
  }

  /**
   * GetRealNode
   *
   * @param strRepository
   * @param strWorkspace
   * @param strIdentifier
   * @return the required node/ the target of a symlink node / null if node was
   *         in trash.
   * @throws RepositoryException
   */
  public static Node getRealNode(String strRepository,
                                 String strWorkspace,
                                 String strIdentifier,
                                 boolean isWCMBase) throws RepositoryException {
    return getRealNode(strRepository, strWorkspace, strIdentifier, isWCMBase, WCMComposer.VISIBILITY_USER);
  }

  /**
   * GetRealNode
   *
   * @param strRepository
   * @param strWorkspace
   * @param strIdentifier
   * @param cacheVisibility the visibility of cache
   *
   * @return the required node/ the target of a symlink node / null if node was
   *         in trash.
   * @throws RepositoryException
   */
  public static Node getRealNode(String strRepository,
                                 String strWorkspace,
                                 String strIdentifier,
                                 boolean isWCMBase,
                                 String cacheVisibility) throws RepositoryException {
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    Node selectedNode;
    if (isWCMBase) {
      selectedNode = getViewableNodeByComposer(strRepository,
                                               strWorkspace,
                                               strIdentifier,
                                               WCMComposer.BASE_VERSION,
                                               cacheVisibility);
    } else {
      selectedNode = getViewableNodeByComposer(strRepository, strWorkspace, strIdentifier, null, cacheVisibility);
    }
    if (selectedNode != null) {
      if (!org.exoplatform.ecm.webui.utils.Utils.isInTrash(selectedNode)) {
        if (linkManager.isLink(selectedNode)) {
          if (linkManager.isTargetReachable(selectedNode)) {
            selectedNode = linkManager.getTarget(selectedNode);
            if (!org.exoplatform.ecm.webui.utils.Utils.isInTrash(selectedNode)) {
              return selectedNode;
            }
          }
        } else {
          return selectedNode;
        }
      }
    }
    return null;
  }

  public static boolean hasEditPermissionOnPage() throws Exception {
    UIPortalApplication portalApp = Util.getUIPortalApplication();
    UIWorkingWorkspace uiWorkingWS = portalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
    UIPageBody pageBody = uiWorkingWS.findFirstComponentOfType(UIPageBody.class);
    UIPage uiPage = (UIPage) pageBody.getUIComponent();
    UserACL userACL = portalApp.getApplicationComponent(UserACL.class);

    if (uiPage != null) {
      return userACL.hasEditPermissionOnPage(uiPage.getOwnerType(),
                                             uiPage.getOwnerId(),
                                             uiPage.getEditPermission());
    }
    UIPortal currentUIPortal = portalApp.<UIWorkingWorkspace> findComponentById(UIPortalApplication.UI_WORKING_WS_ID)
    .findFirstComponentOfType(UIPortal.class);
    UserNode currentNode = currentUIPortal.getSelectedUserNode();
    PageKey pageReference = currentNode.getPageRef();
    if (pageReference == null) {
      return false;
    }
    UserPortalConfigService portalConfigService = portalApp.getApplicationComponent(UserPortalConfigService.class);
    PageContext page = portalConfigService.getPage(pageReference);
    if (page == null) {
      return false;
    }
    return userACL.hasEditPermission(page);
  }

  public static boolean hasEditPermissionOnNavigation() throws Exception {
    UserNavigation selectedNavigation = getSelectedNavigation();
    if(selectedNavigation == null) return false;
    return selectedNavigation.isModifiable();
  }

  public static boolean hasEditPermissionOnPortal() throws Exception {
    UIPortalApplication portalApp = Util.getUIPortalApplication();
    UIPortal currentUIPortal = portalApp.<UIWorkingWorkspace> findComponentById(UIPortalApplication.UI_WORKING_WS_ID)
                                        .findFirstComponentOfType(UIPortal.class);
    UserACL userACL = portalApp.getApplicationComponent(UserACL.class);
    return userACL.hasEditPermissionOnPortal(currentUIPortal.getSiteKey().getTypeName(),
                                             currentUIPortal.getSiteKey().getName(),
                                             currentUIPortal.getEditPermission());
  }

  public static UserNavigation getSelectedNavigation() throws Exception { 
    SiteKey siteKey = Util.getUIPortal().getSiteKey();
    return NavigationUtils.getUserNavigation(
          Util.getPortalRequestContext().getUserPortalConfig().getUserPortal(),
          siteKey);
  }

  public static boolean isEmptyContent(String inputValue) {
    boolean isEmpty = true;
    inputValue = inputValue.trim().replaceAll("<p>", "").replaceAll("</p>", "");
    inputValue = inputValue.replaceAll("\n", "").replaceAll("\t","");
    inputValue = inputValue.replaceAll("&nbsp;", "");
    if(inputValue != null && inputValue.length() > 0) return false;
    return isEmpty;
  }

  /**
   * @purpose     Check if a query is valid
   * @param workspace
   * @param strQuery
   * @param SQLLanguage
   * @return true as valid query, false as Invalid
   */
  public static boolean checkQuery(String workspace, String strQuery, String SQLLanguage) {
    try {
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace,
            WCMCoreUtils.getService(RepositoryService.class).getCurrentRepository());
      QueryManager qm = session.getWorkspace().getQueryManager();
      Query query = qm.createQuery(strQuery, SQLLanguage);
      query.execute();
    }catch(Exception e) {
      return false;
    }
    return true;
  }

  /**
   * get the parameter list from SQL query, the parameter have the ${PARAM} format. <br>
   * For example:
   * <ul>
   *   <li>${folder-id}</li>
   *   <li>${user}</li>
   *   <li>${lang}</li>
   * </ul>
   * @param sqlQuery the given input SQL query
   * @return a list of parameter in input SQL query
   */
  public static HashSet<String> getQueryParams(String sqlQuery) {
    HashSet<String> params = new HashSet<String>();
    if (sqlQuery == null) {
      return params;
    }
    Matcher matcher = Pattern.compile(SQL_PARAM_PATTERN).matcher(sqlQuery);
    while (matcher.find()) {
      String param = matcher.group();
      param = param.replaceAll("\\$\\{", "").replaceAll("\\}", "");
      params.add(param);
    }
    return params;
  }

  /**
   * Replace the parameter with the relevant value from <code>params</code>to
   * build the SQL query
   *
   * @param sqlQuery the input query that contain parameter
   * @param params list of all parameter(key, value) pass to the query
   * @return SQL query after replacing the parameter with value
   */
  public static String buildQuery(String sqlQuery, HashMap<String, String> params) {
    if (!hasParam(sqlQuery) || params == null || params.isEmpty()) {
      return sqlQuery;
    }
    String query = sqlQuery;
    for (String param : params.keySet()) {
      query = query.replaceAll("\\$\\{" + param + "\\}", params.get(param));
    }
    return query;
  }

  /**
   * Check if the input SQL query contains any parameter or not.
   *
   * @param sqlQuery
   * @return <code>false</code> if input SQL query does not contain any
   *         parameter <br>
   *         <code>true</code> if input SQL query one or more parameter
   */
  public static boolean hasParam(String sqlQuery) {
    if (sqlQuery == null || sqlQuery.trim().length() == 0) {
      return false;
    }
    if (Pattern.compile(SQL_PARAM_PATTERN).matcher(sqlQuery).find()) {
      return true;
    }
    return false;
  }

  /**
   * Get download link of a node which stored binary data
   * @param node Node
   * @return download link
   * @throws Exception
   */
  public static String getDownloadLink(Node node) throws Exception {

    if (!Utils.getRealNode(node).isNodeType(NT_FILE)) return null;

    // Get binary data from node
    DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);
    Node jcrContentNode = node.getNode(JCR_CONTENT);
    InputStream input = jcrContentNode.getProperty(JCR_DATA).getStream();

    // Get mimeType of binary data
    String mimeType = jcrContentNode.getProperty(JCR_MIMETYPE).getString() ;

    // Make download stream
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, mimeType);

    // Make extension part for file if it have not yet
    DMSMimeTypeResolver mimeTypeSolver = DMSMimeTypeResolver.getInstance();
    String ext = "." + mimeTypeSolver.getExtension(mimeType) ;
    String fileName = Utils.getRealNode(node).getName();
    if (fileName.lastIndexOf(ext) < 0 && !mimeTypeSolver.getMimeType(fileName).equals(mimeType)) {
      dresource.setDownloadName(fileName + ext);
    } else {
      dresource.setDownloadName(fileName);
    }

    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  /**
   * Get node nt:file if node support multi-language
   *
   * @param currentNode Current Node
   * @return Node which has type nt:file
   * @throws Exception
   */
  public static Node getFileLangNode(Node currentNode) throws Exception {
    if(currentNode.isNodeType(NT_UNSTRUCTURED)) {
      if(currentNode.getNodes().getSize() > 0) {
        NodeIterator nodeIter = currentNode.getNodes() ;
        while(nodeIter.hasNext()) {
          Node ntFile = nodeIter.nextNode() ;
          if(ntFile.isNodeType(NT_FILE)) {
            return ntFile ;
          }
        }
        return currentNode ;
      }
    }
    return currentNode ;
  }

  /**
   * Allows you to add a lock token to the given node
   */
  public static void addLockToken(Node node) throws Exception {
    if (node.isLocked()) {
      String lockToken = LockUtil.getLockToken(node);
      if(lockToken != null) {
        node.getSession().addLockToken(lockToken);
      }
    }
  }
  
  /**
   * sets to lower case n first elements of string
   * @param st
   * @param n
   */
  public static String toLowerCase(String st, int n) {
    StringBuilder sb = new StringBuilder(st);
    for (int i = 0; i < n; i++) {
      if (i < sb.length()) {
        sb.setCharAt(i, Character.toLowerCase(st.charAt(i)));
      }
    }
    return sb.toString();
  }
  /**
   * 
   * @return true if current user is administrative user; false if current user is normal user
   */
  public static boolean isAdministratorUser() {
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    return userACL.isUserInGroup(userACL.getAdminGroups());
  }
  
  public static String getProfileLink(String userId) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    NavigationResource resource =
        new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), "profile");
    return nodeURL.setResource(resource).toString() + "/" + userId;
  }
}
