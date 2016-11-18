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
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Accept only ecd:cloudDrive nodes.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveFiler.java 00000 Nov 5, 2012 pnedonosko $
 * 
 */
public class CloudDriveFilter extends AbstractCloudDriveNodeFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(CloudDriveFilter.class);

  /**
   * Instantiates a new cloud drive filter.
   */
  public CloudDriveFilter() {
    super();
  }

  /**
   * Instantiates a new cloud drive filter.
   *
   * @param providers the providers
   */
  public CloudDriveFilter(List<String> providers) {
    super(providers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(Node node) throws RepositoryException {
    CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
    CloudDrive drive = driveService.findDrive(node);
    try {
      // accept only exactly the drive node
      return drive != null && acceptProvider(drive.getUser().getProvider())
          && drive.getPath().equals(node.getPath());
    } catch (DriveRemovedException e) {
      // doesn't accept removed
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> CloudDriveFilter.accept(" + node.getPath() + ") drive removed " + drive + ": "
            + e.getMessage());
      }
    }
    return false;
  }
}
