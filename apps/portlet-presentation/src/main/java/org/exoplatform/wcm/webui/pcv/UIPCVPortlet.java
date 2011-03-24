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
package org.exoplatform.wcm.webui.pcv;

import java.util.Date;

import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.RenderResponse;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.pcv.config.UIPCVConfig;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.w3c.dom.Element;

/*
 * Created by The eXo Platform SAS
 * Author : Anh Do Ngoc
 * anh.do@exoplatform.com
 * Sep 24, 2008
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class)
public class UIPCVPortlet extends UIPortletApplication {

  /** The Constant QUICK_EDIT_ABLE. */
  public final static String QUICK_EDIT_ABLE         = "quickEditable";

  /** The Constant PARAMETERIZED_MANAGEMENT_PORTLET_POPUP_WINDOW. */
  public static final String PCV_CONFIG_POPUP_WINDOW = "UIPCVConfigPopupWindow";

  /** The Constant REPOSITORY. */
  public final static String PREFERENCE_REPOSITORY   = "repository";

  /** The Constant REPOSITORY. */
  public final static String WORKSPACE               = "workspace";

  /** The Constant SHOW_TITLE. */
  public final static String SHOW_TITLE              = "showTitle";

  /** The Constant SHOW_DATE_CREATED. */
  public final static String SHOW_DATE_CREATED       = "showDateCreated";

  /** The Constant SHOW_BAR. */
  public final static String SHOW_BAR                = "showBar";

  private String             currentNodePath;

  /** The mode_. */
  private PortletMode        mode                    = PortletMode.VIEW;

  /**
   * Instantiates a new uI parameterized content viewer portlet.
   *
   * @throws Exception the exception
   */
  public UIPCVPortlet() throws Exception {
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
    getChildren().clear();
    addChild(UIPopupContainer.class, null, "UIPopupContainer-" + new Date().getTime());
    if (PortletMode.VIEW.equals(mode)) {
      addChild(UIPCVContainer.class, null, null);
    } else if (PortletMode.EDIT.equals(mode)) {
      addChild(UIPCVConfig.class, null, null);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    if (Util.getPortalRequestContext().getRequestParameter("path") != null) {
      setCurrentNodePath(Util.getPortalRequestContext().getRequestParameter("path").substring(1));
    }

    PortletRequestContext pContext = (PortletRequestContext) context;
    PortletMode newMode = pContext.getApplicationMode();
    if (!mode.equals(newMode)) {
      activateMode(newMode);
      mode = newMode;
    }
    UIPCVContainer container =  this.getChild(UIPCVContainer.class);
    if (container!=null && container.getNode()!=null) {
        RenderResponse response = context.getResponse();
      Element title = response.createElement("title");
      title.setTextContent(container.getTitle(container.getNode()));
      response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, title);
    }

    super.processRender(app, context) ;
  }

  /**
   * Can edit portlet.
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  public boolean canEditPortlet() throws Exception {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    String userId = context.getRemoteUser();
    return Utils.canEditCurrentPortal(userId);
  }

  public String getCurrentNodePath() {
    return currentNodePath;
  }

  public void setCurrentNodePath(String currentNodePath) {
    this.currentNodePath = currentNodePath;
  }

}
