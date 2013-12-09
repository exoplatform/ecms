/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.portal.listener;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.DataStorageImpl;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalArtifactsService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public class CreateLivePortalEventListener extends Listener<DataStorageImpl, PortalConfig> {
  private static final Log LOG = ExoLogger.getLogger(CreateLivePortalEventListener.class.getName());
  private boolean autoCreatedDrive = true;
  private List<String> targetDrives = null;
  public static String AUTO_CREATE_DRIVE = "autoCreatedDrive";
  public static String TARGET_DRIVES = "targetDrives";

  public CreateLivePortalEventListener() {
  }

  @SuppressWarnings("unchecked")
  public CreateLivePortalEventListener(InitParams params) throws Exception {
    if(params != null) {
      ValueParam autoCreated = params.getValueParam(AUTO_CREATE_DRIVE);
      if(autoCreated != null)
        autoCreatedDrive = Boolean.parseBoolean(autoCreated.getValue());
      ValuesParam targets = params.getValuesParam(TARGET_DRIVES);
      if(targets != null)
        targetDrives = targets.getValues();
    }
  }
  /*
   * (non-Javadoc)
   *
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public final void onEvent(final Event<DataStorageImpl, PortalConfig> event) throws Exception {
    PortalConfig portalConfig = event.getData();
    if (!PortalConfig.PORTAL_TYPE.equals(portalConfig.getType())) return;
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      try {
        livePortalManagerService.getLivePortal(sessionProvider, portalConfig.getName());
        return;//portal already exists
      } catch (Exception e) {
        //portal did not exists, process to create
        if (LOG.isInfoEnabled()) {
          LOG.info("Creating new resource storage for portal: " + portalConfig.getName());
        }
      }
      // Create site content storage for the portal
      try {
        livePortalManagerService.addLivePortal(sessionProvider, portalConfig);
        if (LOG.isInfoEnabled()) {
          LOG.info("Created new resource storage for portal: " + portalConfig.getName());
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error when create new resource storage: " + portalConfig.getName(), e);
        }
      }
      // create drive for the site content storage
      if(autoCreatedDrive || (!autoCreatedDrive && targetDrives != null && targetDrives.contains(portalConfig.getName()))) {
        ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
        WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
        try {
          Node portal = livePortalManagerService.getLivePortal(sessionProvider, portalConfig.getName());
          createPortalDrive(portal,portalConfig,configurationService,manageDriveService);
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Error when create drive for portal: " + portalConfig.getName(), e);
          }
        }
      }
      //Deploy initial artifacts for this portal
      CreatePortalArtifactsService artifactsInitializerService = WCMCoreUtils.getService(CreatePortalArtifactsService.class);
      try {
        artifactsInitializerService.deployArtifactsToPortal(sessionProvider, portalConfig.getName(),
                                                            portalConfig.getPortalLayout().getId());
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error when create drive for portal: " + portalConfig.getName(), e);
        }
      }
    } finally {
      sessionProvider.close();
    }
  }

  private void createPortalDrive(Node portal,
                                 PortalConfig portalConfig,
                                 WCMConfigurationService wcmConfigService,
                                 ManageDriveService driveService) throws Exception {
    Session session = portal.getSession();
    String workspace = session.getWorkspace().getName();
    DriveData mainDriveData = wcmConfigService.getSiteDriveConfig();
    String permission = portalConfig.getEditPermission();
    String portalPath = portal.getPath();

    String homePath = mainDriveData.getHomePath();
    homePath = homePath.replaceAll(WCMConfigurationService.SITE_NAME_EXP, portal.getName());
    homePath = homePath.replaceAll(WCMConfigurationService.SITE_PATH_EXP, portalPath);

    String views = mainDriveData.getViews();
    String icon = mainDriveData.getIcon();
    boolean viewReferences = mainDriveData.getViewPreferences();
    boolean viewNonDocument = mainDriveData.getViewNonDocument();
    boolean viewSideBar = mainDriveData.getViewSideBar();
    boolean showHiddenNode = mainDriveData.getShowHiddenNode();
    String allowCreateFolder = mainDriveData.getAllowCreateFolders();
    String allowNodeTypesOnTree = mainDriveData.getAllowNodeTypesOnTree();
    String driveName = String.format("%s-category", portal.getName());
    driveService.addDrive(driveName,
                          workspace,
                          permission,
                          homePath,
                          views,
                          icon,
                          viewReferences,
                          viewNonDocument,
                          viewSideBar,
                          showHiddenNode,
                          allowCreateFolder,
                          allowNodeTypesOnTree);
    if (LOG.isInfoEnabled()) {
      LOG.info("Create new drive for portal: " + portalConfig.getName());
    }
  }
}
