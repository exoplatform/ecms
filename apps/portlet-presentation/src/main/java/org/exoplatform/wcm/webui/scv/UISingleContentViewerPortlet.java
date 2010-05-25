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
package org.exoplatform.wcm.webui.scv;

import java.security.AccessControlException;

import javax.jcr.Node;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.WebUIPropertiesConfigService;
import org.exoplatform.wcm.webui.WebUIPropertiesConfigService.PopupWindowProperties;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.scv.config.UIPortletConfig;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 * dzungdev@gmail.com
 * Jun 9, 2008
 */
@ComponentConfig(
  lifecycle = UIApplicationLifecycle.class,
  template = "app:/groovy/SingleContentViewer/UISingleContentView.gtmpl"
)

public class UISingleContentViewerPortlet extends UIPortletApplication {

  /** The REPOSITORY. */
  public static String REPOSITORY = "repository" ;

  /** The WORKSPACE. */
  public static String WORKSPACE = "workspace" ;

  /** The IDENTIFIER. */
  public static String IDENTIFIER = "nodeIdentifier" ;

  /** The mode_. */
  private PortletMode mode = PortletMode.VIEW ;

  /**
   * Instantiates a new uI single content viewer portlet.
   * 
   * @throws Exception the exception
   */
  public UISingleContentViewerPortlet() throws Exception {    
    activateMode(mode) ;    
  }

  /**
   * Activate mode.
   * 
   * @param mode the mode
   * 
   * @throws Exception the exception
   */
  public void activateMode(PortletMode mode) throws Exception {       
    addChild(UIPopupContainer.class, null, null);
    if(PortletMode.VIEW.equals(mode)) {      
    	if (getChild(UIPresentationContainer.class) == null) addChild(UIPresentationContainer.class, null, null);                   
    } else if (PortletMode.EDIT.equals(mode)) {
      WebUIPropertiesConfigService propertiesConfigService = getApplicationComponent(WebUIPropertiesConfigService.class);
      PopupWindowProperties popupProperties = (PopupWindowProperties)propertiesConfigService.getProperties(WebUIPropertiesConfigService.SCV_POPUP_SIZE_EDIT_PORTLET_MODE);
      UIPortletConfig portletConfig = createUIComponent(UIPortletConfig.class,null,null);      
      Utils.createPopupWindow(this, portletConfig, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW, popupProperties.getWidth());
      try {
        portletConfig.init();
      } catch (AccessControlException e) {        
        Utils.closePopupWindow(this, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
        UIApplication uiApp = findFirstComponentOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISingleContentViewerPortlet.msg.AccessControlException", null, 
            ApplicationMessage.WARNING));
        WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
        requestContext.addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform.webui.application.WebuiApplication, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context ;
    PortletMode newMode = pContext.getApplicationMode() ;
    if(!mode.equals(newMode)) {
      activateMode(newMode) ;
      mode = newMode ;
    }
    super.processRender(app, context) ;
  }

  public Node getNodeByPreference() {
    try {
      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
      PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
      String repository = preferences.getValue(REPOSITORY, null);    
      String workspace = preferences.getValue(WORKSPACE, null);
      String nodeIdentifier = preferences.getValue(IDENTIFIER, null) ;
      WCMService wcmService = getApplicationComponent(WCMService.class);
      return wcmService.getReferencedContent(Utils.getSessionProvider(), repository, workspace, nodeIdentifier);
    } catch (Exception e) {
      return null;
    }
  }
    
}