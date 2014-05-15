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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveManager;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.ecm.jcr.model.ClipboardCommand;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Support of Cloud Drive files pasting from ECMS Clipboard. If not a cloud file then original behaviour of
 * {@link PasteManageComponent} will be applied. <br>
 * Code parts of this class based on original {@link PasteManageComponent} (state of ECMS 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDrivePasteManageComponent.java 00000 May 12, 2014 pnedonosko $
 * 
 */
@ComponentConfig(
                 events = { @EventConfig(listeners = CloudDrivePasteManageComponent.PasteActionListener.class) })
public class CloudDrivePasteManageComponent extends PasteManageComponent {

  protected static final Log       LOG         = ExoLogger.getLogger(CloudDrivePasteManageComponent.class);

  public static class PasteActionListener extends UIWorkingAreaActionListener<PasteManageComponent> {
    public void processEvent(Event<PasteManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      try {
        String destParam = event.getRequestContext().getRequestParameter(OBJECTID);
        String destPath;
        Session destSession;
        String destWorkspace;
        if (destParam != null) {
          Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destParam);
          if (matcher.find()) {
            destWorkspace = matcher.group(1);
            destPath = matcher.group(2);
            destSession = uiExplorer.getSessionByWorkspace(destWorkspace);
          } else {
            destPath = null;
            destSession = null;
            destWorkspace = null;
          }
        } else {
          destPath = null;
          destSession = null;
          destWorkspace = null;
        }

        LinkedList<ClipboardCommand> allClipboards = uiExplorer.getAllClipBoard();
        if (allClipboards.size() > 0) {
          // Use the method getNodeByPath because it is link aware
          Node destNode = destPath == null ? uiExplorer.getCurrentNode()
                                          : uiExplorer.getNodeByPath(destPath, destSession);
          destSession = destNode.getSession();

          UIWorkingArea uiWorkingArea = event.getSource().getParent();
          List<ClipboardCommand> virtClipboards = uiWorkingArea.getVirtualClipboards();
          ClipboardCommand current = null; // will refer to last attempted to link
          ClipboardCommand lastLinked = null; // will refer to last successfully pasted-linked
          if (virtClipboards.isEmpty()) { // single file
            current = allClipboards.getLast();
            if (processCreateLink(current, destNode, uiExplorer)) {
              destSession.save();
              // file was successfully linked
              lastLinked = current;
              if (ClipboardCommand.CUT.equals(current.getType())) {
                allClipboards.remove(current);
              }
            }
          } else { // multiple files
            final int virtSize = virtClipboards.size();
            int linked = 0;
            for (Iterator<ClipboardCommand> iter = virtClipboards.iterator(); iter.hasNext();) {
              current = iter.next();
              if (processCreateLink(current, destNode, uiExplorer)) {
                lastLinked = current;
                linked++;
              } else {
                break;
              }
            }

            if (virtSize == linked) {
              destSession.save();
              // all files were linked successfully
              if (lastLinked != null && ClipboardCommand.CUT.equals(lastLinked.getType())) {
                virtClipboards.clear();
              }
            } else {
              // something goes wrong and we will let original code work (paste files)
              destSession.refresh(false);
              lastLinked = null;
              LOG.warn("Links created not for all cloud files. Destenation " + destNode.getPath() + "."
                  + (current != null ? "Last file " + current.getSrcPath() + "." : "")
                  + " Default behaviour will be applied (files Paste).");
            }
          }

          if (lastLinked != null) {
            // TODO do we need this?
            // listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode,
            // desNode.getPath());

            // complete the event here
            uiExplorer.updateAjax(event);
            return;
          } 
        }
      } catch (Exception e) {
        // ignore and return false
        LOG.warn("Error creating link of cloud file. Default behaviour will be applied (file Paste).", e);
      }
      // else... call PasteManageComponent in all other cases
      pasteManage(event, uiExplorer);
    }
  }

  /**
   * 
   */
  public CloudDrivePasteManageComponent() {
    super();
  }

  static boolean processCreateLink(ClipboardCommand clipboard, Node destNode, UIJCRExplorer uiExplorer) throws Exception {
    if (PermissionUtil.canAddNode(destNode) && !uiExplorer.nodeIsLocked(destNode) && destNode.isCheckedOut()) {
      String srcPath = clipboard.getSrcPath();
      String srcWorkspace = clipboard.getWorkspace();
      Session srcSession = uiExplorer.getSessionByWorkspace(srcWorkspace);
      // Use the method getNodeByPath because it is link aware
      Node srcNode = uiExplorer.getNodeByPath(srcPath, srcSession, false);
      // Reset the path to manage the links that potentially create virtual path
      srcPath = srcNode.getPath();
      // Reset the session to manage the links that potentially change of workspace
      srcSession = srcNode.getSession();
      // Reset the workspace name to manage the links that potentially change of workspace
      srcWorkspace = srcSession.getWorkspace().getName();

      UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);

      CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
      CloudDrive destLocal = driveService.findDrive(destNode);
      if (destLocal == null) {
        // paste outside a cloud drive
        if (srcNode.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)) {
          // if cloud file...
          // but cut not supported!
          if (ClipboardCommand.CUT.equals(clipboard.getType())) {
            LOG.warn("Move (by cut-paste) of cloud file to outside the cloud drive not supported: "
                + srcNode.getPath() + " -> " + destNode.getPath());
            uiApp.addMessage(new ApplicationMessage("CloudDriveClipboard.msg.CloudFilePasteMoveToOutsideNotSupported",
                                                    null,
                                                    ApplicationMessage.WARNING));
            return true;
          }

          // check if it is the same workspace
          String destWorkspace = destNode.getSession().getWorkspace().getName();
          if (srcWorkspace.equals(destWorkspace)) {
            // create symlink on destNode
            LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
            String linkName = srcNode.getName();
            String linkTitle = srcNode.hasProperty("exo:title") ? srcNode.getProperty("exo:title")
                                                                         .getString() : linkName;
            Node link = linkManager.createLink(destNode, null, srcNode, linkName, linkTitle);
            LOG.info("Cloud file linked as " + link.getPath()); // TODO info -> debug
          } else {
            // else, we don't support cross-workspaces paste for cloud drive
            LOG.warn("Linking between workspaces not supported for Cloud Drive files. " + srcWorkspace + ":"
                + srcNode.getPath() + " -> " + destWorkspace + ":" + destNode.getPath());
            uiApp.addMessage(new ApplicationMessage("CloudDriveClipboard.msg.CloudFilePasteBetweenWorkspacesNotSupported",
                                                    null,
                                                    ApplicationMessage.WARNING));
          }
          return true;
        }
      } else {
        // it's paste to a cloud drive sub-tree...
        CloudDrive srcLocal = driveService.findDrive(srcNode);
        if (srcLocal != null) {
          if (srcLocal.equals(destLocal)) {
            if (ClipboardCommand.COPY.equals(clipboard.getType())) {
              // track "paste" fact for copy-bahaviour and then let original code work
              new CloudDriveManager(destLocal).initCopy(srcNode, destNode);
            }
          } else {
            // TODO implement support copy/move to another drive
            LOG.warn("Copy or move of cloud file to another cloud drive not supported: " + srcNode.getPath()
                + " -> " + destNode.getPath());
            uiApp.addMessage(new ApplicationMessage("CloudDriveClipboard.msg.CloudFilePasteToAnotherDriveNotSupported",
                                                    null,
                                                    ApplicationMessage.WARNING));
            return true;
          }
        }
      }
    }
    // everything else - original behaviour
    return false;
  }
}
