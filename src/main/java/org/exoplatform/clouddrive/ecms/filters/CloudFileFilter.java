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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;


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
    NodeFinder finder = WCMCoreUtils.getService(NodeFinder.class);

    // doing this we are taking symlinks in account also
    Node actualNode = (Node) finder.getItem(node.getSession(), node.getPath(), true);

    CloudDrive drive = driveService.findDrive(actualNode);
    if (drive != null) {
      try {
        if (drive.hasFile(actualNode.getPath())) {
          WebuiRequestContext.getCurrentInstance().setAttribute(CloudDrive.class, drive);
          return true;
        }
      } catch (DriveRemovedException e) {
        // not accepted
      }
    }

    return false;

    // TODO cleanup
    // boolean accepted = isCloudFile(node);
    // if (!accepted && node.isNodeType("exo:symlink")) {
    // // if it's symlink, check referenced node
    // Node ref = ((ExtendedSession) node.getSession()).getNodeByIdentifier(node.getProperty("exo:uuid")
    // .getString());
    // accepted = isCloudFile(ref);
    // }
  }

  @Deprecated
  protected boolean isCloudFile(Node node) throws RepositoryException {
    if (node.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)) {
      return true;
    } else if (node.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILERESOURCE)) {
      Node parent = node.getParent();
      if (parent.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)) {
        return true;
      }
    }
    return false;
  }
}
