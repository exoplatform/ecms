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
package org.exoplatform.services.workflow;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

/**
 * Plugin to deploy predefined processes.
 *
 * Created by eXo Platform SAS
 * @author Brice Revenant
 * June 1, 2007
 */
public class PredefinedProcessesPlugin extends BaseComponentPlugin {

  /** Process configuration, as specified in the configuration file */
  private ProcessesConfig processesConfig = null;

  /**
   * Plugin constructor.
   * Caches data specified by the container
   *
   * @param params Initialization data
   * as specifed in the configuration file
   */
  public PredefinedProcessesPlugin(InitParams params) {
    ObjectParameter param = params.getObjectParam("predefined.processes");

    if(param != null) {
      // Make sure the Object parameter is specified in the configuration
      this.processesConfig = (ProcessesConfig) param.getObject();
    }
  }

  /**
   * Returns data contained by the plugin
   *
   * @return Processes configuration data
   */
  public ProcessesConfig getProcessesConfig() {
    return this.processesConfig;
  }
}
