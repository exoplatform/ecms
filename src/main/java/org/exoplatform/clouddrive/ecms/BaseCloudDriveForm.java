/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.form.UIForm;

public abstract class BaseCloudDriveForm extends UIForm implements UIPopupComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseCloudDriveForm.class);

  /**
   * Workspace and node path associated with current context.
   */
  protected String workspace, path;
  
  protected void initContext() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // we store current node in the context
      path = uiExplorer.getCurrentNode().getPath();
      workspace = uiExplorer.getCurrentNode().getSession().getWorkspace().getName();
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, path);
    } else {
      workspace = path = null;
      LOG.error("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: "
          + this.getParent());
    }
  }

  @Override
  public void activate() {
    // nothing

  }

  @Override
  public void deActivate() {
    // nothing
  }
}
