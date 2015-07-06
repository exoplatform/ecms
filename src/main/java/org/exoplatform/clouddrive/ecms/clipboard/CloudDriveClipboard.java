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
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.ecms.CloudDriveContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.MoveNodeManageComponent;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.security.AccessControlException;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Support of Cloud Drive files moving and copying in ECMS. If not a cloud file then this helper return
 * <code>false</code> and the caller code should apply default logic for the file. <br>
 * Code parts of this class based on original {@link PasteManageComponent} and {@link MoveNodeManageComponent}
 * (state of ECMS 4.0.4).<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveClipboard.java 00000 May 12, 2014 pnedonosko $
 * 
 */
public class CloudDriveClipboard {

  protected static final Log      LOG               = ExoLogger.getLogger(CloudDriveClipboard.class);

  protected static final String   EXO_PRIVILEGEABLE = "exo:privilegeable";

  protected static final String   MIX_VERSIONABLE   = "mix:versionable";

  protected static final String[] READ_PERMISSION   = new String[] { PermissionType.READ };

  protected final UIJCRExplorer   uiExplorer;

  /**
   * Set of source nodes for the operation. We rely on proper implementation of Node's hash code and equality
   * to avoid duplicates.
   */
  protected final Set<Node>       srcNodes          = new LinkedHashSet<Node>();

  protected String                destWorkspace, destPath;

  protected Node                  destNode;

  protected Space                 destSpace;

  protected String[]              permissions;

  protected Node                  link;

  protected boolean               move;

  /**
   * 
   */
  public CloudDriveClipboard(UIJCRExplorer uiExplorer) {
    this.uiExplorer = uiExplorer;
  }

