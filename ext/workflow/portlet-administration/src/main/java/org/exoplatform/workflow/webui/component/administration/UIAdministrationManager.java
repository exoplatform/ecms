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
package org.exoplatform.workflow.webui.component.administration ;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.Timer;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : tran the  trong
 *          trongtt@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */
@ComponentConfigs({
  @ComponentConfig(
      type = UIGrid.class, id = "UIECMGrid",
      template = "app:/groovy/webui/component/UIECMGrid.gtmpl"
  ),
  @ComponentConfig(
      template = "app:/groovy/webui/component/UITabPaneWithAction.gtmpl",
      events = {
          @EventConfig(listeners = UIAdministrationManager.ViewActionListener.class),
          @EventConfig(listeners = UIAdministrationManager.DeleteActionListener.class,
                       confirm = "UIAdministrationManager.msg.confirm-delete-process"),
          @EventConfig(listeners = UIAdministrationManager.UploadProcessActionListener.class)
      }
  )}
)
public class UIAdministrationManager extends UIContainer {
  private static String[] MONITOR_BEAN_FIELD = {"id", "name", "version"} ;
  private static String[] TIMERS_BEAN_FIELD = {"id", "name", "dueDate"} ;

  private static String[] ACTION = {"View","Delete"} ;
  private static String[] ACTIONS = {"UploadProcess"} ;

  public UIAdministrationManager() throws Exception {
    UIGrid uiMonitorGrid = addChild(UIGrid.class, "UIECMGrid", "UIMonitor") ;
    UIGrid uiTimersGrid = addChild(UIGrid.class, "UIECMGrid", "UITimers").setRendered(false) ;
    uiMonitorGrid.setLabel("UIMonitor") ;
    uiMonitorGrid.getUIPageIterator().setId("UIMonitorGrid") ;
    uiMonitorGrid.configure("id", MONITOR_BEAN_FIELD, ACTION) ;
    updateMonitorGrid() ;

    uiTimersGrid.setLabel("UITimers") ;
    uiTimersGrid.getUIPageIterator().setId("UITimersGrid") ;
    uiTimersGrid.configure("id", TIMERS_BEAN_FIELD, null) ;
    updateTimersGrid() ;
  }

  public String[] getActions() { return ACTIONS ; }

  public void updateMonitorGrid() throws Exception {
    UIGrid uiMonitorGrid = getChildById("UIMonitor");
    WorkflowServiceContainer workflowServiceContainer = getApplicationComponent(WorkflowServiceContainer.class);
    ListAccess<Process> processList = new ListAccessImpl<Process>(Process.class,
                                                                  workflowServiceContainer.getProcesses());
    uiMonitorGrid.getUIPageIterator().setPageList(new LazyPageList<Process>(processList, 10));
  }

  public void updateTimersGrid() throws Exception {
    WorkflowServiceContainer workflowServiceContainer = getApplicationComponent(WorkflowServiceContainer.class);
    UIGrid uiGrid = getChildById("UITimers");
    ListAccess<Timer> timerList = new ListAccessImpl<Timer>(Timer.class,
                                                            workflowServiceContainer.getTimers());
    uiGrid.getUIPageIterator().setPageList(new LazyPageList<Timer>(timerList, 10));
  }

  static  public class ViewActionListener extends EventListener<UIAdministrationManager> {
    public void execute(Event<UIAdministrationManager> event) throws Exception {
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = event.getSource().getParent() ;
      String id = event.getRequestContext().getRequestParameter(OBJECTID) ;
      //UIPopupWindow popup = uiAdministrationPortlet.getChild(UIPopupWindow.class);
      UIPopupWindow popup = uiAdministrationPortlet.getChildById("AdministrationPopup");
      if(popup != null){
        ((UIProcessDetail)popup.getUIComponent()).updateProcessGrid(id);
        popup.setShow(true) ;
        popup.setWindowSize(700, 0);
      }
    }
  }

  static  public class DeleteActionListener extends EventListener<UIAdministrationManager> {
    public void execute(Event<UIAdministrationManager> event) throws Exception {
      UIAdministrationManager uiAdminManager = event.getSource() ;
      String processDef = event.getRequestContext().getRequestParameter(OBJECTID) ;
      WorkflowServiceContainer workflowServiceContainer =
        uiAdminManager.getApplicationComponent(WorkflowServiceContainer.class) ;
      workflowServiceContainer.deleteProcess(processDef);
      uiAdminManager.updateMonitorGrid() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAdminManager) ;
    }
  }

  static  public class UploadProcessActionListener extends EventListener<UIAdministrationManager> {
    public void execute(Event<UIAdministrationManager> event) throws Exception {
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = event.getSource().getParent() ;
      uiAdministrationPortlet.initUploadPopup() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAdministrationPortlet) ;
    }
  }
}
