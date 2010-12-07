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

import java.util.Date;

import javax.jcr.Node;
import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 15, 2008
 */
/**
 * The Class UICLVPortlet.
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class)
public class UICLVPortlet extends UIPortletApplication {

  /** The Constant PREFERENCE_DISPLAY_MODE. */
  public static final String PREFERENCE_DISPLAY_MODE				= "mode";
  
  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_PATH					= "folderPath";

  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_DRIVE					= "nodeDrive";
  
  /** The Constant PREFERENCE_ORDER_BY. */
  public static final String PREFERENCE_ORDER_BY					= "orderBy";
  
  /** The Constant ORDER_BY_TITLE. */
  public static final String ORDER_BY_TITLE							= "OrderByTitle";
  
  /** The Constant ORDER_BY_DATE_CREATED. */
  public static final String ORDER_BY_DATE_CREATED					= "OrderByDateCreated";
  
  /** The Constant ORDER_BY_DATE_MODIFIED. */
  public static final String ORDER_BY_DATE_MODIFIED					= "OrderByDateModified";
  
  /** The Constant ORDER_BY_DATE_PUBLISHED. */
  public static final String ORDER_BY_DATE_PUBLISHED				= "OrderByDatePublished";
  
  /** The Constant ORDER_BY_DATE_START_EVENT. */
  public static final String ORDER_BY_DATE_START_EVENT				= "OrderByDateStartEvent";
  
  /** The Constant ORDER_BY_INDEX. */
  public static final String ORDER_BY_INDEX							= "OrderByIndex";
  
  /** The Constant PREFERENCE_ORDER_TYPE. */
  public static final String PREFERENCE_ORDER_TYPE					= "orderType";
  
  /** The Constant ORDER_TYPE_DESCENDENT. */
  public static final String ORDER_TYPE_DESCENDENT					= "OrderDesc";
  
  /** The Constant ORDER_TYPE_ASCENDENT. */
  public static final String ORDER_TYPE_ASCENDENT					= "OrderAsc";
  
  /** The Constant PREFERENCE_HEADER. */
  public final static String PREFERENCE_HEADER						= "header";
  
  /** The Constant PREFERENCE_AUTOMATIC_DETECTION. */
  public final static String PREFERENCE_AUTOMATIC_DETECTION			= "automaticDetection";
  
  /** The Constant PREFERENCE_DISPLAY_TEMPLATE. */
  public final static String PREFERENCE_DISPLAY_TEMPLATE			= "formViewTemplatePath";
  
  /** The Constant PREFERENCE_PAGINATOR_TEMPLATE. */
  public final static String PREFERENCE_PAGINATOR_TEMPLATE			= "paginatorTemplatePath";
  
  /** The Constant PREFERENCE_ITEMS_PER_PAGE. */
  public final static String PREFERENCE_ITEMS_PER_PAGE				= "itemsPerPage";
  
  /** The Constant PREFERENCE_SHOW_TITLE. */
  public final static String PREFERENCE_SHOW_TITLE					= "showTitle";
  
  /** The Constant PREFERENCE_SHOW_HEADER. */
  public final static String PREFERENCE_SHOW_HEADER					= "showHeader";

  /** The Constant PREFERENCE_SHOW_REFRESH_BUTTON. */
  public final static String PREFERENCE_SHOW_REFRESH_BUTTON			= "showRefreshButton";

  /** The Constant PREFERENCE_SHOW_ILLUSTRATION. */
  /** The Constant PREFERENCE_SHOW_IMAGE. */  
  public final static String PREFERENCE_SHOW_ILLUSTRATION			= "showThumbnailsView";
//  public final static String PREFERENCE_SHOW_IMAGE				= "showImage";
  
  /** The Constant PREFERENCE_SHOW_DATE_CREATED. */
  public final static String PREFERENCE_SHOW_DATE_CREATED			= "showDateCreated";
  
  /** The Constant PREFERENCE_SHOW_MORE_LINK. */
  public final static String PREFERENCE_SHOW_READMORE				= "showReadmore";
