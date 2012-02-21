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
package org.exoplatform.ecm.webui.component.browsecontent;

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 14, 2006 5:15:47 PM
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class
)

public class UIBrowseContentPortlet extends UIPortletApplication  {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIBrowseContentPortlet.class);

  private boolean isViewModing_ = true;
  @SuppressWarnings("unused")
  public UIBrowseContentPortlet() throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String minWidth = portletPref.getValue(Utils.MIN_WIDTH, "");
    if(minWidth != null && minWidth.length() > 0) setMinWidth(Integer.parseInt(minWidth));
    addChild(UIBrowseContentHelp.class, null, null);
    PortletRequestContext context =  (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    UIPopupContainer popup = addChild(UIPopupContainer.class, null, null);
    popup.setId("UICBPopupAction");
    popup.getChild(UIPopupWindow.class).setId("UICBPopupWindow");
    addChild(UIPopupContainer.class, null, "UICBPopupComment");
    UIBrowseContainer uiBrowseContainer = addChild(UIBrowseContainer.class, null , null);
    addChild(UIConfigTabPane.class, null, null);
    try {
      uiBrowseContainer.loadPortletConfig(getPortletPreferences());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot initialize the UIBrowseContentPortlet", e);
      }
    }
  }

  public void  processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().importJavascript("eXo.ecm.ECMUtils","/ecm-wcm-extension/javascript/");
    context.getJavascriptManager().addJavascript("eXo.ecm.ECMUtils.init('UIBrowseContentPortlet');");
    PortletRequestContext portletReqContext = (PortletRequestContext)  context;
    PortletRequest portletRequest = portletReqContext.getRequest();
    String userAgent = portletRequest.getProperty("User-Agent");
    if(userAgent != null && userAgent.contains("MSIE 6.0")) {
      context.getJavascriptManager().importJavascript("eXo.ecm.Select_fix","/ecm-wcm-extension/javascript/") ;
      context.getJavascriptManager().addJavascript("SelectFix.autoRepairFloatingElements(100) ;") ;
    }
    UIConfigTabPane uiTabPane = getChild(UIConfigTabPane.class);
    UIBrowseContainer uiContainer = getChild(UIBrowseContainer.class);
    UIBrowseContentHelp uiBCHelp = getChild(UIBrowseContentHelp.class);
    if(portletReqContext.getApplicationMode() == PortletMode.VIEW) {
      uiTabPane.setRendered(false);
      uiBCHelp.setRendered(false);
      uiContainer.setRendered(true);
      if(isViewModing_) uiContainer.refreshContent();
      isViewModing_ = true;
    } else if(portletReqContext.getApplicationMode() == PortletMode.EDIT) {
      if(!SessionProviderFactory.isAnonim()) {
        if(!uiTabPane.isNewConfig()) uiTabPane.getCurrentConfig();
        uiTabPane.setRendered(true);
        uiBCHelp.setRendered(false);
        uiContainer.setRendered(false);
        uiContainer.getListHistoryNode().clear();
        isViewModing_ = false;
      }
    } else if(portletReqContext.getApplicationMode() == PortletMode.HELP) {
      uiTabPane.setRendered(false);
      uiBCHelp.setRendered(true);
      uiContainer.setRendered(false);
      isViewModing_ = false;
    }
    try {
      super.processRender(app, context);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot display the content of the UIBrowseContentPortlet", e);
      }
    }
  }
  protected void reload() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    WebuiApplication app =  (WebuiApplication)context.getApplication();
    processRender(app, context);
  }

  protected void setPorletMode(PortletMode mode) throws PortletModeException {
    PortletRequestContext portletReqContext =  (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    portletReqContext.setApplicationMode(mode);
  }
  protected boolean isExitsRepo(String repoName) {
    RepositoryService rService = getApplicationComponent(RepositoryService.class);
    try {
      rService.getRepository(repoName);
      return true;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("The repository " + repoName + " cannot be found", e);
      }
      return false;
    }
  }

  public PortletPreferences getPortletPreferences() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletRequest prequest = pcontext.getRequest();
    PortletPreferences portletPref = prequest.getPreferences();
    return portletPref;
  }

  public String getPreferenceRepository() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    return portletPref.getValue(Utils.REPOSITORY, "");
  }
}
