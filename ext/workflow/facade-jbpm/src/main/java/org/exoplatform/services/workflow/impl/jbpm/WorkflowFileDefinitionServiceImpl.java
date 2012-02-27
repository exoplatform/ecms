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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.workflow.FileDefinition;
import org.exoplatform.services.workflow.WorkflowFileDefinitionService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 18, 2007
 */
public class WorkflowFileDefinitionServiceImpl implements WorkflowFileDefinitionService{

  public WorkflowFileDefinitionServiceImpl() {
  }

  public void remove(String processId) {
  }

  public void removeFromCache(String processId) {
  }

  public FileDefinition retrieve(String processId) {
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      WorkflowServiceContainerImpl containerImpl = (WorkflowServiceContainerImpl) container.
          getComponentInstanceOfType(WorkflowServiceContainer.class);
      JbpmContext jbpmContext = containerImpl.openJbpmContext();
      ProcessDefinition processDefinition = jbpmContext.getGraphSession()
                                                       .loadProcessDefinition(Long.parseLong(processId));
      FileDefinitionWapper fileDefinitionWapper = new FileDefinitionWapper(processDefinition.getFileDefinition());
      return fileDefinitionWapper;
    } catch (Exception e) {
      return null;
    }
  }

  public void store(FileDefinition fileDefinition, String processId) {
  }
}
