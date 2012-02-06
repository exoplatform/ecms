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
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 21, 2008 3:43:43 PM
 */
public class RemoveNodeAction implements Action {
  
  private ThumbnailService thumbnailService;
  
  public boolean execute(Context context) throws Exception {
    thumbnailService = WCMCoreUtils.getService(ThumbnailService.class);
    
    //remove thumbnail of node
    Node node = (Node)context.get("currentItem");
    if(thumbnailService.isEnableThumbnail()) {
      try {
        thumbnailService.processRemoveThumbnail(node);
      } catch(Exception e) {
        return false;
      }
    }
    //remove dead symlinks
    Utils.removeDeadSymlinks(node);    
    return false;
  }

}
