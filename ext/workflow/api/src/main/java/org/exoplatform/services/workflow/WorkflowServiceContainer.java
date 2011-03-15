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
package org.exoplatform.services.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * Created by the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 28 juin 2004
 */
public interface WorkflowServiceContainer {

  public static final String ACTOR_ID_KEY_SEPARATOR = ":";

  public void addPlugin(ComponentPlugin plugin) throws Exception;
  public void deployProcess(InputStream iS) throws IOException;

  public List<Process> getProcesses();
  public Process getProcess(String processId);
  public boolean hasStartTask(String processId);

  public List<ProcessInstance> getProcessInstances(String processId);
  public ProcessInstance getProcessInstance(String processInstance);

  public Map getVariables(String processInstanceId, String taskId);

  public List<Task> getTasks(String processInstanceId);
  public Task getTask(String taskId);

  public List<Task> getAllTasks(String user) throws Exception;
  public List<Task> getUserTaskList(String user);
  public List<Task> getGroupTaskList(String user) throws Exception;

  public WorkflowFileDefinitionService getFileDefinitionService();

  public List<Timer> getTimers();

  public void startProcess(String processId);
  public void startProcess(String remoteUser, String processId, Map variables);
  public void startProcessFromName(String remoteUser, String processName, Map variables);
  public void endTask(String taskId, Map variables);
  public void endTask(String taskId, Map variables, String transition);

  public void deleteProcess(String processId);
  public void deleteProcessInstance(String processInstanceId);
}
