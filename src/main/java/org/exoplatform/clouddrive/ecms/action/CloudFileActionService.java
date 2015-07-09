
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
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ThreadExecutor;
import org.exoplatform.clouddrive.jcr.NodeFinder;
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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.security.AccessControlException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

  protected static final Log      LOG                = ExoLogger.getLogger(CloudFileActionService.class);

  protected static final String   GROUPS_PATH        = "groupsPath";

  protected static final String   EXO_PRIVILEGEABLE  = "exo:privilegeable";

  protected static final String   ECD_CLOUDFILELINK  = "ecd:cloudFileLink";

  protected static final String   ECD_DOCUMENTSDRIVE = "ecd:documentsDrive";

  protected static final String   MIX_VERSIONABLE    = "mix:versionable";

  protected static final String   EXO_TRASHFOLDER    = "exo:trashFolder";

  protected static final String[] READ_PERMISSION    = new String[] { PermissionType.READ };

  /**
   * Act on ecd:cloudFileLinkGroup property removal on a cloud file symlink and then unshare the file from the
   * group where it was shared by the link.
   */
  protected class LinkGroupListener implements EventListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(EventIterator events) {
      while (events.hasNext()) {
        Event event = events.nextEvent();
        try {
          final String eventPath = event.getPath();
          if (eventPath.endsWith(ECD_DOCUMENTSDRIVE)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Cloud File link removed. User: " + event.getUserID() + ". Path: " + eventPath);
            }

            String linkPath = eventPath.substring(0, eventPath.indexOf(ECD_DOCUMENTSDRIVE) - 1);
            String cloudFileUUID = removedLinks.remove(linkPath);
            if (cloudFileUUID != null) {
              String driveName = removedShared.get(cloudFileUUID);
              if (driveName != null) {
                // was marked by RemoveCloudFileLinkAction
                Session session = systemSession();
                try {
                  Node fileNode = session.getNodeByUUID(cloudFileUUID);
                  CloudDrive localDrive = cloudDrive.findDrive(fileNode);
                  if (localDrive != null) {
                    DriveData documentDrive = documentDrives.getDriveByName(driveName);
                    if (documentDrive != null) {
                      unshareInDrive(fileNode, localDrive, documentDrive);
                    } else {
                      if (LOG.isDebugEnabled()) {
                        LOG.debug("Cannot find group drive for " + fileNode.getPath()
                            + ". Unsharing not complete for this node.");
                      }
                    }
                    // we also want remove all copied/linked symlinks from the original shared in space
                    for (NodeIterator niter = getCloudFileLinks(fileNode, driveName); niter.hasNext();) {
                      Node linkNode = niter.nextNode();
                      Node parent = linkNode.getParent();
                      linkNode.remove();
                      parent.save();
                    }
                  } // drive not found, may be this file in Trash folder and user is cleaning it, do nothing
                } catch (DriveRemovedException e) {
                  LOG.warn("Cloud File unsharing canceled due to removed drive: " + e.getMessage()
                      + ". Path: " + eventPath);
                } catch (NotCloudDriveException e) {
                  LOG.warn("Cloud File unsharing not possible for not cloud drives: " + e.getMessage()
                      + ". Path: " + eventPath);
                } catch (Throwable e) {
                  e.printStackTrace();
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
                  String cloudFileUUID = fileNode.getUUID();
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
                            LOG.debug("Cloud File link '" + linkItem.getName()
                                + "' successfully removed from the Trash.");
                          }
                        }
                      } catch (PathNotFoundException e) {
                        // node already deleted
                        LOG.warn("Cloud File " + eventPath + " node already removed directly from JCR: "
                            + e.getMessage());
                      } catch (InterruptedException e) {
                        LOG.warn("Cloud File symlink remover interrupted " + e.getMessage());
                        Thread.currentThread().interrupt();
                      } catch (Throwable e) {
                        LOG.error("Error removing node of Cloud File " + eventPath + ". " + e.getMessage(),
                                  e);
                      }
                    }
                  });
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

  protected final LinkManager            linkManager;

  protected final ManageDriveService     documentDrives;

  protected final TrashService           trash;

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

  /**
   * 
   */
  public CloudFileActionService(CloudDriveService cloudDrive,
                                RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                NodeFinder finder,
                                NodeHierarchyCreator hierarchyCreator,
                                LinkManager linkManager,
                                ManageDriveService documentDrives,
                                TrashService trash) {
    this.cloudDrive = cloudDrive;
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.hierarchyCreator = hierarchyCreator;
    this.linkManager = linkManager;
    this.documentDrives = documentDrives;
    this.trash = trash;
  }

  public boolean isGroupDrive(DriveData drive) {
    String groupsPath = hierarchyCreator.getJcrPath(GROUPS_PATH);
    return drive.getHomePath().startsWith(groupsPath);
  }

  public DriveData getNodeDrive(Node node) throws Exception {
    String groupId = getGroupNameFromPath(node.getPath());
    return groupId != null ? documentDrives.getDriveByName(groupId) : null;
  }

  public String getGroupNameFromPath(String nodePath) {
    String groupsPath = hierarchyCreator.getJcrPath(GROUPS_PATH);
    int i = nodePath.indexOf(groupsPath);
    if (i == 0) {
      String[] gpp = nodePath.substring(i, groupsPath.length()).split("/");
      if (gpp.length >= 2) {
        StringBuilder groupName = new StringBuilder();
        groupName.append('.');
        groupName.append(gpp[0]);
        groupName.append('.');
        groupName.append(gpp[1]);
        return groupName.toString();
      }
    }
    return null;
  }

  public Node linkFile(final Node srcNode,
                       final Node destNode,
                       DriveData documentsDrive) throws NotCloudDriveException,
                                                 DriveRemovedException,
                                                 RepositoryException,
                                                 CloudDriveException {
    String linkName = srcNode.getName();
    String linkTitle = documentName(srcNode);
    Node linkNode = linkManager.createLink(destNode, null, srcNode, linkName, linkTitle);
    if (linkNode.canAddMixin(ECD_CLOUDFILELINK)) {
      linkNode.addMixin(ECD_CLOUDFILELINK);
      if (documentsDrive != null) {
        linkNode.setProperty(ECD_DOCUMENTSDRIVE, documentsDrive.getName());
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
          removedShared.put(cloudFileUUID, linkNode.getProperty(ECD_DOCUMENTSDRIVE).getString());
        } catch (PathNotFoundException e) {
          // wasn't shared in documents drive
        }

        return target;
      } catch (ItemNotFoundException e) {
        LOG.warn("Cloud File link has no target node: " + linkNode.getPath() + ". " + e.getMessage());
      }
    } else {
      LOG.warn("Not cloud file link: node " + linkNode.getPath() + " not of type " + ECD_CLOUDFILELINK);
    }
    return null;
  }

  public void shareInDrive(final Node fileNode,
                           final CloudDrive cloudDrive,
                           final DriveData documentDrive) throws NotCloudDriveException,
                                                          DriveRemovedException,
                                                          RepositoryException,
                                                          CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sharing Cloud File " + fileNode.getPath() + " from " + cloudDrive + " to "
          + documentDrive.getName());
    }

    // avoid firing Cloud Drive synchronization
    CloudDriveStorage srcStorage = (CloudDriveStorage) cloudDrive;
    srcStorage.localChange(new Change<Void>() {
      @Override
      public Void apply() throws RepositoryException {
        Node parent = fileNode.getParent();
        String[] permissions = documentDrive.getAllPermissions();
        // we keep access to all sub-files of the src parent restricted
        setParentPermissions(parent, permissions);
        setPermissions(fileNode, permissions);
        parent.save(); // save everything here!
        return null;
      }
    });
  }

  public void unshareInDrive(final Node fileNode,
                             final CloudDrive cloudDrive,
                             final DriveData documentDrive) throws NotCloudDriveException,
                                                            DriveRemovedException,
                                                            RepositoryException,
                                                            CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Unsharing Cloud File " + fileNode.getPath() + " from " + cloudDrive + " in "
          + documentDrive.getName());
    }
    // avoid firing Cloud Drive synchronization
    CloudDriveStorage srcStorage = (CloudDriveStorage) cloudDrive;
    srcStorage.localChange(new Change<Void>() {
      @Override
      public Void apply() throws RepositoryException {
        Node parent = fileNode.getParent();
        String[] permissions = documentDrive.getAllPermissions();
        // we remove access for this drive to all sub-files of the src parent
        removePermissions(fileNode, permissions);
        removePermissions(parent, permissions);
        parent.save(); // save everything here!
        return null;
      }
    });
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
      observation.addEventListener(new LinkGroupListener(),
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
   * Set read permissions on the link target node to all given identities (e.g. space group members).
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or memberships)
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
   * Remove read permissions on the link target node for all given identities (e.g. space group members).
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void removePermissions(Node node, String[] identities) throws AccessControlException,
                                                                   RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    if (target.isNodeType(EXO_PRIVILEGEABLE)) {
      for (String identity : identities) {
        target.removePermission(identity, PermissionType.READ);
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
  protected void setParentPermissions(Node parent, String[] identities) throws AccessControlException,
                                                                        RepositoryException {
    // first we go through all sub-files/folders and enabled exo:privilegeable, this will copy current
    // parent
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

  protected NodeIterator getCloudFileLinks(Node targetNode, String driveName) throws RepositoryException {
    StringBuilder queryCode = new StringBuilder().append("SELECT * FROM ")
                                                    .append(ECD_CLOUDFILELINK)
                                                    .append(" WHERE exo:uuid='")
                                                    .append(targetNode.getUUID())
                                                    .append("'");
    if (driveName != null && driveName.length() > 0) {
      queryCode.append(" AND ecd:documentsDrive='").append(driveName).append("'");
    }

    QueryManager queryManager = targetNode.getSession().getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryCode.toString(), Query.SQL);
    QueryResult queryResult = query.execute();
    return queryResult.getNodes();
  }

}
