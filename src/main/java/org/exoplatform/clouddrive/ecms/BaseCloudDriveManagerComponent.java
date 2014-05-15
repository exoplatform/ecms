/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseCloudDriveManagerComponent.java 00000 Sep 26, 2012 pnedonosko $
 */
public abstract class BaseCloudDriveManagerComponent extends UIAbstractManagerComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseCloudDriveManagerComponent.class);

  public BaseCloudDriveManagerComponent() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIAbstractManager.class;
  }

  protected void initContext(CloudProvider provider) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // we store current node in the context
      String nodePath = uiExplorer.getCurrentNode().getPath();
      String workspace = uiExplorer.getCurrentNode().getSession().getWorkspace().getName();
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, nodePath, provider);
    } else {
      LOG.error("Cannot find ancestor of type UIJCRExplorer in component " + this);
    }
  }

  protected void initContext() throws Exception {
    initContext(null);
  }

  @Deprecated // TODO not used, see RefreshViewManagerComponent
  protected void initView() throws Exception {
    UIWorkingArea workingArea = getAncestorOfType(UIWorkingArea.class);
    if (workingArea != null) {
      UIDocumentWorkspace document = workingArea.getChild(UIDocumentWorkspace.class);
      if (document != null) {
        // add RefreshView component
        RefreshViewForm refresh = document.getChild(RefreshViewForm.class);
        if (refresh == null) {
          document.addChild(RefreshViewForm.class, null, null);
          // TODO cleanup
          LOG.info(">>>> RefreshViewForm added " + workingArea.getChild(RefreshViewForm.class));
        }
      }
    } else {
      LOG.error("Cannot find ancestor of type UIWorkingArea in component " + this);
    }
  }
}
