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
package org.exoplatform.processes.publishing;

import java.util.Date;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.workflow.impl.jbpm.WorkflowServiceContainerImpl;
import org.jbpm.db.JbpmSession;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.scheduler.exe.Timer;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 13, 2007  
 */
public class ScheduleBackupTimerActionHandler extends BackupContentActionHandler {

  private static final long serialVersionUID = 1L;
  private static final Log LOG  = ExoLogger.getLogger(ScheduleBackupTimerActionHandler.class);
  
  public void execute(ExecutionContext context) {    
    try {              
      Date currentDate = new Date();
      Date endDate = null;
      if (context.getVariable("endDate") instanceof Date) {
        endDate = (Date) context.getVariable("endDate");
      }
      if (endDate != null) {
        if (endDate.after(currentDate)) {        
          // Create and save the Action object
          Delegation delegation = new Delegation();
          delegation.setClassName("org.exoplatform.processes.publishing.BackupContentActionHandler");        
          delegation.setProcessDefinition(context.getProcessDefinition());
          
          Action backupAction = new Action();
          backupAction.setName("backupAction");
          backupAction.setActionDelegation(delegation);
          backupAction.setProcessDefinition(context.getProcessDefinition());        
          context.getProcessDefinition().addAction(backupAction);
          // create the timer               
          //Token token = new Token(context.getToken(),context.getNode().getName());               
          Timer timer = new Timer(context.getToken());        
          timer.setName("backupTimer");
          timer.setDueDate(endDate);
          timer.setGraphElement(context.getEventSource());
          timer.setTaskInstance(context.getTaskInstance());
          timer.setAction(backupAction);
          timer.setTransitionName("end");        
          context.getSchedulerInstance().schedule(timer);
          //TODO we should change this code to update process by asynchronys technichque
          WorkflowServiceContainerImpl containerImpl = ProcessUtil.getService(WorkflowServiceContainerImpl.class);
          JbpmSession session = containerImpl.openSession();
          session.beginTransaction();
          session.getGraphSession().saveProcessInstance(context.getProcessInstance());        
          session.commitTransaction();
        } else {                
          backupContent(context);        
          context.getToken().signal("backup-done");
        }      
      }
    } catch (Exception ex) {
      ExoLogger.getLogger(this.getClass()).equals(ex);
      LOG.error("Unexpected error", ex);
    }
  }

}