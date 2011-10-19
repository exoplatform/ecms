package org.exoplatform.workflow.webui.component.administration;

import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "classpath:groovy/ecm/webui/UIGridWithButton.gtmpl", 
    events = { 
        @EventConfig(listeners = UIRunningProcesses.CancelActionListener.class),
        @EventConfig(listeners = UIRunningProcesses.ViewActionListener.class),
        @EventConfig(listeners = UIRunningProcesses.DeleteActionListener.class,
                      confirm = "UIProcessDetail.msg.confirm-delete-process")
        
})
public class UIRunningProcesses extends UIGrid {  
  
  protected String[] genericActions;
  private static String[] ACTION                = { "View", "Delete" };
  private static String[] RUNNING_ACTIONS       = {"Cancel"};
  private static String[] PROCESS_BEAN_FIELD    = { "processInstanceId", "processId",
    "processName", "startDate", "endDate"};
 
  public UIRunningProcesses() throws Exception {
    getUIPageIterator().setId(this.getId());
    configure("processInstanceId", PROCESS_BEAN_FIELD, ACTION, RUNNING_ACTIONS);
  }
  
  public String[] getActions() {
    return genericActions;
  }
  
  public void configure(String beanIdField, String[] beanField, String[] actions, String[] genericActions) {
    this.genericActions = genericActions;
    super.configure(beanIdField, beanField, actions);
  }
  
  static public class CancelActionListener extends EventListener<UIRunningProcesses> {
    public void execute(Event<UIRunningProcesses> event) throws Exception {
      UIRunningProcesses uicomp = event.getSource();
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class);
      UIPopupWindow popup = uiAdministrationPortlet.getChildById("AdministrationPopup");
      if(popup != null){
        popup.setShow(false);
      }
    }
  }
  
  static  public class ViewActionListener extends EventListener<UIRunningProcesses> {
    public void execute(Event<UIRunningProcesses> event) throws Exception {
      UIProcessDetail uicomp = event.getSource().getParent();
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class);
      UIPopupWindow uiPopup = uiAdministrationPortlet.getChildById("TaskListOfProcessPopup");
      if (uiPopup == null)
        uiPopup = uiAdministrationPortlet.addChild(UIPopupWindow.class,
                                                   null,
                                                   "TaskListOfProcessPopup");
      uiPopup.setWindowSize(530, 300);
      uiPopup.setShowMask(true);
      UITaskListOfProcess uiTaskListOfProcess = uiAdministrationPortlet.createUIComponent(UITaskListOfProcess.class,
                                                                                          null,
                                                                                          null);
      String instance = event.getRequestContext().getRequestParameter(OBJECTID);
      for (ProcessInstance processInstance : uicomp.getRunningProcessInstance()){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild(UIRunningProcesses.class);
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
  
  static  public class DeleteActionListener extends EventListener<UIRunningProcesses> {
    public void execute(Event<UIRunningProcesses> event) throws Exception {
      UIProcessDetail uicomp = event.getSource().getParent();
      String instance = event.getRequestContext().getRequestParameter(OBJECTID);
      WorkflowServiceContainer workflowServiceContainer =
        uicomp.getApplicationComponent(WorkflowServiceContainer.class);
      for (ProcessInstance processInstance : uicomp.getRunningProcessInstance()){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild(UIRunningProcesses.class);
          break;
        }
      }
      workflowServiceContainer.deleteProcessInstance(instance);
      uicomp.updateProcessGrid(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
    }
  }  
}
