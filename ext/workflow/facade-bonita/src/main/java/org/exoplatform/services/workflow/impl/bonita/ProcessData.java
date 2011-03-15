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
package org.exoplatform.services.workflow.impl.bonita;

//import hero.interfaces.BnProjectLightValue;

import org.exoplatform.services.workflow.Process;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;

/**
 * Created by Bull R&D
 * @author Brice Revenant
 * Dec 27, 2005
 */

public class ProcessData implements Process {
  String id             = null;
  String name           = null;
  int    version        = 0;
  String startStateName = null;

  /**
   * By convention an empty String represents the start state name. This does
   * not match any state in Bonita but is local to the service implementation.
   */
  public static final String START_STATE_NAME = new String();

  public ProcessData(ProcessDefinition projectValue) {
    this.id      = projectValue.getUUID().toString();
    this.name    = projectValue.getName();
    this.version = 1;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public int getVersion() {
    return this.version;
  }

  public String getStartStateName() {
    // There is no concept of start state in Bonita so by convention an
    // empty string indicates that the process needs to be instantiated.
    return ProcessData.START_STATE_NAME;
  }
}
