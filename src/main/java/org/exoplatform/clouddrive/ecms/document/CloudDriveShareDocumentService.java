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
package org.exoplatform.clouddrive.ecms.document;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveSecurity;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudDriveStorage.Change;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ecms.action.CloudFileActionService;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.wcm.ext.component.document.service.ShareDocumentService;

/**
 * We need care about shared Cloud Files in special way: use dedicated activity
 * type.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveShareDocumentService.java 00000 Jun 25, 2018
 *          pnedonosko $
 */
public class CloudDriveShareDocumentService extends ShareDocumentService {

  /** The Constant LOG. */
  protected static final Log             LOG = ExoLogger.getLogger(CloudDriveShareDocumentService.class);

  /** The activity manager. */
  protected final ActivityManager        activityManager;

  /** The link manager. */
  protected final LinkManager            linkManager;

  /** The cloud drive service. */
  protected final CloudDriveService      cloudDrives;

  /** The hierarchy creator. */
  protected final NodeHierarchyCreator   hierarchyCreator;

  /** The cloud file actions. */
  protected final CloudFileActionService cloudFileActions;

  /** The groups path. */
  protected final String                 groupsPath;

  /** The users path. */
  protected final String                 usersPath;

  /**
   * Instantiates a new share cloud drive document service.
   *
   * @param repoService the repo service
   * @param linkManager the link manager
   * @param sessionProviderService the session provider service
   * @param activityManager the activity manager
   * @param hierarchyCreator the hierarchy creator
   * @param cloudDrives the cloud drive service
   * @param cloudFileActions the cloud file actions
   */
  public CloudDriveShareDocumentService(RepositoryService repoService,
                                        LinkManager linkManager,
                                        SessionProviderService sessionProviderService,
                                        ActivityManager activityManager,
                                        NodeHierarchyCreator hierarchyCreator,
                                        CloudDriveService cloudDrives,
                                        CloudFileActionService cloudFileActions) {
    super(repoService, linkManager, sessionProviderService);
    this.activityManager = activityManager;
    this.linkManager = linkManager;
    this.hierarchyCreator = hierarchyCreator;
    this.cloudDrives = cloudDrives;
    this.cloudFileActions = cloudFileActions;

    this.groupsPath = hierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    this.usersPath = hierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String publishDocumentToSpace(String groupId, Node node, String comment, String perm) {
    CloudDrive localDrive = findCloudDrive(node);
    if (localDrive != null) {
      return applyInDrive(localDrive, new Change<String>() {
        @Override
        public String apply() throws RepositoryException {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Sharing Cloud File " + node.getPath() + " of " + localDrive + " to " + " " + groupId + " " + perm);
          }
          String activityId = CloudDriveShareDocumentService.super.publishDocumentToSpace(groupId, node, comment, perm);
          // We fix what super did to allow only read the cloud
          // file (instead of read, add node, set property and remove for 'Can
          // Write' option).
          cloudFileActions.shareToGroup(node, groupId);
          // We change activity type to CloudDrive's one
          ExoSocialActivity activity = activityManager.getActivity(activityId);
          if (activity != null && !CloudFileActionService.SHARE_CLOUD_FILES_SPACES.equals(activity.getType())) {
            activity.setType(CloudFileActionService.SHARE_CLOUD_FILES_SPACES);
            activityManager.updateActivity(activity);
          }
          // Finally share if the drive supports this
          shareInDrive(node, localDrive, groupId);
          // TODO do we need CloudDriveContext.init() here ?
          return activityId;
        }
      });
    }
    // If not cloud file - super's behaviour
    return super.publishDocumentToSpace(groupId, node, comment, perm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void publishDocumentToUser(String userId, Node node, String comment, String perm) {
    CloudDrive localDrive = findCloudDrive(node);
    if (localDrive != null) {
      applyInDrive(localDrive, new Change<Void>() {
        @Override
        public Void apply() throws RepositoryException {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Sharing Cloud File " + node.getPath() + " of " + localDrive + " to " + userId + " " + perm);
          }
          CloudDriveShareDocumentService.super.publishDocumentToUser(userId, node, comment, perm);
          // We fix what super did to allow only read the cloud
          // file (instead of read, add node, set property and remove for 'Can
          // Write' option).
          cloudFileActions.shareToUser(node, userId);
          // Finally share if the drive supports this
          shareInDrive(node, localDrive, userId);
          // TODO do we need CloudDriveContext.init() here ?
          return null;
        }
      });
      return;
    }
    // If not cloud file - super's behaviour
    super.publishDocumentToUser(userId, node, comment, perm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unpublishDocumentToUser(String userId, ExtendedNode node) {
    CloudDrive localDrive = findCloudDrive(node);
    if (localDrive != null) {
      applyInDrive(localDrive, new Change<Void>() {
        @Override
        public Void apply() throws RepositoryException, CloudDriveException {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Unsharing Cloud File " + node.getPath() + " of " + localDrive);
          }
          // XXX original ShareDocumentService's method works not clean, it will
          // not remove copied or renamed link nodes, even more in case of link
          // rename it will fail to delete it with PathNotFoundException
          // internally but the exception will not be thrown out (only logged).
          // Thus we need make better cleanup after its logic.
          // In case of JCR error this will rollback the above work
          CloudDriveShareDocumentService.super.unpublishDocumentToUser(userId, node);
          node.refresh(false);
          cloudFileActions.unshareToUser(node, userId);
          // Finally unshare if the drive supports this
          unshareInDrive(node, localDrive, userId);
          return null;
        }
      });
      return;
    }
    // If not cloud file - super's behaviour
    super.unpublishDocumentToUser(userId, node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unpublishDocumentToSpace(String groupId, ExtendedNode node) {
    CloudDrive localDrive = findCloudDrive(node);
    if (localDrive != null) {
      applyInDrive(localDrive, new Change<Void>() {
        @Override
        public Void apply() throws RepositoryException {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Unsharing Cloud File " + node.getPath() + " of " + localDrive);
          }
          // XXX original ShareDocumentService's method works not clean, it will
          // not remove copied or renamed link nodes, even more in case of link
          // rename it will fail to delete it with PathNotFoundException
          // internally but the exception will not be thrown out (only logged).
          // Thus we need make better cleanup after its logic.
          CloudDriveShareDocumentService.super.unpublishDocumentToSpace(groupId, node);
          // in case of JCR error this will rollback the above work
          node.refresh(false);
          cloudFileActions.unshareToSpace(node, groupId);
          // Finally unshare if the drive supports this
          unshareInDrive(node, localDrive, groupId);
          return null;
        }
      });
      return;
    }
    // If not cloud file - super's behaviour
    super.unpublishDocumentToSpace(groupId, node);
  }

  // ****************** Internals ******************

  protected CloudDrive findCloudDrive(Node node) {
    try {
      if (node.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)) {
        CloudDrive localDrive = cloudDrives.findDrive(node);
        if (localDrive != null) {
          return localDrive;
        } else {
          LOG.warn("Cloud File node not be found in any of registered drives: " + node.getPath());
        }
      }
    } catch (RepositoryException e) {
      LOG.warn("Error reading node of shared document " + node, e);
    }
    return null;
  }

  protected <R> R applyInDrive(CloudDrive cloudDrive, Change<R> change) {
    try {
      // Apply local file node changes in JCR via Change object to
      // avoid firing Cloud Drive synchronization
      CloudDriveStorage cdStorage = (CloudDriveStorage) cloudDrive;
      return cdStorage.localChange(change);
    } catch (RepositoryException e) {
      LOG.error("Error applying cloud drive changes in " + cloudDrive, e);
    } catch (NotCloudDriveException e) {
      LOG.warn("Cannot apply changes to not connected cloud drive: " + cloudDrive, e);
    } catch (DriveRemovedException e) {
      LOG.warn("Cannot apply changes to removed cloud drive: " + cloudDrive, e);
    } catch (CloudDriveException e) {
      LOG.error("Error applying cloud drive changes: " + cloudDrive, e);
    }
    return null;
  }

  /**
   * Share cloud file (by its node) in Cloud Drive.
   *
   * @param node the cloud file node
   * @param localDrive the local drive
   * @param identity the identity
   */
  protected void shareInDrive(Node node, CloudDrive localDrive, String identity) {
    // share file in cloud provider (if applicable)
    CloudDriveSecurity srcSecurity = (CloudDriveSecurity) localDrive;
    if (srcSecurity.isSharingSupported()) {
      try {
        srcSecurity.shareFile(node, identity);
      } catch (RepositoryException e) {
        LOG.error("Error sharing cloud file: " + node, e);
      } catch (CloudDriveException e) {
        LOG.error("Error sharing cloud file: " + node, e);
      }
    }
  }

  /**
   * Unshare cloud file (by its node) in Cloud Drive.
   *
   * @param node the cloud file node
   * @param localDrive the local drive
   * @param identity the identity
   */
  protected void unshareInDrive(Node node, CloudDrive localDrive, String identity) {
    // share file in cloud provider (if applicable)
    CloudDriveSecurity srcSecurity = (CloudDriveSecurity) localDrive;
    if (srcSecurity.isSharingSupported()) {
      try {
        srcSecurity.unshareFile(node, identity);
      } catch (RepositoryException e) {
        LOG.error("Error unsharing cloud file: " + node, e);
      } catch (CloudDriveException e) {
        LOG.error("Error unsharing cloud file: " + node, e);
      }
    }
  }

}
