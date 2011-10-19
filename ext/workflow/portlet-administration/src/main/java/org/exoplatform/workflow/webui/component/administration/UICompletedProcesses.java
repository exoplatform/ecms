package org.exoplatform.workflow.webui.component.administration;

import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "classpath:groovy/ecm/webui/UIGridWithButton.gtmpl", 
    events = { @EventConfig(listeners = UICompletedProcesses.FlushAllActionListener.class,
                             confirm = "UIProcessDetail.msg.confirm-delete-completed-process"),
               @EventConfig(listeners = UICompletedProcesses.CancelActionListener.class),
               @EventConfig(listeners = UICompletedProcesses.ViewActionListener.class),
               @EventConfig(listeners = UICompletedProcesses.DeleteActionListener.class,
                             confirm = "UIProcessDetail.msg.confirm-delete-process")
})
public class UICompletedProcesses extends UIGrid {  
  
  protected String[] genericActions;
  
  private static String[] COMPLETED_ACTIONS = {"FlushAll", "Cancel"};
  private static String[] PROCESS_BEAN_FIELD    = { "processInstanceId", "processId",
    "processName", "startDate", "endDate"};
  private static String[] ACTION                = { "View", "Delete" };
  
  public UICompletedProcesses() throws Exception {
    getUIPageIterator().setId(this.getId());
    configure("processInstanceId", PROCESS_BEAN_FIELD, ACTION, COMPLETED_ACTIONS);
  }
  
  public String[] getActions() {
    return genericActions;
  }
  public void configure(String beanIdField, String[] beanField, String[] actions, String[] genericActions) {
    this.genericActions = genericActions;
    super.configure(beanIdField, beanField, actions);
  }
  
  static  public class FlushAllActionListener extends EventListener<UICompletedProcesses> {
    public void execute(Event<UICompletedProcesses> event) throws Exception {
      UIProcessDetail uicomp = event.getSource().getParent();
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class) ;
      if(uicomp.getCompletedProcessInstance().size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIProcessDetail.msg.result-delete-completed-process", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      WorkflowServiceContainer workflowServiceContainer = uicomp.getApplicationComponent(WorkflowServiceContainer.class);
      for (ProcessInstance processInstance : uicomp.getCompletedProcessInstance()){
        workflowServiceContainer.deleteProcessInstance(processInstance.getProcessInstanceId());
      }
      uicomp.updateProcessGrid(null);
      uicomp.setRenderedChild(UICompletedProcesses.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
    }
  }

  static public class CancelActionListener extends EventListener<UICompletedProcesses> {
    public void execute(Event<UICompletedProcesses> event) throws Exception {
      UICompletedProcesses uicomp = event.getSource();
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class);
      UIPopupWindow popup = uiAdministrationPortlet.getChildById("AdministrationPopup");
      if(popup != null){
        popup.setShow(false);
      }
    }
  }  
  
  static  public class DeleteActionListener extends EventListener<UICompletedProcesses> {
    public void execute(Event<UICompletedProcesses> event) throws Exception {
      UIProcessDetail uicomp = event.getSource().getParent();
      String instance = event.getRequestContext().getRequestParameter(OBJECTID);
      WorkflowServiceContainer workflowServiceContainer =
        uicomp.getApplicationComponent(WorkflowServiceContainer.class);
      for (ProcessInstance processInstance : uicomp.getCompletedProcessInstance()){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild(UICompletedProcesses.class);
          break;
        }
      }
      workflowServiceContainer.deleteProcessInstance(instance);
      uicomp.updateProcessGrid(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
    }
  }
  
  static  public class ViewActionListener extends EventListener<UICompletedProcesses> {
    public void execute(Event<UICompletedProcesses> event) throws Exception {
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
      for (ProcessInstance processInstance : uicomp.getCompletedProcessInstance()){
        if(processInstance.getProcessInstanceId().equals(instance)){
          uicomp.setRenderedChild(UICompletedProcesses.class);
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
}
