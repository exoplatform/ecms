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

import javax.portlet.PortletMode;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 15, 2008
 */
/**
 * The Class UICLVPortlet.
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class)
public class UICLVPortlet extends UIPortletApplication {

  /** The Constant PREFERENCE_ITEMS_PER_PAGE. */
  public final static String PREFERENCE_ITEMS_PER_PAGE          = "itemsPerPage";

  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_PATH               = "folderPath";
  
  /** The Constant PREFERENCE_HEADER. */
  public final static String PREFERENCE_HEADER                  = "header";

  /** The Constant PREFERENCE_DISPLAY_TEMPLATE. */
  public final static String PREFERENCE_DISPLAY_TEMPLATE        = "formViewTemplatePath";

  /** The Constant PREFERENCE_PAGINATOR_TEMPLATE. */
  public final static String PREFERENCE_PAGINATOR_TEMPLATE      = "paginatorTemplatePath";

  /** The Constant PREFERENCE_SHOW_REFRESH_BUTTON. */
  public final static String PREFERENCE_SHOW_REFRESH_BUTTON     = "showRefreshButton";

  /** The Constant PREFERENCE_SHOW_ILLUSTRATION. */
  public final static String PREFERENCE_SHOW_ILLUSTRATION       = "showThumbnailsView";

  /** The Constant PREFERENCE_SHOW_TITLE. */
  public final static String PREFERENCE_SHOW_TITLE              = "showTitle";

  /** The Constant PREFERNECE_SHOW_SUMMARY. */
  public final static String PREFERENCE_SHOW_SUMMARY            = "showSummary";

  /** The Constant PREFERENCE_SHOW_DATE_CREATED. */
  public final static String PREFERENCE_SHOW_DATE_CREATED       = "showDateCreated";

  /** The Constant PREFERENCE_SHOW_HEADER. */
  public final static String PREFERENCE_SHOW_HEADER             = "showHeader";
  
  /** The Constant PREFERENCE_SHOW_READMORE. */
  public final static String PREFERENCE_SHOW_READMORE           = "showReadmore";

  /** The Constant PREFERENCE_SHOW_LINK. */
  public final static String PREFERENCE_SHOW_LINK               = "showLink";
  
  /** The Constant PREFERENCE_TARGET_PAGE. */
  public final static String  PREFERENCE_TARGET_PAGE            = "basePath";
  
  /** The Constant PREFERENCE_DISPLAY_MODE. */
  public static final String PREFERENCE_DISPLAY_MODE            = "mode";
  
  /** The Constant DISPLAY_MODE_MANUAL. */
  public static final String DISPLAY_MODE_MANUAL                = "ManualViewerMode";

  /** The Constant DISPLAY_MODE_AUTOMATIC. */
  public static final String DISPLAY_MODE_AUTOMATIC             = "AutoViewerMode";
  
  /** The Constant PREFERENCE_ORDER_BY. */
  public static final String PREFERENCE_ORDER_BY                = "orderBy";
  
  /** The Constant ORDER_BY_TITLE. */
  public static final String ORDER_BY_TITLE                     = "OrderByTitle";

  /** The Constant ORDER_BY_DATE_CREATED. */
  public static final String ORDER_BY_DATE_CREATED              = "OrderByDateCreated";

  /** The Constant ORDER_BY_DATE_MODIFIED. */
  public static final String ORDER_BY_DATE_MODIFIED             = "OrderByDateModified";

  /** The Constant ORDER_BY_DATE_PUBLISHED. */
  public static final String ORDER_BY_DATE_PUBLISHED            = "OrderByDatePublished";
  
  /** The Constant ORDER_BY_DATE_START_EVENT. */
  public static final String ORDER_BY_DATE_START_EVENT            = "OrderByDateStartEvent";
  
  /** The Constant PREFERENCE_ORDER_TYPE. */
  public static final String PREFERENCE_ORDER_TYPE              = "orderType";
  
  /** The Constant ORDER_TYPE_DESCENDENT. */
  public static final String ORDER_TYPE_DESCENDENT              = "OrderDesc";
  
  /** The Constant ORDER_TYPE_ASCENDENT. */
  public static final String ORDER_TYPE_ASCENDENT               = "OrderAsc";
  
  
  /**
   * Instantiates a new uICLV portlet.
   * 
   * @throws Exception the exception
   */
  public UICLVPortlet() throws Exception {
    addChild(UIPopupContainer.class, null, null);
    addChild(UICLVFolderMode.class, null, null).setRendered(false);
    addChild(UICLVManualMode.class, null, null).setRendered(false);
    addChild(UICLVConfig.class, null, null).setRendered(false);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform.webui.application.WebuiApplication, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context;
    PortletMode mode = pContext.getApplicationMode();
    if (mode == PortletMode.VIEW) {
      String displayMode = pContext.getRequest().getPreferences().getValue(PREFERENCE_DISPLAY_MODE, null);
      if (displayMode.equals(DISPLAY_MODE_AUTOMATIC)) {
        getChild(UICLVManualMode.class).setRendered(false);
        UICLVFolderMode clvFolderMode = getChild(UICLVFolderMode.class);
        clvFolderMode.init();
        clvFolderMode.setRendered(true);
      } else if (displayMode.equals(DISPLAY_MODE_MANUAL)) {
        getChild(UICLVFolderMode.class).setRendered(false);
        UICLVManualMode clvManualMode = getChild(UICLVManualMode.class);
        clvManualMode.init();
        clvManualMode.setRendered(true);
      }
    } else if (mode == PortletMode.EDIT) {
      getChild(UICLVConfig.class).setRendered(true);
    }
    super.processRender(app, context);
  }
}
