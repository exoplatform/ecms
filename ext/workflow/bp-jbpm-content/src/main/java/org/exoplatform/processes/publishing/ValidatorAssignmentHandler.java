/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Jan 2, 2008
 */
public class ValidatorAssignmentHandler implements AssignmentHandler {

  public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
    String validator = (String) executionContext.getVariable("exo:validator");
    String initiator = (String) executionContext.getVariable("initiator");
    String delegate_flg = (String) executionContext.getVariable("delegate_flg");

    if ((delegate_flg != null) && delegate_flg.equals("true")) {                  /* When execute delegate process */
      assignable.setActorId(initiator);
    } else
      assignable.setActorId(validator);
  }

}
