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
import org.exoplatform.ecm.webui.component.explorer.sidebar.UIClipboard;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;

import javax.jcr.Node;

/**
 * Extended ECMS clipboard with support of Cloud Drive files pasting as symlinks. This class used as component
 * config for actual {@link UIClipboard} in {@link CloudDriveClipboardActionComponent}.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UICloudDriveClipboard.java 00000 May 13, 2014 pnedonosko $
 * 
 */
@ComponentConfig(template = "app:/groovy/webui/component/explorer/sidebar/UIClipboard.gtmpl",
                 events = { @EventConfig(listeners = UICloudDriveClipboard.PasteActionListener.class),
                     @EventConfig(listeners = UIClipboard.DeleteActionListener.class),
                     @EventConfig(listeners = UIClipboard.ClearAllActionListener.class) })
public class UICloudDriveClipboard extends UIClipboard {

  protected static final Log LOG = ExoLogger.getLogger(UICloudDriveClipboard.class);

  public static class PasteActionListener extends UIClipboard.PasteActionListener {
    public void execute(Event<UIClipboard> event) throws Exception {
      UIClipboard uiClipboard = event.getSource();
      UIJCRExplorer uiExplorer = uiClipboard.getAncestorOfType(UIJCRExplorer.class);
      String indexParam = event.getRequestContext().getRequestParameter(OBJECTID);
      int index = Integer.parseInt(indexParam);
      ClipboardCommand selectedClipboard = uiClipboard.getClipboardData().get(index - 1);
      Node destNode = uiExplorer.getCurrentNode();

      CloudFileAction symlinks = new CloudFileAction(uiExplorer);
      try {
        symlinks.setDestination(destNode);
        symlinks.addSource(selectedClipboard.getWorkspace(), selectedClipboard.getSrcPath());
        boolean isCut = ClipboardCommand.CUT.equals(selectedClipboard.getType());
        if (isCut) {
          symlinks.move();
        }
        if (symlinks.apply()) {
          if (isCut) {
            uiClipboard.getClipboardData().remove(selectedClipboard);
          }
          uiExplorer.updateAjax(event);
          return;
        }
      } catch (CloudFileActionException e) {
        // this exception is a part of logic and it interrupts the move operation
        LOG.warn(e.getMessage());
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(e.getUIMessage());
        symlinks.rollback();
        uiExplorer.updateAjax(event);
        return;
      } catch (Exception e) {
        // let original code to work
        LOG.warn("Error creating link of cloud file. Default behaviour will be applied (file Paste).", e);
      }

      // else... original behaviour
      super.execute(event);
    }
  }

  /**
   * @throws Exception
   */
  public UICloudDriveClipboard() throws Exception {
    super();
  }
}
