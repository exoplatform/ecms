
/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveSecurity;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ThreadExecutor;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
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
import org.picocontainer.Startable;

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

/**
 * Integration of Cloud Drive with eXo Platform apps.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileActionService.java 00000 Jul 6, 2015 pnedonosko $
 * 
 */
public class CloudFileActionService implements Startable {

  protected static final Log      LOG                      = ExoLogger.getLogger(CloudFileActionService.class);

  protected static final String   SPACES_GROUP             = "spaces";

  protected static final String   SHARE_CLOUD_FILES_SPACES = "sharecloudfiles:spaces";

  protected static final String   EXO_OWNEABLE             = "exo:owneable";

  protected static final String   EXO_PRIVILEGEABLE        = "exo:privilegeable";

  protected static final String   ECD_CLOUDFILELINK        = "ecd:cloudFileLink";

  protected static final String   ECD_SHAREIDENTITY        = "ecd:shareIdentity";

  protected static final String   MIX_VERSIONABLE          = "mix:versionable";

  protected static final String   EXO_TRASHFOLDER          = "exo:trashFolder";

  protected static final String[] READER_PERMISSION          = new String[] { PermissionType.READ };

  protected static final String[] MANAGER_PERMISSION       = new String[] { PermissionType.READ, PermissionType.REMOVE };

