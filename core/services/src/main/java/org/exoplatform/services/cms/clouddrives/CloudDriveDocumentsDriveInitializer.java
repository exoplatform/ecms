package org.exoplatform.services.cms.clouddrives;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

/**
 * The type Cloud drive documents drive initializer.
 */
public class CloudDriveDocumentsDriveInitializer extends BaseComponentPlugin implements CloudDriveListener {

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
   * Add document drive.
   *
   * @param event the local
   */
  protected void addDocumentDrive(CloudDriveEvent event) {
    try {
      manageDriveService.addDrive(event.getTitle(),
                                  event.getWorkspace(),
                                  event.getLocalUser(),
                                  event.getNodePath(),
                                  DRIVE_VIEWS,
                                  DRIVE_ICON,
                                  DRIVE_VIEW_REFERENCES,
                                  DRIVE_VIEW_NON_DOCUMENT,
                                  DRIVE_VIEW_SIDE_BAR,
                                  DRIVE_SHOW_HIDDEN_NODE,
                                  DRIVE_ALLOW_CREATE_FOLDER,
                                  DRIVE_ALLOW_NODE_TYPES_ON_TREE);
    } catch (Exception e) {
      LOG.warn("Warning adding a drive '" + event.getTitle() + "' by ManageDriveService", e);
    }
  }

  /**
   * Delete document drive.
   *
   * @param event the event
   */
  protected void deleteDocumentDrive(CloudDriveEvent event) {
    try {
      manageDriveService.removeDrive(event.getTitle());
    } catch (Exception e) {
      LOG.warn("Warning removing a drive '" + event.getTitle() + "' by ManageDriveService", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemove(CloudDriveEvent event) {
    deleteDocumentDrive(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(CloudDriveEvent event) {
    addDocumentDrive(event);
  }
}
