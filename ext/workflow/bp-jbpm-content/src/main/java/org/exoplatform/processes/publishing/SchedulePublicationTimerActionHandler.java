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

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 13, 2007
 */
public class SchedulePublicationTimerActionHandler extends ManagePublicationActionHandler {

  private static final long serialVersionUID = 1L;

  private Log log = ExoLogger.getLogger(this.getClass());

  public void execute(ExecutionContext context) {
    ProcessUtil.approve(context);
    Date startDate = (Date) context.getVariable("startDate");
    Date currentDate = new Date();
    if (startDate.before(currentDate)) {
      try {
        publishContent(context);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error(e);
        }
      }
      //context.getToken().signal("publication-done");
      context.getProcessInstance().getRootToken().signal("publication-done");
    } else {
      try{
        moveToPending(context);
      }catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error(e);
        }
      }
      //Create and save the Action object
      Delegation delegation = new Delegation();
      delegation.setClassName("org.exoplatform.processes.publishing.ManagePublicationActionHandler");
      delegation.setProcessDefinition(context.getProcessDefinition());

      Action publicationAction = new Action();
      publicationAction.setName("publicationAction");
      publicationAction.setActionDelegation(delegation);
      context.getProcessDefinition().addAction(publicationAction);

      //create the timer
      org.jbpm.job.Timer jobTimer = new org.jbpm.job.Timer();

      jobTimer.setName("publicationTimer");
      jobTimer.setDueDate(startDate);
      jobTimer.setGraphElement(context.getEventSource());
      jobTimer.setTaskInstance(context.getTaskInstance());
      jobTimer.setAction(publicationAction);
      jobTimer.setTransitionName("manage-backup");
      ProcessUtil.createTimer(context, jobTimer);

      /*Timer timer = new Timer(context.getToken());
      timer.setName("publicationTimer");
      timer.setDueDate(startDate);
      timer.setGraphElement(context.getEventSource());
      timer.setTaskInstance(context.getTaskInstance());
      timer.setAction(publicationAction);
      timer.setTransitionName("manage-backup");
      context.getSchedulerInstance().schedule(timer);*/
    }
  }

  private void moveToPending(ExecutionContext context) throws Exception {
    String[] currentLocation = ProcessUtil.getCurrentLocation(context);
    String currentWorkspace = currentLocation[1];
    String currentPath = currentLocation[2];
    String pendingWorksapce = (String)context.getVariable("exo:pendingWorkspace");
    String pendingPath = (String)context.getVariable("exo:pendingPath");
    String destPath = ProcessUtil.computeDestinationPath(context, currentPath,pendingPath);
    CmsService cmsService = ProcessUtil.getService(context, CmsService.class);
    cmsService.moveNode(currentPath, currentWorkspace, pendingWorksapce, destPath);
    ProcessUtil.setCurrentLocation(context,pendingWorksapce,destPath);
    ProcessUtil.waitForPublish(context);
  }
}
