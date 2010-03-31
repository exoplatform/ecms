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

import java.util.HashMap;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.core.UIPopupWindow;
import org.exoplatform.web.application.ApplicationMessage;
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
    if (popupContainer == null) return false;
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    if (popupWindow == null) return false;
    return true;
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
   * 
   * @return true, if successful
   * 
   * @throws Exception the exception
   */
  public static boolean canEditCurrentPortal(String remoteUser) throws Exception {
    if (remoteUser == null) return false;
    IdentityRegistry identityRegistry = Util.getUIPortalApplication().getApplicationComponent(IdentityRegistry.class);
    Identity identity = identityRegistry.getIdentity(remoteUser);
    if (identity == null) return false;
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
   * 
   * @return the string
   */
  public static String cleanString(String str) {
      Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
      str = accentsconverter.transliterate(str); 
      //the character ? seems to not be changed to d by the transliterate function 
      StringBuffer cleanedStr = new StringBuffer(str.trim());
      // delete special character
      for(int i = 0; i < cleanedStr.length(); i++) {
        char c = cleanedStr.charAt(i);
        if(c == ' ') {
          if (i > 0 && cleanedStr.charAt(i - 1) == '_') {
            cleanedStr.deleteCharAt(i--);
          } else {
            c = '_';
            cleanedStr.setCharAt(i, c);
          }
          continue;
        }
        if(i > 0 && !(Character.isLetterOrDigit(c) || c == '_')) {
          cleanedStr.deleteCharAt(i--);
          continue;
        }
        if(i > 0 && c == '_' && cleanedStr.charAt(i-1) == '_')
          cleanedStr.deleteCharAt(i--);
      }
      return cleanedStr.toString().toLowerCase();
  }

  
  /**
   * Refresh whole portal by AJAX.
   * @param context the portlet request context
   */
  public static void updatePortal(PortletRequestContext context) {
    UIPortalApplication portalApplication = Util.getUIPortalApplication();   
    PortalRequestContext portalRequestContext = (PortalRequestContext)context.getParentAppRequestContext();
    UIWorkingWorkspace uiWorkingWS = portalApplication.getChildById(UIPortalApplication.UI_WORKING_WS_ID);    
    portalRequestContext.addUIComponentToUpdateByAjax(uiWorkingWS) ;    
    portalRequestContext.setFullRender(true);
  }
  
  /**
	 * Gets the viewable node by WCMComposer (depends on site mode)
	 * @param repository the repository's name 
	 * @param workspace the workspace's name
	 * @param nodeIdentifier the node's path or node's UUID
	 * @return the viewable node. Return <code>null</code> if <code>nodeIdentifier</code> is invalid
	 * @see #getViewableNodeByComposer(String repository, String workspace, String nodeIdentifier, String version) getViewableNodeByComposer()
	 */
	public static Node getViewableNodeByComposer(String repository, String workspace, String nodeIdentifier) {
		return getViewableNodeByComposer(repository, workspace, nodeIdentifier, null);
	} 

	/**
	 * Gets the viewable node by WCMComposer (depends on site mode)
	 * @param repository the repository's name 
	 * @param workspace the workspace's name
	 * @param nodeIdentifier the node's path or node's UUID
	 * @param version the base version (e.g. <code>WCMComposer.BASE_VERSION</code>)
	 * @return the viewable node. Return <code>null</code> if <code>nodeIdentifier</code> is invalid
	 * @see #getViewableNodeByComposer(String repository, String workspace, String nodeIdentifier) getViewableNodeByComposer()
	 * @see WCMComposer
	 */
	public static Node getViewableNodeByComposer(String repository, String workspace, String nodeIdentifier, String version) {
		try {
			HashMap<String, String> filters = new HashMap<String, String>();
			filters.put(WCMComposer.FILTER_MODE, Utils.getCurrentMode());
			if (version != null) filters.put(WCMComposer.FILTER_VERSION, version);
			return getService(WCMComposer.class).getContent(repository, workspace, nodeIdentifier, filters, getSessionProvider());
		} catch (Exception e) {
			return null;
		}
	}
  
  /**
   * Gets the current mode of the site
   * @return the current mode (e.g. <code>WCMComposer.MODE_EDIT</code>)
   * @see WCMComposer
   */
  public static String getCurrentMode() {
    Object isQuickEditable = Util.getPortalRequestContext().getRequest().getSession().getAttribute(TURN_ON_QUICK_EDIT);
    if(isQuickEditable == null) return WCMComposer.MODE_LIVE;
    boolean turnOnQuickEdit = Boolean.parseBoolean(isQuickEditable.toString()); 
    return turnOnQuickEdit ? WCMComposer.MODE_EDIT : WCMComposer.MODE_LIVE;
  }
  
  /**
   * Check if the content is draft and current mode of the site is edit mode
   * @param content the content node.  
   * @return true, the content is draft and current mode is edit mode, otherwise return false.
   */
  public static boolean isShowDraft(Node content) {
  	if (content == null) return false;
  	try {
  	  if(content.isNodeType("nt:frozenNode")) return false;
  		WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
  		String contentState = wcmPublicationService.getContentState(content);
  		boolean isDraftContent = false;
  		if (PublicationDefaultStates.DRAFT.equals(contentState)) isDraftContent = true;
  		boolean isShowDraft = false;
  		if (WCMComposer.MODE_EDIT.equals(getCurrentMode())) isShowDraft = true;
  		return isDraftContent && isShowDraft;
		} catch (Exception e) {
			return false;
		}
  }
  
  /**
   * Check if the current mode of the site is edit mode
   * @return true, if current mode is edit mode
   */
  public static boolean isShowQuickEdit() {
  	try {
  		boolean isEditMode = false;
  		if (WCMComposer.MODE_EDIT.equals(getCurrentMode())) isEditMode = true;
  		return isEditMode;
		} catch (Exception e) {
			return false;
		}
  }
  
  /**
   * Check if the content is editable and current mode of the site is edit mode
   * @param content the content node
   * @return true if there is no content if the content is editable and current mode is edit mode
   */
  public static boolean isShowQuickEdit(Node content) {
  	if (content == null) return true;
  	try {
  		boolean isEditMode = false;
  		if (WCMComposer.MODE_EDIT.equals(getCurrentMode())) isEditMode = true;
  		((ExtendedNode) content).checkPermission(PermissionType.SET_PROPERTY);
  		((ExtendedNode) content).checkPermission(PermissionType.ADD_NODE);
      ((ExtendedNode) content).checkPermission(PermissionType.REMOVE);
  		return isEditMode;
		} catch (Exception e) {
			return false;
		}
  }
  
  /**
   * Creates the popup window. Each portlet have a <code>UIPopupContainer</code>. <br/>
   * Every <code>UIPopupWindow</code> created by this method is belong to this container.
   * @param container the current container
   * @param component the component which will be display as a popup
   * @param popupWindowId the popup's ID
   * @param width the width of the popup
   * @param height the height of the popup
   * @throws Exception the exception
   */
  public static void createPopupWindow(UIContainer container, UIComponent component, String popupWindowId, int width) throws Exception {
    UIPopupContainer popupContainer = getPopupContainer(container);
    popupContainer.removeChildById(popupWindowId);
    UIPopupWindow popupWindow = popupContainer.addChild(UIPopupWindow.class, null, popupWindowId);
    popupWindow.setUIComponent(component);
    popupWindow.setWindowSize(width, 0);
    popupWindow.setShow(true);
    popupWindow.setRendered(true);
    popupWindow.setResizable(true);
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(popupContainer);
  }
  
  /**
   * Close popup window.
   * @param container the current container
   * @param popupWindowId the popup's ID
   */
  public static void closePopupWindow(UIContainer container, String popupWindowId) {
    UIPopupContainer popupContainer = getPopupContainer(container);
    popupContainer.removeChildById(popupWindowId);
  }
  
  /**
   * Update popup window.
   * @param container the container
   * @param component the component which will be replace for the old one in the same popup
   * @param popupWindowId the popup's ID
   */
  public static void updatePopupWindow(UIContainer container, UIComponent component, String popupWindowId) {
    UIPopupContainer popupContainer = getPopupContainer(container);
    UIPopupWindow popupWindow = popupContainer.getChildById(popupWindowId);
    popupWindow.setUIComponent(component);
  }
  
  /**
   * Gets the popup container.
   * @param container the current container
   * @return the popup container
   */
  public static UIPopupContainer getPopupContainer(UIContainer container) {
  	if (container instanceof UIPortletApplication) return container.getChild(UIPopupContainer.class);
    UIPortletApplication portletApplication = container.getAncestorOfType(UIPortletApplication.class);
    return portletApplication.getChild(UIPopupContainer.class);
  }
  
  /**
   * Creates the popup message.
   * @param container the current container
   * @param message the message key
   * @param args the arguments to show in the message 
   * @param type the message's type (e.g. <code>ApplicationMessage.INFO</code>)
   * @see ApplicationMessage
   */
  public static void createPopupMessage(UIContainer container, String message, Object[] args, int type) {
    UIApplication application = container.getAncestorOfType(UIApplication.class);
    application.addMessage(new ApplicationMessage(message, args, type)) ;
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    requestContext.addUIComponentToUpdateByAjax(application.getUIPopupMessages()) ;
  }

  @Deprecated
  /**
   * Gets the service.  Try to use WCMCoreUtils.getService(Class<T> clazz).
   * 
   * @param clazz the class of the service
   * 
   * @return the service
   */
  public static <T> T getService(Class<T> clazz) {
  	UIPortalApplication portalApplication = Util.getUIPortalApplication();
  	return clazz.cast(portalApplication.getApplicationComponent(clazz));
  }
  
  @Deprecated
  /**
   * Gets the session provider. Try to use WCMCoreUtils.getUserSessionProvider().
   * 
   * @return the session provider
   */
  public static SessionProvider getSessionProvider() {
  	return WCMCoreUtils.getUserSessionProvider();
  }
  
  @Deprecated
  /**
   * Gets the system session provider. Try to use WCMCoreUtils.getSystemSessionProvider().
   * 
   * @return the system session provider
   */
  public static SessionProvider getSystemSessionProvider() {
    return WCMCoreUtils.getSystemSessionProvider();
  }
  
  /**
   * Get one portlet preference by name
   * 
   * @param preferenceName the name of preference
   * 
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
  
}
