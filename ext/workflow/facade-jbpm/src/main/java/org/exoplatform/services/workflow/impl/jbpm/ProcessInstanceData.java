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

import org.jbpm.graph.exe.ProcessInstance;



/**
 * @author benjaminmestrallet
 */
public class ProcessInstanceData implements org.exoplatform.services.workflow.ProcessInstance{

  public static final String STARTED = "started";
  public static final String FINISHED = "finished";

  private String state = STARTED;
  private ProcessInstance processInstance;

  public ProcessInstanceData(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }
  public Date getEndDate() {
    return processInstance.getEnd();
  }

  public String getProcessId() {
    return ""+processInstance.getProcessDefinition().getId();
  }

  public String getProcessInstanceId() {
    return ""+processInstance.getId();
  }

  public Date getStartDate() {
    return processInstance.getStart();
  }

  public String getProcessName() {
    return processInstance.getProcessDefinition().getName();
  }

}
