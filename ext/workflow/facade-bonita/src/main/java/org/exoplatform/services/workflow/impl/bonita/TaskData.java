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
package org.exoplatform.services.workflow.impl.bonita;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.exoplatform.services.workflow.Task;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaException;

/**
 * Created by Bull R&D
 * @author Brice Revenant
 * Jan 1, 2006
 */
public class TaskData implements Task {
  private String actorId           = null;
  private String description       = null;
  private String id                = null;
  private String processId         = null;
  private String processInstanceId = null;
  private String taskName          = null;
  private Date   end               = null;

  private static Logger log = Logger.getLogger(TaskData.class.getName());

  public TaskData(ActivityInstance<TaskInstance> task) {
    if(task.getBody().isTaskAssigned()){
      this.actorId = task.getBody().getTaskUser();
    } else {
      this.actorId = "";
      boolean separator = false;
      for(String candidat : task.getBody().getTaskCandidates()){
        if(separator) this.actorId += " , ";
        else separator = true;
        this.actorId += candidat;
      }
    }
    try {
      this.description = AccessorUtil.getQueryAPIAccessor()
                                     .getQueryDefinitionAPI()
                                     .getProcessActivity(task.getProcessDefinitionUUID(),
                                                         task.getActivityId())
                                     .getDescription();
      this.id = task.getBody().getUUID().toString();
      this.processInstanceId = task.getProcessInstanceUUID().toString();
      this.taskName = task.getActivityId();
      this.processId = task.getProcessDefinitionUUID().toString();
      //TODO delete the IllegalStateException catching

      this.end = task.getBody().getEndedDate();
  } catch (BonitaException e) {
    log.log(Level.WARNING, e.getMessage(), e);
  }
  if(log.isLoggable(Level.INFO)){
      log.info("New task created [taskId,taskName,actorId,processId,instanceId,description,end]:"
          + "[" + this.id + "," + this.taskName + "," + this.actorId + "," + this.processId + ","
          + this.processInstanceId + "," + this.description + "," + this.end);
  }
  }

  public String getActorId() {
    return this.actorId;
  }

  public String getDescription() {
    return this.description;
  }

  public String getId() {
    return this.id;
  }

  public String getProcessId() {
    return this.processId;
  }

  public String getProcessInstanceId() {
    return this.processInstanceId;
  }

  public String getTaskName() {
    return this.taskName;
  }

  public Date getEnd() {
    return this.end;
  }
}