  /**
   * Act on ecd:cloudFileLinkGroup property removal on a cloud file symlink and then unshare the file from the
   * group where it was shared by the link.
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
                Session session = systemSession();
                try {
                  Node fileNode = session.getNodeByUUID(cloudFileUUID);
                  CloudDrive localDrive = cloudDrive.findDrive(fileNode);
                  if (localDrive != null) {
                    if (getCloudFileLinks(fileNode, identity, true).getSize() == 0) {
                      // unshare only if no more links found for given identity (user or group)
                      DriveData documentDrive = null;
                      if (identity.startsWith("/")) {
                        // it's group drive
                        documentDrive = documentDrives.getDriveByName(identity.replace('/', '.'));
                        if (documentDrive != null) {
                          unshareCloudFile(fileNode, localDrive, documentDrive.getAllPermissions());
                        }
                      } else {
                        // try as user drive
                        documentDrive = getUserDrive(identity);
                        if (documentDrive != null) {
                          unshareCloudFile(fileNode, localDrive, identity);
                        }
                      }
                      if (documentDrive == null) {
                        if (LOG.isDebugEnabled()) {
                          LOG.debug("Cannot find documents drive for " + fileNode.getPath()
                              + ". Unsharing not complete for this node.");
                        }
                      }
                    }
                  } // drive not found, may be this file in Trash folder and user is cleaning it, do nothing
                } catch (DriveRemovedException e) {
                  LOG.warn("Cloud File unsharing canceled due to removed drive: " + e.getMessage() + ". Path: " + eventPath);
                } catch (NotCloudDriveException e) {
                  LOG.warn("Cloud File unsharing not possible for not cloud drives: " + e.getMessage() + ". Path: "
                      + eventPath);
                } catch (Throwable e) {
                  LOG.error("Cloud File unsharing error: " + e.getMessage() + ". Path: " + eventPath, e);
                } finally {
                  session.logout();
                }
              }
            }
          }
        } catch (RepositoryException e) {
          LOG.error("Symlink removal listener error: " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Act on Cloud File symlink trashing.
   */
  protected class LinkTrashListener implements EventListener {

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
          Session session = systemSession();
          Item linkItem = session.getItem(eventPath);
          if (linkItem.isNode() && linkManager.isLink(linkItem)) {
            try {
              Node fileNode = linkManager.getTarget((Node) linkItem, true);
              CloudDrive localDrive = cloudDrive.findDrive(fileNode);
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

  protected final CloudDriveService      cloudDrive;

  protected final RepositoryService      jcrService;

  protected final NodeHierarchyCreator   hierarchyCreator;

  protected final SessionProviderService sessionProviders;

  protected final OrganizationService    orgService;

  protected final LinkManager            linkManager;

  protected final ManageDriveService     documentDrives;

  protected final TrashService           trash;

  protected final ListenerService        listenerService;

  protected final CmsService             cmsService;

  /**
   * Symlink to cloud file UUID mappings.
   */
  protected final Map<String, String>    removedLinks   = new ConcurrentHashMap<String, String>();

  /**
   * Cloud file UUID to shared group name mappings.
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

  protected final String                 groupsPath;

  protected final String                 usersPath;

  /**
   * 
   */
  public CloudFileActionService(CloudDriveService cloudDrive,
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
    this.cloudDrive = cloudDrive;
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

  public boolean isGroupDrive(DriveData drive) {
    return drive.getHomePath().startsWith(groupsPath);
  }

  public boolean isUserDrive(DriveData drive) {
    return drive.getHomePath().startsWith(usersPath);
  }

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

  public Node getUserPublicNode(String userName) throws Exception {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      String userPublic = hierarchyCreator.getJcrPath("userPublic");
      return hierarchyCreator.getUserNode(ssp, userName).getNode(userPublic != null ? userPublic : "Public");
    }
    throw new RepositoryException("Cannot get session provider.");
  }

  public void removeLinks(Node fileNode, String shareIdentity) throws RepositoryException {
    // remove all copied/linked symlinks from the original shared to given identity (or all if it is null)
    for (NodeIterator niter = getCloudFileLinks(fileNode, shareIdentity, true); niter.hasNext();) {
      Node linkNode = niter.nextNode();
      Node parent = linkNode.getParent();
      linkNode.remove();
      parent.save();
    }
  }

  public DriveData getNodeDrive(Node node) throws Exception {
    String groupId = getDriveNameFromPath(node.getPath());
    // TODO in case of user drive its home path may be not filled with actual value (contains $userId
    // instead)
    return groupId != null ? documentDrives.getDriveByName(groupId) : null;
  }

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

  public void shareCloudFile(final Node fileNode,
                             final CloudDrive cloudDrive,
                             final String... identities) throws NotCloudDriveException,
                                                         DriveRemovedException,
                                                         RepositoryException,
                                                         CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sharing Cloud File " + fileNode.getPath() + " of " + cloudDrive + " to " + " "
          + Arrays.toString(identities));
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

  public String postSharedActivity(Node node, Node link, String comment) throws CloudDriveException {
    try {
      Utils.setActivityType(SHARE_CLOUD_FILES_SPACES);
      ExoSocialActivity activity = Utils.postFileActivity(link, "", false, false, comment);
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

  protected void listenFileLinks() throws RepositoryException {
    Session session = systemSession();
    try {
      ObservationManager observation = session.getWorkspace().getObservationManager();
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
    } finally {
      session.logout();
    }
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space group members). If node not
   * yet <code>exo:privilegeable</code> it will add such mixin to allow set the permissions first. Requested
   * permissions will be set to all children nodes if the child already <code>exo:privilegeable</code>.<br>
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setPermissions(Node node, String... identities) throws AccessControlException, RepositoryException {
    setPermissions(node, true, true, identities);
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space group members). Permissions
   * will not be set if target not <code>exo:privilegeable</code> and <code>forcePrivilegeable</code> is
   * <code>false</code>. If <code>deep</code> is <code>true</code> the target children nodes will be checked
   * also for a need to set the requested permissions. <br>
   * 
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also children nodes will be set to the requested
   *          permissions
   * @param forcePrivilegeable {@link Boolean} if <code>true</code> and node not yet
   *          <code>exo:privilegeable</code> it will add such mixin to allow set the permissions.
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setPermissions(Node node,
                                boolean deep,
                                boolean forcePrivilegeable,
                                String... identities) throws AccessControlException, RepositoryException {
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
        // It is for special debug cases
        // if (LOG.isDebugEnabled()) {
        // LOG.debug(">>> hasPermission " + identity + " identity: "
        // + IdentityHelper.hasPermission(target.getACL(), identity, PermissionType.READ));
        // }
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we want allow given identity read only and additionally let managers remove the
          // link
          String managerMembership;
          try {
            MembershipType managerType = orgService.getMembershipTypeHandler().findMembershipType("manager");
            managerMembership = managerType.getName();
          } catch (Exception e) {
            LOG.error("Error finding manager membership in organization service. "
                + "Will use any (*) to allow remove shared cloud file link", e);
            managerMembership = "*";
          }
          target.setPermission(new StringBuilder(managerMembership).append(':').append(ids[1]).toString(),
                               MANAGER_PERMISSION);
          target.setPermission(identity, READER_PERMISSION);
        } else {
          // in other cases, we assume it's user identity and user should be able to remove the link
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
   * Remove read permissions on the target node for all given identities (e.g. space group members). If
   * <code>deep</code> is <code>true</code> then permissions will be removed on all ancestor nodes (sub-tree
   * for folders).
   * 
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also remove the permissions from children nodes
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void removePermissions(Node node, boolean deep, String... identities) throws AccessControlException,
                                                                                  RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      for (String identity : identities) {
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we should remove read link permissions for given identity and additionally remove
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
          // in other cases, we assume it's user identity and remove both read and remove link permissions
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
   * Set permissions on a parent node of a link target: all child nodes will become exo:privilegeable (thus
   * copy permissions from its priviligeable parent). If a child already of priviligeable type - nothing
   * will
   * be performed, by this we assume its permissions already handled as required. After this read
   * permissions
   * will be added to the parent node.<br>
   * This method SHOULD be used before setting permissions to a link target node in this parent. In this way
   * we will keep permission of the target in consistent state.
   * 
   * @param parent
   * @param identities
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setParentPermissions(Node parent, String... identities) throws AccessControlException, RepositoryException {
    // first we go through all sub-files/folders and enabled exo:privilegeable, this will copy current
    // parent permissions to the child nodes for those that aren't privilegeable already.
    for (NodeIterator citer = parent.getNodes(); citer.hasNext();) {
      ExtendedNode child = (ExtendedNode) citer.nextNode();
      if (child.canAddMixin(EXO_PRIVILEGEABLE)) {
        child.addMixin(EXO_PRIVILEGEABLE);
      } // else, this child already has permissions, we assume they are OK and do nothing
    }

    // then we set read permissions to the parent only
    setPermissions(parent, false, true, identities);
  }

  /**
   * Set all available permissions to given node for given identities.
   * 
   * @param node
   * @param identities
   * @throws AccessControlException
   * @throws RepositoryException
   */
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
   * @param document
   * @return
   * @throws RepositoryException
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
   * @return
   * @throws RepositoryException
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

}
