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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.jaas.BasicCallbackHandler;
import org.exoplatform.services.workflow.FileDefinition;
import org.exoplatform.services.workflow.Form;
import org.exoplatform.services.workflow.PredefinedProcessesPlugin;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.ProcessesConfig;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.Timer;
import org.exoplatform.services.workflow.WorkflowFileDefinitionService;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.dataType.BasicTypeDefinition;
import org.ow2.bonita.facade.def.dataType.DataTypeValue;
import org.ow2.bonita.facade.def.dataType.EnumerationTypeDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.var.Enumeration;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.facade.uuid.TaskUUID;
import org.ow2.bonita.facade.uuid.UUIDFactory;
import org.ow2.bonita.identity.auth.BonitaPrincipal;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaException;
import org.ow2.bonita.util.Misc;
import org.picocontainer.Startable;

/**
 * Bonita implementation of the Workflow Service in eXo Platform
 *
 * Created by Bull R&D
 * @author Brice Revenant , Rodrigue Le Gall
 * Dec 25, 2005
 */

public class WorkflowServiceContainerImpl implements WorkflowServiceContainer, Startable {

  /**
   * Holds variables to be set while instantiating a Process. Indeed Bonita
   * currently executes some mappers and hooks prior having the opportunity to
   * set variables in the instance. By accessing this Thread Local, mappers and
   * hooks can retrieve them.
   */
  public static ThreadLocal<Map<String, Object>> InitialVariables      = new ThreadLocal<Map<String, Object>>();

  /** Configuration of the Service */
  private ArrayList<ProcessesConfig>             configurations;

  /** Reference to the Configuration Manager Service */
  private ConfigurationManager                   configurationManager  = null;

  /** Reference to the File Definition Service */
  private WorkflowFileDefinitionService          fileDefinitionService = null;

  /** Reference to the Workflow Forms Service */
  private WorkflowFormsService                   formsService          = null;

  /** Reference to the Organization Service */
  private OrganizationService                    organizationService   = null;

  private String                                 superUser_            = "root";

  private String                                 superPass_            = null;

  private String                                 jaasLoginContext_     = "gatein-domain";

  private static final Log LOG  = ExoLogger.getLogger(WorkflowServiceContainerImpl.class.getName());

  /**
   * Instantiates a new Bonita service instance.
   * This constructor requires the injection of the Forms Service.
   *
   * @param fileDefinitionService reference to the File Definition Service
   * @param formsService          reference to the Forms Service
   * @param organizationService   reference to the Organization Service
   * @param configuration         reference to the Configuration Manager
   * @param params                initialization parameters of the service
   */
  public WorkflowServiceContainerImpl(WorkflowFileDefinitionService fileDefinitionService,
      WorkflowFormsService formsService, OrganizationService organizationService,
      ConfigurationManager configurationManager, InitParams params) {

    // Store references to dependent services
    this.fileDefinitionService = fileDefinitionService;
    this.formsService = formsService;
    this.organizationService = organizationService;
    this.configurationManager = configurationManager;
    // Initialize some fields
    this.configurations = new ArrayList<ProcessesConfig>();
    if (params != null) {
      ValueParam superUserParam = params.getValueParam("super.user");
      if (superUserParam != null && superUserParam.getValue().length() > 0) {
        this.superUser_ = superUserParam.getValue();
      }
      ValueParam jaasLoginContextParam = params.getValueParam("jaas.login.context");
      if (jaasLoginContextParam != null && jaasLoginContextParam.getValue().length() > 0) {
        this.jaasLoginContext_ = jaasLoginContextParam.getValue();
      }
      ValueParam superPassParam = params.getValueParam("super.pass");
      if (superPassParam != null && superPassParam.getValue().length() > 0) {
        this.superPass_ = superPassParam.getValue();
      }
    }
  }

