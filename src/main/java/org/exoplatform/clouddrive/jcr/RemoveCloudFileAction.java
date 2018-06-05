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
package org.exoplatform.clouddrive.jcr;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;

import org.apache.commons.chain.Context;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveManager;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Care about ecd:cloudFile nodes removal - this action should remove the file
 * on cloud provider side. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAction.java 00000 Oct 5, 2012 pnedonosko $
 */
public class RemoveCloudFileAction extends AbstractJCRAction {

  /** The log. */
  private static Log LOG = ExoLogger.getLogger(RemoveCloudFileAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean execute(Context context) throws Exception {
    Node fileNode = (Node) context.get(InvocationContext.CURRENT_ITEM);
    // we work only with node removal (no matter what set in the action config)
    if (ExtendedEvent.NODE_REMOVED == (Integer) context.get(InvocationContext.EVENT)) {
      CloudDriveService drives = getComponent(context, CloudDriveService.class);
      CloudDrive localDrive = drives.findDrive(fileNode);
      if (localDrive != null) {
        if (accept(localDrive)) {
          if (localDrive.isConnected()) {
            try {
              start(localDrive);
              try {
                new CloudDriveManager(localDrive).initRemove(fileNode);
              } catch (SyncNotSupportedException e) {
                LOG.error("Cannor remove cloud drive file " + fileNode.getPath() + ": " + e.getMessage());
              }
            } finally {
              done();
            }
            return true;
          } else {
            try {
              Node driveParent = fileNode.getSession().getItem(localDrive.getPath()).getParent();
              if (driveParent.isNodeType(JCRLocalCloudDrive.EXO_TRASHFOLDER)) {
                // we ignore files of trashed drives
                return true;
              }
            } catch (ItemNotFoundException e) {
              // file in the root of workspace
            }
            LOG.warn("Cloud drive not connected " + localDrive.getPath());
          }
        } else {
          // it's internal work on this file/drive
          return true;
        }
      } // drive not found, may be this file in Trash folder and user is
        // cleaning it, do nothing
    } else {
      LOG.warn(RemoveCloudFileAction.class.getName() + " supports only node removal. Check configuration. Item skipped: "
          + fileNode.getPath());
    }
    return false;
  }

}
