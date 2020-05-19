package org.exoplatform.services.cms.clouddrives;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The type Cloud drive documents drive initializer.
 */
public class CloudDriveDocumentsDriveInitializer extends BaseComponentPlugin {

  /**
   * The Constant LOG.
   */
  protected static final Log         LOG                            =
                                         ExoLogger.getLogger(CloudDriveDocumentsDriveInitializer.class);

  /**
   * The constant DRIVE_VIEWS.
   */
  protected static final String      DRIVE_VIEWS                    = "List, Icons, Admin";

  /**
   * The constant DRIVE_ICON.
   */
  protected static final String      DRIVE_ICON                     = "";

  /**
   * The constant DRIVE_VIEW_REFERENCES.
   */
  protected static final boolean     DRIVE_VIEW_REFERENCES          = false;

  /**
   * The constant DRIVE_VIEW_NON_DOCUMENT.
   */
  protected static final boolean     DRIVE_VIEW_NON_DOCUMENT        = false;

  /**
   * The constant DRIVE_VIEW_SIDE_BAR.
   */
  protected static final boolean     DRIVE_VIEW_SIDE_BAR            = false;

  /**
   * The constant DRIVE_SHOW_HIDDEN_NODE.
   */
  protected static final boolean     DRIVE_SHOW_HIDDEN_NODE         = false;

  /**
   * The constant DRIVE_ALLOW_CREATE_FOLDER.
   */
  protected static final String      DRIVE_ALLOW_CREATE_FOLDER      = "nt:folder,nt:unstructured";

  /**
   * The constant DRIVE_ALLOW_NODE_TYPES_ON_TREE.
   */
  protected static final String      DRIVE_ALLOW_NODE_TYPES_ON_TREE = "*";

  /**
   * The Manage Drive Service.
   */
  protected final ManageDriveService manageDriveService;

  /**
   * Instantiates a new Cloud drive documents drive initializer.
   *
   * @param manageDriveService the manage drive service
   */
  public CloudDriveDocumentsDriveInitializer(ManageDriveService manageDriveService) {
    this.manageDriveService = manageDriveService;
  }

  /**
   * Add documents drive.
   *
   * @param local the local
   */
  public void addDocumentsDrive(CloudDrive local, Node driveNode) throws RepositoryException, DriveRemovedException {
    try {
      manageDriveService.addDrive(local.getTitle(),
                                  local.getWorkspace(),
                                  driveNode.getSession().getUserID(),
                                  local.getPath(),
                                  DRIVE_VIEWS,
                                  DRIVE_ICON,
                                  DRIVE_VIEW_REFERENCES,
                                  DRIVE_VIEW_NON_DOCUMENT,
                                  DRIVE_VIEW_SIDE_BAR,
                                  DRIVE_SHOW_HIDDEN_NODE,
                                  DRIVE_ALLOW_CREATE_FOLDER,
                                  DRIVE_ALLOW_NODE_TYPES_ON_TREE);
    } catch (Exception e) {
      LOG.warn("Warning adding a drive '" + local.getTitle() + "' by ManageDriveService", e);
    }
  }

  /**
   * Delete document drive.
   *
   * @param event the event
   */
  public void deleteDocumentDrive(CloudDriveEvent event) {
    event.getRemoved();

    String removedDriveName = new StringBuilder(event.getUser().getProvider().getName()).append(" - ")
                                                                                        .append(event.getUser().getEmail())
                                                                                        .toString();
    try {
      manageDriveService.removeDrive(removedDriveName);
    } catch (Exception e) {
      LOG.warn("Warning removing a drive '" + removedDriveName + "' by ManageDriveService", e);
    }
  }
}
