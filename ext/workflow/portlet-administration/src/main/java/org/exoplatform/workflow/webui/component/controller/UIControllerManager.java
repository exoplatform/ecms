/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.workflow.Form;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 13, 2009
 */
@ComponentConfigs({
  @ComponentConfig(
      type = UIGrid.class, id = "UIControllerGrid",
      template = "app:/groovy/webui/component/UIECMGrid.gtmpl"
  ),
  @ComponentConfig(
      template = "app:/groovy/webui/component/UITabPane.gtmpl",
      events = {@EventConfig(listeners = UIControllerManager.ManageStartActionListener.class)}
  )
})
public class UIControllerManager extends UIContainer {
  private static String[] BPDEFINITION_BEAN_FIELD = {"id", "name", "version"} ;
  private static String[] ACTION = {"ManageStart"} ;
  private WorkflowServiceContainer service_;

  public UIControllerManager() throws Exception {
    service_ = getApplicationComponent(WorkflowServiceContainer.class) ;
    addChild(UITaskList.class, null, null) ;
    UIGrid uiBPDefinitionGrid = addChild(UIGrid.class, "UIControllerGrid", "UIBPDefinition").setRendered(false) ;
    uiBPDefinitionGrid.setLabel("UIBPDefinition") ;
    uiBPDefinitionGrid.getUIPageIterator().setId("UIBPDefinitionGrid") ;
    uiBPDefinitionGrid.configure("id", BPDEFINITION_BEAN_FIELD, ACTION) ;
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    WorkflowFormsService workflowFormsService = getApplicationComponent(WorkflowFormsService.class);
    List<Process> processes = service_.getProcesses();

    List<Process> visibleDefinitions = new ArrayList<Process>();
    for (Process process : processes) {
      workflowFormsService.removeForms(process.getId());
      Form form = workflowFormsService.getForm(process.getId(), process.getStartStateName(), locale);
      if (form != null && !form.isDelegatedView())
        visibleDefinitions.add(process);
    }
    UIGrid uiBPDefinitionGrid = getChild(UIGrid.class);
    ListAccess<Process> processList = new ListAccessImpl<Process>(Process.class, visibleDefinitions);
    uiBPDefinitionGrid.getUIPageIterator().setPageList(new LazyPageList<Process>(processList, 10));
    super.processRender(context);
  }

  static public class ManageStartActionListener extends EventListener<UIControllerManager> {
    public void execute(Event<UIControllerManager> event) throws Exception {
      UIControllerManager uiControllerManager = event.getSource();
      uiControllerManager.setRenderedChild(UIGrid.class);
      String processId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (uiControllerManager.service_.hasStartTask(processId)) {
        UIWorkflowControllerPortlet portlet = uiControllerManager.getParent();
        UIPopupContainer uiPopup = portlet.getChild(UIPopupContainer.class);
        UITaskManager uiTaskManager = portlet.createUIComponent(UITaskManager.class, null, null);
        uiTaskManager.setTokenId(processId);
        uiTaskManager.setIsStart(true);
        uiTaskManager.checkBeforeActive();
        uiPopup.getChild(UIPopupWindow.class).setShowMask(true);
        uiPopup.activate(uiTaskManager, 730, 500);
      } else {
        uiControllerManager.service_.startProcess(processId);
      }
    }
  }
}
