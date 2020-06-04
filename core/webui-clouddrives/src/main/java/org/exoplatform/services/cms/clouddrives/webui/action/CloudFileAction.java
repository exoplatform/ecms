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

import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.MoveNodeManageComponent;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveManager;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.webui.CloudDriveContext;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Support of Cloud Drive file operations (move, copy and linking) in ECMS. If
 * not a cloud file then this helper return <code>false</code> and the caller
 * code should apply default logic for the file. <br>
 * Code parts of this class based on original {@link PasteManageComponent} and
 * {@link MoveNodeManageComponent} (state of ECMS 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAction.java 00000 May 12, 2014 pnedonosko $
 */
public class CloudFileAction {

  /** The Constant LOG. */
  protected static final Log    LOG         = ExoLogger.getLogger(CloudFileAction.class);

  /** The empty params. */
  protected final String[]      emptyParams = new String[0];

  /** The ui explorer. */
  protected final UIJCRExplorer uiExplorer;

  /**
   * Set of source nodes for the operation. We rely on proper implementation of
   * Node's hash code and equality to avoid duplicates.
   */
  protected final Set<Node>     srcNodes    = new LinkedHashSet<Node>();

  /** The dest path. */
  protected String              destWorkspace, destPath;

  /** The dest node. */
  protected Node                destNode;

  /** The link. */
  protected Node                link;

  /** The move. */
  protected boolean             move;

  /**
   * Instantiates a new cloud file action.
   *
   * @param uiExplorer the ui explorer
   */
  public CloudFileAction(UIJCRExplorer uiExplorer) {
    this.uiExplorer = uiExplorer;
  }

  /**
   * Add source file (target file).
   *
   * @param srcNode {@link Node}
   * @return the cloud file action
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
   * @return the cloud file action
   * @throws Exception if cannot find node by given path
   */
  public CloudFileAction addSource(String srcInfo) throws Exception {
    // FYI don't take a target, use current context node
    return addSource(getNodeByInfo(srcInfo, false));
  }

  /**
   * Add source file by its wokrspace name and path.
   *
   * @param srcWorkspace {@link String}
   * @param srcPath {@link String}
   * @return the cloud file action
   * @throws Exception the exception
   */
  public CloudFileAction addSource(String srcWorkspace, String srcPath) throws Exception {
    // FYI don't take a target, use current context node
    return addSource(getNodeByPath(srcWorkspace, srcPath, false));
  }

  /**
   * Set link destination node.
   *
   * @param destNode {@link Node}
   * @return the cloud file action
   * @throws Exception the exception
   */
  public CloudFileAction setDestination(Node destNode) throws Exception {
    this.destWorkspace = destNode.getSession().getWorkspace().getName();
    // FYI take real target node, not the link
    this.destNode = getNodeByPath(this.destWorkspace, destNode.getPath(), true);
    this.destPath = this.destNode.getPath();
    return this;
  }

  /**
   * Set path of link destination.
   *
   * @param destInfo {@link String} in format of portal request ObjectId, see
   *          {@link UIWorkingArea#FILE_EXPLORER_URL_SYNTAX}
   * @return the cloud file action
   * @throws Exception if cannot find node by given path or cannot read its
   *           metadata
   */
  public CloudFileAction setDestination(String destInfo) throws Exception {
    // FYI take real target node, not the link
    return setDestination(getNodeByInfo(destInfo, true));
  }

  /**
   * Link behaviour as instead of "move" operation. Behaviour of "copy" by
   * default.
   *
   * @return the cloud file action
   */
  public CloudFileAction move() {
    this.move = true;
    return this;
  }

  /**
   * Return symlink-node created by {@link #apply()} method if it returned
   * <code>true</code>. This method has sense only after calling the mentioned
   * method, otherwise this method returns <code>null</code>.
   * 
   * @return the link {@link Node} or <code>null</code> if link not yet created
   *         or creation wasn't successful.
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
   * @throws RepositoryException the repository exception
   */
  public void rollback() throws RepositoryException {
    destNode.getSession().refresh(false);
  }

  /**
   * Apply an action for all added source files: move, copy or link from source
   * to destination. This method will save the session of destination node in
   * case of symlink creation.
   *
   * @return <code>true</code> if all sources were linked in destination
   *         successfully, <code>false</code> otherwise. If nothing applied
   *         return <code>false</code> also.
   * @throws CloudFileActionException if destination cannot be created locally,
   *           this exception will contain detailed message and a
   *           internationalized text for WebUI application.
   * @throws Exception the exception
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
          LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
          CloudDrive destLocal = driveService.findDrive(destNode);

          int linksCreated = 0;
          for (Node srcNode : srcNodes) {
            CloudDrive srcLocal = driveService.findDrive(srcNode);
            String srcPath = srcNode.getPath();
            if (!destPath.startsWith(srcPath)) {
              if (srcLocal != null && srcLocal.isDrive(srcNode)) {
                // it is a drive node as source - reject it
                throw new CloudFileActionException("Copy or move of cloud drive not supported: " + srcPath + " to " + destPath,
                                                   new ApplicationMessage("CloudFile.msg.CloudDriveCopyMoveNotSupported",
                                                                          null,
                                                                          ApplicationMessage.WARNING));
              }
            }

            if (destLocal == null) {
              // paste outside a cloud drive
              if (srcLocal != null && !linkManager.isLink(srcNode)) {
                // if cloud file, not a link to it...
                // it is also not a cloud drive root node as was checked
                // above...
                // then move not supported for the moment!
                if (move) {
                  if (srcLocal.hasFile(srcPath)) {
                    throw new CloudFileActionException("Move of cloud file to outside the cloud drive not supported: " + srcPath
                        + " -> " + destPath,
                                                       new ApplicationMessage("CloudFile.msg.MoveToOutsideDriveNotSupported",
                                                                              null,
                                                                              ApplicationMessage.WARNING));
                  } // else, it's local (ignored) node in the drive folder - use
                    // default behaviour for it
                } else {
                  // it's copy... check if it is the same workspace
                  String srcWorkspace = srcNode.getSession().getWorkspace().getName();
                  if (srcWorkspace.equals(destWorkspace)) {
                    // need create symlink into destNode
                    if (groupId != null && destSpace != null) {
                      // it's link in group documents (e.g. space documents):
                      // need share with the group
                      IShareDocumentService shareService = WCMCoreUtils.getService(IShareDocumentService.class);
                      shareService.publishDocumentToSpace(groupId, srcNode, "", PermissionType.READ);
                      // ShareDocumentService will create a link into Shared
                      // folder of the space docs, so we need move it to a right
                      // place at pointed destination by Paste action.
                      // XXX we use names hardcoded in
                      // ShareDocumentService.publishDocumentToSpace()
                      try {
                        String sharedFolderPath = documentsDrive.getHomePath() + "/Shared";
                        for (NodeIterator niter = actions.getCloudFileLinks(srcNode, groupId, true); niter.hasNext();) {
                          Node link = niter.nextNode();
                          if (link.getParent().getPath().equals(sharedFolderPath)) {
                            Session sysSession = link.getSession();
                            sysSession.refresh(true);
                            try {
                              sysSession.move(link.getPath(), destNode.getPath() + "/" + link.getName());
                              sysSession.save();
                            } catch (ItemExistsException e) {
                              throw new CloudFileActionException("Cannot move pasted Cloud File to a destination. " + srcWorkspace
                                  + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                                 new ApplicationMessage("CloudFile.msg.DestinationItemNameExists",
                                                                                        null,
                                                                                        ApplicationMessage.ERROR), e);
                            }
                            // don't touch others, assume the first is ours
                            break;
                          }
                        }
                      } catch (Exception e) {
                        throw new CloudFileActionException("Error moving pasted Cloud File to a destination. " + srcWorkspace
                            + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                           new ApplicationMessage("CloudFile.msg.ErrorMoveToDestination",
                                                                                  null,
                                                                                  ApplicationMessage.ERROR), e);
                      }
                      linksCreated++;
                    } else {
                      // else, since 1.6.0 we don't support sharing cloud files
                      // to non space groups (due to PLF5 UI limitation in
                      // ECMS's UIShareDocuments)
                      throw new CloudFileActionException("Linking to not space groups not supported for Cloud Drive files. "
                          + srcWorkspace + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                         new ApplicationMessage("CloudFile.msg.FileLinksNotSupportedToThisDestination",
                                                                                null,
                                                                                ApplicationMessage.ERROR));
                    }
                  } else {
                    // else, we don't support cross-workspaces paste for cloud
                    // drive
                    throw new CloudFileActionException("Linking between workspaces not supported for Cloud Drive files. "
                        + srcWorkspace + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                       new ApplicationMessage("CloudFile.msg.MoveBetweenWorkspacesNotSupported",
                                                                              null,
                                                                              ApplicationMessage.WARNING));
                  }
                }
              } // else, dest and src nulls means this happens not with cloud
                // drive files at all
            } else {
              // it's paste to a cloud drive sub-tree...
              if (srcLocal != null) {
                if (srcLocal.equals(destLocal)) {
                  if (!move) {
                    // track "paste" fact for copy-behaviour and then let
                    // original code work
                    new CloudDriveManager(destLocal).initCopy(srcNode, destNode);
                  }
                } else {
                  // TODO implement support copy/move to another drive
                  // For support of move also need refresh paths of all
                  // items in clipboard to reflect the
                  // moved parents, see PasteManageComponent.updateClipboard()
                  throw new CloudFileActionException("Copy or move of cloud file to another cloud drive not supported: " + srcPath
                      + " -> " + destPath,
                                                     new ApplicationMessage("CloudFile.msg.MoveToAnotherDriveNotSupported",
                                                                            null,
                                                                            ApplicationMessage.WARNING));
                }
              } // otherwise, let original code to copy the file to cloud drive
                // sub-tree
              // TODO do links need special handling for copy-to-drive?
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

  /**
   * Gets the node by info.
   *
   * @param pathInfo the path info
   * @param giveTarget the give target
   * @return the node by info
   * @throws Exception the exception
   */
  protected Node getNodeByInfo(String pathInfo, boolean giveTarget) throws Exception {
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(pathInfo);
    String workspace, path;
    if (matcher.find()) {
      workspace = matcher.group(1);
      path = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + pathInfo + "'");
    }
    return getNodeByPath(workspace, path, giveTarget);
  }

  /**
   * Gets the node by path.
   *
   * @param workspace the workspace
   * @param path the path
   * @param giveTarget the give target
   * @return the node by path
   * @throws Exception the exception
   */
  protected Node getNodeByPath(String workspace, String path, boolean giveTarget) throws Exception {
    Session srcSession = uiExplorer.getSessionByWorkspace(workspace);
    return uiExplorer.getNodeByPath(path, srcSession, giveTarget);
  }

}
