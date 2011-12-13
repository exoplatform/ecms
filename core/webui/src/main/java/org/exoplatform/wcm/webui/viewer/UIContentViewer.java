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
package org.exoplatform.wcm.webui.viewer;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 9, 2009
 */
@ComponentConfig(
  lifecycle = Lifecycle.class
)
public class UIContentViewer extends UIBaseNodePresentation {

  public static final String TEMPLATE_NOT_SUPPORT = "UIContentViewer.msg.template-not-support";

  private NodeLocation originalNodeLocation;

  private NodeLocation viewNodeLocation;

  public Node getOriginalNode() {
    return NodeLocation.getNodeByLocation(originalNodeLocation);
  }

  public void setOriginalNode(Node originalNode) throws Exception{
    originalNodeLocation = NodeLocation.make(originalNode);
  }

  public Node getNode() {
    return NodeLocation.getNodeByLocation(viewNodeLocation);
  }

  public void setNode(Node viewNode) {
    viewNodeLocation = NodeLocation.make(viewNode);
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    try {
      String nodeType = getOriginalNode().getPrimaryNodeType().getName();
      if(templateService.isManagedNodeType(nodeType))
        return templateService.getTemplatePathByUser(false, nodeType, userName) ;
    } catch (PathNotFoundException e) {
      Utils.createPopupMessage(this, TEMPLATE_NOT_SUPPORT, null, ApplicationMessage.ERROR);
    } catch (Exception e) {
      Utils.createPopupMessage(this, TEMPLATE_NOT_SUPPORT, null, ApplicationMessage.ERROR);
    }
    return null ;
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    try {
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
      return new JCRResourceResolver(workspace);
    } catch (Exception e) {
      return null;
    }
  }

  public String getRepositoryName() {
    try {
      return getRepository();
    }catch (Exception ex) {
      return null;
    }
  }

  public String getTemplatePath() {
    return null;
  }

  public String getNodeType() {
    return null;
  }

  public boolean isNodeTypeSupported() {
    return false;
  }

  public UIComponent getCommentComponent() {
    return null;
  }

  public UIComponent getRemoveAttach() {
    return null;
  }

  public UIComponent getRemoveComment() {
    return null;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getUIComponent(mimeType, this);
  }

}
