/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component.controller;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 13, 2009
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template =  "app:/groovy/webui/component/UIWorkflowPortlet.gtmpl",
    events = {
      @EventConfig(listeners = UIWorkflowControllerPortlet.RefreshSessionActionListener.class)
    }
)
public class UIWorkflowControllerPortlet extends UIPortletApplication {
  private boolean isShowMonitor = false ;

  public boolean isShowMonitor() { return isShowMonitor ; }

  public UIWorkflowControllerPortlet() throws Exception {
    addChild(UIControllerManager.class, null, null) ;
    UIPopupContainer uiWorkflowPopup = addChild(UIPopupContainer.class, null, null) ;
    uiWorkflowPopup.getChild(UIPopupWindow.class).setId("ControllerPopup") ;
  }

  static public class RefreshSessionActionListener extends EventListener<UIWorkflowControllerPortlet> {
    public void execute(Event<UIWorkflowControllerPortlet> event) throws Exception {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      UIApplication uiApp = context.getUIApplication();
      String mess = "UIWorkflowControllerPortlet.msg.refresh-session-success";
      uiApp.addMessage(new ApplicationMessage(mess, null, ApplicationMessage.INFO));
      UIControllerManager uiControllerManager = event.getSource().getChild(UIControllerManager.class);
      UITaskList uiTaskList = uiControllerManager.getChild(UITaskList.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaskList) ;
    }
  }
}
