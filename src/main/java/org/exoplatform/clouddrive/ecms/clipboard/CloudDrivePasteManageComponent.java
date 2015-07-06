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
package org.exoplatform.clouddrive.ecms.clipboard;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Support of Cloud Drive files pasting from ECMS Clipboard. If not a cloud file then original behaviour of
 * {@link PasteManageComponent} will be
 * applied. <br>
 * Code parts of this class based on original {@link PasteManageComponent} (state of ECMS
 * 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDrivePasteManageComponent.java 00000 May 12, 2014 pnedonosko $
 * 
 */
@ComponentConfig(events = {
    @EventConfig(listeners = CloudDrivePasteManageComponent.PasteActionListener.class) })
public class CloudDrivePasteManageComponent extends PasteManageComponent {

  protected static final Log    LOG         = ExoLogger.getLogger(CloudDrivePasteManageComponent.class);

  protected static final String GROUPS_PATH = "groupsPath";

  public static class PasteActionListener extends PasteManageComponent.PasteActionListener {
    public void processEvent(Event<PasteManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);

      CloudDriveClipboard symlinks = new CloudDriveClipboard(uiExplorer);
      try {
        String destParam = event.getRequestContext().getRequestParameter(OBJECTID);
        if (destParam == null) {
          symlinks.setDestination(uiExplorer.getCurrentNode());
        } else {
          symlinks.setDestination(destParam);
        }

        DriveData drive = uiExplorer.getDriveData();
        if (drive != null) {
          NodeHierarchyCreator hierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
          String groupsPath = hierarchyCreator.getJcrPath(GROUPS_PATH);
          if (drive.getHomePath().startsWith(groupsPath)) {
            // it's space documents
            String[] drivePermissions = drive.getAllPermissions();
            symlinks.setPermissions(drivePermissions);

            SpaceService spaces = WCMCoreUtils.getService(SpaceService.class);
            String groupId = drive.getName().replace('.', '/');
            symlinks.setDestinationSpace(spaces.getSpaceByGroupId(groupId));
          }
        }

        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
        Deque<ClipboardCommand> allClipboards = new LinkedList<ClipboardCommand>(clipboardService.getClipboardList(userId,
                                                                                                                   false));
        if (allClipboards.size() > 0) {
          Set<ClipboardCommand> virtClipboards = clipboardService.getClipboardList(userId, true);
          ClipboardCommand current = null; // will refer to last attempted to link
          if (virtClipboards.isEmpty()) { // single file
            current = allClipboards.getLast();
            boolean isCut = ClipboardCommand.CUT.equals(current.getType());
            symlinks.addSource(current.getWorkspace(), current.getSrcPath());
            if (isCut) {
              symlinks.move();
            }
            if (symlinks.create()) {
              symlinks.save();
              // file was successfully linked
              if (isCut) {
                // TODO should not happen until we will support cut-paste between drives
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
                symlinks.addSource(current.getWorkspace(), current.getSrcPath());
                linked.add(current);
              } else {
                // we have unexpected state when items in group clipboard have different types of operation
                LOG.warn("Cannot handle different types of clipboard operations for group action. Files "
                    + (isCut ? " cut-paste" : " copy-paste") + " already started but "
                    + (isThisCut ? " cut-paste" : " copy-paste") + " found for " + current.getSrcPath());
                // let default logic deal with this
                break;
              }
            }

            if (virtSize == linked.size()) {
              if (isCut != null && isCut) {
                symlinks.move();
              }
              if (symlinks.create()) {
                symlinks.save();
                // files was successfully linked
                if (isCut) {
                  // TODO should not happen until we will support cut-paste between drives
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
              symlinks.rollback();
              LOG.warn("Links cannot be created for all cloud files. Destination "
                  + symlinks.getDestonationPath() + "."
                  + (current != null ? " Last file " + current.getSrcPath() + "." : "")
                  + " Default behaviour will be applied (files Paste).");
            }
          }
        }
      } catch (CloudFileSymlinkException e) {
        // this exception is a part of logic and it interrupts the operation
        LOG.warn(e.getMessage());
        UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(e.getUIMessage());
        symlinks.rollback();
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
   * 
   */
  public CloudDrivePasteManageComponent() {
    super();
  }

}
