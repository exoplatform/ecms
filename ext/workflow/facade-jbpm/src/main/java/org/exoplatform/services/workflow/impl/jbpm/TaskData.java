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
package org.exoplatform.services.workflow.impl.jbpm;

import java.util.Date;

import org.exoplatform.services.workflow.Task;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;


/**
 * @author benjaminmestrallet
 */
public class TaskData implements Task{

  public static final String STARTED = "started";
  public static final String FINISHED = "finished";

  private TaskInstance taskInstance;
  private String imageURL_;

  public TaskData(TaskInstance taskInstance) {
    this.taskInstance = taskInstance;
  }

  public String getId(){
    return "" + taskInstance.getId();
  }

  public String getTaskName(){
    return taskInstance.getName();
  }

  public String getActorId(){
    String actorId = taskInstance.getActorId();
    if(actorId == null) {
      SwimlaneInstance swimlane = taskInstance.getSwimlaneInstance();
      if(swimlane != null)
        actorId = swimlane.getActorId();
    }
    if(actorId == null)
      actorId = "N/A";
    return actorId;
  }

  public String getSwimlane(){
    return taskInstance.getSwimlaneInstance().getName();
  }

  public Date getEnd(){
    return taskInstance.getEnd();
  }

  public String getProcessId() {
    return "" + taskInstance.getToken().getProcessInstance().getProcessDefinition().getId();
  }

  public String getProcessInstanceId() {
    return "" + taskInstance.getToken().getProcessInstance().getId();
  }

  public String getDescription() {
    return taskInstance.getTask().getDescription();
  }

}
