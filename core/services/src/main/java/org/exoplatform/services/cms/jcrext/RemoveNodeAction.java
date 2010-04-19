/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.jcrext;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.command.action.Action;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 21, 2008 3:43:43 PM
 */
public class RemoveNodeAction implements Action {

  public boolean execute(Context context) throws Exception {
    ExoContainer container = (ExoContainer)context.get("exocontainer");
    ThumbnailService thumbnailService = 
      (ThumbnailService)container.getComponentInstanceOfType(ThumbnailService.class);
    if(thumbnailService.isEnableThumbnail()) {
      Node node = (Node)context.get("currentItem");
      try {
        thumbnailService.processRemoveThumbnail(node);
      } catch(Exception e) {
        return false;
      }
    }
    return false;
  }

}
