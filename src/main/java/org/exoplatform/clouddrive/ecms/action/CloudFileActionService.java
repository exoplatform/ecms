
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
package org.exoplatform.clouddrive.ecms.action;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.picocontainer.Startable;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveSecurity;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ThreadExecutor;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.ecm.connector.platform.ManageDocumentService;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.web.application.ApplicationMessage;

/**
 * Integration of Cloud Drive with eXo Platform apps.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileActionService.java 00000 Jul 6, 2015 pnedonosko $
 */
public class CloudFileActionService implements Startable {

  /** The Constant ECD_CLOUDFILELINK. */
  public static final String      ECD_CLOUDFILELINK        = "ecd:cloudFileLink";

  /** The Constant ECD_SHAREIDENTITY. */
  public static final String      ECD_SHAREIDENTITY        = "ecd:shareIdentity";

  /** The Constant SHARE_CLOUD_FILES_SPACES. */
  public static final String      SHARE_CLOUD_FILES_SPACES = "sharecloudfiles:spaces";

  /** The Constant LOG. */
  protected static final Log      LOG                      = ExoLogger.getLogger(CloudFileActionService.class);

  /** The Constant SPACES_GROUP. */
  protected static final String   SPACES_GROUP             = "spaces";

  /** The Constant EXO_OWNEABLE. */
  protected static final String   EXO_OWNEABLE             = "exo:owneable";

  /** The Constant EXO_PRIVILEGEABLE. */
  protected static final String   EXO_PRIVILEGEABLE        = "exo:privilegeable";

  /** The Constant MIX_VERSIONABLE. */
  protected static final String   MIX_VERSIONABLE          = "mix:versionable";

  /** The Constant EXO_TRASHFOLDER. */
  protected static final String   EXO_TRASHFOLDER          = "exo:trashFolder";

  /** The Constant READER_PERMISSION. */
  protected static final String[] READER_PERMISSION        = new String[] { PermissionType.READ };

  /** The Constant MANAGER_PERMISSION. */
  protected static final String[] MANAGER_PERMISSION       = new String[] { PermissionType.READ, PermissionType.REMOVE,
      PermissionType.SET_PROPERTY };

