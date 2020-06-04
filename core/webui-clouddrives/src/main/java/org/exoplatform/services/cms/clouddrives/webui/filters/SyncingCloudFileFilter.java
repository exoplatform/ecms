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
package org.exoplatform.services.cms.clouddrives.webui.filters;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.DriveRemovedException;
import org.exoplatform.services.cms.clouddrives.NotCloudDriveException;
import org.exoplatform.services.cms.clouddrives.NotCloudFileException;
import org.exoplatform.services.cms.clouddrives.NotYetCloudFileException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Filter for cloud files currently synchronizing.
 */
public class SyncingCloudFileFilter extends AbstractCloudDriveNodeFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(SyncingCloudFileFilter.class);

  /**
   * Instantiates a new syncing cloud file filter.
   */
  public SyncingCloudFileFilter() {
    super();
  }

  /**
   * Instantiates a new syncing cloud file filter.
   *
   * @param providers the providers
   */
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
              // newly creating file we accept: UI should render it properly
              // according file existence
              return true;
            }

            // FilesState driveState = drive.getState();
            // return driveState != null ? driveState.isUpdating(path) : false;
            // XXX accept only "not yet cloud files", thus synchronizing first
            // time only
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
