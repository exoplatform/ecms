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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentLifecycle;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.workflow.PredefinedProcessesPlugin;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.ProcessesConfig;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.Timer;
import org.exoplatform.services.workflow.WorkflowFileDefinitionService;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.hibernate.cfg.Configuration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JbpmSchema;
import org.jbpm.db.JbpmSession;
import org.jbpm.db.JbpmSessionFactory;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchiveDeployer;
import org.jbpm.taskmgmt.exe.TaskInstance;
/**
 * Created by the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 28 juin 2004
 */
public class WorkflowServiceContainerImpl implements
    WorkflowServiceContainer,
    ComponentLifecycle,
    ComponentRequestLifecycle {

  private ConfigurationManager configurationManager_;
  private List<ProcessesConfig> configs_;
  private JbpmSessionFactory sessionFactory_;
  private OrganizationService orgService_;
  private ThreadLocal threadLocal_;
  private String hibernateServiceName_;
  private WorkflowFileDefinitionService fileDefinitionService_ ;
  private static final Log LOG  = ExoLogger.getLogger(WorkflowServiceContainerImpl.class.getName());
  
  public WorkflowServiceContainerImpl(OrganizationService orgService,WorkflowFileDefinitionService fileDefinitionService,
      ConfigurationManager conf, InitParams params) throws Exception {
    hibernateServiceName_ = params.getValueParam("hibernate.service").getValue();
    configs_ =  new ArrayList<ProcessesConfig>();
    this.configurationManager_ = conf;
    threadLocal_ = new ThreadLocal();
    orgService_ = orgService;
    this.fileDefinitionService_ = fileDefinitionService;
  }

  /**
   * Add a plugin to the Workflow service.
   * This method currently only supports plugins to deploy predefined processes.
   * 
   * @param plugin the plugin to add
   * @throws Exception if the plugin type is unknown.
   */
  public void addPlugin(ComponentPlugin plugin) throws Exception {
    if(plugin instanceof PredefinedProcessesPlugin) {
      this.configs_.add(((PredefinedProcessesPlugin) plugin).
        getProcessesConfig());
    }
    else {
      throw new RuntimeException(
        plugin.getClass().getName()
        + " is an unknown plugin type.") ;
    }
  }
  
  public void initComponent(ExoContainer container) throws Exception {
    HibernateService hservice = null;
    if (hibernateServiceName_ == null || hibernateServiceName_.length() == 0
        || "default".equals(hibernateServiceName_)) {
      hservice = (HibernateService) container
          .getComponentInstanceOfType(HibernateService.class);
    } else {
      hservice = (HibernateService) container
          .getComponentInstance(hibernateServiceName_);
    }
    Configuration hconf = hservice.getHibernateConfiguration();
    sessionFactory_ = JbpmSessionFactory.buildJbpmSessionFactory(hconf);
    JbpmSchema schema = sessionFactory_.getJbpmSchema();
    if (!schema.hasJbpmTables()) {
      schema.createSchema();
    }
    init();
  }

  public void startComponent(ExoContainer arg0) throws Exception {
  }

  public void stopComponent(ExoContainer arg0) throws Exception {
  }

  public void destroyComponent(ExoContainer arg0) throws Exception {
  }

  public JbpmSessionFactory getJbpmSessionFactory() {
    return sessionFactory_;
  }

  public JbpmSession openSession() {
    JbpmSession currentSession = (JbpmSession) threadLocal_.get();
    if (currentSession == null) {
      currentSession = this.sessionFactory_
          .openJbpmSessionAndBeginTransaction();
      threadLocal_.set(currentSession);
    }
    return currentSession;
  }

  private void init() {
    // init the default processes
    JbpmSession session = openSession();
    try {
      if (!session.getGraphSession().findAllProcessDefinitions().isEmpty())
        return;

      // Iterate through each Processes Configuration to deploy
      for(ProcessesConfig processConfig : this.configs_) {
        HashSet predefinedProcesses = processConfig.getPredefinedProcess();
        String processLoc = processConfig.getProcessLocation();

        for (Iterator iter = predefinedProcesses.iterator(); iter.hasNext();) {
          String parFile = (String) iter.next();
          InputStream iS;
          try {
            iS = configurationManager_.getInputStream(processLoc + parFile);
            ProcessArchiveDeployer.deployZipInputStream(new ZipInputStream(iS),
              this.sessionFactory_);
          } catch (Exception e) {
            // process does not exist
          	LOG.error(e);
          }
        }
      }
    } finally {
      closeSession();
    }
  }

  public void closeSession() {
    JbpmSession s = (JbpmSession) threadLocal_.get();
    this.closeSession(s);
  }
  
  public void closeSession(JbpmSession session) {
    if (session == null)
      return;
    try {
      if (session.getTransaction() != null)
        session.commitTransactionAndClose();
    } catch (Throwable t) {
    	LOG.error(t);
      session.rollbackTransactionAndClose();
    }
    threadLocal_.set(null);
  }

  public void rollback() {
    JbpmSession s = (JbpmSession) threadLocal_.get();
    this.rollback(s);
  }

  public void rollback(JbpmSession session) {
    if (session == null)
      return;
    try {
      session.rollbackTransactionAndClose();
    } catch (Throwable t) {
    }
    threadLocal_.set(null);
  }

  public List<Task> getAllTasks(String user) throws Exception {
    List<Task> allTasks = new ArrayList<Task>();       
    allTasks.addAll(getUserTaskList(user));    
    List<Task> groupTask = getGroupTaskList(user);
    for(Task task:allTasks) {
      for(Task checkTask:groupTask) {
        if(checkTask.getId().equalsIgnoreCase(task.getId())) {
          groupTask.remove(checkTask);
        }
      }
    }    
    allTasks.addAll(groupTask);        
    return allTasks;
  }

  public List<Task> getUserTaskList(String user) {
    JbpmSession session = openSession();
    List taskInstances = session.getTaskMgmtSession().findTaskInstances(user);    
    return wrapTasks(taskInstances);

  }

  public List<Task> getGroupTaskList(String user) throws Exception {
    List<Task> groupTasks = new ArrayList<Task>();
    HashSet<TaskInstance> hashSet = new HashSet<TaskInstance>();
    String key = null;
    Collection groups = orgService_.getGroupHandler().findGroupsOfUser(user);
    Collection<?> membershipCollection = orgService_.getMembershipTypeHandler().findMembershipTypes();
    JbpmSession session = openSession();
    for (Iterator iter = groups.iterator(); iter.hasNext();) {
      Group group = (Group) iter.next();
      Collection memberships = 
        orgService_.getMembershipHandler().findMembershipsByUserAndGroup(user, group.getId());
      for (Iterator iterator = memberships.iterator(); iterator.hasNext();) {
        Membership membership = (Membership) iterator.next();
        if(membership.getMembershipType().equals("*")) {
          for(Object obj : membershipCollection){
            key = ((MembershipType)obj).getName() + ACTOR_ID_KEY_SEPARATOR + group.getId();
            List tasks = session.getTaskMgmtSession().findTaskInstances(key);
            if (tasks.size() > 0) {
              hashSet.addAll(tasks);
            }
          }
          key = membership.getMembershipType() + ACTOR_ID_KEY_SEPARATOR + group.getId();
          List tasks = session.getTaskMgmtSession().findTaskInstances(key);
          if (tasks.size() > 0) {
            hashSet.addAll(tasks);
          }
        } else {
          key = membership.getMembershipType() + ACTOR_ID_KEY_SEPARATOR + group.getId();
          List tasks = session.getTaskMgmtSession().findTaskInstances(key);
          if (tasks.size() > 0) hashSet.addAll(tasks);
          String starKey = "*" + ACTOR_ID_KEY_SEPARATOR + group.getId();
          List tasksWithStar = session.getTaskMgmtSession().findTaskInstances(starKey);
          if(tasksWithStar.size() > 0) hashSet.addAll(tasksWithStar);
        }
    
      }
    }
    for(Iterator iterator = hashSet.iterator();iterator.hasNext();) {
      TaskInstance instance = (TaskInstance)iterator.next();
      groupTasks.add(new TaskData(instance));
    }
    return groupTasks;
  }

  private static List<Task> wrapTasks(List tasks) {
    List<Task> wrappedTokens = new ArrayList<Task>();
    for (Iterator iter = tasks.iterator(); iter.hasNext();) {
      TaskInstance task = (TaskInstance) iter.next();
      wrappedTokens.add(new TaskData(task));
    }
    return wrappedTokens;
  }

  public List<Process> getProcesses() {
    List<Process> processes = new ArrayList<Process>();
    JbpmSession session = openSession();
    List definitions = session.getGraphSession().findAllProcessDefinitions();
    for (Iterator iter = definitions.iterator(); iter.hasNext();) {
      ProcessDefinition def = (ProcessDefinition) iter.next();
      processes.add(new ProcessData(def));
    }
    return processes;
  }

  public boolean hasStartTask(String processId) {
    JbpmSession session = openSession();
    ProcessDefinition processDef = session.getGraphSession()
        .loadProcessDefinition(Long.parseLong(processId));
    if (processDef.getTaskMgmtDefinition().getStartTask() != null)
      return true;
    return false;
  }

  public void startProcess(String processId) {
    JbpmSession session = openSession();
    ProcessDefinition processDef = session.getGraphSession()
        .loadProcessDefinition(Long.parseLong(processId));
    org.jbpm.graph.exe.ProcessInstance processInstance = new org.jbpm.graph.exe.ProcessInstance(
        processDef);
    processInstance.signal();
    session.getGraphSession().saveProcessInstance(processInstance);
  }

  public void startProcess(String remoteUser, String processId, Map variables) {
    JbpmSession session = openSession();
    GraphSession graphSession = session.getGraphSession();
    ProcessDefinition processDef = graphSession.loadProcessDefinition(Long
        .parseLong(processId));
    org.jbpm.graph.exe.ProcessInstance instance = new org.jbpm.graph.exe.ProcessInstance(
        processDef);
    ContextInstance contextInstance = instance.getContextInstance();

    variables.put("initiator", remoteUser);
    contextInstance.addVariables(variables);

    TaskInstance taskInstance = instance.getTaskMgmtInstance()
        .createStartTaskInstance();
    taskInstance.setActorId(remoteUser);
    taskInstance.end();

    // instance.signal();
    graphSession.saveProcessInstance(instance);
  }
  
  public void startProcessFromName(String remoteUser, String processName,
      Map variables) {
    JbpmSession session = openSession();
    GraphSession graphSession = session.getGraphSession();
    ProcessDefinition processDef = graphSession.findLatestProcessDefinition(processName);
    org.jbpm.graph.exe.ProcessInstance instance = new org.jbpm.graph.exe.ProcessInstance(processDef);
    ContextInstance contextInstance = instance.getContextInstance();
    contextInstance.addVariables(variables);
    TaskInstance taskInstance = instance.getTaskMgmtInstance()
        .createStartTaskInstance();
    taskInstance.setActorId(remoteUser);
    taskInstance.end();
    graphSession.saveProcessInstance(instance);
  }

  public List<ProcessInstance> getProcessInstances(String processId) {
    List<ProcessInstance> processInstances = new ArrayList<ProcessInstance>();
    JbpmSession session = openSession();
    List jbpmProcessInstances = session.getGraphSession().findProcessInstances(
        new Long(processId).longValue());
    for (Iterator iter = jbpmProcessInstances.iterator(); iter.hasNext();) {
      org.jbpm.graph.exe.ProcessInstance processInstance = (org.jbpm.graph.exe.ProcessInstance) iter
          .next();
      processInstances.add(new ProcessInstanceData(processInstance));

    }
    return processInstances;
  }

  public List<Task> getTasks(String processInstanceId) {
    List<Task> tasks = new ArrayList<Task>();
    JbpmSession session = openSession();
    org.jbpm.graph.exe.ProcessInstance processInstance = session
        .getGraphSession().loadProcessInstance(
            new Long(processInstanceId).longValue());

    Collection taskInstances = processInstance.getTaskMgmtInstance()
        .getTaskInstances();
    for (Iterator iter = taskInstances.iterator(); iter.hasNext();) {
      TaskInstance task = (TaskInstance) iter.next();
      tasks.add(new TaskData(task));
    }
    return tasks;
  }

  public Process getProcess(String processId) {
    JbpmSession session = openSession();
    ProcessDefinition processDef = session.getGraphSession()
        .loadProcessDefinition(Long.parseLong(processId));
    return new ProcessData(processDef);
  }

  public Task getTask(String taskId) {
    JbpmSession session = openSession();
    TaskInstance task = session.getTaskMgmtSession().loadTaskInstance(
        Long.parseLong(taskId));
    return new TaskData(task);
  }

  public ProcessInstance getProcessInstance(String processInstance) {
    JbpmSession session = openSession();
    org.jbpm.graph.exe.ProcessInstance jbpmProcessInstance = session
        .getGraphSession().loadProcessInstance(Long.parseLong(processInstance));
    return new ProcessInstanceData(jbpmProcessInstance);
  }

  public Map getVariables(String processInstanceId, String taskId) {
    JbpmSession session = openSession();
    org.jbpm.graph.exe.ProcessInstance jbpmProcessInstance = session
        .getGraphSession().loadProcessInstance(
            Long.parseLong(processInstanceId));
    return jbpmProcessInstance.getContextInstance().getVariables();
  }

  public void endTask(String taskId, Map variables) {
    JbpmSession session = openSession();
    TaskInstance taskInstance = session.getTaskMgmtSession().loadTaskInstance(
        Long.parseLong(taskId));
    org.jbpm.graph.exe.ProcessInstance processInstance = taskInstance
        .getToken().getProcessInstance();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.addVariables(variables);
    taskInstance.end();
    session.getGraphSession().saveProcessInstance(processInstance);
  }

  public void endTask(String taskId, Map variables, String transition) {
    JbpmSession session = openSession();
    TaskInstance taskInstance = session.getTaskMgmtSession().loadTaskInstance(
        Long.parseLong(taskId));
    org.jbpm.graph.exe.ProcessInstance processInstance = taskInstance
        .getToken().getProcessInstance();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.addVariables(variables);
    taskInstance.end(transition);
    session.getGraphSession().saveProcessInstance(processInstance);
  }

  public List<Timer> getTimers() {
    List<Timer> timers = new ArrayList<Timer>();
    JbpmSession session = openSession();
    Iterator jbpmTimers = session.getSchedulerSession().findTimersByDueDate();
    while (jbpmTimers.hasNext()) {
      org.jbpm.scheduler.exe.Timer jbpmTimer = (org.jbpm.scheduler.exe.Timer) jbpmTimers
          .next();
      timers.add(new TimerData(jbpmTimer));
    }
    return timers;
  }

  public void deployProcess(InputStream iS) throws IOException {
    JbpmSession session = openSession();
    session.getContextSession();
    ProcessArchiveDeployer.deployZipInputStream(new JarInputStream(iS),
        session.getJbpmSessionFactory());
  }

  public void deleteProcess(String processId) {
    JbpmSession session = openSession();
    session.getGraphSession().deleteProcessDefinition(Long.parseLong(processId));
    
    /*
     * Notify the Forms Service. Its reference cannot be constructor injected
     * as the Forms Service already depends on the Workflow Service Container.
     */
    WorkflowFormsService formsService = (WorkflowFormsService) ExoContainerContext
        .getCurrentContainer().getComponentInstanceOfType(WorkflowFormsService.class);
    formsService.removeForms(processId);
  }
  
  public void deleteProcessInstance(String processInstanceId) {
    JbpmSession session = openSession();
    session.getGraphSession().deleteProcessInstance(Long.parseLong(processInstanceId));
  }

  public void startRequest(ExoContainer arg0) {
    /*
     * In ECM1, the current user id was pushed into JbpmDefaultAuthenticator:
     * JbpmDefaultAuthenticator.pushAuthenticatedActorId(remoteUser);
     * This operation is used by jBPM when determining the initiator swimlane.
     * However, this one is overriden when setting the actorId of the initial
     * Task, in startProcessFromName(). So it was decided to remove it.
     */
  }
  
  public void endRequest(ExoContainer arg0) {
    /*
     * Commit the changes. The jBPM session is created lazily by openSession().
     */
    this.closeSession();
  }

  public WorkflowFileDefinitionService getFileDefinitionService() { 
    return fileDefinitionService_;
  }
}
