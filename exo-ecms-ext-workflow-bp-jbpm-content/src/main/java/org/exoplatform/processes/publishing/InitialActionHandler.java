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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 19, 2007  
 */
public class InitialActionHandler implements ActionHandler {
  
  private static final long serialVersionUID = 1L;      
  
  public void execute(ExecutionContext context) throws Exception {
    initialVariables(context);
    ProcessUtil.requestForValidation(context);
  }
  
  protected void initialVariables(ExecutionContext context) throws Exception {
    String actionName = (String) context.getVariable("actionName");
    String nodePath = (String) context.getVariable("nodePath");
    String srcPath = (String) context.getVariable("srcPath");
    String srcWorkspace = (String) context.getVariable("srcWorkspace");    
    String repository = (String) context.getVariable("repository");    
    ProcessUtil.setCurrentLocation(context,srcWorkspace,nodePath);    
    RepositoryService repositoryService = ProcessUtil.getService(RepositoryService.class);
    ActionServiceContainer actionServiceContainer = ProcessUtil.getService(ActionServiceContainer.class);
    ManageableRepository manageableRepository = repositoryService.getRepository(repository);
    Session session = manageableRepository.getSystemSession(srcWorkspace);   
    Node actionableNode = (Node) session.getItem(srcPath);
    if(!actionableNode.isNodeType("exo:actionable")) {
        actionableNode = (Node) session.getItem(nodePath);
    } 
    Node actionNode = actionServiceContainer.getAction(actionableNode, actionName);
    /* incase of workflow publication */
    if (actionNode == null) actionNode = actionableNode;
    fillVariables(actionNode,ProcessUtil.EXO_PUBLISH_LOCATION,context);
    fillVariables(actionNode,ProcessUtil.EXO_PENDING_LOCATION,context);
    fillVariables(actionNode,ProcessUtil.EXO_BACKUP_LOCATION,context);
    fillVariables(actionNode,ProcessUtil.EXO_TRASH_LOCATION,context);
    session.logout();
  }
  
  private void fillVariables(Node node,String nodeType,ExecutionContext context) throws Exception {
    NodeTypeManager nodeTypeManager = node.getSession().getWorkspace().getNodeTypeManager();
    NodeType publicationable = nodeTypeManager.getNodeType(nodeType);
    for(PropertyDefinition prodef: publicationable.getPropertyDefinitions()) {
      String propName = prodef.getName();
      try{
        String value = node.getProperty(propName).getString();
        context.setVariable(propName,value);
      }catch (Exception e) {
      }
    }
  }
}
