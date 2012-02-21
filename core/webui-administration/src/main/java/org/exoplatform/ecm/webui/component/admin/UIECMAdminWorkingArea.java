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
package org.exoplatform.ecm.webui.component.admin;

import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletPreferences;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Sep 19, 2006
 * 8:30:33 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/UIECMAdminWorkingArea.gtmpl"
)
public class UIECMAdminWorkingArea extends UIContainer {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIECMAdminWorkingArea.class);
  private String renderedCompId_ ;

  public String getRenderedCompId() { return renderedCompId_ ; }
  public void setRenderedCompId(String renderedId) { this.renderedCompId_ = renderedId ; }

  public <T extends UIComponent> void setChild(Class<T> type) {
    renderedCompId_ = getChild(type).getId();
    setRenderedChild(type);
  }

  public UIECMAdminWorkingArea() throws Exception {}

  public void init() throws Exception {
    UIECMAdminPortlet portlet = getAncestorOfType(UIECMAdminPortlet.class);
    UIECMAdminControlPanel controlPanel = portlet.getChild(UIECMAdminControlPanel.class);
    List<UIAbstractManagerComponent> managers = controlPanel.getManagers();
    List<UIAbstractManagerComponent> rejectedManagers = null;
    if (managers == null) {
      return;
    }
    for (UIAbstractManagerComponent manager : managers) {
      UIAbstractManager uiManager = getChild(manager.getUIAbstractManagerClass());
      if (uiManager == null) {
        uiManager = addChild(manager.getUIAbstractManagerClass(), null, null);
        if (renderedCompId_ == null) {
          try {
            uiManager.init();
            renderedCompId_ = uiManager.getId();
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("The manager " + uiManager.getClass() + " cannot be initialized, it will be unregistered", e);
            }
            if (rejectedManagers == null) {
              rejectedManagers = new ArrayList<UIAbstractManagerComponent>();
            }
            rejectedManagers.add(manager);
            removeChild(manager.getUIAbstractManagerClass());
          }
        } else {
          uiManager.setRendered(false);
        }
      } else {
        uiManager.refresh();
      }
    }
    if (rejectedManagers != null) {
      for (UIAbstractManagerComponent manager : rejectedManagers) {
        controlPanel.unregister(manager);
      }
    }
  }

  public void checkRepository() throws Exception{
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext
        .getCurrentInstance();
    PortletPreferences pref = pcontext.getRequest().getPreferences();
    try {
      getApplicationComponent(RepositoryService.class).getCurrentRepository();
    } catch (Exception e) {
      String defaultRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository()
          .getConfiguration().getName();
      pref.setValue(Utils.REPOSITORY, defaultRepo);
      pref.store();
    }
  }

}
