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
package org.exoplatform.workflow.webui.component.administration;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Dec 15, 2006
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template =  "app:/groovy/webui/component/UIWorkflowPortlet.gtmpl",
    events = {
      @EventConfig(listeners = UIWorkflowAdministrationPortlet.RefreshSessionActionListener.class)
    }
)
public class UIWorkflowAdministrationPortlet extends UIPortletApplication {
  public UIWorkflowAdministrationPortlet() throws Exception {
    addChild(UIAdministrationManager.class, null, null) ;
    UIPopupWindow popup = addChild(UIPopupWindow.class, null, "AdministrationPopup") ;
    popup.setUIComponent(createUIComponent(UIProcessDetail.class, null, null)) ;
  }

  public void initUploadPopup() throws Exception {
    UIPopupWindow uiPopup = getChildById("UploadProcessPopup") ;
    if(uiPopup == null) uiPopup = addChild(UIPopupWindow.class, null, "UploadProcessPopup") ;
    uiPopup.setWindowSize(530, 300);
    UIUploadProcess uiUploadProcess = createUIComponent(UIUploadProcess.class, null, null) ;
    uiPopup.setUIComponent(uiUploadProcess) ;
    uiPopup.setRendered(true) ;
    uiPopup.setShowMask(false);
    uiPopup.setShow(true) ;
  }

  public static class RefreshSessionActionListener extends EventListener<UIWorkflowAdministrationPortlet> {
    public void execute(Event<UIWorkflowAdministrationPortlet> event) throws Exception {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      UIApplication uiApp = context.getUIApplication();
      UIAdministrationManager uiAdministrationManager = event.getSource().getChild(UIAdministrationManager.class);
      uiAdministrationManager.updateMonitorGrid();
      uiAdministrationManager.updateTimersGrid();
      String mess = "UIWorkflowAdministrationPortlet.msg.refresh-session-success";
      uiApp.addMessage(new ApplicationMessage(mess, null, ApplicationMessage.INFO));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAdministrationManager);
    }
  }
}
