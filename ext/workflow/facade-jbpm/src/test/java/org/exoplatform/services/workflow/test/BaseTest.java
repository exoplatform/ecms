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
import java.io.InputStream;
import java.net.URL;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.services.workflow.impl.jbpm.WorkflowServiceContainerImpl;
import org.exoplatform.test.BasicTestCase;
import org.jbpm.graph.def.ProcessDefinition;




/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 10 mai 2004
 */
public abstract class BaseTest extends BasicTestCase {

  protected static final String PROCESS_PATH = "file:./src/conf/processes/";
  protected WorkflowServiceContainer workflowServiceContainer;
  protected WorkflowFormsService workflowFormsService;

  public BaseTest(String name) {
    super(name);
  }

  public void setUp() {
    workflowServiceContainer = (WorkflowServiceContainer) PortalContainer.
        getInstance().getComponentInstanceOfType(WorkflowServiceContainer.class);
    workflowFormsService = (WorkflowFormsService) PortalContainer.
        getInstance().getComponentInstanceOfType(WorkflowFormsService.class);
  }

  protected void deployProcess(String process, String[] files) throws IOException{
    URL url = new URL(PROCESS_PATH + process);
    InputStream is = url.openStream();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlInputStream(is);

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        url = new URL(PROCESS_PATH + file);
        processDefinition.getFileDefinition().addFile(file, url.openStream());
      }
    }
    ((WorkflowServiceContainerImpl) workflowServiceContainer).openJbpmContext().deployProcessDefinition(processDefinition);
  }

}
