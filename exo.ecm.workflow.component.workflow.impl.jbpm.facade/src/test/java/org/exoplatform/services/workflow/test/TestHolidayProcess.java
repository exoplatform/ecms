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
package org.exoplatform.services.workflow.test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.workflow.impl.jbpm.WorkflowServiceContainerImpl;
import org.jbpm.db.JbpmSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;



/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 10 mai 2004
 */
public class TestHolidayProcess extends BaseTest{

  private static final String PROCESS_FILE = "holidays.xml";
  private long processInstanceId;

  public TestHolidayProcess(String name) {
    super(name);
  }

  protected String getDescription() {
    return "test holiday process";
  }

  public void setUp()  {
    super.setUp();
    try {
      deployProcess(PROCESS_FILE, null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void tearDown() throws Exception {
    ((WorkflowServiceContainerImpl) workflowServiceContainer).closeSession();
  }  

  public void testProcessRegistration() {
    JbpmSession session = ((WorkflowServiceContainerImpl) workflowServiceContainer).openSession();
    List definitions = session.getGraphSession().findAllProcessDefinitions();
    ProcessDefinition definition = null;
    for (Iterator iter = definitions.iterator(); iter.hasNext();) {
      ProcessDefinition currentDefinition = (ProcessDefinition) iter.next();
      if("holiday process".equals(currentDefinition.getName())){
        definition = currentDefinition;
        break;
      }
    }
    assertEquals("holiday process", definition.getName());
    assertEquals(1, definition.getVersion());
    assertEquals("request", definition.getStartState().getName());
    ((WorkflowServiceContainerImpl) workflowServiceContainer).closeSession();
  }

  public void testProcessStart() throws Exception {
    JbpmSession session = ((WorkflowServiceContainerImpl) workflowServiceContainer).openSession();
    //session.beginTransaction();
    List definitions = session.getGraphSession().findAllProcessDefinitions();
    ProcessDefinition definition = null;
    for (Iterator iter = definitions.iterator(); iter.hasNext();) {
      ProcessDefinition currentDefinition = (ProcessDefinition) iter.next();
      if("holiday process".equals(currentDefinition.getName())){
        definition = currentDefinition;
        break;
      }
    }    
    
    Map variables = new HashMap();
    Date start =  new Date();
    variables.put("start.date", start);
    variables.put("end.date", new Date(start.getTime() + 24*3600));  
    
    
    Task startTask = definition.getTaskMgmtDefinition().getStartTask();
    System.out.println("Start Task Name " +startTask.getName());    
    
    ProcessInstance processInstance = new ProcessInstance(definition);
    processInstance.getContextInstance().addVariables(variables);

    TaskInstance taskInstance2 = processInstance.getTaskMgmtInstance().createStartTaskInstance();    
    System.out.println("TASK INSTANCE NAMMEEEEE "+taskInstance2.getName());
    System.out.println("TASK INSTANCE ACTOR "+taskInstance2.getActorId());
    System.out.println("TASK INSTANCE PREVIOUS ACTOR "+taskInstance2.getPreviousActorId());
    System.out.println("TASK INSTANCE SWIMLANE ACTOR "+taskInstance2.getSwimlaneInstance().getActorId());
    
    processInstance.signal();    
    
    session.getGraphSession().saveProcessInstance(processInstance);
    processInstanceId = processInstance.getId();        
    ((WorkflowServiceContainerImpl) workflowServiceContainer).closeSession();
    
    session = ((WorkflowServiceContainerImpl) workflowServiceContainer).openSession();
    
    processInstance = session.getGraphSession().loadProcessInstance(processInstanceId);
    
    TaskInstance taskInstance = (TaskInstance) session.getTaskMgmtSession().findTaskInstances(
        "bossOfBenj").get(0);
     
     
    System.out.println("TASK INSTANCE NAME "+taskInstance.getName());
    List formVariables = taskInstance.getTaskFormParameters();
    System.out.println("Variables  "+ formVariables);
    System.out.println("Previous Actor ID "+taskInstance.getPreviousActorId());
    System.out.println("Actor ID "+taskInstance.getActorId());
    SwimlaneInstance swimlanemInstance = taskInstance.getSwimlaneInstance();

    System.out.println("SWIMLANE Name "+swimlanemInstance.getName());
    System.out.println("SWIMLANE Actor "+swimlanemInstance.getActorId());
    
    List processInstances = session.getGraphSession().findProcessInstances(
        definition.getId()); 
    System.out.println("PROCESS NSTANCES FOUND "+processInstances.size());    
  }

  
  
  
}

