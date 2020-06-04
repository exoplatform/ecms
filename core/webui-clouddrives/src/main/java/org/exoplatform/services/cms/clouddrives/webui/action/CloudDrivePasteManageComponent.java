/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;

/**
 * Support of Cloud Drive files pasting from ECMS Clipboard. If not a cloud file
 * then original behaviour of {@link PasteManageComponent} will be applied. <br>
 * Code parts of this class based on original {@link PasteManageComponent}
 * (state of ECMS 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDrivePasteManageComponent.java 00000 May 12, 2014
 *          pnedonosko $
 */
@ComponentConfig(events = { @EventConfig(listeners = CloudDrivePasteManageComponent.PasteActionListener.class) })
public class CloudDrivePasteManageComponent extends PasteManageComponent {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(CloudDrivePasteManageComponent.class);

  /**
   * The listener interface for receiving pasteAction events. The class that is
   * interested in processing a pasteAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addPasteActionListener</code> method. When the
   * pasteAction event occurs, that object's appropriate method is invoked.
   */
  public static class PasteActionListener extends PasteManageComponent.PasteActionListener {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<PasteManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);

      CloudFileAction action = new CloudFileAction(uiExplorer);
      try {
        String destParam = event.getRequestContext().getRequestParameter(OBJECTID);
        if (destParam == null) {
          action.setDestination(uiExplorer.getCurrentNode());
        } else {
          action.setDestination(destParam);
        }

        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
        Deque<ClipboardCommand> allClipboards =
                                              new LinkedList<ClipboardCommand>(clipboardService.getClipboardList(userId, false));
        if (allClipboards.size() > 0) {
          Set<ClipboardCommand> virtClipboards = clipboardService.getClipboardList(userId, true);
          // will refer to last attempted to link
          ClipboardCommand current = null;
          if (virtClipboards.isEmpty()) { // single file
            current = allClipboards.getLast();
            boolean isCut = ClipboardCommand.CUT.equals(current.getType());
            action.addSource(current.getWorkspace(), current.getSrcPath());
            if (isCut) {
              action.move();
            }
            if (action.apply()) {
              // file was linked
              if (isCut) {
                // TODO should not happen until we will support cut-paste
                // between drives
                virtClipboards.clear();
                allClipboards.remove(current);
              }
              // complete the event here
              uiExplorer.updateAjax(event);
              return;
            }
          } else { // multiple files
            final int virtSize = virtClipboards.size();
            Set<ClipboardCommand> linked = new LinkedHashSet<ClipboardCommand>();
            Boolean isCut = null;
            for (Iterator<ClipboardCommand> iter = virtClipboards.iterator(); iter.hasNext();) {
              current = iter.next();
              boolean isThisCut = ClipboardCommand.CUT.equals(current.getType());
              if (isCut == null) {
                isCut = isThisCut;
              }
              if (isCut.equals(isThisCut)) {
                action.addSource(current.getWorkspace(), current.getSrcPath());
                linked.add(current);
              } else {
                // we have unexpected state when items in group clipboard have
                // different types of operation
                LOG.warn("Cannot handle different types of clipboard operations for group action. Files "
                    + (isCut ? " cut-paste" : " copy-paste") + " already started but "
                    + (isThisCut ? " cut-paste" : " copy-paste") + " found for " + current.getSrcPath());
                // let default logic deal with this
                break;
              }
            }

            if (virtSize == linked.size()) {
              if (isCut != null && isCut) {
                action.move();
              }
              if (action.apply()) {
                // files was successfully linked
                if (isCut) {
                  // TODO should not happen until we will support cut-paste
                  // between drives
                  virtClipboards.clear();
                  for (ClipboardCommand c : linked) {
                    allClipboards.remove(c);
                  }
                }
                // complete the event here
                uiExplorer.updateAjax(event);
                return;
              }
            } else {
              // something goes wrong and we will let default code to work
              action.rollback();
              LOG.warn("Links cannot be created for all cloud files. Destination " + action.getDestonationPath() + "."
                  + (current != null ? " Last file " + current.getSrcPath() + "." : "")
                  + " Default behaviour will be applied (files Paste).");
            }
          }
        }
      } catch (CloudFileActionException e) {
        // this exception is a part of logic and it interrupts the operation
        LOG.warn(e.getMessage(), e);
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(e.getUIMessage());
        action.rollback();
        // complete the event here
        uiExplorer.updateAjax(event);
        return;
      } catch (Exception e) {
        // ignore and return false
        LOG.warn("Error creating link of cloud file. Default behaviour will be applied (file Paste).", e);
      }

      // else... call PasteManageComponent in all other cases
      super.processEvent(event);
    }
  }

  /**
   * Instantiates a new cloud drive paste manage component.
   */
  public CloudDrivePasteManageComponent() {
    super();
  }

}
