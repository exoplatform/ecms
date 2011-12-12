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
package org.exoplatform.services.plugin.actions.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.actions.impl.ActionConfig;
import org.exoplatform.services.cms.actions.impl.BaseActionLauncherListener;
import org.exoplatform.services.cms.actions.impl.BaseActionPlugin;
import org.exoplatform.services.cms.actions.impl.ECMEventListener;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.plugin.actions.activation.BPActionActivationJob;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.WorkflowServiceContainer;

public class BPActionPlugin extends BaseActionPlugin implements ComponentPlugin {

  public static final String ACTION_TYPE = "exo:businessProcessAction";

  private WorkflowServiceContainer workflowServiceContainer_;
  private ActionConfig config_;
  private RepositoryService repositoryService_;

  public BPActionPlugin(RepositoryService repositoryService, InitParams params,
      WorkflowServiceContainer workflowServiceContainer) throws Exception {
    workflowServiceContainer_ = workflowServiceContainer;
    repositoryService_ = repositoryService;
    config_ = (ActionConfig) params.getObjectParamValues(ActionConfig.class).get(0);
  }

  public Collection<String> getActionExecutables(String repository) throws Exception {
    List<Process> processes = workflowServiceContainer_.getProcesses();
    Collection<String> businessProcesses = new ArrayList<String>();
    for (Iterator<Process> iter = processes.listIterator(); iter.hasNext();) {
      Process process =  iter.next();
      businessProcesses.add(process.getName());
    }
    return businessProcesses;
  }

  protected ECMEventListener createEventListener(String actionName, String moveExecutable,
      String repository, String srcWorkspace, String srcPath, Map variables, String actionType) throws Exception {
    return new BPActionLauncherListener(actionName, moveExecutable, repository,
                                        srcWorkspace, srcPath, variables);
  }

  public String getActionExecutableLabel() { return "Business Processes:"; }
  public String getExecutableDefinitionName() { return "exo:businessProcess"; }

  protected String getWorkspaceName() { return config_.getWorkspace() ; }

  protected List<RepositoryEntry> getRepositories() {
    return repositoryService_.getConfig().getRepositoryConfigurations() ;
  }
  protected ManageableRepository getRepository() throws Exception {
    return repositoryService_.getCurrentRepository();
  }

  protected String getActionType() { return ACTION_TYPE ; }
  protected List getActions() { return config_.getActions() ; }
  public String getName() { return "exo:businessProcessAction" ; }
  public void setName(String s) {}
  public String getDescription() { return "Add an action service" ; }
  public void setDescription(String desc) {}

  @SuppressWarnings("unchecked")
  public void executeAction(String userId, Node actionNode, Map variables, String repository) throws Exception {
    String businessProcess = actionNode.getProperty("exo:businessProcess").getString();
    //TODO check maybe don't need put repository here
    variables.put("repository",repository) ;
    executeAction(userId, businessProcess, variables);
  }

  @Deprecated
  public void executeAction(String userId, String executable, Map variables, String repository) {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    WorkflowServiceContainer workflowSContainer = (WorkflowServiceContainer) container
    .getComponentInstanceOfType(WorkflowServiceContainer.class);
    workflowSContainer.startProcessFromName(userId, executable, variables);
  }
  
  public void executeAction(String userId, String executable, Map variables) {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    WorkflowServiceContainer workflowSContainer = (WorkflowServiceContainer) container
    .getComponentInstanceOfType(WorkflowServiceContainer.class);
    workflowSContainer.startProcessFromName(userId, executable, variables);
  }  

  public class BPActionLauncherListener extends BaseActionLauncherListener {

    public BPActionLauncherListener(String actionName, String businessProcess, String repository,
        String srcWorkspace, String srcPath, Map actionVariables) throws Exception {
      super(actionName, businessProcess, repository, srcWorkspace, srcPath, actionVariables);
    }

    public void triggerAction(String userId, Map variables, String repository) {
      executeAction(userId, super.executable_, variables, repository);
    }

  }

  public void activateAction(String userId, String executable, Map variables, String repository) throws Exception {
    executeAction(userId, executable, variables, repository) ;

  }

  protected Class createActivationJob() throws Exception {
    return BPActionActivationJob.class ;
  }

  @Override
  public void activateAction(String userId, String executable, Map variables) throws Exception {
    executeAction(userId, executable, variables) ;
  }

}
