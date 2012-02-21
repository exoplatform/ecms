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

import org.exoplatform.services.log.Log;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.log.ExoLogger;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 24, 2007
 */
public class TrashMovementActionHandler implements ActionHandler {

  private static final long serialVersionUID = 1L;

  private boolean executed = false;
  private static final Log LOG  = ExoLogger.getLogger(TrashMovementActionHandler.class);

  public void execute(ExecutionContext context) {
    try {
      if (executed)
        return;
      executed = true;
      moveTrash(context);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    } finally {
      context.getToken().signal("move-done");
    }
  }

  private void moveTrash(ExecutionContext context) {
    String[] location = ProcessUtil.getCurrentLocation(context);
    String currentWorkspace = location[1];
    String currentPath = location[2];
    String trashWorkspace = (String)context.getVariable("exo:trashWorkspace");
    String trashPath = (String)context.getVariable("exo:trashPath");
    String destPath = ProcessUtil.computeDestinationPath(context, currentPath,trashPath);
    CmsService cmsService = ProcessUtil.getService(context, CmsService.class);
    cmsService.moveNode(currentPath, currentWorkspace, trashWorkspace, destPath);
    ProcessUtil.setCurrentLocation(context,trashWorkspace,destPath);
    ProcessUtil.moveTrash(context);
  }
}
