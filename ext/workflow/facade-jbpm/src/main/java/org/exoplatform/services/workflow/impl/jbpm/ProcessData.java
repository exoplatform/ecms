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

import org.exoplatform.services.workflow.Process;
import org.jbpm.graph.def.ProcessDefinition;

public class ProcessData implements Process {

  private ProcessDefinition def_;

  public ProcessData(ProcessDefinition def) {
    def_ = def;
  }

  public String getId() {
    return ""+def_.getId();
  }

  public String getName() {
    return ""+def_.getName();
  }

  public int getVersion() {
    return def_.getVersion();
  }

  public String getStartStateName() {
    return def_.getStartState().getName();
  }

}