  /**
   * Add source file (target file).
   * 
   * @param srcNode {@link Node}
   * @return
   */
  public CloudDriveClipboard addSource(Node srcNode) {
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
  public CloudDriveClipboard addSource(String srcInfo) throws Exception {
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
  public CloudDriveClipboard addSource(String srcWorkspace, String srcPath) throws Exception {
    return addSource(getNodeByPath(srcWorkspace, srcPath));
  }

  /**
   * Set link destination node.
   * 
   * @param destNode {@link Node}
   * @return
   * @throws Exception
   */
  public CloudDriveClipboard setDestination(Node destNode) throws Exception {
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
  public CloudDriveClipboard setDestination(String destInfo) throws Exception {
    return setDestination(getNodeByInfo(destInfo));
  }

  /**
   * Set destination space.
   * 
   * @param destSpace
   * @return
   * @throws Exception
   */
  public CloudDriveClipboard setDestinationSpace(Space destSpace) throws Exception {
    this.destSpace = destSpace;
    return this;
  }

  /**
   * Set permissions that source and destination file or link should have.
   * 
   * @param permissions
   * @return
   * @throws Exception
   */
  public CloudDriveClipboard setPermissions(String[] permissions) throws Exception {
    this.permissions = permissions;
    return this;
  }

  /**
   * Link behaviour as instead of "move" operation. Behaviour of "copy" by default.
   * 
   * @return
   */
  public CloudDriveClipboard move() {
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
   * Permissions that source and destination file or link should have.
   * 
   * @return the permissions
   */
  public String[] getPermissions() {
    return permissions;
  }

  /**
   * Save the symlink at its destination in JCR.
   * 
   * @throws RepositoryException
   */
  public void save() throws RepositoryException {
    // XXX we use destination node session assuming it will save all our changes,
    // but source nodes changes this save may not persist in general case.
    // See create(Node) where source nodes permissions saved explicitly
    destNode.getSession().save();
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
   * Create links in destination folder for all added source files. Note that this method doesn't save the
   * session of destination node - this should be done by the caller code to persist the changes.
   * 
   * @return <code>true</code> if all added links created successfully, <code>false</code> otherwise. If
   *         nothing added return <code>false</code> also.
   * @throws CloudFileSymlinkException if link cannot be created logical, this exception will contain detailed
   *           message and a internationalized text for WebUI application.
   * @throws Exception
   */
  public boolean create() throws CloudFileSymlinkException, Exception {
    if (srcNodes.size() > 0) {
      for (Node srcNode : srcNodes) {
        if (!create(srcNode)) {
          return false;
        }
      }

      RequestContext rcontext = WebuiRequestContext.getCurrentInstance();
      if (rcontext != null) {
        String multiple = srcNodes.size() > 1 ? "s" : "";
        String destName;
        try {
          destName = destNode.getProperty("exo:title").getString();
        } catch (PathNotFoundException e) {
          destName = destNode.getName();
        }

        ApplicationMessage title = new ApplicationMessage("CloudFile.msg.LinkCreated",
                                                          new String[] { multiple });
        ApplicationMessage text;
        if (destSpace != null) {
          text = new ApplicationMessage("CloudFile.msg.FileLinksCreatedInSpace",
                                        new String[] { multiple, destName, destSpace.getDisplayName() });
        } else {
          text = new ApplicationMessage("CloudFile.msg.FileLinksCreated",
                                        new String[] { multiple, destName });
        }

        ResourceBundle res = rcontext.getApplicationResourceBundle();
        title.setResourceBundle(res);
        text.setResourceBundle(res);
        CloudDriveContext.showInfo(rcontext, title.getMessage(), text.getMessage());
      }
      return true;
    }
    return false;
  }

  protected boolean create(final Node srcNode) throws CloudFileSymlinkException, Exception {
    if (destWorkspace != null) {
      String srcPath = srcNode.getPath();
      if (!destPath.startsWith(srcPath)) {
        if (PermissionUtil.canAddNode(destNode) && !uiExplorer.nodeIsLocked(destNode)
            && destNode.isCheckedOut()) {

          CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);

          CloudDrive srcLocal = driveService.findDrive(srcNode);
          if (srcLocal != null && srcLocal.isDrive(srcNode)) {
            // it is a drive node as source - reject it
            throw new CloudFileSymlinkException("Copy or move of cloud drive not supported: " + srcPath
                + " -> " + destPath,
                                                new ApplicationMessage("CloudFile.msg.CloudDriveCopyMoveNotSupported",
                                                                       null,
                                                                       ApplicationMessage.WARNING));
          }

          CloudDrive destLocal = driveService.findDrive(destNode);
          if (destLocal == null) {
            // paste outside a cloud drive
            if (srcLocal != null) {
              // if cloud file... (it is not a cloud drive node as we already checked above)
              // then move not supported for the moment!
              if (move) {
                if (srcLocal.hasFile(srcPath)) {
                  throw new CloudFileSymlinkException("Move of cloud file to outside the cloud drive not supported: "
                      + srcPath + " -> " + destPath,
                                                      new ApplicationMessage("CloudFile.msg.MoveToOutsideDriveNotSupported",
                                                                             null,
                                                                             ApplicationMessage.WARNING));
                } else {
                  // it's local (ignored) node in the drive folder - use default behaviour for it
                  return false;
                }
              } else {
                // it's copy... check if it is the same workspace
                String srcWorkspace = srcNode.getSession().getWorkspace().getName();
                if (srcWorkspace.equals(destWorkspace)) {
                  // create symlink on destNode
                  LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
                  String linkName = srcNode.getName();
                  String linkTitle = srcNode.hasProperty("exo:title") ? srcNode.getProperty("exo:title")
                                                                               .getString()
                                                                      : linkName;
                  this.link = linkManager.createLink(destNode, null, srcNode, linkName, linkTitle);

                  // ensure the srcNode and target link has access permissions (actual when copying to a
                  // space)
                  if (permissions != null) {
                    setPermissions(link, permissions);
                    // avoid firing Cloud Drive synchronization
                    CloudDriveStorage srcStorage = (CloudDriveStorage) srcLocal;
                    srcStorage.localChange(new Change<Void>() {
                      @Override
                      public Void apply() throws RepositoryException {
                        Node parent = srcNode.getParent();
                        // we keep access to all sub-files of the src parent restricted
                        setParentPermissions(parent, permissions);
                        setPermissions(srcNode, permissions);
                        // XXX To let ECMS action filters work smoothly need make srcNode versionable,
                        // otherwise they will search for versionable ancestor and may face with
                        // AccessDeniedException when file shared to outside the drive owner user folder.
//                        if (srcNode.canAddMixin(MIX_VERSIONABLE)) {
//                          srcNode.addMixin(MIX_VERSIONABLE);
//                        }
                        parent.save(); // save everything here!
                        return null;
                      }
                    });
                  }
                } else {
                  // else, we don't support cross-workspaces paste for cloud drive
                  throw new CloudFileSymlinkException("Linking between workspaces not supported for Cloud Drive files. "
                      + srcWorkspace + ":" + srcPath + " -> " + destWorkspace + ":" + destPath,
                                                      new ApplicationMessage("CloudFile.msg.MoveBetweenWorkspacesNotSupported",
                                                                             null,
                                                                             ApplicationMessage.WARNING));
                }
              }
              return true;
            }
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

                throw new CloudFileSymlinkException("Copy or move of cloud file to another cloud drive not supported: "
                    + srcPath + " -> " + destPath,
                                                    new ApplicationMessage("CloudFile.msg.MoveToAnotherDriveNotSupported",
                                                                           null,
                                                                           ApplicationMessage.WARNING));
              }
            }
          }
        }
      }
    } else {
      throw new IllegalArgumentException("Destination should be defined.");
    }

    // everything else - link not created
    return false;
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

  /**
   * Set read permissions on the link target node to all given identities (e.g. space group members).
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user indetities (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setPermissions(Node node, String[] identities) throws AccessControlException,
                                                                RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      target.addMixin(EXO_PRIVILEGEABLE);
    }
    for (String identity : identities) {
      // It is for special debug cases
      // if (LOG.isDebugEnabled()) {
      // LOG.debug(">>> hasPermission " + identity + " identity: "
      // + IdentityHelper.hasPermission(target.getACL(), identity, PermissionType.READ));
      // }
      target.setPermission(identity, READ_PERMISSION);
    }
  }

  /**
   * Set permissions on a parent node of a link target: all child nodes will become exo:privilegeable (thus
   * copy permissions from its priviligeable parent). If a child already of priviligeable type - nothing will
   * be performed, by this we assume its permissions already handled as required. After this read permissions
   * will be added to the parent node.<br>
   * This method SHOULD be used before setting permissions to a link target node in this parent. In this way
   * we will keep permission of the target in consistent state.
   * 
   * @param parent
   * @param identities
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setParentPermissions(Node parent, String[] identities) throws AccessControlException,
                                                                        RepositoryException {
    // first we go through all sub-files/folders and enabled exo:privilegeable, this will copy current parent
    // permissions to the child nodes for those that aren't privilegeable already.
    for (NodeIterator citer = parent.getNodes(); citer.hasNext();) {
      ExtendedNode child = (ExtendedNode) citer.nextNode();
      if (child.canAddMixin(EXO_PRIVILEGEABLE)) {
        child.addMixin(EXO_PRIVILEGEABLE);
      } // else, this child already has permissions, we assume they are OK and do nothing
    }

    // then we set read permissions to the parent
    setPermissions(parent, identities);
  }

}
