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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseCloudDriveManagerComponent.java 00000 Sep 26, 2012 pnedonosko $
 */
public abstract class BaseCloudDriveManagerComponent extends UIAbstractManagerComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseCloudDriveManagerComponent.class);

  /**
   * Workspace and node path associated with current context.
   */
  protected String           workspace, path;

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIAbstractManager.class;
  }

  protected void initContext() throws Exception {
    Node contextNode;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // when in document explorer
      contextNode = uiExplorer.getCurrentNode();
    } else if (getParent() instanceof org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation) {
      // when in social activity stream (file view)
      UIBaseNodePresentation docViewer = getParent();
      contextNode = docViewer.getNode();
    } else {
      workspace = path = null;
      LOG.error("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: " + this.getParent());
      return;
    }
    if (contextNode != null) {
      // we store current node in the context
      path = contextNode.getPath();
      workspace = contextNode.getSession().getWorkspace().getName();
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, path);
    }
  }
}
