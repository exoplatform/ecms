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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com
 * Mar 19, 2009
 */
@SuppressWarnings("deprecation")
@ComponentConfig(
  lifecycle = UIContainerLifecycle.class
)
public class UIPublicationPagesContainer extends UIContainer {

  /**
   * Inits the.
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  public void init(Node node) throws Exception {
    UIPublicationPages publicationPages = addChild(UIPublicationPages.class, null, null);
    List<String> runningPortals = getRunningPortals(node.getSession().getUserID());
    String portalName = Util.getPortalRequestContext().getPortalOwner();
    publicationPages.init(node, portalName, runningPortals);
    UIPopupWindow popupWindow = null;
    popupWindow = getChildById("UIClvPopupContainer");
    if (popupWindow == null ) popupWindow = addChild(UIPopupWindow.class, null, "UIClvPopupContainer");
  }

  /**
   * Gets the running portals.
   *
   * @param userId the user id
   *
   * @return the running portals
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private List<String> getRunningPortals(String userId) throws Exception {
    List<String> listPortalName = new ArrayList<String>();
    DataStorage service = WCMCoreUtils.getService(DataStorage.class);
    Query<PortalConfig> query = new Query<PortalConfig>(null, null, null, null, PortalConfig.class) ;
    PageList pageList = service.find(query) ;
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    for(Object object:pageList.getAll()) {
      PortalConfig portalConfig = (PortalConfig)object;
      if(userACL.hasPermission(portalConfig)) {
        listPortalName.add(portalConfig.getName());
      }
    }
    return listPortalName;
  }
}
