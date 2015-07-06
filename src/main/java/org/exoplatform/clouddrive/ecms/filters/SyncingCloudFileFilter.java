/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.filters;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotYetCloudFileException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Filter for cloud files currently synchronizing.
 * 
 */
public class SyncingCloudFileFilter extends AbstractCloudDriveNodeFilter {

  protected static final Log LOG = ExoLogger.getLogger(SyncingCloudFileFilter.class);

  public SyncingCloudFileFilter() {
    super();
  }

  public SyncingCloudFileFilter(List<String> providers) {
    super(providers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(Node node) throws RepositoryException {
    if (node != null) {
      CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
      CloudDrive drive = driveService.findDrive(node);
      if (drive != null) {
        if (acceptProvider(drive.getUser().getProvider())) {
          String path = node.getPath();
          try {
            WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
            rcontext.setAttribute(CloudDrive.class, drive);

            try {
              CloudFile file = drive.getFile(path);
              // attribute may be used in UI
              rcontext.setAttribute(CloudFile.class, file);
            } catch (NotYetCloudFileException e) {
              // newly creating file we accept: UI should render it properly according file existence
              return true;
            }

            // FilesState driveState = drive.getState();
            // return driveState != null ? driveState.isUpdating(path) : false;
            // XXX accept only "not yet cloud files", thus synchronizing first time only
            return false;
          } catch (DriveRemovedException e) {
            // doesn't accept
          } catch (NotCloudFileException e) {
            // doesn't accept
          } catch (NotCloudDriveException e) {
            // doesn't accept
          }
        }
      }
    }
    return false;
  }
}
