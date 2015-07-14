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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveManager;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.ecms.CloudDriveContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.MoveNodeManageComponent;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Support of Cloud Drive file operations (move, copy and linking) in ECMS. If not a cloud file then this
 * helper return
 * <code>false</code> and the caller code should apply default logic for the file. <br>
 * Code parts of this class based on original {@link PasteManageComponent} and {@link MoveNodeManageComponent}
 * (state of ECMS 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAction.java 00000 May 12, 2014 pnedonosko $
 * 
 */
public class CloudFileAction {

  protected static final Log    LOG         = ExoLogger.getLogger(CloudFileAction.class);

  protected final String[]      emptyParams = new String[0];

  protected final UIJCRExplorer uiExplorer;

  /**
   * Set of source nodes for the operation. We rely on proper implementation of Node's hash code and equality
   * to avoid duplicates.
   */
  protected final Set<Node>     srcNodes    = new LinkedHashSet<Node>();

  protected String              destWorkspace, destPath;

  protected Node                destNode;

  protected Node                link;

  protected boolean             move;

  /**
   * 
   */
  public CloudFileAction(UIJCRExplorer uiExplorer) {
    this.uiExplorer = uiExplorer;
  }

  /**
   * Add source file (target file).
   * 
   * @param srcNode {@link Node}
   * @return
   */
  public CloudFileAction addSource(Node srcNode) {
    this.srcNodes.add(srcNode);
    return this;
  }

  /**
   * Add source file path (path to target file).
   * 
   * @param srcInfo {@link String} in format of portal request ObjectId, see
   *          {@link UIWorkingArea#FILE_EXPLORER_URL_SYNTAX}
   * @return
   * @throws Exception if cannot find node by given path
   */
  public CloudFileAction addSource(String srcInfo) throws Exception {
    return addSource(getNodeByInfo(srcInfo));
  }

  /**
   * Add source file by its wokrspace name and path.
   * 
   * @param srcWorkspace {@link String}
   * @param srcPath {@link String}
   * @return
   * @throws Exception
   */
  public CloudFileAction addSource(String srcWorkspace, String srcPath) throws Exception {
    return addSource(getNodeByPath(srcWorkspace, srcPath));
  }

  /**
   * Set link destination node.
   * 
   * @param destNode {@link Node}
   * @return
   * @throws Exception
   */
  public CloudFileAction setDestination(Node destNode) throws Exception {
    this.destWorkspace = destNode.getSession().getWorkspace().getName();
    this.destNode = getNodeByPath(this.destWorkspace, destNode.getPath());
    this.destPath = this.destNode.getPath();
    return this;
  }

  /**
   * Set path of link destination.
   * 
   * @param destInfo {@link String} in format of portal request ObjectId, see
   *          {@link UIWorkingArea#FILE_EXPLORER_URL_SYNTAX}
   * @return
   * @throws Exception if cannot find node by given path or cannot read its metadata
   */
  public CloudFileAction setDestination(String destInfo) throws Exception {
    return setDestination(getNodeByInfo(destInfo));
  }

  /**
   * Link behaviour as instead of "move" operation. Behaviour of "copy" by default.
   * 
   * @return
   */
  public CloudFileAction move() {
    this.move = true;
    return this;
  }

  /**
   * Return symlink-node created by {@link #create()} method if it returned <code>true</code>. This method has
   * sense only after calling the mentioned method, otherwise this method returns <code>null</code>.
   * 
   * @return the link {@link Node} or <code>null</code> if link not yet created or creation wasn't successful.
   */
  public Node getLink() {
    return link;
  }

  /**
   * Workspace of already set destination or <code>null</code> if not yet set.
   * 
   * @return the destWorkspace {@link String}
   */
  public String getDestinationWorkspace() {
    return destWorkspace;
  }

  /**
   * Path of already set destination or <code>null</code> if not yet set.
   * 
   * @return the destPath {@link String}
   */
  public String getDestonationPath() {
    return destPath;
  }

  /**
   * Node of already set destination or <code>null</code> if not yet set.
   * 
   * @return the destNode {@link Node}
   */
  public Node getDestinationNode() {
    return destNode;
  }

  /**
   * Rollback the symlink at its destination in JCR.
   * 
   * @throws RepositoryException
   */
  public void rollback() throws RepositoryException {
    destNode.getSession().refresh(false);
  }

  /**
   * Apply an action for all added source files: move, copy or link from source to destination.
   * This method will save the session of destination node in case of symlink creation.
   * 
   * @return <code>true</code> if all sources were linked in destination successfully, <code>false</code>
   *         otherwise. If nothing applied return <code>false</code> also.
   * @throws CloudFileActionException if destination cannot be created locally, this exception will contain
   *           detailed message and a internationalized text for WebUI application.
   * @throws Exception
   */
  public boolean apply() throws CloudFileActionException, Exception {
    if (destWorkspace != null) {
      if (PermissionUtil.canAddNode(destNode) && !uiExplorer.nodeIsLocked(destNode) && destNode.isCheckedOut()) {
        if (srcNodes.size() > 0) {
          Space destSpace = null;
          String groupId = null;
          CloudFileActionService actions = WCMCoreUtils.getService(CloudFileActionService.class);
          DriveData documentsDrive = uiExplorer.getDriveData();
          if (documentsDrive != null) {
            if (actions.isGroupDrive(documentsDrive)) {
              groupId = documentsDrive.getName().replace('.', '/');
              // destination in group documents
              SpaceService spaces = WCMCoreUtils.getService(SpaceService.class);
              destSpace = spaces.getSpaceByGroupId(groupId);
            }
          }

          CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
          CloudDrive destLocal = driveService.findDrive(destNode);

          int linksCreated = 0;
          for (Node srcNode : srcNodes) {
            CloudDrive srcLocal = driveService.findDrive(srcNode);
            String srcPath = srcNode.getPath();
            if (!destPath.startsWith(srcPath)) {
              if (srcLocal != null && srcLocal.isDrive(srcNode)) {
                // it is a drive node as source - reject it
                throw new CloudFileActionException("Copy or move of cloud drive not supported: " + srcPath
                    + " to " + destPath,
                                                   new ApplicationMessage("CloudFile.msg.CloudDriveCopyMoveNotSupported",
                                                                          null,
                                                                          ApplicationMessage.WARNING));
              }
            }

            if (destLocal == null) {
              // paste outside a cloud drive
              if (srcLocal != null) {
                // if cloud file... (it is not a cloud drive node as was checked above)
                // then move not supported for the moment!
                if (move) {
                  if (srcLocal.hasFile(srcPath)) {
                    throw new CloudFileActionException("Move of cloud file to outside the cloud drive not supported: "
                        + srcPath + " -> " + destPath,
                                                       new ApplicationMessage("CloudFile.msg.MoveToOutsideDriveNotSupported",
                                                                              null,
                                                                              ApplicationMessage.WARNING));
                  } // else, it's local (ignored) node in the drive folder - use default behaviour for it
                } else {
                  // it's copy... check if it is the same workspace
                  String srcWorkspace = srcNode.getSession().getWorkspace().getName();
                  if (srcWorkspace.equals(destWorkspace)) {
                    // need create symlink into destNode
                    if (groupId != null) {
                      // it's link in group documents (e.g. space documents): need share with the group
                      String[] driveIdentity = documentsDrive.getAllPermissions();
                      actions.shareCloudFile(srcNode, srcLocal, driveIdentity);
                      this.link = actions.linkFile(srcNode, destNode, groupId);
                      actions.setAllPermissions(link, driveIdentity);
                    } else {
                      this.link = actions.linkFile(srcNode, destNode, null);
                    }
                    linksCreated++;
                  } else {
                    // else, we don't support cross-workspaces paste for cloud drive
                    throw new CloudFileActionException("Linking between workspaces not supported for Cloud Drive files. "
                        + srcWorkspace + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                       new ApplicationMessage("CloudFile.msg.MoveBetweenWorkspacesNotSupported",
                                                                              null,
                                                                              ApplicationMessage.WARNING));
                  }
                }
              } // else, dest and src nulls means this happens not with cloud drive files at all
            } else {
              // it's paste to a cloud drive sub-tree...
              if (srcLocal != null) {
                if (srcLocal.equals(destLocal)) {
                  if (!move) {
                    // track "paste" fact for copy-bahaviour and then let original code work
                    new CloudDriveManager(destLocal).initCopy(srcNode, destNode);
                  }
                } else {
                  // TODO implement support copy/move to another drive
                  // TODO if implement, do we need inform activities (via ContentMovedActivityListener etc)?
                  // if (activityService.isAcceptedNode(desNode) ||
                  // desNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
                  // listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode,
                  // desNode.getPath());
                  // }
                  // TODO for support of move also need refresh paths of all items in clipboard to reflect the
                  // moved parents, see PasteManageComponent.updateClipboard()
                  throw new CloudFileActionException("Copy or move of cloud file to another cloud drive not supported: "
                      + srcPath + " -> " + destPath,
                                                     new ApplicationMessage("CloudFile.msg.MoveToAnotherDriveNotSupported",
                                                                            null,
                                                                            ApplicationMessage.WARNING));
                }
              }
            }
          }

          if (linksCreated > 0) {
            RequestContext rcontext = WebuiRequestContext.getCurrentInstance();
            if (rcontext != null) {
              String multiple = linksCreated > 1 ? "s" : "";
              String destName = actions.documentName(destNode);

              ApplicationMessage title = new ApplicationMessage("CloudFile.msg.LinkCreated", new String[] { multiple });
              ApplicationMessage text;
              if (destSpace != null) {
                text = new ApplicationMessage("CloudFile.msg.FileLinksSharedInSpace",
                                              new String[] { multiple, destName, destSpace.getDisplayName() });
              } else if (groupId != null) {
                OrganizationService orgService = WCMCoreUtils.getService(OrganizationService.class);
                Group group = orgService.getGroupHandler().findGroupById(groupId);
                text = new ApplicationMessage("CloudFile.msg.FileLinksSharedInGroup",
                                              new String[] { multiple, destName, group.getGroupName() });
              } else {
                text = new ApplicationMessage("CloudFile.msg.FileLinksCreated", new String[] { multiple, destName });
              }

              ResourceBundle res = rcontext.getApplicationResourceBundle();
              title.setResourceBundle(res);
              text.setResourceBundle(res);
              CloudDriveContext.showInfo(rcontext, title.getMessage(), text.getMessage());
            }

            // finally save the link and related changes
            destNode.getSession().save();
            return true;
          } else {
            return false;
          }
        } else {
          throw new CloudFileActionException("Source should be defined.",
                                             new ApplicationMessage("CloudFile.msg.SourceNotDefined", emptyParams));
        }
      } else {
        throw new CloudFileActionException("Destination not writtable.",
                                           new ApplicationMessage("CloudFile.msg.DestinationNotWrittable", emptyParams));
      }
    } else {
      throw new CloudFileActionException("Destination should be defined.",
                                         new ApplicationMessage("CloudFile.msg.DestinationNotDefined", emptyParams));
    }
  }

  protected Node getNodeByInfo(String pathInfo) throws Exception {
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(pathInfo);
    String workspace, path;
    if (matcher.find()) {
      workspace = matcher.group(1);
      path = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + pathInfo + "'");
    }
    return getNodeByPath(workspace, path);
  }

  protected Node getNodeByPath(String workspace, String path) throws Exception {
    Session srcSession = uiExplorer.getSessionByWorkspace(workspace);
    return uiExplorer.getNodeByPath(path, srcSession, true);
  }

}
