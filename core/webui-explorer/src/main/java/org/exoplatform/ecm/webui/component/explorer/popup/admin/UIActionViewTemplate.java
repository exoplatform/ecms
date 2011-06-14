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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Nov 15, 2006 10:08:29 AM
 */

@ComponentConfig()

public class UIActionViewTemplate extends UIContainer {

  private String documentType_ ;
  private NodeLocation node_ ;

  public void setTemplateNode(Node node) throws Exception {
    node_ = NodeLocation.getNodeLocationByNode(node);
    documentType_ = node.getPrimaryNodeType().getName() ;
  }

  public String getViewTemplatePath(){
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    try {
      return templateService.getTemplatePathByUser(false, documentType_, userName) ;
    } catch (Exception e) {
      return null ;
    }
  }

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    PortalContainerInfo containerInfo =
      (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class) ;
    return containerInfo.getContainerName() ;
  }

  public String getWorkspaceName() throws Exception {
    return NodeLocation.getNodeByLocation(node_).getSession().getWorkspace().getName() ;
  }

  public String getTemplate() { return getViewTemplatePath() ;}

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver() ;
  }

  public Node getNode() {
    return NodeLocation.getNodeByLocation(node_);
  }
}
