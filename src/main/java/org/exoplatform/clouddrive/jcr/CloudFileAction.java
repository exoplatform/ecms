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

import javax.jcr.Item;
import javax.jcr.Node;
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
 * Care about property changes on ecd:cloudFileResource nodes, i.e. changes of
 * actual content. Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAction.java 00000 Oct 5, 2012 pnedonosko $
 */
public class CloudFileAction extends AbstractJCRAction {

  private static Log                     LOG     = ExoLogger.getLogger(CloudFileAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean execute(Context context) throws Exception {
    Item item = (Item) context.get(InvocationContext.CURRENT_ITEM);

    // 1. if it's content property
    if (item.getName().equals("jcr:data")) {
      // 2. check if it's property of ecd:cloudFile
      try {
        Node fileNode = (item.isNode() ? (Node) item : item.getParent());
        CloudDriveService drives = drives(context);
        CloudDrive localDrive = drives.findDrive(fileNode);
        if (localDrive != null && accept(localDrive)) {
          // it's a node on path of some cloud drive...
          // to avoid updates caused by the synchronization itself we use thread-local flag
          start(localDrive);
          try {
            // 3. try to synchronize
            // Event types:
            // if addProperty or changeProperty - push changes to the cloud drive this will be cared
            // relying on Item.isNew
            // if removeProperty - remove from the cloud also removal of jcr:data isn't possible
            localDrive.synchronize(fileNode);
            return true;
          } catch (SyncNotSupportedException e) {
            LOG.error("Node cannot be stored in Cloud Drive. Action Node was: " + fileNode.getPath(), e);
          } catch (NotConnectedException e) {
            LOG.error("Drive not connected. Action Node was: " + fileNode.getPath(), e);
          } finally {
            done();
          }
        }
      } catch (RepositoryException e) {
        LOG.error("Action item isn't property of ecd:cloudFile or invalid JCR session. Item: " + item, e);
      }
    }
    return false;
  }

}
