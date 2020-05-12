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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.DriveRemovedException;
import org.exoplatform.services.cms.clouddrives.NotCloudDriveException;
import org.exoplatform.services.cms.clouddrives.NotCloudFileException;
import org.exoplatform.services.cms.clouddrives.NotYetCloudFileException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Filter for cloud files.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NotCloudDriveOrFileFilter.java 00000 Jul 6, 2015 pnedonosko $
 */
public class NotCloudDriveOrFileFilter extends AbstractCloudDriveNodeFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(NotCloudDriveOrFileFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(Node node) throws RepositoryException {
    if (node != null) {
      CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
      CloudDrive drive = driveService.findDrive(node);
      if (drive != null) {
        try {
          if (acceptProvider(drive.getUser().getProvider())) {
            if (drive.getPath().equals(node.getPath())) {
              return false;
            } else {
              // call it for exceptions it can throw away
              drive.getFile(node.getPath());
              return false;
            }
          }
        } catch (DriveRemovedException e) {
          // don't accept it!
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> NotCloudDriveOrFileFilter.accept(" + node.getPath() + ") drive removed " + drive + ": "
                + e.getMessage());
          }
          return false;
        } catch (NotYetCloudFileException e) {
          // don't accept it!
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> NotCloudDriveOrFileFilter.accept(" + node.getPath() + ") not yet cloud file: " + e.getMessage());
          }
          return false;
        } catch (NotCloudFileException e) {
          // accept it
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> NotCloudDriveOrFileFilter.accept(" + node.getPath() + ") not cloud file: " + e.getMessage());
          }
        } catch (NotCloudDriveException e) {
          // accept it
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> NotCloudDriveOrFileFilter.accept(" + node.getPath() + ") not in cloud drive: " + e.getMessage());
          }
        }
      }
    }
    return true;
  }
}
