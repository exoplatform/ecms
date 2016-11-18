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
package org.exoplatform.clouddrive.ecms.filters;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Filter for nodes are not cloud files but existing in cloud drive folder.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LocalNodeFilter.java 00000 May 25, 2014 pnedonosko $
 */
public class LocalNodeFilter extends AbstractCloudDriveNodeFilter {

  protected static final Log LOG = ExoLogger.getLogger(LocalNodeFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(Node node) throws RepositoryException {
    CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
    CloudDrive drive = driveService.findDrive(node);
    if (drive != null) {
      if (acceptProvider(drive.getUser().getProvider())) {
        try {
          if (((CloudDriveStorage) drive).isLocal(node)) {
            WebuiRequestContext.getCurrentInstance().setAttribute(CloudDrive.class, drive);
            return true;
          }
        } catch (DriveRemovedException e) {
          // doesn't accept removed drive
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> LocalNodeFilter.accept(" + node.getPath() + ") drive removed " + drive + ": "
                + e.getMessage());
          }
        }
      }
    }
    return false;
  }
}
