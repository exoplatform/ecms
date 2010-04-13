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

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 24, 2007  
 */
public class DelegateActionHandler implements ActionHandler {
  
  private static final long serialVersionUID = 1L;

  private boolean executed = false;
  
  public void execute(ExecutionContext context) {    
    try {
      System.err.println("Delegate"+context.getVariable("delegator").toString());
      System.err.println("initiator"+context.getVariable("initiator").toString());
      System.err.println("exo:validator"+context.getVariable("exo:validator").toString());
      context.setVariable("delegate_flg","true");                /* Set flag for delegate process */
      ProcessUtil.delegate(context);
    } catch (Exception e) {      
    } 
  }
  
}
