/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication.listener.post;

import javax.jcr.Node;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham	
 *          hoa.phamvu@exoplatform.com
 * Mar 6, 2009  
 */
public class PostEditContentEventListener extends Listener<CmsService,Node> {

  /** The pservice. */
  private WCMPublicationService publicationService;

  /**
   * Instantiates a new post edit content event listener.
   * 
   * @param pservice the pservice
   */
  public PostEditContentEventListener(WCMPublicationService publicationService) {
    this.publicationService = publicationService;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<CmsService, Node> event) throws Exception {
  	Node currentNode = event.getData();
  	if( currentNode.isNodeType("exo:cssFile") || 
        currentNode.isNodeType("exo:jsFile") || currentNode.getParent().isNodeType("exo:actionStorage")){
      return;    
    }
    String siteName = Util.getPortalRequestContext().getPortalOwner();
    String remoteUser = Util.getPortalRequestContext().getRemoteUser();
    publicationService.updateLifecyleOnChangeContent(currentNode, siteName, remoteUser);
  }
}
