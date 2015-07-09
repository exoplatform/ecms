/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.MoveNodeManageComponent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;

/**
 * Move node support for Cloud Drive files. Instead of moving the cloud file node we create a symlink to the
 * file in drive folder.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MoveNodeManageComponent.java 00000 May 19, 2014 pnedonosko $
 * 
 */
@ComponentConfig(
                 events = { @EventConfig(
                                         listeners = CloudDriveMoveNodeManageComponent.MoveNodeActionListener.class,
                                         confirm = "UIWorkingArea.msg.confirm-move") })
public class CloudDriveMoveNodeManageComponent extends MoveNodeManageComponent {

  protected static final Log LOG = ExoLogger.getLogger(CloudDriveMoveNodeManageComponent.class);

  public static class MoveNodeActionListener extends MoveNodeManageComponent.MoveNodeActionListener {
    public void processEvent(Event<MoveNodeManageComponent> event) throws Exception {
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      String destInfo = event.getRequestContext().getRequestParameter("destInfo");

      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      CloudFileAction action = new CloudFileAction(uiExplorer);
      try {
        action.setDestination(destInfo);
        if (srcPath.indexOf(";") > -1) {
          // multiple nodes move
          String[] srcPaths = srcPath.split(";");
          for (String srcp : srcPaths) {
            action.addSource(srcp);
          }
        } else {
          // single node move
          action.addSource(srcPath);
        }
        if (action.move().apply()) {
          uiExplorer.updateAjax(event);
          return;
        } // else default logic
      } catch (CloudFileActionException e) {
        // this exception is a part of logic and it interrupts the move operation
        LOG.warn(e.getMessage());
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(e.getUIMessage());
        action.rollback();
        uiExplorer.updateAjax(event);
        return;
      } catch (Exception e) {
        // let default logic handle this in super
        LOG.warn("Error creating link of cloud file. Default behaviour will be applied (file move).", e);
      }

      // invoke super in all other cases
      super.processEvent(event);
    }
  }

  /**
   * 
   */
  public CloudDriveMoveNodeManageComponent() {
    super();
  }

}
