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
import java.util.HashMap;
import java.util.HashSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.gatein.portal.controller.resource.ResourceScope;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 15, 2008
 */
/**
 * The Class UICLVPortlet.
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "system:/groovy/ContentListViewer/UICLVPortlet.gtmpl")
public class UICLVPortlet extends UIPortletApplication {

  /** The Constant PREFERENCE_DISPLAY_MODE. */
  public static final String PREFERENCE_DISPLAY_MODE              = "mode";

  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_PATH                 = "folderPath";

  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_DRIVE                = "nodeDrive";

  /** The Constant PREFERENCE_ORDER_BY. */
  public static final String PREFERENCE_ORDER_BY                  = "orderBy";

  /** The Constant ORDER_BY_TITLE. */
  public static final String ORDER_BY_TITLE                       = "OrderByTitle";

  /** The Constant ORDER_BY_DATE_CREATED. */
  public static final String ORDER_BY_DATE_CREATED                = "OrderByDateCreated";

  /** The Constant ORDER_BY_DATE_MODIFIED. */
  public static final String ORDER_BY_DATE_MODIFIED               = "OrderByDateModified";

  /** The Constant ORDER_BY_DATE_PUBLISHED. */
  public static final String ORDER_BY_DATE_PUBLISHED              = "OrderByDatePublished";

  /** The Constant ORDER_BY_DATE_START_EVENT. */
  public static final String ORDER_BY_DATE_START_EVENT            = "OrderByDateStartEvent";

  /** The Constant ORDER_BY_INDEX. */
  public static final String ORDER_BY_INDEX                       = "OrderByIndex";

  /** The Constant PREFERENCE_ORDER_TYPE. */
  public static final String PREFERENCE_ORDER_TYPE                = "orderType";

  /** The Constant ORDER_TYPE_DESCENDENT. */
  public static final String ORDER_TYPE_DESCENDENT                = "OrderDesc";

  /** The Constant ORDER_TYPE_ASCENDENT. */
  public static final String ORDER_TYPE_ASCENDENT                 = "OrderAsc";

  /** The Constant PREFERENCE_HEADER. */
  public final static String PREFERENCE_HEADER                    = "header";

  /** The Constant PREFERENCE_AUTOMATIC_DETECTION. */
  public final static String PREFERENCE_AUTOMATIC_DETECTION       = "automaticDetection";

  /** The Constant PREFERENCE_DISPLAY_TEMPLATE. */
  public final static String PREFERENCE_DISPLAY_TEMPLATE          = "formViewTemplatePath";

  /** The Constant PREFERENCE_PAGINATOR_TEMPLATE. */
  public final static String PREFERENCE_PAGINATOR_TEMPLATE        = "paginatorTemplatePath";

  /** The Constant PREFERENCE_ITEMS_PER_PAGE. */
  public final static String PREFERENCE_ITEMS_PER_PAGE            = "itemsPerPage";

  /** The Constant PREFERENCE_SHOW_TITLE. */
  public final static String PREFERENCE_SHOW_TITLE                = "showTitle";

  /** The Constant PREFERENCE_SHOW_HEADER. */
  public final static String PREFERENCE_SHOW_HEADER               = "showHeader";

  /** The Constant PREFERENCE_SHOW_REFRESH_BUTTON. */
  public final static String PREFERENCE_SHOW_REFRESH_BUTTON       = "showRefreshButton";

  /** The Constant PREFERENCE_SHOW_ILLUSTRATION. */
  /** The Constant PREFERENCE_SHOW_IMAGE. */
  public final static String PREFERENCE_SHOW_ILLUSTRATION         = "showThumbnailsView";

  /** The Constant PREFERENCE_SHOW_DATE_CREATED. */
  public final static String PREFERENCE_SHOW_DATE_CREATED         = "showDateCreated";

  /** The Constant PREFERENCE_SHOW_MORE_LINK. */
  public final static String PREFERENCE_SHOW_READMORE             = "showReadmore";

  /** The Constant PREFERNECE_SHOW_SUMMARY. */
  public final static String PREFERENCE_SHOW_SUMMARY              = "showSummary";

  /** The Constant PREFERENCE_SHOW_LINK. */
  public final static String PREFERENCE_SHOW_LINK                 = "showLink";

  /** The Constant PREFERENCE_SHOW_RSSLINK. */
  public final static String PREFERENCE_SHOW_RSSLINK              = "showRssLink";

  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER         = "contextualFolder";

  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER_ENABLE. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER_ENABLE  = "contextualEnable";

  /** The Constant PREFERENCE_CONTEXTUAL_FOLDER_DISABLE. */
  public final static String PREFERENCE_CONTEXTUAL_FOLDER_DISABLE = "contextualDisable";

  /** The Constant PREFERENCE_TARGET_PAGE. */
  public final static String PREFERENCE_TARGET_PAGE               = "basePath";

  /** The Constant PREFERENCE_SHOW_SCL_WITH. */
  public final static String PREFERENCE_SHOW_SCV_WITH             = "showScvWith";

  /** The Constant PREFERENCE_SHOW_CLV_BY. */
  public final static String PREFERENCE_SHOW_CLV_BY               = "showClvBy";

  /** The Constant PREFERENCE_CACHE_ENABLED. */
  public final static String PREFERENCE_CACHE_ENABLED             = "sharedCache";

  /** The Constant CONTENT_BY_QUERY. */
  public final static String PREFERENCE_CONTENTS_BY_QUERY         = "query";

  /** The Constant PREFERENCE_WORKSPACE. */
  public final static String PREFERENCE_WORKSPACE                 = "workspace";

  /** The Constant DISPLAY_MODE_MANUAL. */
  public static final String DISPLAY_MODE_MANUAL                  = "ManualViewerMode";

  /** The Constant DISPLAY_MODE_AUTOMATIC. */
  public static final String DISPLAY_MODE_AUTOMATIC               = "AutoViewerMode";

  public static final String DEFAULT_SHOW_CLV_BY                  = "folder-id";

  public static final String DEFAULT_SHOW_SCV_WITH                = "content-id";

  public static final String PREFERENCE_APPLICATION_TYPE          = "application";

  public static final String APPLICATION_CLV_BY_QUERY             = "ContentsByQuery";

  public static final String PREFERENCE_SHARED_CACHE              = "sharedCache";
  /* Dynamic parameter for CLV by query */
  public static final String QUERY_USER_PARAMETER                 = "user";
  public static final String QUERY_LANGUAGE_PARAMETER             = "lang";

  private PortletMode        cpMode;

  private UICLVFolderMode    folderMode;

  private UICLVManualMode    manualMode;

  private UICLVConfig        clvConfig;

  private String             currentFolderPath;

  private String             header;

  private String             currentDisplayMode;

  private String             currentApplicationMode;

  /**
   * Instantiates a new uICLV portlet.
   *
   * @throws Exception the exception
   */
  public UICLVPortlet() throws Exception {
    addChild(UIPopupContainer.class, null, "UIPopupContainer-" + new Date().getTime());
    currentFolderPath = getFolderPath();
  }

  public String getHeader() {
    return header;
  }

  public void setCurrentFolderPath(String value) {
    currentFolderPath = value;
  }

  public String getFolderPath() {
    PortalRequestContext preq = Util.getPortalRequestContext();
    currentFolderPath = "";
    if (!preq.useAjax()) {
      currentFolderPath = getFolderPathParamValue();
    }
    try {
      if (currentFolderPath != null && currentFolderPath.length() > 0) {
        Node folderNode = null;
        NodeLocation folderLocation = NodeLocation.getNodeLocationByExpression(currentFolderPath);
        folderNode = NodeLocation.getNodeByLocation(folderLocation);
        if (folderNode == null) {
          header = null;
        } else {
          if (folderNode.hasProperty(org.exoplatform.ecm.webui.utils.Utils.EXO_TITLE))
            header = folderNode.getProperty(org.exoplatform.ecm.webui.utils.Utils.EXO_TITLE).getString();
          else header = folderNode.getName();
        }
      } else header = null;
    } catch(IllegalArgumentException ex) {
      header = null;
    } catch(ItemNotFoundException ex) {
      header = null;
    } catch(PathNotFoundException ex) {
      header = null;
    } catch(NoSuchWorkspaceException ex) {
      header = null;
    } catch(RepositoryException ex) {
      header = null;
    }
    PortletPreferences preferences = Utils.getAllPortletPreferences();
    currentDisplayMode = preferences.getValue(PREFERENCE_DISPLAY_MODE, null);
    currentApplicationMode = preferences.getValue(PREFERENCE_APPLICATION_TYPE, null);
    if (DISPLAY_MODE_AUTOMATIC.equals(currentDisplayMode)) {
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
        if (folderNode == null)
          return null;
      } catch (Exception e) {
        folderNode = null;
        folderPath = null;
      }
    }
    return folderPath;
  }

  /**
   *
   * @param params
   * @return
   */
  public HashMap<String, String> getQueryParammeter(HashSet<String> params) {

    HashMap<String, String> paramMap = new HashMap<String, String>();
    PortalRequestContext context = Util.getPortalRequestContext();
    for (String param : params) {
      String value = context.getRequestParameter(param);
      if (value != null) {
        paramMap.put(param, value);
      } else {
        paramMap.put(param, "");
      }
    }
    paramMap.put(UICLVPortlet.QUERY_USER_PARAMETER, context.getRemoteUser());
    paramMap.put(UICLVPortlet.QUERY_LANGUAGE_PARAMETER, context.getLocale().getLanguage());

    return paramMap;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context;
    PortletPreferences preferences = pContext.getRequest().getPreferences();

    Boolean sharedCache = "true".equals(preferences.getValue(PREFERENCE_SHARED_CACHE, "true"));

    if (context.getRemoteUser() == null
        || (Utils.isLiveMode() && sharedCache && !Utils.isPortalEditMode() && Utils.isPortletViewMode(pContext))) {
      WCMService wcmService = getApplicationComponent(WCMService.class);
      pContext.getResponse().setProperty(MimeResponse.EXPIRATION_CACHE,
                                         "" + wcmService.getPortletExpirationCache());
      if (log.isTraceEnabled())
        log.trace("CLV rendering : cache set to " + wcmService.getPortletExpirationCache());
    }
    String nDisplayMode = preferences.getValue(PREFERENCE_DISPLAY_MODE, null);
    PortletMode npMode = pContext.getApplicationMode();
    if (!nDisplayMode.equals(currentDisplayMode)) {
      activateMode(npMode, nDisplayMode);
    } else {
      if (!npMode.equals(cpMode)) {
        activateMode(npMode, nDisplayMode);
      }
    }
    setId(UICLVPortlet.class.getSimpleName() + "_" + pContext.getWindowId());
    
    if (context.getRemoteUser() != null && WCMComposer.MODE_EDIT.equals(Utils.getCurrentMode())) {
      pContext.getJavascriptManager().loadScriptResource(ResourceScope.SHARED, "content-selector");
      pContext.getJavascriptManager().loadScriptResource(ResourceScope.SHARED, "quick-edit");
    }
    super.processRender(app, context);
  }

  /**
   * Decide which element will be displayed for correspond PortletMode/DisplayMode
   * @param npMode: View/Edit
   * @param nDisplayMode : FolderMode/ManualMode
   * @throws Exception : Exception will be throws if child addition action fails
   */
  private void activateMode(PortletMode npMode, String nDisplayMode) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    if (npMode.equals(cpMode)) {
      // Switch manual/auto
      // Not reach in the case of queryMode.
      removeChildren();
      if (Utils.isPortalEditMode()){
        clvConfig = addChild(UICLVConfig.class, null, null);
        clvConfig.setModeInternal(false);
      }else {
        if (nDisplayMode.equals(DISPLAY_MODE_AUTOMATIC)) {
          folderMode = addChild(UICLVFolderMode.class, null, UICLVFolderMode.class.getSimpleName() +
              "_" + pContext.getWindowId());
          folderMode.init();
          folderMode.setRendered(true);
        } else {
          manualMode = addChild(UICLVManualMode.class, null, UICLVManualMode.class.getSimpleName() +
              "_" + pContext.getWindowId());
          manualMode.init();
          manualMode.setRendered(true);
        }
      }
    } else {
      if (npMode.equals(PortletMode.VIEW)) { //Change from edit to iew
        removeChildren();
        if (nDisplayMode.equals(DISPLAY_MODE_AUTOMATIC)) {
          folderMode = addChild(UICLVFolderMode.class, null, UICLVFolderMode.class.getSimpleName() +
              "_" + pContext.getWindowId());
          folderMode.init();
          folderMode.setRendered(true);
        } else {
          manualMode = addChild(UICLVManualMode.class, null, UICLVManualMode.class.getSimpleName() +
              "_" + pContext.getWindowId());
          manualMode.init();
          manualMode.setRendered(true);
        }
      } else {
        // Change from view to edit
        removeChildren();
        clvConfig = addChild(UICLVConfig.class, null, null);
        clvConfig.setModeInternal(true);
      }
    }
    cpMode = npMode;
    currentDisplayMode = nDisplayMode;
  }
  private void removeChildren() {
    clvConfig = getChild(UICLVConfig.class);
    if (clvConfig != null)
      removeChild(UICLVConfig.class);
    folderMode = getChild(UICLVFolderMode.class);
    if (folderMode != null)
      removeChild(UICLVFolderMode.class);
    manualMode = getChild(UICLVManualMode.class);
    if (manualMode != null)
      removeChild(UICLVManualMode.class);
  }
  /**
   * Force porlet to change to ViewMode
   *
   * @throws Exception
   */
  public void changeToViewMode() throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    portletRequestContext.setApplicationMode(PortletMode.VIEW);
    updatePortlet();
  }

  /**
   * Update the current portlet if config/data changed.
   * @throws Exception
   */
  public void updatePortlet() throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletMode npMode = portletRequestContext.getApplicationMode();
    PortletPreferences preferences = Utils.getAllPortletPreferences();
    String nDisplayMode = preferences.getValue(PREFERENCE_DISPLAY_MODE, null);
    activateMode(npMode, nDisplayMode);
  }

  /**
   *
   * @param sqlQuery
   * @return
   */
  public String getQueryStatement(String sqlQuery) {
    HashSet<String> params = Utils.getQueryParams(sqlQuery);
    HashMap<String, String> queryParam = getQueryParammeter(params);

    return Utils.buildQuery(sqlQuery, queryParam);
  }

  /**
   *
   * @return
   */
  public boolean isQueryApplication() {
    return APPLICATION_CLV_BY_QUERY.equals(currentApplicationMode);
  }
}
