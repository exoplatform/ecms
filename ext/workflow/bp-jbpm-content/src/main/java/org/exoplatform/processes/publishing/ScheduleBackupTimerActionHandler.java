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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.workflow.impl.jbpm.WorkflowServiceContainerImpl;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;

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
          //create the timer
          org.jbpm.job.Timer jobTimer = new org.jbpm.job.Timer(context.getToken());
          jobTimer.setName("backupTimer");
          jobTimer.setDueDate(endDate);
          jobTimer.setGraphElement(context.getEventSource());
          jobTimer.setTaskInstance(context.getTaskInstance());
          jobTimer.setAction(backupAction);
          jobTimer.setTransitionName("end");
          ProcessUtil.createTimer(context, jobTimer);

          //TODO we should change this code to update process by asynchronys technichque
          WorkflowServiceContainerImpl containerImpl = ProcessUtil.getService(context, WorkflowServiceContainerImpl.class);
          JbpmContext jbpmContext = containerImpl.openJbpmContext();
          jbpmContext.save(context.getProcessInstance());
        } else {
          backupContent(context);
          context.getProcessInstance().getRootToken().signal("backup-done");
        }
      }
    } catch (Exception ex) {
      ExoLogger.getLogger(this.getClass()).equals(ex);
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", ex);
      }
    }
  }

}
