/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.pcv.config;

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.pcv.UIPCVPortlet;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 *          ngoc.tran@exoplatform.com
 * Oct 20, 2009
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/ParameterizedContentViewer/config/UIPCVConfig.gtmpl",
                 events = {
                     @EventConfig(listeners = UIPCVConfig.CancelActionListener.class),
                     @EventConfig(listeners = UIPCVConfig.SaveActionListener.class)
                   }
               )
public class UIPCVConfig extends UIForm {

  /** The Constant SHOW_TITLE. */
  public static final String SHOW_TITLE                      = "ShowTitle";

  /** The Constant SHOW_DATE_CREATED. */
  public static final String SHOW_DATE_CREATED               = "ShowDateCreated";

  /** The Constant SHOW_BAR. */
  public static final String SHOW_BAR = "ShowBar";

  @SuppressWarnings("unchecked")
  public UIPCVConfig() {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = context.getRequest().getPreferences();

    UIFormCheckBoxInput titleViewerCheckbox = new UIFormCheckBoxInput(SHOW_TITLE, SHOW_TITLE, null);
    titleViewerCheckbox.setChecked(true);
    String titleShowAble = portletPreferences.getValue(UIPCVPortlet.SHOW_TITLE, null);
    titleViewerCheckbox.setChecked(Boolean.parseBoolean(titleShowAble));

    UIFormCheckBoxInput dateCreatedViewerCheckbox = new UIFormCheckBoxInput(SHOW_DATE_CREATED, SHOW_DATE_CREATED, null);
    dateCreatedViewerCheckbox.setChecked(true);
    String dateShowAble = portletPreferences.getValue(UIPCVPortlet.SHOW_DATE_CREATED, null);
    dateCreatedViewerCheckbox.setChecked(Boolean.parseBoolean(dateShowAble));

    UIFormCheckBoxInput barViewerCheckbox = new UIFormCheckBoxInput(SHOW_BAR, SHOW_BAR, null);
    barViewerCheckbox.setChecked(true);
    String barShowable = portletPreferences.getValue(UIPCVPortlet.SHOW_BAR, null);
    barViewerCheckbox.setChecked(Boolean.parseBoolean(barShowable));

    addChild(titleViewerCheckbox);
    addChild(dateCreatedViewerCheckbox);
    addChild(barViewerCheckbox);

    setActions(new String[] { "Save", "Cancel" });
  }



  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UIPCVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPCVConfig> event) throws Exception {
      UIPCVConfig uiPCVConfigForm = event.getSource();
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
      RepositoryService repositoryService = uiPCVConfigForm.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

      String repository = manageableRepository.getConfiguration().getName();
      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();

      String showTitle = uiPCVConfigForm.getUIFormCheckBoxInput(UIPCVConfig.SHOW_TITLE).isChecked() ? "true" : "false";
      String showDateCreated = uiPCVConfigForm.getUIFormCheckBoxInput(UIPCVConfig.SHOW_DATE_CREATED)
                                              .isChecked() ? "true" : "false";
      String showBar = uiPCVConfigForm.getUIFormCheckBoxInput(UIPCVConfig.SHOW_BAR).isChecked() ? "true" : "false";

      portletPreferences.setValue(UIPCVPortlet.PREFERENCE_REPOSITORY, repository);
      portletPreferences.setValue(UIPCVPortlet.WORKSPACE, workspace);
      portletPreferences.setValue(UIPCVPortlet.SHOW_TITLE, showTitle);
      portletPreferences.setValue(UIPCVPortlet.SHOW_DATE_CREATED, showDateCreated);
      portletPreferences.setValue(UIPCVPortlet.SHOW_BAR, showBar);
      portletPreferences.store();

      if (Utils.isQuickEditMode(uiPCVConfigForm, UIPCVPortlet.PCV_CONFIG_POPUP_WINDOW)) {
        Utils.closePopupWindow(uiPCVConfigForm, UIPCVPortlet.PCV_CONFIG_POPUP_WINDOW);
      } else {
        Utils.createPopupMessage(uiPCVConfigForm, "UIPCVConfig.msg.saving-success", null, ApplicationMessage.INFO);
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UIPCVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPCVConfig> event) throws Exception {
      UIPCVConfig viewerManagementForm = event.getSource();
      Utils.closePopupWindow(viewerManagementForm, UIPCVPortlet.PCV_CONFIG_POPUP_WINDOW);
      ((PortletRequestContext)event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
    }
  }
}
