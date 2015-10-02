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
package org.exoplatform.wcm.webui.fastcontentcreator;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.wcm.webui.fastcontentcreator.config.UIFCCConfig;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class
)
public class UIFCCPortlet extends UIPortletApplication {

  /**
   * Instantiates a new uIFCC portlet.
   *
   * @throws Exception the exception
   */
  public UIFCCPortlet() throws Exception {
    addChild(UIPopupContainer.class, null, null);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
//    context.getJavascriptManager().importJavascript("eXo.ecm.ECMUtils",
//                                                    "/ecm-wcm-extension/javascript/");
    context.getJavascriptManager().require("SHARED/ecm-utils", "ecmutil").
            addScripts("ecmutil.ECMUtils.init('UIFastContentCreatorPortlet');");
    PortletRequestContext portletRequestContext = (PortletRequestContext) context;
    if (portletRequestContext.getApplicationMode() == PortletMode.VIEW) {
      if (getChild(UIFCCConfig.class) != null) {
        removeChild(UIFCCConfig.class);
      }
      if (getChild(UIFCCForm.class) == null) {
        UIFCCForm fastContentCreatorForm = addChild(UIFCCForm.class, null, null);
        PortletPreferences preferences = UIFCCUtils.getPortletPreferences();
        fastContentCreatorForm.setTemplateNode(preferences.getValue(UIFCCConstant.PREFERENCE_TYPE,
                                                                    ""));
        fastContentCreatorForm.setWorkspace(preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE,
                                                                 ""));
        fastContentCreatorForm.setStoredPath(preferences.getValue(UIFCCConstant.PREFERENCE_PATH, ""));
        fastContentCreatorForm.setRepositoryName(getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                                                 .getConfiguration()
                                                                                                 .getName());
      }
    } else if (portletRequestContext.getApplicationMode() == PortletMode.EDIT) {
      if (getChild(UIFCCForm.class) != null) {
        removeChild(UIFCCForm.class);
      }
      if (getChild(UIFCCConfig.class) == null) {
        UIFCCConfig fastContentCreatorConfig = addChild(UIFCCConfig.class, null, null);
        fastContentCreatorConfig.initEditMode();
      }
    }
    super.processRender(app, context) ;
  }
}
