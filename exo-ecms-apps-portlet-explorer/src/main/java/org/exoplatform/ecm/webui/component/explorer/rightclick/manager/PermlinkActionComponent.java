/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.admin.manager.UIAbstractManager;
import org.exoplatform.ecm.webui.component.admin.manager.UIAbstractManagerComponent;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)      
public class PermlinkActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS 
  		= Arrays.asList(new UIExtensionFilter[] { new IsNotInTrashFilter(),
  																							new IsDocumentFilter() });
  
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  public String getPermlink(Node node) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    String portalUrl = pcontext.getPortalURI();
    String nodePathUrl = pcontext.getNodePath();
    String portletId = nodePathUrl.split("/")[1];
    String drivename = uiExplorer.getDriveData().getName();
    String drivePath = uiExplorer.getDriveData().getHomePath();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if (drivePath.contains("${userId}")) drivePath = drivePath.replace("${userId}", userId);
    String nodePath = node.getPath().replace(drivePath, "/").replaceAll("/+", "/");
    String repository = uiExplorer.getRepositoryName();
    StringBuffer bf = new StringBuffer(1024);
    return bf.append(portalUrl).append(portletId).append("/").append(repository).append("/").append(drivename).append(nodePath).toString();
  }
  
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    // TODO Auto-generated method stub
    return null;
  }

}
