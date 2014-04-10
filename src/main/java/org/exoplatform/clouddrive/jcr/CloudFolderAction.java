/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


/**
 * Care about ecd:cloudFolder nodes: creation of new folders and removal of ones.<br/>
 * Created by The eXo Platform SAS.
 * 
 * TODO not used
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFolderAction.java 00000 Oct 9, 2012 pnedonosko $
 */
public class CloudFolderAction extends AbstractJCRAction {

  private static Log LOG = ExoLogger.getLogger(CloudFolderAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean execute(Context context) throws Exception {
    Property prop = (Property) context.get(InvocationContext.CURRENT_ITEM);

    // 1. TODO if it's folder property
    try {
      Node cloudFolder = prop.getParent();
      if (cloudFolder.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFOLDER)) {
        CloudDriveService drives = drives(context);
        CloudDrive localDrive = drives.findDrive(cloudFolder);
        if (accept(localDrive)) {
          try {
            start(localDrive);
            // 3. try to synchronize
            localDrive.synchronize(); // TODO ?
            return true;
          } catch (SyncNotSupportedException e) {
            LOG.error("Node cannot be stored in Cloud Drive. Action Node was: " + cloudFolder.getPath(), e);
          } catch (NotConnectedException e) {
            LOG.error("Drive not connected. Action Node was: " + cloudFolder.getPath(), e);
          } finally {
            done();
          }
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Action item isn't property of ecd:cloudFolder or invalid JCR session. Item: " + prop, e);
    }

    return false;
  }

}
