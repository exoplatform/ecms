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
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Filter for cloud files.
 */
public class CloudFileFilter extends AbstractCloudDriveNodeFilter {

  protected static final Log LOG = ExoLogger.getLogger(CloudFileFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(Node node) throws RepositoryException {
    CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
    CloudDrive drive = driveService.findDrive(node);
    if (drive != null) {
      try {
        if (drive.hasFile(node.getPath())) {
          // attribute used in CloudFileViewer.gtmpl
          WebuiRequestContext.getCurrentInstance().setAttribute(CloudDrive.class, drive);
          return true;
        }
      } catch (DriveRemovedException e) {
        // doesn't accept
      } 
    }
    return false;
  }
}
