/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.thumbnail.impl;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 8 avr. 2009
 */
public final class ThumbnailUtils {

  private static final Log LOG = ExoLogger.getLogger(ThumbnailUtils.class);
  
  static Node getThumbnailFolder(Node parentNode) throws RepositoryException {
    if (!parentNode.hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER)) {
      try {
        Node thumbnailFolder = parentNode.addNode(ThumbnailService.EXO_THUMBNAILS_FOLDER,
                                             ThumbnailService.EXO_THUMBNAILS);
        parentNode.getSession().save();
        if (thumbnailFolder.canAddMixin(ThumbnailService.HIDDENABLE_NODETYPE)) {
          thumbnailFolder.addMixin(ThumbnailService.HIDDENABLE_NODETYPE);
        }
        parentNode.getSession().save();
        return thumbnailFolder;
      } catch (ItemExistsException e) {
        // The folder could already be created due to potential concurrent access
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    return parentNode.getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
  }

  static Node getThumbnailNode(Node thumbnailFolder, String identifier) throws RepositoryException {
    if (!thumbnailFolder.hasNode(identifier)) {
      try {
        Node thumbnailNode = thumbnailFolder.addNode(identifier, ThumbnailService.EXO_THUMBNAIL);
        thumbnailFolder.getSession().save();
        return thumbnailNode;
      } catch (ItemExistsException e) {
        // The folder could already be created due to potential concurrent access
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    return thumbnailFolder.getNode(identifier);
  }
}