//  public final static String PREFERENCE_SHOW_MORE_LINK			= "showMoreLink";

  /** The Constant PREFERNECE_SHOW_SUMMARY. */
  public final static String PREFERENCE_SHOW_SUMMARY				= "showSummary";
  
  /** The Constant PREFERENCE_SHOW_LINK. */
  public final static String PREFERENCE_SHOW_LINK					= "showLink";
  
  /** The Constant PREFERENCE_SHOW_RSSLINK. */
  public final static String PREFERENCE_SHOW_RSSLINK				= "showRssLink";
  
  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER			= "contextualFolder";
  
  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER_ENABLE. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER_ENABLE	= "contextualEnable";
  
  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER_DISABLE. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER_DISABLE	= "contextualDisable";
  
  /** The Constant PREFERENCE_TARGET_PAGE. */
  public final static String  PREFERENCE_TARGET_PAGE				= "basePath";
  
  /** The Constant PREFERENCE_SHOW_SCL_WITH. */
  public final static String PREFERENCE_SHOW_SCV_WITH				= "showScvWith";

  /** The Constant PREFERENCE_SHOW_CLV_BY. */
  public final static String PREFERENCE_SHOW_CLV_BY					= "showClvBy";
  
  /** The Constant DISPLAY_MODE_MANUAL. */
  public static final String DISPLAY_MODE_MANUAL					= "ManualViewerMode";

  /** The Constant DISPLAY_MODE_AUTOMATIC. */
  public static final String DISPLAY_MODE_AUTOMATIC					= "AutoViewerMode";
  
  public static final String DEFAULT_SHOW_CLV_BY					= "folder-id";
  public static final String DEFAULT_SHOW_SCV_WITH					= "content-id";
  
  public static final String PREFERENCE_APPLICATION_TYPE			= "application";			
  
  private PortletMode     mode;
  
  private UICLVFolderMode folderMode;
  
  private UICLVManualMode manualMode;
  
  private UICLVConfig     config;
  private String          currentFolderPath;

  private static final Log log = ExoLogger.getLogger(UICLVPortlet.class);
  /**
   * Instantiates a new uICLV portlet.
   * 
   * @throws Exception the exception
   */
  public UICLVPortlet() throws Exception {
    addChild(UIPopupContainer.class, null, "UIPopupContainer-" + new Date().getTime());
    folderMode = addChild(UICLVFolderMode.class, null, null).setRendered(false);
    manualMode = addChild(UICLVManualMode.class, null, null).setRendered(false);
    config = addChild(UICLVConfig.class, null, null).setRendered(false);
    currentFolderPath = getFolderPath();
  }
  
  public void setCurrentFolderPath(String value) {
    currentFolderPath = value;
  }
  
  public String getFolderPath() {
    PortalRequestContext preq = Util.getPortalRequestContext();
    if (!preq.useAjax()) {
       currentFolderPath= getFolderPathParamValue();
    }
    PortletPreferences preferences = Utils.getAllPortletPreferences();
    String displayMode = preferences.getValue(PREFERENCE_DISPLAY_MODE, null);
    if (DISPLAY_MODE_AUTOMATIC.equals(displayMode)) {
	    if (currentFolderPath == null || currentFolderPath.length() == 0) {
	      currentFolderPath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH);
	    }
    }
    return currentFolderPath;
  }
  
  public String getFolderPathParamValue() {
    PortletPreferences preferences = Utils.getAllPortletPreferences();
    String contextualMode = preferences.getValue(PREFERENCE_CONTEXTUAL_FOLDER, null);
    Node folderNode = null;
    String folderPath = null;
    if (PREFERENCE_CONTEXTUAL_FOLDER_ENABLE.equals(contextualMode)) {
    	String folderParamName = preferences.getValue(PREFERENCE_SHOW_CLV_BY, null);
    	if (folderParamName == null || folderParamName.length() == 0)
    		folderParamName = DEFAULT_SHOW_CLV_BY;
    	folderPath = Util.getPortalRequestContext().getRequestParameter(folderParamName);
    	try {
    		NodeLocation folderLocation = NodeLocation.getNodeLocationByExpression(folderPath);
    		folderNode = NodeLocation.getNodeByLocation(folderLocation);
    	}
    	catch (Exception e) {
    		folderNode = null;
    		folderPath = null;
    	}
    }
    return folderPath;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform.webui.application.WebuiApplication, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context;
    
  	if (context.getRemoteUser()==null) {
      WCMService wcmService = getApplicationComponent(WCMService.class);
	    pContext.getResponse().setProperty(MimeResponse.EXPIRATION_CACHE, ""+wcmService.getPortletExpirationCache());
	    if (log.isTraceEnabled())
	      log.trace("CLV rendering : cache set to "+wcmService.getPortletExpirationCache());
  	  }

    
    PortletPreferences preferences = pContext.getRequest().getPreferences();
    String displayMode = preferences.getValue(PREFERENCE_DISPLAY_MODE, null);

    PortletMode currentMode = pContext.getApplicationMode();
    
    if (displayMode.equals(DISPLAY_MODE_AUTOMATIC) || getFolderPath() != null) {
      if (currentMode != mode) {
        folderMode.init();
        mode = currentMode;
      }
      folderMode.setRendered(true);
      manualMode.setRendered(false);
      config.setRendered(false);
    } else if (displayMode.equals(DISPLAY_MODE_MANUAL)) {
      if (currentMode != mode) {
        manualMode.init();
        mode = currentMode;
      }
      manualMode.setRendered(true);
      folderMode.setRendered(false);
      config.setRendered(false);
    }
    
    if (currentMode == PortletMode.EDIT) {
      folderMode.setRendered(false);
      manualMode.setRendered(false);
      config.setRendered(true);
      config.setModeInternal(true);
    } else if (currentMode == PortletMode.VIEW) {
      config.setModeInternal(false);
      if (displayMode.equals(DISPLAY_MODE_AUTOMATIC) || getFolderPath() != null) {
        manualMode.setRendered(false);
        folderMode.init();        
        folderMode.setRendered(true);
      }else {
        folderMode.setRendered(false);
        manualMode.init();
        manualMode.setRendered(true);
      }      
      config.setRendered(false);      
    }
    
    super.processRender(app, context);
  }
  public void changeToViewMode() throws Exception{
      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
      portletRequestContext.setApplicationMode(PortletMode.VIEW);      
  }
}