  /**
   * Add a plugin to the Workflow service.
   * This method currently only supports plugins to deploy predefined processes.
   *
   * @param plugin the plugin to add
   * @throws Exception if the plugin type is unknown.
   */
  public void addPlugin(ComponentPlugin plugin) throws Exception {
    if (plugin instanceof PredefinedProcessesPlugin) {
      this.configurations.add(((PredefinedProcessesPlugin) plugin).getProcessesConfig());
    } else {
      throw new RuntimeException(plugin.getClass().getName() + " is an unknown plugin type.");
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#deleteProcess(java.lang.String)
   */
  public void deleteProcess(String processId) {
    this.commit();
    try {
      // Get the process
      QueryDefinitionAPI dApi = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();
      ProcessDefinition p = dApi.getProcess(UUIDFactory.getProcessDefinitionUUID(processId));

      // Undeploy it
      ManagementAPI mAPI = AccessorUtil.getAPIAccessor().getManagementAPI();
      mAPI.deletePackage(p.getPackageDefinitionUUID());
      // Clear form cache
      this.formsService.removeForms(processId);
      this.fileDefinitionService.remove(processId);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected problem occurs while deleting this process "+processId+" ");
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#deleteProcessInstance(java.lang.String)
   */
  public void deleteProcessInstance(String processInstanceId) {
    // Security Check
    this.commit();
    try {
      RuntimeAPI rApi = AccessorUtil.getAPIAccessor().getRuntimeAPI();
      rApi.deleteProcessInstance(UUIDFactory.getProcessInstanceUUID(processInstanceId));
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#deployProcess(java.io.InputStream)
   */
  public void deployProcess(InputStream jarInputStream) throws IOException {
    this.commit();

    try {
      boolean xpdl = false;
      // Get the byte array of the input file
      byte[] barFile = Misc.getAllContentFrom(jarInputStream);
      // check if it is an archive or not
      final InputStream in = new ByteArrayInputStream(barFile);
      ZipInputStream zis = null;
      zis = new ZipInputStream(in);
      try {
        // If the inputStream is not a zip file, the next line will throw an exception
        zis.getNextEntry().getName();
        zis.close();
      } catch (Exception e1) {
        if (LOG.isErrorEnabled()) {
          LOG.error(e1);
        }
        xpdl = true;
      }
      in.close();

      // Get the deployer API
      ManagementAPI dAPI = AccessorUtil.getAPIAccessor().getManagementAPI();
      // deploy the process
      Collection<ProcessDefinition> processes;
      if (xpdl) {
        processes = dAPI.deployXpdl(barFile).values();
      } else {
        try {
          processes = dAPI.deployBar(barFile).values();
        } catch(Exception e) {
          processes=null;
          if (LOG.isErrorEnabled()) {
            LOG.error(e);
          }
          throw e;
        }
      }
      // Create the file definition
      Iterator<ProcessDefinition> ite = processes.iterator();
      if (ite.hasNext()) {
        // Get the id of the deployed process
        ProcessDefinition p = ite.next();
        String processId = p.getProcessDefinitionUUID().toString();
        // Create the FileDefinition and store it
        FileDefinition fileDefinition;
        if (xpdl) {
          fileDefinition = new XPDLFileDefinition(new ByteArrayInputStream(barFile));
        } else {
          fileDefinition = new BARFileDefinition(new ByteArrayInputStream(barFile));
        }
        this.fileDefinitionService.store(fileDefinition, processId);
      }
    } catch (Exception e) {
      IOException pde = new IOException();
      pde.initCause(e);
      throw pde;
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#endTask(java.lang.String, java.util.Map)
   */
  public void endTask(String taskId, Map variables) {
    // Security Check
    this.commit();
    Map<String, Object> activityVariables = new HashMap<String, Object>();
    Map<String, Object> instanceVariables = new HashMap<String, Object>();
    try {
      QueryRuntimeAPI qAPI = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
      RuntimeAPI rApi = AccessorUtil.getAPIAccessor().getRuntimeAPI();
      QueryDefinitionAPI dAPI = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();

      // Get Task
      ActivityInstance<TaskInstance> t = qAPI.getTask(UUIDFactory.getTaskUUID(taskId));
      // Convert variables
      if (variables != null) {
        Set<String> keys = variables.keySet();
        Iterator<String> ite = keys.iterator();
        while (ite.hasNext()) {
          String key = ite.next();
          // Check the enumeration type
          try {
            ActivityDefinition activity = dAPI.getProcessActivity(t.getProcessDefinitionUUID(), t
                .getActivityId());
            DataFieldDefinition d = dAPI.getActivityDataField(activity.getUUID(), key);
            DataTypeValue typeValue = d.getDataType().getValue();
            Object v = getVariableValue(variables.get(key), typeValue);
            if (v != null)
              activityVariables.put(key, v);
          } catch (org.ow2.bonita.facade.exception.DataFieldNotFoundException e) {
            org.ow2.bonita.facade.runtime.ProcessInstance p;
            p = qAPI.getProcessInstance(t.getProcessInstanceUUID());
            DataFieldDefinition d;
            try {
              d = dAPI.getProcessDataField(p.getProcessDefinitionUUID(), key);
              DataTypeValue typeValue = d.getDataType().getValue();
              Object v = getVariableValue(variables.get(key), typeValue);
              if (v != null)
                instanceVariables.put(key, v);
            } catch (org.ow2.bonita.facade.exception.DataFieldNotFoundException e1) {
              if (LOG.isWarnEnabled()) {
                LOG.warn(e1.getMessage());
              }
            }
          }
        }
      }
      // start, setVariables and end task
      rApi.startTask(UUIDFactory.getTaskUUID(taskId), true);
      if (activityVariables != null) {
        Set<String> keys = activityVariables.keySet();
        Iterator<String> ite = keys.iterator();
        while (ite.hasNext()) {
          String key = ite.next();
          rApi.setActivityInstanceVariable(t.getUUID(), key, activityVariables.get(key));
        }
      }
      if (instanceVariables != null) {
        Set<String> keys = instanceVariables.keySet();
        Iterator<String> ite = keys.iterator();
        while (ite.hasNext()) {
          String key = ite.next();
          rApi.setProcessInstanceVariable(t.getProcessInstanceUUID(), key, instanceVariables
              .get(key));
        }
      }
      // Finish the task
      rApi.finishTask(UUIDFactory.getTaskUUID(taskId), true);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.workflow.WorkflowServiceContainer#endTask(java
   * .lang.String, java.util.Map, java.lang.String)
   */
  public void endTask(String taskId, Map variables, String transition) {
    /*
     * In Bonita, we consider this is the Workflow duty to determine which
     * activity comes next hence transition name is unused.
     */
    endTask(taskId, variables);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getAllTasks(java.lang.String)
   */
  public List<Task> getAllTasks(String user) throws Exception {
    List<Task> allTasks = new ArrayList<Task>();
    allTasks.addAll(getUserTaskList(user));
    allTasks.addAll(getGroupTaskList(user));
    return allTasks;
  }

  public WorkflowFileDefinitionService getFileDefinitionService() {
    return this.fileDefinitionService;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getGroupTaskList(java.lang.String)
   */
  public List<Task> getGroupTaskList(String user) throws Exception {
    // TODO Determine if something can be implemented
    return new ArrayList<Task>();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getProcess(java.lang.String)
   */
  public Process getProcess(String processId) {
    ProcessData p = null;
    // Security Check
    this.commit();
    try {
      // Get the uuid object
      ProcessDefinitionUUID uuid = UUIDFactory.getProcessDefinitionUUID(processId);
      // get the process definition
      QueryDefinitionAPI dAPI = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();
      ProcessDefinition pd = dAPI.getProcess(uuid);
      p = new ProcessData(pd);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return p;

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getProcesses()
   */
  public List<Process> getProcesses() {
    // Security Check
    this.commit();
    // get the processes
    QueryDefinitionAPI dAPI = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();
    Set<ProcessDefinition> list = dAPI.getProcesses(ProcessState.DEPLOYED);

    // map with the right format
    ArrayList<Process> rList = new ArrayList<Process>();
    Iterator<ProcessDefinition> ite = list.iterator();
    while (ite.hasNext()) {
      ProcessDefinition pd = ite.next();
      rList.add(new ProcessData(pd));
    }

    Collections.sort(rList, new ProcessComparator());
    return rList;

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getProcessInstance(java.lang.String)
   */
  public ProcessInstance getProcessInstance(String processInstanceId) {
    ProcessInstance p = null;

    // Security Check
    this.commit();
    try {
      // Get the uuid object
      ProcessInstanceUUID uuid = UUIDFactory.getProcessInstanceUUID(processInstanceId);
      // Get api accessors
      QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
      // Get the instance and definition
      org.ow2.bonita.facade.runtime.ProcessInstance instance = rApi.getProcessInstance(uuid);
      p = new ProcessInstanceData(instance);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return p;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getProcessInstances(java.lang.String)
   */
  public List<ProcessInstance> getProcessInstances(String processId) {
    // Define the result
    ArrayList<ProcessInstance> list = new ArrayList<ProcessInstance>();

    // Security Check
    this.commit();
    try {
      // Get the uuid
      ProcessDefinitionUUID uuid = UUIDFactory.getProcessDefinitionUUID(processId);
      //Get the instance of the given process
      QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
      Set<org.ow2.bonita.facade.runtime.ProcessInstance> pis;
      pis = rApi.getProcessInstances(uuid);
      Iterator<org.ow2.bonita.facade.runtime.ProcessInstance> ite = pis.iterator();
      while (ite.hasNext()) {
        org.ow2.bonita.facade.runtime.ProcessInstance i = ite.next();
        list.add(new ProcessInstanceData(i));
      }
      Collections.sort(list, new ProcessInstanceComparator());
    } catch (BonitaException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return list;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getTask(java.lang.String)
   */
  public Task getTask(String taskId) {
    if (LOG.isInfoEnabled()) {
      LOG.info("get the task [taskId=" + taskId + "]");
    }
    Task task = null;
    // Security Check
    this.commit();
    try {
      // Get the uuid object
      TaskUUID uuid = UUIDFactory.getTaskUUID(taskId);
      //Get task
      QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
      ActivityInstance<TaskInstance> taskInstance = rApi.getTask(uuid);
      task = new TaskData(taskInstance);
    } catch (BonitaException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return task;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getTasks(java.lang.String)
   */
  public List<Task> getTasks(String processInstanceId) {
    List<Task> tasks = new ArrayList<Task>();
    // Security Check
    this.commit();

    // Get the uuid
    ProcessInstanceUUID uuid = UUIDFactory.getProcessInstanceUUID(processInstanceId);

    QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
    QueryDefinitionAPI dApi = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();

    Collection<ActivityInstance<TaskInstance>> todoList = new ArrayList<ActivityInstance<TaskInstance>>();

    todoList.addAll(rApi.getTaskList(ActivityState.READY));
    todoList.addAll(rApi.getTaskList(ActivityState.EXECUTING));
    todoList.addAll(rApi.getTaskList(ActivityState.SUSPENDED));
    todoList.addAll(rApi.getTaskList(ActivityState.FINISHED));

    Iterator<ActivityInstance<TaskInstance>> ite = todoList.iterator();
    while (ite.hasNext()) {
      ActivityInstance<TaskInstance> task = ite.next();
      if (task.getProcessInstanceUUID().equals(uuid)) {
        try {
          tasks.add(new TaskData(task));
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error(e);
          }
        }
      }
    }
    Collections.sort(tasks, new TaskComparator());
    return tasks;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getTimers()
   */
  public List<Timer> getTimers() {
    //TODO:complete the getTimers code source
    return new ArrayList<Timer>();
    //    DeadlineEjbTimerSessionLocal deadlineSession = null;
    //    List<Timer> timers = new ArrayList<Timer>();
    //
    //    try {
    //      DeadlineEjbTimerSessionLocalHome deadlineHome =
    //        DeadlineEjbTimerSessionUtil.getLocalHome();
    //      deadlineSession = deadlineHome.create();
    //      Collection<hero.util.TimerData> deadlines = deadlineSession.getTimers();
    //      for(hero.util.TimerData timerData : deadlines) {
    //        timers.add(new TimerData(timerData));
    //      }
    //    }
    //    catch(Exception e) {
    //      e.printStackTrace();
    //    }
    //    finally {
    //      try {
    //        deadlineSession.remove();
    //      }
    //      catch(Exception ignore) {
    //      }
    //    }
    //
    //    return timers;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getUserTaskList(java.lang.String)
   */
  public List<Task> getUserTaskList(String user) {
    List<Task> tasks = new ArrayList<Task>();

    // Security Check
    this.commit();

    QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();

    Collection<ActivityInstance<TaskInstance>> todoList = new ArrayList<ActivityInstance<TaskInstance>>();

    todoList.addAll(rApi.getTaskList(ActivityState.READY));
    todoList.addAll(rApi.getTaskList(ActivityState.EXECUTING));
    todoList.addAll(rApi.getTaskList(ActivityState.SUSPENDED));

    Iterator<ActivityInstance<TaskInstance>> ite = todoList.iterator();
    while (ite.hasNext()) {
      ActivityInstance<TaskInstance> task = ite.next();
      try {
        tasks.add(new TaskData(task));
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error(e);
        }
      }
    }
    Collections.sort(tasks, new TaskComparator());

    return tasks;
  }

  /**
   * Check the type of the value and convert it in the right object type
   * @param value
   * @param typeValue
   * @return the value object
   * @throws ParseException
   */
  private Object getVariableValue(Object value, DataTypeValue typeValue) throws ParseException {
    if (EnumerationTypeDefinition.class.isInstance(typeValue)) {
      Set<String> possibleValues = EnumerationTypeDefinition.class.cast(typeValue)
          .getEnumerationValues();
      if (possibleValues.contains(value)) {
        value = new Enumeration(possibleValues, (String) value);
      }
    } else if (BasicTypeDefinition.class.isInstance(typeValue)) {
      // check for simple type and convert from string to adequat type
      BasicTypeDefinition type = BasicTypeDefinition.class.cast(typeValue);
      switch (type.getType()) {
      case BOOLEAN:
        value = Boolean.valueOf((String) value);
        break;
      case DATETIME:
        if (!Date.class.isInstance(value)) {
          if (value != null && ((String) value).length() != 0) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            value = formatter.parse((String) value);
          }
        }
        break;
      case INTEGER:
        if (((String) value).trim().compareTo("") == 0) {
          value = Integer.valueOf(0);
        } else {
          value = Integer.decode((String) value);
        }
        break;
      case FLOAT:
        if (((String) value).trim().compareTo("") == 0) {
          value = Double.valueOf(0);
        } else {
          value = Double.parseDouble((String) value);
        }
        break;
      }
    }
    return value;
  }

  private Map<String, Object> cleanVariables(Map<String, Object> vList) {
    if (vList == null)
      return null;
    Map<String, Object> variables = new HashMap<String, Object>();
    for (String key : vList.keySet()) {
      if (Enumeration.class.isInstance(vList.get(key))) {
        variables.put(key, Enumeration.class.cast(vList.get(key)).getSelectedValue());
      } else {
        variables.put(key, vList.get(key));
      }
    }
    return variables;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#getVariables(java.lang.String)
   */
  public Map getVariables(String InstanceId, String taskId) {

    Map<String, Object> variables = new HashMap<String, Object>();

    QueryRuntimeAPI rApi = AccessorUtil.getQueryAPIAccessor().getQueryRuntimeAPI();
    TaskUUID uuid = UUIDFactory.getTaskUUID(taskId);
    try {
      ActivityInstance<TaskInstance> task = rApi.getTask(uuid);
      variables = cleanVariables(rApi.getProcessInstanceVariables(task.getProcessInstanceUUID()));
      variables.putAll(cleanVariables(rApi.getActivityInstanceVariables(task.getUUID())));

      //          String processId = new APIAccessorImpl().getRecordQuerierAPI().getInstanceRecord(InstanceId).getProcessId();
      //      Form form = this.formsService.getForm(processId, activityId, Locale.getDefault());
      //        List<Map<String, Object>> formVariables = form.getVariables();
      //
      //        // Convert String to Objects based on Form information
      //        for(Map<String, Object> formVariable : formVariables) {
      //          String key       = (String) formVariable.get("name");
      //          String component = (String) formVariable.get("component");
      //          variables.put(key, WorkflowServiceContainerHelper.stringToObject(
      //            (String) variables.get(key),
      //            component));
      //        }

    } catch (BonitaException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }

    return variables;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#hasStartTask(java.lang.String)
   */
  public boolean hasStartTask(String processId) {
    // Retrieve the Start Form and determine if it contains variables
    Form form = this.formsService.getForm(processId, ProcessData.START_STATE_NAME, Locale
        .getDefault());

    return !form.getVariables().isEmpty();
  }

  /*
   * Deploy the predefined processes. This is done in the <tt>start()</tt>
   * method and not the <tt>initComponent()</tt> method as the predefined users
   * are not created yet in that case, which makes the login fail. We expect the
   * <tt>start()</tt> method of the Organization service to be called prior the
   * <tt>start()</tt> method of the Workflow service because of the
   * dependencies.
   *
   * (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    //Request life cycle begin/end
    LoginContext lc = null;
    try {
      RequestLifeCycle.begin((ComponentRequestLifecycle)this.organizationService);
      UserHandler userHandler = this.organizationService.getUserHandler();
      User user = userHandler.findUserByName(superUser_);
      char[] password = user.getPassword() == null ? superPass_.toCharArray() : user.getPassword().toCharArray();
      BasicCallbackHandler handler = new BasicCallbackHandler(superUser_, password);
      lc = new LoginContext(jaasLoginContext_, handler);
      lc.login();
      // Retrieve the already deployed Processes.

      Collection<Process> projects = this.getProcesses();

      // If the predefined Processes need to be deployed
      if (projects.isEmpty()) {

        // Deploy each predefined Process
        for (ProcessesConfig processConfig : configurations) {
          HashSet predefinedProcesses = processConfig.getPredefinedProcess();
          String processLoc = processConfig.getProcessLocation();
          for (Iterator iter = predefinedProcesses.iterator(); iter.hasNext();) {
            String parFile = (String) iter.next();
            InputStream iS;
            URL url = this.configurationManager.getURL(processLoc + parFile);
            try {
              iS = this.configurationManager.getInputStream(processLoc + parFile);
              this.deployProcess(iS);
            } catch (Exception e) {
              // Process does not exist
              if (LOG.isErrorEnabled()) {
                LOG.error(e);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } finally {
      try {
        /*
         * Logout. This does not hurt if it fails as Exceptions are ignored.
         */
        lc.logout();
        RequestLifeCycle.end();
      } catch (Exception ignore) {
        // Do nothing
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.WorkflowServiceContainer#startProcess(java.lang.String)
   */
  public void startProcess(String processId) {

    // Delegate the call
    this.startProcess(null, processId, new HashMap());
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.workflow.WorkflowServiceContainer#startProcess
   * (java.lang.String, java.lang.String, java.util.Map)
   */
  public void startProcess(String remoteUser, String processId, Map initialVariables) {
    // Security Check
    this.commit();
    try {
      RuntimeAPI rApi = AccessorUtil.getAPIAccessor().getRuntimeAPI();
      QueryDefinitionAPI dAPI = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();
      ProcessDefinitionUUID uuid = UUIDFactory.getProcessDefinitionUUID(processId);
      Map<String, Object> variables = new HashMap<String, Object>();
      // Check the enumerations
      if (initialVariables != null) {
        Set<DataFieldDefinition> dataFields = dAPI.getProcessDataFields(uuid);
        for (DataFieldDefinition dataField : dataFields) {
          if (initialVariables.containsKey(dataField.getDataFieldId())) {
            DataTypeValue typeValue = dataField.getDataType().getValue();
            if (EnumerationTypeDefinition.class.isInstance(typeValue)) {
              Set<String> possibleValues = EnumerationTypeDefinition.class.cast(typeValue)
                  .getEnumerationValues();
              String strEnumValue = (String)initialVariables.get(dataField.getDataFieldId());
              if (possibleValues.contains(strEnumValue)) {
                variables.put(dataField.getDataFieldId(), new Enumeration(possibleValues,strEnumValue));
              }
            } else if (BasicTypeDefinition.class.isInstance(typeValue)) {
              // check for simple type and convert from string to adequat type
              BasicTypeDefinition type = BasicTypeDefinition.class.cast(typeValue);
              Object value = initialVariables.get(dataField.getDataFieldId());
              switch (type.getType()) {
              case BOOLEAN:
                variables.put(dataField.getDataFieldId(), Boolean.valueOf((String) value));
                break;
              case DATETIME:
                if (!Date.class.isInstance(value)) {
                  SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                  variables.put(dataField.getDataFieldId(), formatter.parse((String) value));
                } else {
                  variables.put(dataField.getDataFieldId(), value);
                }
                break;
              case INTEGER:
                if (((String) value).trim().compareTo("") == 0) {
                  variables.put(dataField.getDataFieldId(), Integer.valueOf(0));
                } else {
                  variables.put(dataField.getDataFieldId(), Integer.decode((String) value));
                }
                break;
              case FLOAT:
                if (((String) value).trim().compareTo("") == 0) {
                  value = Double.valueOf(0);
                } else {
                  value = Double.parseDouble((String) value);
                }
                break;
              default:
                variables.put(dataField.getDataFieldId(), (String) value);
                break;
              }
            }
          }
        }
      }
      // instantiate the process
      ProcessInstanceUUID instanceUuid = rApi.instantiateProcess(uuid, variables);
    } catch (ProcessNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } catch (ParseException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } catch (VariableNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.workflow.WorkflowServiceContainer#startProcessFromName
   * (java.lang.String, java.lang.String, java.util.Map)
   */
  public void startProcessFromName(String remoteUser, String processName, Map variables) {
    this.commit();
    try {
      // Search the process by name (aka processId)
      QueryDefinitionAPI dAPI = AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI();
      Set<ProcessDefinition> processes = dAPI.getProcesses(processName);
      ProcessDefinition process = null;
      for (ProcessDefinition p : processes) {
        if (dAPI.getPackage(p.getPackageDefinitionUUID()).getUndeployedDate() == null) {
          // The process is deployed
          process = p;
          break;
        }
      }
      if (process != null) {
        String uuid = process.getProcessDefinitionUUID().toString();
        this.startProcess(remoteUser, uuid, variables);
      }

    } catch (BonitaException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }

  private void commit() {
    LoginContext lc = null;
    try {
      // Change for the trunk version
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (identity.getSubject() != null) {
        Subject s = new Subject();
        s.getPrincipals().add(new BonitaPrincipal(identity.getUserId()));
        try {
          lc = new LoginContext("Bonita", s);
        } catch (LoginException le) {
          if (LOG.isErrorEnabled()) {
            LOG.error(le);
          }
        }
      } else {
        try {
          RequestLifeCycle.begin((ComponentRequestLifecycle)this.organizationService);
          UserHandler userHandler = this.organizationService.getUserHandler();
          User user = userHandler.findUserByName(identity.getUserId());
          char[] password = user.getPassword() == null ? superPass_.toCharArray() : user.getPassword().toCharArray();
          BasicCallbackHandler handler = new BasicCallbackHandler(identity.getUserId(), password);
          lc = new LoginContext(jaasLoginContext_, handler);
        } finally {
          RequestLifeCycle.end();
        }
      }
      lc.login();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
  }
}
