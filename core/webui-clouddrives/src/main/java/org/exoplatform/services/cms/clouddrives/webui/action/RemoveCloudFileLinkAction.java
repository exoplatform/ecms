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
package org.exoplatform.services.cms.clouddrives.webui.action;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.exoplatform.services.cms.clouddrives.jcr.AbstractJCRAction;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Care about symlinks to ecd:cloudFile nodes removal - this action should
 * initiate group permissions removal from the cloud file added in time of
 * sharing to group drives. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RemoveCloudFileLinkAction.java 00000 Jul 7, 2015 pnedonosko $
 */
public class RemoveCloudFileLinkAction extends AbstractJCRAction {

  /** The log. */
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
      // Don't care about removal from Trash: it can be Cloud Drive itself when
      // removing cloud file by sync op.
      if (!trash.getTrashHomeNode().isSame(linkNode.getParent())) {
        CloudFileActionService actions = getComponent(context, CloudFileActionService.class);
        Node targetNode = actions.markRemoveLink(linkNode);
        if (targetNode != null) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Cloud File link marked for removal: " + linkNode.getPath() + " -> " + targetNode.getPath());
          }
        }
      }
    } else {
      LOG.warn(RemoveCloudFileLinkAction.class.getName() + " supports only node removal. Check configuration. Item skipped: "
          + linkNode.getPath());
    }
    return false;
  }

}
