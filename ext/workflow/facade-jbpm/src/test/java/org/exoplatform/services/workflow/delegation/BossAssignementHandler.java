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
package org.exoplatform.services.workflow.delegation;


import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 12 mai 2004
 */
public class BossAssignementHandler implements AssignmentHandler{

  public void assign(Assignable assignable, ExecutionContext assignmentContext) throws Exception {
    System.out.println("In selectActor of BossAssignementHandler : ");
    TaskInstance taskInstance = assignmentContext.getTaskInstance();
    System.out.println("  --> Previous actor : " + taskInstance.getPreviousActorId());
    if("benj".equals(taskInstance.getPreviousActorId()) ||
        "bossOfBenj".equals(taskInstance.getPreviousActorId())){
      System.out.println("  --> Next actor : bossOfBenj");
    }
    assignable.setActorId("bossOfBenj");
  }
}
