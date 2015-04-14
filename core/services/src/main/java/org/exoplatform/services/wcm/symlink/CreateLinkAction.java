/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.wcm.symlink;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 8, 2010
 */
public class CreateLinkAction implements Action{

  public static final String UPDATE_EVENT = "WCMPublicationService.event.updateState";
  public static  final String  EXO_SORTABLE = "exo:sortable";

  public boolean execute(Context context) throws Exception {
    Property property = (Property)context.get("currentItem");
    if (!"exo:uuid".equals(property.getName())) return false;

    Node linkNode = property.getParent();
    if (!linkNode.isNodeType(EXO_SORTABLE) && !linkNode.canAddMixin(EXO_SORTABLE))
      return false;

    PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
    String containerName = containerInfo.getContainerName();
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class, containerName);
    Node targetNode = linkManager.getTarget(linkNode, true);

    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class, containerName);
    CmsService cmsService = WCMCoreUtils.getService(CmsService.class, containerName);

    listenerService.broadcast(UPDATE_EVENT, cmsService, targetNode);

    return true;
  }

}
