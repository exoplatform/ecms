/*
 * Copyright (C) 2003-2008 eXo Platform SAS. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.search;

import javax.portlet.PortletMode;

import org.exoplatform.wcm.webui.search.config.UIPortletConfig;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class)
public class UIWCMSearchPortlet extends UIPortletApplication {

  /** The mode. */
  private PortletMode        mode                             = PortletMode.VIEW;

  /** The Constant SEARCH_FORM_TEMPLATE_PATH. */
  public static final String SEARCH_FORM_TEMPLATE_PATH        = "searchFormTemplatePath";

  /** The Constant SEARCH_RESULT_TEMPLATE_PATH. */
  public static final String SEARCH_RESULT_TEMPLATE_PATH      = "searchResultTemplatePath";

  /** The Constant SEARCH_PAGINATOR_TEMPLATE_PATH. */
  public static final String SEARCH_PAGINATOR_TEMPLATE_PATH   = "searchPaginatorTemplatePath";

  /** The Constant SEARCH_PAGE_LAYOUT_TEMPLATE_PATH. */
  public static final String SEARCH_PAGE_LAYOUT_TEMPLATE_PATH = "searchPageLayoutTemplatePath";

  /** The Constant REPOSITORY. */
  public static final String REPOSITORY                       = "repository";

  /** The Constant WORKSPACE. */
  public static final String WORKSPACE                        = "workspace";

  /** The Constant ITEMS_PER_PAGE. */
  public final static String ITEMS_PER_PAGE                   = "itemsPerPage";

  /** The Constant PAGE_MODE. */
  public final static String  PAGE_MODE                       = "pageMode";

  /** The Constant SHOW_DATE_CREATED. */
  public final static String BASE_PATH                        = "basePath";
  
  /** The Constant DETAIL_PARAMETER_NAME. */
  public final static String DETAIL_PARAMETER_NAME            = "detailParameterName";
  
  

  /**
   * Instantiates a new uIWCM search portlet.
   *
   * @throws Exception the exception
   */
  public UIWCMSearchPortlet() throws Exception {
    activateMode(mode);

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
    PortletMode newMode = pContext.getApplicationMode();
    if (!mode.equals(newMode)) {
      activateMode(newMode);
      mode = newMode;
    }
    super.processRender(app, context);
  }

  /**
   * Activate mode.
   *
   * @param mode the mode
   * @throws Exception the exception
   */
  private void activateMode(PortletMode mode) throws Exception {
    getChildren().clear();
    addChild(UIPopupContainer.class, null, "UISearchedContentEdittingPopup");
    if (PortletMode.VIEW.equals(mode)) {
      addChild(UISearchPageLayout.class, null, UIPortletApplication.VIEW_MODE);
    } else if (PortletMode.EDIT.equals(mode)) {
      addChild(UIPortletConfig.class, null, UIPortletApplication.EDIT_MODE);
    }
  }
}
