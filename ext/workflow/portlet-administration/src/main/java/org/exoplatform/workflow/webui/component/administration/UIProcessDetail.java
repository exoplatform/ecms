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
package org.exoplatform.workflow.webui.component.administration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.workflow.ProcessInstance;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : tran the  trong
 *          trongtt@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */
@ComponentConfig(template = "app:/groovy/webui/component/UITabPane.gtmpl")

public class UIProcessDetail extends UIContainer {

  private String processInstanceId;
  private List<ProcessInstance> completedProcessInstanceList = new ArrayList<ProcessInstance>();
  private List<ProcessInstance> runningProcessInstanceList = new ArrayList<ProcessInstance>();

  public UIProcessDetail() throws Exception {
    addChild(UIRunningProcesses.class, null, null);
    addChild(UICompletedProcesses.class, null, null).setRendered(false);
  }

  public void updateProcessGrid(String id) throws Exception {
    completedProcessInstanceList.clear();
    runningProcessInstanceList.clear();
    WorkflowServiceContainer workflowServiceContainer = getApplicationComponent(WorkflowServiceContainer.class);
    if (id != null)
      processInstanceId = id;
    List<ProcessInstance> processInstanceList = workflowServiceContainer.getProcessInstances(processInstanceId);
    for (ProcessInstance processInstance : processInstanceList) {
      if (processInstance.getEndDate() != null)
        completedProcessInstanceList.add(processInstance);
      else
        runningProcessInstanceList.add(processInstance);
    }

    UICompletedProcesses uiCompletedProcess = getChild(UICompletedProcesses.class);
    ListAccess<ProcessInstance> completedProcessList = new ListAccessImpl<ProcessInstance>(ProcessInstance.class,
                                                                                           completedProcessInstanceList);
    uiCompletedProcess.getUIPageIterator()
                      .setPageList(new LazyPageList<ProcessInstance>(completedProcessList, 10));

    UIRunningProcesses uiRunningProcess = getChild(UIRunningProcesses.class);
    ListAccess<ProcessInstance> runningProcessList = new ListAccessImpl<ProcessInstance>(ProcessInstance.class,
                                                                                         runningProcessInstanceList);
    uiRunningProcess.getUIPageIterator()
                    .setPageList(new LazyPageList<ProcessInstance>(runningProcessList, 10));
  }
  
  public List<ProcessInstance> getRunningProcessInstance() {
    return runningProcessInstanceList;
  }
  
  public List<ProcessInstance> getCompletedProcessInstance() {
    return completedProcessInstanceList;
  }  

  static public class TaskIdComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      String id1 = ((Task) o1).getId();
      String id2 = ((Task) o2).getId();
      return id1.compareTo(id2);
    }
  }

}