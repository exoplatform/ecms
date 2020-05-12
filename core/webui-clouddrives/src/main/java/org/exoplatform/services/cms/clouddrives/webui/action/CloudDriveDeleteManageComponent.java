/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.webui.action;

import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.DeleteManageComponent;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.webui.action.CloudDriveDeleteManageComponent.DeleteActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;

/**
 * Overridden component to keep listener context with correct node (in case of
 * Cloud File links it should be a link node, not a target. XXX it's a
 * workaround).<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveDeleteManageComponent.java 00000 Jun 29, 2018
 *          pnedonosko $
 */
@ComponentConfig(events = { @EventConfig(listeners = DeleteActionListener.class) })
public class CloudDriveDeleteManageComponent extends DeleteManageComponent {

  /** The LOG. */
  private static final Log LOG = ExoLogger.getLogger(CloudDriveDeleteManageComponent.class.getName());

  /**
   * The listener interface for receiving deleteAction events.
   */
  public static class DeleteActionListener extends
                                           org.exoplatform.ecm.webui.component.explorer.rightclick.manager.DeleteManageComponent.DeleteActionListener {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> createContext(Event<DeleteManageComponent> event) throws Exception {
      // Take original context
      Map<String, Object> context = super.createContext(event);
      if (context != null) {
        UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
        String nodePath = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID);
        // For cloud files we set context node without finding its target in
        // case of symlink
        try {
          Node currentNode = getNodeByPath(nodePath, uiExplorer, false);
          CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
          CloudDrive drive = driveService.findDrive(currentNode);
          if (drive != null) {
            context.put(Node.class.getName(), currentNode);
          }
        } catch (PathNotFoundException pte) {
          throw new MessageException(new ApplicationMessage("UIPopupMenu.msg.path-not-found", null, ApplicationMessage.WARNING));
        } catch (Exception e) {
          LOG.error("Unexpected error while updating listener context", e);
        }
      }
      return context;
    }

    /**
     * Gets the node by path (code copied from super's private method).
     *
     * @param nodePath the node path
     * @param uiExplorer the ui explorer
     * @param giveTarget the give target
     * @return the node by path
     * @throws Exception the exception
     */
    private Node getNodeByPath(String nodePath, UIJCRExplorer uiExplorer, boolean giveTarget) throws Exception {
      nodePath = uiExplorer.getCurrentWorkspace() + ":" + nodePath;
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
      String wsName = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
      } else {
        wsName = uiExplorer.getCurrentWorkspace();
      }
      Session session = uiExplorer.getSessionByWorkspace(wsName);
      return uiExplorer.getNodeByPath(nodePath, session, giveTarget);
    }
  }
}
