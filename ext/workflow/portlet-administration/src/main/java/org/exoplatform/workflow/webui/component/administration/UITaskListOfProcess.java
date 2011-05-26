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
package org.exoplatform.workflow.webui.component.administration ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : tran the  trong
 *          trongtt@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */
@ComponentConfigs({
  @ComponentConfig(
    type = UIGrid.class, id = "UITaskListOfProcess",
    template = "app:/groovy/webui/component/UIECMGrid.gtmpl"
  ),
  @ComponentConfig(
    template = "app:/groovy/webui/component/UITabPaneWithAction.gtmpl",
    events = {
        @EventConfig(listeners = UITaskListOfProcess.CancelActionListener.class)
    }
  )
})
public class UITaskListOfProcess extends UIContainer {

  private static String[] TASK_BEAN_FIELD = {"id", "taskName", "actorId", "end"} ;
  private static String[] ACTIONS = {"Cancel"};

  public UITaskListOfProcess() throws Exception {
    UIGrid uiTasksGrid = addChild(UIGrid.class, "UITaskListOfProcess", "UITasksGrid");
    uiTasksGrid.setLabel("UITasksGrid");
    uiTasksGrid.getUIPageIterator().setId("UITasksGrid") ;
    uiTasksGrid.configure("id", TASK_BEAN_FIELD, null) ;
  }

  public String[] getActions() { return ACTIONS; }

  public void updateTasksGrid(String id) throws Exception {
    WorkflowServiceContainer workflowServiceContainer =
      getApplicationComponent(WorkflowServiceContainer.class);
    UIGrid uiGrid = getChildById("UITasksGrid") ;
    List<Task> haveEndDateList = new ArrayList<Task>() ;
    for(Task task : workflowServiceContainer.getTasks(id)) {
      haveEndDateList.add(task);
    }
    Collections.sort(haveEndDateList, new TaskIdComparator());
    ListAccess<Task> taskList = new ListAccessImpl<Task>(Task.class, haveEndDateList);
    uiGrid.getUIPageIterator().setPageList(new LazyPageList<Task>(taskList, 10));
  }

  static public class TaskIdComparator implements Comparator<Task> {
    public int compare(Task o1, Task o2) throws ClassCastException {
      String id1 = o1.getId();
      String id2 = o2.getId();
      return id1.compareTo(id2);
    }
  }

  static public class CancelActionListener extends EventListener<UITaskListOfProcess> {
    public void execute(Event<UITaskListOfProcess> event) throws Exception {
      UITaskListOfProcess uicomp = event.getSource();
      UIWorkflowAdministrationPortlet uiAdministrationPortlet = uicomp.getAncestorOfType(UIWorkflowAdministrationPortlet.class);
      UIPopupWindow popup = uiAdministrationPortlet.getChildById("TaskListOfProcessPopup");
      if(popup != null){
        popup.setShow(false);
      }
    }
  }
}