  /**
   * Act on ecd:cloudFileLinkGroup property removal on a cloud file symlink and
   * then unshare the file from the group where it was shared by the link.
   */
  protected class LinkRemoveListener implements EventListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(EventIterator events) {
      while (events.hasNext()) {
        Event event = events.nextEvent();
        try {
          final String eventPath = event.getPath();
          if (eventPath.endsWith(ECD_SHAREIDENTITY)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Cloud File link removed. User: " + event.getUserID() + ". Path: " + eventPath);
            }

            String linkPath = eventPath.substring(0, eventPath.indexOf(ECD_SHAREIDENTITY) - 1);
            String cloudFileUUID = removedLinks.remove(linkPath);
            if (cloudFileUUID != null) {
              String identity = removedShared.get(cloudFileUUID);
              if (identity != null) {
                // was marked by RemoveCloudFileLinkAction
                try {
                  Node fileNode = systemSession().getNodeByUUID(cloudFileUUID);
                  CloudDrive localDrive = cloudDrives.findDrive(fileNode);
                  if (localDrive != null) {
                    if (getCloudFileLinks(fileNode, identity, true).getSize() == 0) {
                      // unshare only if no more links found for given identity
                      // (user or group)
                      DriveData documentDrive = null;
                      if (identity.startsWith("/")) {
                        // it's group drive
                        documentDrive = getGroupDrive(identity);
                        if (documentDrive != null) {
                          removeCloudFilePermission(fileNode, localDrive, new StringBuilder("*:").append(identity).toString());
                        }
                      } else {
                        // try as user drive
                        documentDrive = getUserDrive(identity);
                        if (documentDrive != null) {
                          removeCloudFilePermission(fileNode, localDrive, identity);
                        }
                      }
                      if (documentDrive == null) {
                        if (LOG.isDebugEnabled()) {
                          LOG.debug("Cannot find documents drive for " + fileNode.getPath()
                              + ". Unsharing not complete for this node.");
                        }
                      }
                    }
                  } // drive not found, may be this file in Trash folder and
                    // user is cleaning it, do nothing
                } catch (DriveRemovedException e) {
                  LOG.warn("Cloud File unsharing canceled due to removed drive: " + e.getMessage() + ". Path: " + eventPath);
                } catch (NotCloudDriveException e) {
                  LOG.warn("Cloud File unsharing not possible for not cloud drives: " + e.getMessage() + ". Path: " + eventPath);
                } catch (Throwable e) {
                  LOG.error("Cloud File unsharing error: " + e.getMessage() + ". Path: " + eventPath, e);
                }
              }
            }
          }
        } catch (RepositoryException e) {
          LOG.error("Symlink removal listener error: " + e.getMessage(), e);
        }
      }
    }

    protected void removeCloudFilePermission(final Node fileNode,
                                             final CloudDrive cloudDrive,
                                             final String sharedIndentity) throws NotCloudDriveException,
                                                                           DriveRemovedException,
                                                                           RepositoryException,
                                                                           CloudDriveException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Unsharing Cloud File " + fileNode.getPath() + " of " + cloudDrive + " from " + " " + sharedIndentity);
      }
      // avoid firing Cloud Drive synchronization
      CloudDriveStorage cdStorage = (CloudDriveStorage) cloudDrive;
      cdStorage.localChange(new Change<Void>() {
        @Override
        public Void apply() throws RepositoryException {
          removePermission(fileNode, sharedIndentity);
          return null;
        }
      });
    }
  }

  /**
   * Act on Cloud File symlink trashing.
   */
  protected class LinkTrashListener implements EventListener {

    /** The processing links. */
    private final Queue<String> processingLinks = new ConcurrentLinkedQueue<String>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(EventIterator events) {
      while (events.hasNext()) {
        Event event = events.nextEvent();
        try {
          final String eventPath = event.getPath();
          Item linkItem = systemSession().getItem(eventPath);
          if (linkItem.isNode() && linkManager.isLink(linkItem)) {
            try {
              Node fileNode = linkManager.getTarget((Node) linkItem, true);
              CloudDrive localDrive = cloudDrives.findDrive(fileNode);
              if (localDrive != null) {
                if (localDrive.isConnected()) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud File link trashed. User: " + event.getUserID() + ". Path: " + eventPath);
                  }
                  // update removals with trashed path
                  final String cloudFileUUID = fileNode.getUUID();
                  if (!processingLinks.contains(cloudFileUUID)) {
                    processingLinks.add(cloudFileUUID);
                    removedLinks.values().remove(cloudFileUUID);
                    removedLinks.put(eventPath, cloudFileUUID);
                    // remove symlink with a delay in another thread
                    workerExecutor.submit(new Runnable() {
                      @Override
                      public void run() {
                        try {
                          Thread.sleep(1000 * 2); // wait a bit for ECMS actions

                          Item linkItem = systemSession().getItem(eventPath);
                          if (linkItem.isNode()) {
                            Node linkNode = (Node) linkItem;
                            Node parent = linkNode.getParent();
                            // FYI target node permissions will be updated by
                            // LinkRemoveListener
                            linkNode.remove();
                            parent.save();
                            if (LOG.isDebugEnabled()) {
                              LOG.debug("Cloud File link '" + linkItem.getName() + "' successfully removed from the Trash.");
                            }
                          }
                        } catch (PathNotFoundException e) {
                          // node already deleted
                          LOG.warn("Cloud File " + eventPath + " node already removed directly from JCR: " + e.getMessage());
                        } catch (InterruptedException e) {
                          LOG.warn("Cloud File symlink remover interrupted " + e.getMessage());
                          Thread.currentThread().interrupt();
                        } catch (Throwable e) {
                          LOG.error("Error removing node of Cloud File " + eventPath + ". " + e.getMessage(), e);
                        } finally {
                          processingLinks.remove(cloudFileUUID);
                        }
                      }
                    });
                  } // else, link already processing
                } else {
                  LOG.warn("Cloud Drive not connected for " + fileNode.getPath() + ". Drive: " + localDrive);
                }
              } else {
                LOG.warn("Cloud Drive not found for " + fileNode.getPath());
              }
            } catch (Exception e) {
              LOG.error("Symlink " + eventPath + " removal error: " + e.getMessage(), e);
            }
          }
        } catch (PathNotFoundException e) {
          if (LOG.isDebugEnabled()) {
            try {
              LOG.debug("Trashed item not found " + event.getPath() + ". " + e.getMessage(), e);
            } catch (RepositoryException ee) {
              // ignore
            }
          }
        } catch (RepositoryException e) {
          LOG.error("Symlink listener error: " + e.getMessage(), e);
        }
      }
    }
  }

  /** The cloud drive. */
  protected final CloudDriveService      cloudDrives;

  /** The jcr service. */
  protected final RepositoryService      jcrService;

  /** The hierarchy creator. */
  protected final NodeHierarchyCreator   hierarchyCreator;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /** The org service. */
  protected final OrganizationService    orgService;

  /** The link manager. */
  protected final LinkManager            linkManager;

  /** The document drives. */
  protected final ManageDriveService     documentDrives;

  /** The trash. */
  protected final TrashService           trash;

  /** The listener service. */
  protected final ListenerService        listenerService;

  /** The cms service. */
  protected final CmsService             cmsService;

  /**
   * Symlink to cloud file UUID mappings.
   */
  protected final Map<String, String>    removedLinks   = new ConcurrentHashMap<String, String>();

  /**
   * Cloud file UUID to shared group/user name mappings.
   */
  protected final Map<String, String>    removedShared  = new ConcurrentHashMap<String, String>();

  /**
   * Node finder facade on actual storage implementation.
   */
  protected final NodeFinder             finder;

  /**
   * Trashed symlinks removal workers.
   */
  protected final ThreadExecutor         workerExecutor = ThreadExecutor.getInstance();

  /** The groups path. */
  protected final String                 groupsPath;

  /** The users path. */
  protected final String                 usersPath;

  /**
   * Instantiates a new cloud file action service.
   *
   * @param cloudDrives the cloud drive
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param orgService the org service
   * @param finder the finder
   * @param hierarchyCreator the hierarchy creator
   * @param linkManager the link manager
   * @param documentDrives the document drives
   * @param trash the trash
   * @param listeners the listeners
   * @param cmsService the cms service
   */
  public CloudFileActionService(CloudDriveService cloudDrives,
                                RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                OrganizationService orgService,
                                NodeFinder finder,
                                NodeHierarchyCreator hierarchyCreator,
                                LinkManager linkManager,
                                ManageDriveService documentDrives,
                                TrashService trash,
                                ListenerService listeners,
                                CmsService cmsService) {
    this.cloudDrives = cloudDrives;
    this.jcrService = jcrService;
    this.orgService = orgService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.hierarchyCreator = hierarchyCreator;
    this.linkManager = linkManager;
    this.documentDrives = documentDrives;
    this.trash = trash;
    this.listenerService = listeners;
    this.cmsService = cmsService;

    this.groupsPath = hierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    this.usersPath = hierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
  }

  /**
   * Checks if is group drive.
   *
   * @param drive the drive
   * @return true, if is group drive
   */
  public boolean isGroupDrive(DriveData drive) {
    return drive.getHomePath().startsWith(groupsPath);
  }

  /**
   * Checks if is user drive.
   *
   * @param drive the drive
   * @return true, if is user drive
   */
  public boolean isUserDrive(DriveData drive) {
    return drive.getHomePath().startsWith(usersPath);
  }

  /**
   * Checks if is group path.
   *
   * @param path the path
   * @return true, if is group path
   */
  public boolean isGroupPath(String path) {
    return path.startsWith(groupsPath);
  }

  /**
   * Checks if is user path.
   *
   * @param path the path
   * @return true, if is user path
   */
  public boolean isUserPath(String path) {
    return path.startsWith(usersPath);
  }

  /**
   * Link share to user.
   *
   * @param fileNode the file node
   * @param fileDrive the file drive
   * @param userName the user name
   * @return the node
   * @throws Exception the exception
   */
  @Deprecated
  public Node linkShareToUser(Node fileNode, CloudDrive fileDrive, String userName) throws Exception {
    Node userDocs = getUserPublicNode(userName);

    shareCloudFile(fileNode, fileDrive, userName);
    Node link;
    NodeIterator links = getCloudFileLinks(fileNode, userName, true);
    if (links.getSize() == 0) {
      link = linkFile((Node) systemSession().getItem(fileNode.getPath()), userDocs, userName);
      // set all permissions on the link to the target user
      setAllPermissions(link, userName);
      userDocs.save();
    } else {
      link = links.nextNode();
    }
    return link;
  }

  /**
   * Gets the group drive.
   *
   * @param groupId the group id
   * @return the space drive
   * @throws Exception the exception
   */
  public DriveData getGroupDrive(String groupId) throws Exception {
    return documentDrives.getDriveByName(groupId.replace('/', '.'));
  }

  /**
   * Gets the user drive.
   *
   * @param userName the user name
   * @return the user drive
   * @throws Exception the exception
   */
  public DriveData getUserDrive(String userName) throws Exception {
    DriveData userDrive = null;
    String homePath = null;
    for (Iterator<DriveData> diter = documentDrives.getPersonalDrives(userName).iterator(); diter.hasNext();) {
      DriveData drive = diter.next();
      String path = drive.getHomePath();
      if (path.startsWith(usersPath) && path.endsWith("/Private")) {
        homePath = path;
        userDrive = drive;
        // we prefer drives with already defined real user path as home
        if (homePath.indexOf("${userId}") < 0) {
          break;
        } else if (!diter.hasNext()) {
          // if no real home path found and no more drives, do this job here
          homePath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, userName);
          userDrive.setHomePath(homePath);
          break;
        }
      }
    }
    return userDrive;
  }

  /**
   * Gets the user public node.
   *
   * @param userName the user name
   * @return the user public node
   * @throws Exception the exception
   */
  public Node getUserPublicNode(String userName) throws Exception {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      String userPublic = hierarchyCreator.getJcrPath("userPublic");
      return hierarchyCreator.getUserNode(ssp, userName).getNode(userPublic != null ? userPublic : "Public");
    }
    throw new RepositoryException("Cannot get session provider.");
  }

  /**
   * Share cloud file to an user by its ID.
   *
   * @param fileNode the file node
   * @param userId the user id
   * @throws RepositoryException the repository exception
   */
  public void shareToUser(Node fileNode, String userId) throws RepositoryException {
    setUserFilePermission(fileNode, userId, true);
    fileNode.save();
    // initialize files links in the user docs
    try {
      DriveData userDrive = getUserDrive(userId);
      if (userDrive != null) {
        initCloudFileLink(fileNode, userId, userDrive.getHomePath());
      }
    } catch (Exception e) {
      LOG.error("Error reading Cloud File links of " + fileNode.getPath(), e);
    }
  }

  public void unshareToUser(Node fileNode, String userId) throws RepositoryException {
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    removeLinks(fileNode, userId);
    // remove sharing permissions
    removePermission(fileNode, userId);
  }

  /**
   * Share cloud file to a group by its ID.
   *
   * @param fileNode the file node
   * @param groupId the group id
   * @throws RepositoryException the repository exception
   */
  public void shareToGroup(Node fileNode, String groupId) throws RepositoryException {
    // set permission on the cloud file
    setGroupFilePermission(fileNode, groupId, true);
    fileNode.save();
    // initialize files links in space docs
    try {
      DriveData documentDrive = getGroupDrive(groupId);
      if (documentDrive != null) {
        initCloudFileLink(fileNode, groupId, documentDrive.getHomePath());
      }
    } catch (Exception e) {
      LOG.error("Error reading Cloud File links of " + fileNode.getPath(), e);
    }
  }

  /**
   * Unshare to space.
   *
   * @param fileNode the file node
   * @param groupId the group id
   * @throws RepositoryException the repository exception
   */
  public void unshareToSpace(Node fileNode, String groupId) throws RepositoryException {
    String identity = new StringBuilder("*:").append(groupId).toString();
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    removeLinks(fileNode, identity);
    // remove sharing permissions
    removePermission(fileNode, identity);
  }

  /**
   * Gets the node drive.
   *
   * @param node the node
   * @return the node drive
   * @throws Exception the exception
   */
  @Deprecated // not complete logic
  public DriveData getNodeDrive(Node node) throws Exception {
    String groupId = getDriveNameFromPath(node.getPath());
    // TODO in case of user drive its home path may be not filled with actual
    // value (contains $userId
    // instead)
    return groupId != null ? documentDrives.getDriveByName(groupId) : null;
  }

  /**
   * Gets the drive name from path.
   *
   * @param nodePath the node path
   * @return the drive name from path
   * @throws CloudFileActionException the cloud file action exception
   */
  @Deprecated // TODO not complete logic
  public String getDriveNameFromPath(String nodePath) throws CloudFileActionException {
    List<DriveData> allDrives;
    try {
      allDrives = documentDrives.getAllDrives();
    } catch (Exception e) {
      throw new CloudFileActionException("Error reading document drives: "
          + e.getMessage(), new ApplicationMessage("CloudFile.msg.ErrorReadingDrives", null, ApplicationMessage.ERROR));
    }
    for (DriveData drive : allDrives) {
      String drivePath = drive.getHomePath();
      if (nodePath.startsWith(drivePath)) {
        // StringBuilder groupName = new StringBuilder();
        if (drivePath.startsWith(groupsPath)) {
          // XXX Names hardcoded as they already did in other places in eXo
          // group drives have path /Groups${groupId}/Documents
          // String[] gpp = drivePath.substring(drivePath.indexOf(groupsPath),
          // groupsPath.length()).split("/");
        } else if (drivePath.startsWith(usersPath)) {
          // personal drives have path /Users/${userId}/Private
          // String[] upp = drivePath.substring(drivePath.indexOf(usersPath),
          // usersPath.length()).split("/");
        }
        return drive.getName();
      }
    }
    return null;
  }

  /**
   * Link file.
   *
   * @param srcNode the src node
   * @param destNode the dest node
   * @param destIdentity the dest identity
   * @return the node
   * @throws NotCloudDriveException the not cloud drive exception
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  @Deprecated
  public Node linkFile(Node srcNode, Node destNode, String destIdentity) throws NotCloudDriveException,
                                                                         DriveRemovedException,
                                                                         RepositoryException,
                                                                         CloudDriveException {
    String linkName = srcNode.getName();
    String linkTitle = documentName(srcNode);
    Node linkNode = linkManager.createLink(destNode, null, srcNode, linkName, linkTitle);
    if (linkNode.canAddMixin(ECD_CLOUDFILELINK)) {
      linkNode.addMixin(ECD_CLOUDFILELINK);
      if (destIdentity != null) {
        linkNode.setProperty(ECD_SHAREIDENTITY, destIdentity);
      }
    } else {
      LOG.warn("Cannot add mixin " + ECD_CLOUDFILELINK + " to symlink " + linkNode.getPath());
    }
    return linkNode;
  }

  /**
   * Mark remove link.
   *
   * @param linkNode the link node
   * @return the node
   * @throws NotCloudDriveException the not cloud drive exception
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  public Node markRemoveLink(final Node linkNode) throws NotCloudDriveException,
                                                  DriveRemovedException,
                                                  RepositoryException,
                                                  CloudDriveException {

    if (linkNode.isNodeType(ECD_CLOUDFILELINK)) {
      try {
        Node target = linkManager.getTarget(linkNode, true);
        String cloudFileUUID = target.getUUID();
        removedLinks.put(linkNode.getPath(), cloudFileUUID);

        try {
          removedShared.put(cloudFileUUID, linkNode.getProperty(ECD_SHAREIDENTITY).getString());
        } catch (PathNotFoundException e) {
          // wasn't shared in documents drive
        }

        return target;
      } catch (ItemNotFoundException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Cloud File link has no target node: " + linkNode.getPath() + ". " + e.getMessage());
        }
      }
    } else {
      LOG.warn("Not cloud file link: node " + linkNode.getPath() + " not of type " + ECD_CLOUDFILELINK);
    }
    return null;
  }

  /**
   * Share cloud file.
   *
   * @param fileNode the file node
   * @param cloudDrive the cloud drive
   * @param identities the identities
   * @throws NotCloudDriveException the not cloud drive exception
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  @Deprecated
  public void shareCloudFile(final Node fileNode,
                             final CloudDrive cloudDrive,
                             final String... identities) throws NotCloudDriveException,
                                                         DriveRemovedException,
                                                         RepositoryException,
                                                         CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sharing Cloud File " + fileNode.getPath() + " of " + cloudDrive + " to " + " " + Arrays.toString(identities));
    }

    // set local file node permissions in JCR
    // avoid firing Cloud Drive synchronization
    CloudDriveStorage srcStorage = (CloudDriveStorage) cloudDrive;
    srcStorage.localChange(new Change<Void>() {
      @Override
      public Void apply() throws RepositoryException {
        Node parent = fileNode.getParent();
        // we keep access to all sub-files of the src parent restricted
        setParentPermissions(parent, identities);
        setPermissions(fileNode, identities);
        parent.save(); // save everything here!
        return null;
      }
    });
    // share file in cloud provider (if applicable)
    CloudDriveSecurity srcSecurity = (CloudDriveSecurity) cloudDrive;
    if (srcSecurity.isSharingSupported()) {
      srcSecurity.shareFile(fileNode, identities);
    }
  }

  /**
   * Unshare cloud file.
   *
   * @param fileNode the file node
   * @param cloudDrive the cloud drive
   * @param identities the identities
   * @throws NotCloudDriveException the not cloud drive exception
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  @Deprecated
  public void unshareCloudFile(final Node fileNode,
                               final CloudDrive cloudDrive,
                               final String... identities) throws NotCloudDriveException,
                                                           DriveRemovedException,
                                                           RepositoryException,
                                                           CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Unsharing Cloud File " + fileNode.getPath() + " of " + cloudDrive + " from " + " "
          + Arrays.toString(identities));
    }
    // avoid firing Cloud Drive synchronization
    CloudDriveStorage srcStorage = (CloudDriveStorage) cloudDrive;
    srcStorage.localChange(new Change<Void>() {
      @Override
      public Void apply() throws RepositoryException {
        Node parent = fileNode.getParent();
        // we remove access for this drive to all sub-files of the src parent
        removePermissions(fileNode, true, identities);
        removePermissions(parent, false, identities);
        parent.save(); // save everything here!
        return null;
      }
    });
  }

  /**
   * Post shared activity.
   *
   * @param node the node
   * @param link the link
   * @param comment the comment
   * @return the string
   * @throws CloudDriveException the cloud drive exception
   */
  @Deprecated
  public String postSharedActivity(Node node, Node link, String comment) throws CloudDriveException {
    try {
      Utils.setActivityType(SHARE_CLOUD_FILES_SPACES);
      ExoSocialActivity activity = Utils.postFileActivity(link, "", true, false, comment, "");
      if (activity != null) {
        return activity.getId();
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new CloudDriveException("Error posting stared activity: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      listenFileLinks();
    } catch (RepositoryException e) {
      LOG.error("Error starting symlinks remove listener", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // nothing
  }

  // ******************* internals *******************

  /**
   * Listen file links.
   *
   * @throws RepositoryException the repository exception
   */
  protected void listenFileLinks() throws RepositoryException {
    ObservationManager observation = systemSession().getWorkspace().getObservationManager();
    observation.addEventListener(new LinkTrashListener(),
                                 Event.NODE_ADDED,
                                 null,
                                 false,
                                 null,
                                 new String[] { EXO_TRASHFOLDER },
                                 false);
    observation.addEventListener(new LinkRemoveListener(),
                                 Event.PROPERTY_REMOVED,
                                 null,
                                 false,
                                 null,
                                 new String[] { ECD_CLOUDFILELINK },
                                 false);
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space
   * group members). If node not yet <code>exo:privilegeable</code> it will add
   * such mixin to allow set the permissions first. Requested permissions will
   * be set to all children nodes if the child already
   * <code>exo:privilegeable</code>.<br>
   *
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or
   *          memberships)
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected void setPermissions(Node node, String... identities) throws AccessControlException, RepositoryException {
    setPermissions(node, true, true, identities);
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space
   * group members). Permissions will not be set if target not
   * <code>exo:privilegeable</code> and <code>forcePrivilegeable</code> is
   * <code>false</code>. If <code>deep</code> is <code>true</code> the target
   * children nodes will be checked also for a need to set the requested
   * permissions. <br>
   *
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also children nodes
   *          will be set to the requested permissions
   * @param forcePrivilegeable {@link Boolean} if <code>true</code> and node not
   *          yet <code>exo:privilegeable</code> it will add such mixin to allow
   *          set the permissions.
   * @param identities array of {@link String} with user identifiers (names or
   *          memberships)
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected void setPermissions(Node node, boolean deep, boolean forcePrivilegeable, String... identities)
                                                                                                           throws AccessControlException,
                                                                                                           RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    boolean setPermissions = true;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        target.addMixin(EXO_PRIVILEGEABLE);
      } else {
        // will not set permissions on this node, but will check the child nodes
        setPermissions = false;
      }
    } // else, already exo:privilegeable
    if (setPermissions) {
      for (String identity : identities) {
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we want allow given identity read only and
          // additionally let managers remove the link
          String managerMembership;
          try {
            MembershipType managerType = orgService.getMembershipTypeHandler().findMembershipType("manager");
            managerMembership = managerType.getName();
          } catch (Exception e) {
            LOG.error("Error finding manager membership in organization service. "
                + "Will use any (*) to allow remove shared cloud file link", e);
            managerMembership = "*";
          }
          target.setPermission(new StringBuilder(managerMembership).append(':').append(ids[1]).toString(), MANAGER_PERMISSION);
          target.setPermission(identity, READER_PERMISSION);
        } else {
          // in other cases, we assume it's user identity and user should be
          // able to remove the link
          target.setPermission(identity, MANAGER_PERMISSION);
        }
      }
    }
    if (deep) {
      // check the all children also, but don't force adding exo:privilegeable
      for (NodeIterator niter = target.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        setPermissions(child, true, false, identities);
      }
    }
  }

  /**
   * Remove read permissions on the target node for all given identities (e.g.
   * space group members). If <code>deep</code> is <code>true</code> then
   * permissions will be removed on all ancestor nodes (sub-tree for folders).
   *
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also remove the
   *          permissions from children nodes
   * @param identities array of {@link String} with user identifiers (names or
   *          memberships)
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected void removePermissions(Node node, boolean deep, String... identities) throws AccessControlException,
                                                                                  RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      for (String identity : identities) {
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we should remove read link permissions for given
          // identity and additionally remove
          // link for managers (see setPermissions())
          String managerMembership;
          try {
            MembershipType managerType = orgService.getMembershipTypeHandler().findMembershipType("manager");
            managerMembership = managerType.getName();
          } catch (Exception e) {
            LOG.error("Error finding manager membership in organization service. "
                + "Will use any (*) to remove permissions of shared cloud file link", e);
            managerMembership = "*";
          }
          String managerId = new StringBuilder(managerMembership).append(':').append(ids[1]).toString();
          target.removePermission(managerId, PermissionType.READ);
          target.removePermission(managerId, PermissionType.REMOVE);
          target.removePermission(identity, PermissionType.READ);
        } else {
          // in other cases, we assume it's user identity and remove both read
          // and remove link permissions
          target.removePermission(identity, PermissionType.READ);
          target.removePermission(identity, PermissionType.REMOVE);
        }
      }
    }
    if (deep) {
      for (NodeIterator niter = target.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        removePermissions(child, true, identities);
      }
    }
  }

  /**
   * Set permissions on a parent node of a link target: all child nodes will
   * become exo:privilegeable (thus copy permissions from its priviligeable
   * parent). If a child already of priviligeable type - nothing will be
   * performed, by this we assume its permissions already handled as required.
   * After this read permissions will be added to the parent node.<br>
   * This method SHOULD be used before setting permissions to a link target node
   * in this parent. In this way we will keep permission of the target in
   * consistent state.
   *
   * @param parent the parent
   * @param identities the identities
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected void setParentPermissions(Node parent, String... identities) throws AccessControlException, RepositoryException {
    // first we go through all sub-files/folders and enabled exo:privilegeable,
    // this will copy current
    // parent permissions to the child nodes for those that aren't privilegeable
    // already.
    for (NodeIterator citer = parent.getNodes(); citer.hasNext();) {
      ExtendedNode child = (ExtendedNode) citer.nextNode();
      if (child.canAddMixin(EXO_PRIVILEGEABLE)) {
        child.addMixin(EXO_PRIVILEGEABLE);
      } // else, this child already has permissions, we assume they are OK and
        // do nothing
    }

    // then we set read permissions to the parent only
    setPermissions(parent, false, true, identities);
  }

  /**
   * Set all available permissions to given node for given identities.
   *
   * @param node the node
   * @param identities the identities
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected void setAllPermissions(Node node, String... identities) throws AccessControlException, RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      target.addMixin(EXO_PRIVILEGEABLE);
    }
    for (String identity : identities) {
      target.setPermission(identity, PermissionType.ALL);
    }
  }

  /**
   * Find pretty name of the document.
   *
   * @param document the document
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String documentName(Node document) throws RepositoryException {
    try {
      return document.getProperty("exo:title").getString();
    } catch (PathNotFoundException te) {
      try {
        return document.getProperty("exo:name").getString();
      } catch (PathNotFoundException ne) {
        return document.getName();
      }
    }
  }

  /**
   * System session in default workspace of current JCR repository.
   *
   * @return the session
   * @throws RepositoryException the repository exception
   */
  protected Session systemSession() throws RepositoryException {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      ManageableRepository jcrRepository = jcrService.getCurrentRepository();
      String workspaceName = jcrRepository.getConfiguration().getDefaultWorkspaceName();
      return ssp.getSession(workspaceName, jcrRepository);
    }
    throw new RepositoryException("Cannot get session provider.");
  }

  /**
   * Gets the cloud file links.
   *
   * @param targetNode the target node
   * @param shareIdentity the share identity
   * @param useSystemSession the use system session
   * @return the cloud file links
   * @throws RepositoryException the repository exception
   */
  protected NodeIterator getCloudFileLinks(Node targetNode,
                                           String shareIdentity,
                                           boolean useSystemSession) throws RepositoryException {
    StringBuilder queryCode = new StringBuilder().append("SELECT * FROM ")
                                                 .append(ECD_CLOUDFILELINK)
                                                 .append(" WHERE exo:uuid='")
                                                 .append(targetNode.getUUID())
                                                 .append("'");
    if (shareIdentity != null && shareIdentity.length() > 0) {
      queryCode.append(" AND " + ECD_SHAREIDENTITY + "='").append(shareIdentity).append("'");
    }

    QueryManager queryManager;
    if (useSystemSession) {
      queryManager = systemSession().getWorkspace().getQueryManager();
    } else {
      queryManager = targetNode.getSession().getWorkspace().getQueryManager();
    }

    Query query = queryManager.createQuery(queryCode.toString(), Query.SQL);
    QueryResult queryResult = query.execute();
    return queryResult.getNodes();
  }

  /**
   * Removes the links.
   *
   * @param fileNode the file node
   * @param shareIdentity the share identity
   * @throws RepositoryException the repository exception
   */
  protected void removeLinks(Node fileNode, String shareIdentity) throws RepositoryException {
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    for (NodeIterator niter = getCloudFileLinks(fileNode, shareIdentity, true); niter.hasNext();) {
      Node linkNode = niter.nextNode();
      Node parent = linkNode.getParent();
      linkNode.remove();
      parent.save();
    }
  }

  /**
   * Gets organizational membership name by its type name.
   *
   * @param membershipType the membership type
   * @return the membership name
   */
  protected String getMembershipName(String membershipType) {
    try {
      return orgService.getMembershipTypeHandler().findMembershipType(membershipType).getName();
    } catch (Exception e) {
      LOG.error("Error finding manager membership in organization service. "
          + "Will use any (*) to remove permissions of shared cloud file link", e);
      return "*";
    }
  }

  /**
   * Removes the permission of an identity on given node.
   *
   * @param node the node, expected {@link ExtendedNode} here
   * @param identity the identity
   */
  protected void removePermission(Node node, String identity) {
    try {
      ExtendedNode target = (ExtendedNode) node;
      target.removePermission(identity);
      target.save();
    } catch (Exception e) {
      LOG.warn("Failed to remove permissions on " + node + " for " + identity, e);
    }
  }

  /**
   * Adds the user permission.
   *
   * @param fileNode the file node
   * @param userId the user id
   * @param forcePrivilegeable if need force adding of exo:privilegeable mixin
   *          to the node
   * @throws RepositoryException the repository exception
   */
  protected void setUserFilePermission(Node fileNode, String userId, boolean forcePrivilegeable) throws RepositoryException {
    ExtendedNode target = (ExtendedNode) fileNode;

    boolean setPermission = false;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        target.addMixin(EXO_PRIVILEGEABLE);
        setPermission = true;
      } // else will not set permissions on this node, but will check the child
        // nodes
    } else if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      // clean what ShareDocumentService could add
      removeWritePermissions(target, userId);
      target.setPermission(userId, READER_PERMISSION);
    }

    // For folders we do recursive (in 1.6.0 this is not actual as UI will not
    // let to share a folder to other user)
    if (fileNode.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      // check the all children but don't force adding exo:privilegeable
      for (NodeIterator niter = target.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        setGroupFilePermission(child, userId, false);
      }
    }
    // Don't save the all here, do this once in the caller
    // target.save();
  }

  /**
   * Adds the group permission for cloud file.
   *
   * @param fileNode the node
   * @param groupId the group id
   * @param forcePrivilegeable if need force adding of exo:privilegeable mixin
   *          to the node
   * @throws RepositoryException the repository exception
   */
  protected void setGroupFilePermission(Node fileNode, String groupId, boolean forcePrivilegeable) throws RepositoryException {
    ExtendedNode target = (ExtendedNode) fileNode;

    boolean setPermission = false;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        target.addMixin(EXO_PRIVILEGEABLE);
        setPermission = true;
      } // else will not set permissions on this node, but will check the child
        // nodes
    } else if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      // clean what ShareDocumentService could add
      removeWritePermissions(target, groupId);
      target.setPermission(new StringBuilder("*:").append(groupId).toString(), READER_PERMISSION);
    }

    // For folders we do recursive
    if (fileNode.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      // check the all children but don't force adding exo:privilegeable
      for (NodeIterator niter = target.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        setGroupFilePermission(child, groupId, false);
      }
    }

    // Don't save the all here, do this once in the caller
    // target.save();
  }

  /**
   * Adds the permission for a cloud file link: user who shared and to whom or
   * group manager have full permissions, others can read only.
   *
   * @param linkNode the node
   * @param ownerId the owner id
   * @param sharedIdentity the group id
   * @param forcePrivilegeable the force adding of exo:privilegeable mixin to
   *          the node
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  protected void setLinkPermission(Node linkNode, String ownerId, String sharedIdentity) throws AccessControlException,
                                                                                         RepositoryException {
    ExtendedNode target = (ExtendedNode) linkNode;
    boolean setPermission = false;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      target.addMixin(EXO_PRIVILEGEABLE);
      setPermission = true;
    } else if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      if (sharedIdentity.startsWith("/")) {
        // space link
        // Remove write to all
        removeWritePermissions(target, sharedIdentity);
        // Allow any member read only
        target.setPermission(new StringBuilder("*:").append(sharedIdentity).toString(), READER_PERMISSION);
        // Owner like manager
        target.setPermission(ownerId, MANAGER_PERMISSION);
        // Space manager
        String managerMembership = getMembershipName("manager");
        target.setPermission(new StringBuilder(managerMembership).append(':').append(sharedIdentity).toString(),
                             MANAGER_PERMISSION);
      } else {
        // user link
        // Owner like manager
        target.setPermission(ownerId, MANAGER_PERMISSION);
        // Target user also has full rights
        target.setPermission(sharedIdentity, MANAGER_PERMISSION);
      }
    }
    // Don't save the all here, do this once in the caller
    // target.save();
  }

  /**
   * Adds the cloud file link node (mixin node type and permissions).
   *
   * @param fileNode the file node
   * @param sharedIdentity the identity this link was shared to
   * @param targetPath the target path
   */
  protected void initCloudFileLink(Node fileNode, String sharedIdentity, String targetPath) {
    try {
      // FYI link(s) should be created by CloudDriveShareDocumentService
      List<Node> links = linkManager.getAllLinks(fileNode, ManageDocumentService.EXO_SYMLINK);
      for (Node linkNode : links) {
        // do only for a target (space or user)
        if (linkNode.getPath().startsWith(targetPath)) {
          if (linkNode.canAddMixin(CloudFileActionService.ECD_CLOUDFILELINK)) {
            linkNode.addMixin(CloudFileActionService.ECD_CLOUDFILELINK);
            if (sharedIdentity != null) {
              linkNode.setProperty(CloudFileActionService.ECD_SHAREIDENTITY, sharedIdentity);
            }
            setLinkPermission(linkNode, ((ExtendedNode) fileNode).getACL().getOwner(), sharedIdentity);
            linkNode.save();
          } else {
            // TODO it seems a normal case, should we update ECD_SHAREIDENTITY
            // on existing link?
            LOG.warn("Cannot add mixin " + CloudFileActionService.ECD_CLOUDFILELINK + " to symlink " + linkNode.getPath());
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error initializing cloud file link: " + fileNode, e);
    }
  }

  /**
   * Remove write permissions for all kinds of membership for this group or
   * user.
   *
   * @param target the target
   * @param groupOrUserId the group or user id
   * @throws RepositoryException the repository exception
   */
  protected void removeWritePermissions(ExtendedNode target, String groupOrUserId) throws RepositoryException {
    for (AccessControlEntry acle : target.getACL().getPermissionEntries()) {
      if (acle.getIdentity().equals(groupOrUserId)
          || (acle.getMembershipEntry() != null && acle.getMembershipEntry().getGroup().equals(groupOrUserId))) {
        if (PermissionType.ADD_NODE.equals(acle.getPermission()) || PermissionType.REMOVE.equals(acle.getPermission())
            || PermissionType.SET_PROPERTY.equals(acle.getPermission())) {
          target.removePermission(acle.getIdentity(), acle.getPermission());
        }
      }
    }
  }

}
