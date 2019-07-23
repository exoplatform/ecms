
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
package org.exoplatform.clouddrive.ecms.action;

import java.security.AccessControlException;
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
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ThreadExecutor;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
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
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

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
                    final ExoContainer container = ExoContainerContext.getCurrentContainer();
                    workerExecutor.submit(new Runnable() {
                      @Override
                      public void run() {
                        try {
                          Thread.sleep(1000 * 2); // wait a bit for ECMS actions
                          ExoContainerContext.setCurrentContainer(container);
                          RequestLifeCycle.begin(container);
                          try {
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
                          } finally {
                            RequestLifeCycle.end();
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
    Node profileNode = getUserProfileNode(userName);
    String userPublic = hierarchyCreator.getJcrPath("userPublic");
    return profileNode.getNode(userPublic != null ? userPublic : "Public");
  }

  /**
   * Gets the user profile node (a node where /Private and /Public nodes live).
   *
   * @param userName the user name
   * @return the user profile node
   * @throws Exception the exception
   */
  public Node getUserProfileNode(String userName) throws Exception {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      return hierarchyCreator.getUserNode(ssp, userName);
    }
    throw new RepositoryException("Cannot get session provider.");
  }

  /**
   * Share cloud file to an user by its ID.
   *
   * @param fileNode the file node
   * @param userId the user id
   * @param canEdit the can edit flag, if <code>true</code> then user should be
   *          able to modify the shared link node, read-only link otherwise
   * @throws RepositoryException the repository exception
   */
  public void shareToUser(Node fileNode, String userId, boolean canEdit) throws RepositoryException {
    setUserFilePermission(fileNode, userId, true);
    fileNode.save();
    // initialize files links in the user docs
    try {
      DriveData userDrive = getUserDrive(userId);
      if (userDrive != null) {
        initCloudFileLink(fileNode, userId, userDrive.getHomePath(), canEdit);
      }
    } catch (Exception e) {
      LOG.error("Error reading Cloud File links of " + fileNode.getPath(), e);
    }
  }

  /**
   * Unshare to user.
   *
   * @param fileNode the file node
   * @param userId the user id
   * @throws RepositoryException the repository exception
   */
  public void unshareToUser(Node fileNode, String userId) throws RepositoryException {
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    removeLinks(fileNode, userId);
    // remove sharing permissions
    removePermission(fileNode, userId);
  }

  /**
   * Share cloud file to a group by its ID and create a symlink in the target
   * space.
   *
   * @param fileNode the file node
   * @param groupId the group id
   * @param canEdit the can edit flag, if <code>true</code> then group members
   *          should be able to modify the shared link node, read-only link
   *          otherwise
   * @throws RepositoryException the repository exception
   */
  public void shareToGroup(Node fileNode, String groupId, boolean canEdit) throws RepositoryException {
    // set permission on the cloud file
    setGroupFilePermission(fileNode, groupId, true);
    fileNode.save();
    // initialize files links in space docs
    try {
      DriveData documentDrive = getGroupDrive(groupId);
      if (documentDrive != null) {
        initCloudFileLink(fileNode, groupId, documentDrive.getHomePath(), canEdit);
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
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    removeLinks(fileNode, groupId);
    // remove sharing permissions
    removePermission(fileNode, new StringBuilder("*:").append(groupId).toString());
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
   * Removes the links.
   *
   * @param fileNode the file node
   * @param identity the shared identity
   * @throws RepositoryException the repository exception
   */
  protected void removeLinks(Node fileNode, String identity) throws RepositoryException {
    // remove all copied/linked symlinks from the original shared to given
    // identity (or all if it is null)
    for (NodeIterator niter = getCloudFileLinks(fileNode, identity, true); niter.hasNext();) {
      Node linkNode = niter.nextNode();
      // Care about Social activity removal (avoid showing null activity posts),
      // do it safe for the logic
      try {
        Utils.deleteFileActivity(linkNode);
      } catch (Throwable e) {
        LOG.warn("Failed to remove unpublished file activity: " + linkNode.getPath(), e);
      }
      Node parent = linkNode.getParent();
      // Remove the link itself
      linkNode.remove();
      parent.save();
    }
  }

  /**
   * Gets the cloud file links.
   * 
   * @param targetNode the target node
   * @param shareIdentity the share identity
   * @param scopePath the scope path (only nodes from this sub-tree will be
   *          searched)
   * @param useSystemSession the use system session
   * @return the cloud file links
   * @throws RepositoryException the repository exception
   */
  public NodeIterator getCloudFileLinks(Node targetNode,
                                        String shareIdentity,
                                        String scopePath,
                                        boolean useSystemSession) throws RepositoryException {
    StringBuilder queryCode = new StringBuilder().append("SELECT * FROM ")
                                                 .append(ECD_CLOUDFILELINK)
                                                 .append(" WHERE exo:uuid='")
                                                 .append(targetNode.getUUID())
                                                 .append("'");
    if (shareIdentity != null && shareIdentity.length() > 0) {
      queryCode.append(" AND " + ECD_SHAREIDENTITY + "='").append(shareIdentity).append("'");
    }
    if (scopePath != null && scopePath.length() > 0) {
      queryCode.append(" AND jcr:path LIKE '").append(scopePath).append("/%'");
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
   * @param permIdentity the identity, it should include membership if actual
   *          (e.g. for space it's <code>*:/space/my_team</code>, but simply
   *          <code>john</code> for an user)
   */
  protected void removePermission(Node node, String permIdentity) {
    try {
      ExtendedNode target = (ExtendedNode) node;
      target.removePermission(permIdentity);
      target.save();
    } catch (Exception e) {
      LOG.warn("Failed to remove permissions on " + node + " for " + permIdentity, e);
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
    ExtendedNode file = (ExtendedNode) fileNode;

    boolean setPermission = false;
    if (file.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        file.addMixin(EXO_PRIVILEGEABLE);
        setPermission = true;
      } // else will not set permissions on this node, but will check the child
        // nodes
    } else if (file.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      // XXX we DON'T clean what ShareDocumentService could add to let
      // UIShareDocuments show permissions correctly,
      // we only ensure that required rights are OK
      if (!file.getACL().getPermissions(userId).contains(PermissionType.READ)) {
        file.getACL().addPermissions(userId, READER_PERMISSION);
      }
    }

    // For folders we do recursive (in 1.6.0 this is not actual as UI will not
    // let to share a folder to other user)
    if (fileNode.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      // check the all children but don't force adding exo:privilegeable
      for (NodeIterator niter = file.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        setUserFilePermission(child, userId, false);
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
    ExtendedNode file = (ExtendedNode) fileNode;

    boolean setPermission = false;
    if (file.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        file.addMixin(EXO_PRIVILEGEABLE);
        setPermission = true;
      } // else will not set permissions on this node, but will check the child
        // nodes
    } else if (file.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      // XXX we DON'T clean what ShareDocumentService could add to let
      // UIShareDocuments show permissions correctly,
      // we only ensure that required rights are OK
      String identity = new StringBuilder("*:").append(groupId).toString();
      if (!file.getACL().getPermissions(identity).contains(PermissionType.READ)) {
        file.getACL().addPermissions(identity, READER_PERMISSION);
      }
    }

    // For folders we do recursive
    if (fileNode.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      // check the all children but don't force adding exo:privilegeable
      for (NodeIterator niter = file.getNodes(); niter.hasNext();) {
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
   * @param canEdit the can edit flag
   * @throws AccessControlException the access control exception
   * @throws RepositoryException the repository exception
   */
  protected void setLinkPermission(Node linkNode, String ownerId, String sharedIdentity, boolean canEdit)
                                                                                                          throws AccessControlException,
                                                                                                          RepositoryException {
    ExtendedNode link = (ExtendedNode) linkNode;
    boolean setPermission = false;
    if (link.canAddMixin(EXO_PRIVILEGEABLE)) {
      link.addMixin(EXO_PRIVILEGEABLE);
      setPermission = true;
    } else if (link.isNodeType(EXO_PRIVILEGEABLE)) {
      // already exo:privilegeable
      setPermission = true;
    }
    if (setPermission) {
      if (sharedIdentity.startsWith("/")) {
        // It's space link
        // File owner can do everything in any case
        link.setPermission(ownerId, PermissionType.ALL);
        if (canEdit) {
          // Allow any member to modify the link node
          link.setPermission(new StringBuilder("*:").append(sharedIdentity).toString(), MANAGER_PERMISSION);
        } else {
          // First remove write rights to anybody of the group
          removeWritePermissions(link, sharedIdentity);
          // Allow any member read only (this includes copy)
          link.setPermission(new StringBuilder("*:").append(sharedIdentity).toString(), READER_PERMISSION);
          // But space manager should be able to move/rename/delete and change
          // properties also
          String managerMembership = getMembershipName("manager");
          link.setPermission(new StringBuilder(managerMembership).append(':').append(sharedIdentity).toString(),
                             MANAGER_PERMISSION);
        }
      } else {
        // It's user link
        // File owner can do everything
        link.setPermission(ownerId, PermissionType.ALL);
        // Target user should be able to move/rename/delete and change
        // properties
        link.setPermission(sharedIdentity, MANAGER_PERMISSION);
      }
    }
    // Don't save the all here, do this once in the caller
    // target.save();
  }

  /**
   * Initialize the cloud file link node (mixin node type and permissions).
   *
   * @param fileNode the file node
   * @param identity the identity this link was shared to
   * @param targetPath the target path
   * @param canEdit the can edit flag
   */
  protected void initCloudFileLink(Node fileNode, String identity, String targetPath, boolean canEdit) {
    try {
      // FYI link(s) should be created by CloudDriveShareDocumentService, it
      // creates them under system session, thus we will do the same.
      SessionProvider systemSession = sessionProviders.getSystemSessionProvider(null);
      List<Node> links = linkManager.getAllLinks(fileNode, ManageDocumentService.EXO_SYMLINK, systemSession);
      for (Node linkNode : links) {
        // do only for a target (space or user)
        if (linkNode.getPath().startsWith(targetPath)) {
          if (linkNode.canAddMixin(CloudFileActionService.ECD_CLOUDFILELINK)) {
            linkNode.addMixin(CloudFileActionService.ECD_CLOUDFILELINK);
            if (identity != null) {
              linkNode.setProperty(CloudFileActionService.ECD_SHAREIDENTITY, identity);
            }
            setLinkPermission(linkNode, ((ExtendedNode) fileNode).getACL().getOwner(), identity, canEdit);
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

  /**
   * Gets the cloud file links.
   *
   * @param targetNode the target node
   * @param shareIdentity the share identity
   * @param useSystemSession the use system session
   * @return the cloud file links
   * @throws RepositoryException the repository exception
   */
  public NodeIterator getCloudFileLinks(Node targetNode,
                                        String shareIdentity,
                                        boolean useSystemSession) throws RepositoryException {
    return getCloudFileLinks(targetNode, shareIdentity, null, useSystemSession);
  }

}
