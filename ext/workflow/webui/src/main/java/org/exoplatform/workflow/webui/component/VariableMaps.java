/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.workflow.webui.component;

import java.util.Map;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 9, 2009
 */
public class VariableMaps {
  private Map workflowVariables;
  private Map jcrVariables;

  public VariableMaps(Map workflowVariables, Map jcrVariables) {
    this.workflowVariables = workflowVariables;
    this.jcrVariables = jcrVariables;
  }

  public Map getJcrVariables() {
    return jcrVariables;
  }
  public Map getWorkflowVariables() {
    return workflowVariables;
  }
}
