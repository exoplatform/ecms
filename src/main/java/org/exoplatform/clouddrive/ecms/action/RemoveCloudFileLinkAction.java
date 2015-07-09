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
package org.exoplatform.clouddrive.ecms.action;

import org.apache.commons.chain.Context;
import org.exoplatform.clouddrive.jcr.AbstractJCRAction;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;

/**
 * Care about symlinks to ecd:cloudFile nodes removal - this action should initiate group permissions removal
 * from the cloud file added in time of sharing to group drives. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RemoveCloudFileLinkAction.java 00000 Jul 7, 2015 pnedonosko $
 */
public class RemoveCloudFileLinkAction extends AbstractJCRAction {

  private static Log LOG = ExoLogger.getLogger(RemoveCloudFileLinkAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean execute(Context context) throws Exception {
    Node linkNode = (Node) context.get(InvocationContext.CURRENT_ITEM);
    // we work only with node removal (no matter what set in the action config)
    if (ExtendedEvent.NODE_REMOVED == (Integer) context.get(InvocationContext.EVENT)) {
      TrashService trash = getComponent(context, TrashService.class);
      // Don't care about removal from Trash: it can be Cloud Drive itself when removing cloud file by sync op.
      if (!trash.getTrashHomeNode().isSame(linkNode.getParent())) {
        CloudFileActionService actions = getComponent(context, CloudFileActionService.class);
        Node targetNode = actions.markRemoveLink(linkNode);
        if (targetNode != null) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Cloud File link marked for removal: " + linkNode.getPath() + " -> "
                + targetNode.getPath());
          }
        }
      }
    } else {
      LOG.warn(RemoveCloudFileLinkAction.class.getName()
          + " supports only node removal. Check configuration. Item skipped: " + linkNode.getPath());
    }
    return false;
  }

}
