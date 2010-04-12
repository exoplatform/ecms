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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
    type = UIGrid.class, 
    id = "UIProcessGrid",
    template = "app:/groovy/webui/component/UIECMGrid.gtmpl"
  ),
  @ComponentConfig(    
    template = "app:/groovy/webui/component/UITabPaneWithAction.gtmpl",
    events = {
      @EventConfig(listeners = UIProcessDetail.ViewActionListener.class),
      @EventConfig(listeners = UIProcessDetail.DeleteActionListener.class, confirm = "UIProcessDetail.msg.confirm-delete-process"),
      @EventConfig(listeners = UIProcessDetail.FlushAllActionListener.class, confirm = "UIProcessDetail.msg.confirm-delete-completed-process"),
      @EventConfig(listeners = UIProcessDetail.CancelActionListener.class)
    }
  )
})
public class UIProcessDetail extends UIContainer {
  private static String[] PROCESS_BEAN_FIELD = {"processInstanceId", "processId", "processName", "startDate", "endDate"};
  
  private static String[] ACTION = {"View","Delete"};
  private static String[] ACTIONS = {"FlushAll", "Cancel"};
  private String processInstanceId;
  private List<ProcessInstance> completedProcessInstanceList = new ArrayList<ProcessInstance>();
  private List<ProcessInstance> runningProcessInstanceList = new ArrayList<ProcessInstance>();
  
  public UIProcessDetail() throws Exception {
    UIGrid uiRunningProcess = addChild(UIGrid.class, "UIProcessGrid", "UIRunningProcessGrid");
    UIGrid uiCompletedProcess = addChild(UIGrid.class, "UIProcessGrid", "UICompletedProcessGrid").setRendered(false);    
    
    uiRunningProcess.setLabel("UIRunningProcessGrid");
    uiRunningProcess.getUIPageIterator().setId("UIRunningProcessGrid");
    uiRunningProcess.configure("processInstanceId", PROCESS_BEAN_FIELD, ACTION);
    
    uiCompletedProcess.setLabel("UICompletedProcessGrid");
    uiCompletedProcess.getUIPageIterator().setId("UICompletedProcessGrid");
    uiCompletedProcess.configure("processInstanceId", PROCESS_BEAN_FIELD, ACTION);        
  }
  
  public String[] getActions() { return ACTIONS; }
  
  public void updateProcessGrid(String id) throws Exception {
    completedProcessInstanceList.clear();
    runningProcessInstanceList.clear();
    WorkflowServiceContainer workflowServiceContainer = getApplicationComponent(WorkflowServiceContainer.class);
    if(id != null) processInstanceId = id;    
    List<ProcessInstance> processInstanceList = workflowServiceContainer.getProcessInstances(processInstanceId);
    for (ProcessInstance processInstance : processInstanceList){
      if(processInstance.getEndDate() != null)
        completedProcessInstanceList.add(processInstance);
      else
        runningProcessInstanceList.add(processInstance);
    }
    
    UIGrid uiCompletedProcess = getChildById("UICompletedProcessGrid");    
    uiCompletedProcess.getUIPageIterator().setPageList(new ObjectPageList(completedProcessInstanceList, 10));
    
    UIGrid uiRunningProcess = getChildById("UIRunningProcessGrid");
    uiRunningProcess.getUIPageIterator().setPageList(new ObjectPageList(runningProcessInstanceList, 10));
  }
    
  @SuppressWarnings("unchecked")
  public void updateTasksGrid(String id) throws Exception {
    WorkflowServiceContainer workflowServiceContainer = 
      getApplicationComponent(WorkflowServiceContainer.class);
    UIGrid uiGrid = getChildById("UIRunningProcessGrid");
    List<Task> haveEndDateList = new ArrayList<Task>();
    for(Task task : workflowServiceContainer.getTasks(id)) {      
      haveEndDateList.add(task);
    }
    Collections.sort(haveEndDateList, new TaskIdComparator());
    uiGrid.getUIPageIterator().setPageList(new ObjectPageList(haveEndDateList, 10));
  }

  /*
  static public class DateComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      Date date1 = ((Task) o1).getEnd();
      Date date2 = ((Task) o2).getEnd();
      return date1.compareTo(date2);
    }
  }
  */
  
  static public class TaskIdComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      String id1 = ((Task) o1).getId();
      String id2 = ((Task) o2).getId();
      return id1.compareTo(id2);
    }
  }
  
  static  public class ViewActionListener extends EventListener<UIProcessDetail> {
    public void execute(Event<UIProcessDetail> event) throws Exception {
      UIProcessDetail uicomp = event.getSource() ;
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class); 
      UIPopupWindow uiPopup = uiAdministrationPortlet.getChildById("TaskListOfProcessPopup");
      if(uiPopup == null) uiPopup = uiAdministrationPortlet.addChild(UIPopupWindow.class, null, "TaskListOfProcessPopup");
      uiPopup.setWindowSize(530, 300);
      UITaskListOfProcess uiTaskListOfProcess = uiAdministrationPortlet.createUIComponent(UITaskListOfProcess.class, null, null);
      String instance = event.getRequestContext().getRequestParameter(OBJECTID);
      for (ProcessInstance processInstance : uicomp.completedProcessInstanceList){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild("UICompletedProcessGrid");
          break;
        }
      }
      for (ProcessInstance processInstance : uicomp.runningProcessInstanceList){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild("UIRunningProcessGrid");
          break;
        }
      }
      uiTaskListOfProcess.updateTasksGrid(instance);
      uiPopup.setUIComponent(uiTaskListOfProcess);
      uiPopup.setRendered(true) ;
      uiPopup.setShow(true);
      uiPopup.setWindowSize(650, 0);
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UIProcessDetail> {
    public void execute(Event<UIProcessDetail> event) throws Exception {
      UIProcessDetail uicomp = event.getSource();
      String instance = event.getRequestContext().getRequestParameter(OBJECTID);
      WorkflowServiceContainer workflowServiceContainer = 
        uicomp.getApplicationComponent(WorkflowServiceContainer.class);      
      for (ProcessInstance processInstance : uicomp.completedProcessInstanceList){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild("UICompletedProcessGrid");
          break;
        }
      }
      for (ProcessInstance processInstance : uicomp.runningProcessInstanceList){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild("UIRunningProcessGrid");
          break;
        }
      }
      workflowServiceContainer.deleteProcessInstance(instance);      
      uicomp.updateProcessGrid(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
    }
  }
  
  static  public class FlushAllActionListener extends EventListener<UIProcessDetail> {
    public void execute(Event<UIProcessDetail> event) throws Exception {
      UIProcessDetail uicomp = event.getSource();      
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class) ;      
      if(uicomp.completedProcessInstanceList.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIProcessDetail.msg.result-delete-completed-process", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      }      
      WorkflowServiceContainer workflowServiceContainer = uicomp.getApplicationComponent(WorkflowServiceContainer.class);
      for (ProcessInstance processInstance : uicomp.completedProcessInstanceList){
        workflowServiceContainer.deleteProcessInstance(processInstance.getProcessInstanceId());
      }      
      uicomp.updateProcessGrid(null);
      uicomp.setRenderedChild("UICompletedProcessGrid");
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
    }
  }
  
  static public class CancelActionListener extends EventListener<UIProcessDetail> {
    public void execute(Event<UIProcessDetail> event) throws Exception {
      UIProcessDetail uicomp = event.getSource();
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class);
      UIPopupWindow popup = uiAdministrationPortlet.getChildById("AdministrationPopup");
      if(popup != null){
        popup.setShow(false);
      }
    }
  }  
}