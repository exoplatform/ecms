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
package org.exoplatform.wcm.webui.pcv;

import java.util.Arrays;

import javax.jcr.Node;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/*
 * Created by The eXo Platform SAS
 * Author : Anh Do Ngoc
 * anh.do@exoplatform.com
 * Sep 24, 2008
 */

/**
 * The Class UIContentViewer.
 */
@ComponentConfig(lifecycle = Lifecycle.class)
public class UIPCVPresentation extends UIBaseNodePresentation {

  /** The content node. */
  private NodeLocation contentNodeLocation;

  /** The orginal node. */
  private NodeLocation originalNodeLocation;

  /** The resource resolver. */
  private JCRResourceResolver resourceResolver;

  /** The repository. */
  private String              repository;

  /** The workspace. */
  private String              workspace;

  /** The Constant CONTENT_NOT_FOUND_EXC. */
  public final static String  CONTENT_NOT_FOUND_EXC = "UIMessageBoard.msg.content-not-found";

  /** The Constant ACCESS_CONTROL_EXC. */
  public final static String  ACCESS_CONTROL_EXC    = "UIMessageBoard.msg.access-control-exc";

  /** The Constant CONTENT_UNSUPPORT_EXC. */
  public final static String  CONTENT_UNSUPPORT_EXC = "UIMessageBoard.msg.content-unsupport-exc";

  /** Content can't be printed. */
  public final static String  CONTENT_NOT_PRINTED = "UIMessageBoard.msg.content-invisible";

  /** Content is obsolete. */
  public final static String OBSOLETE_CONTENT = "UIMessageBoard.msg.content-obsolete";

  /** The Constant PARAMETER_REGX. */
  public final static String  PARAMETER_REGX        = "(.*)/(.*)";

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getNode()
   */
  @Override
  public Node getNode() throws Exception {
    return Utils.getViewableNodeByComposer(contentNodeLocation.getRepository(),
                                           contentNodeLocation.getWorkspace(),
                                           contentNodeLocation.getPath());
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getOriginalNode()
   */
  @Override
  public Node getOriginalNode() throws Exception {
    return Utils.getViewableNodeByComposer(originalNodeLocation.getRepository(),
                                           originalNodeLocation.getWorkspace(),
                                           originalNodeLocation.getPath(),
                                           WCMComposer.BASE_VERSION);
  }

  /**
   * Sets the orginal node.
   *
   * @param orginalNode the new orginal node
   */
  public void setOriginalNode(Node originalNode) {
    originalNodeLocation = NodeLocation.make(originalNode);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getRepositoryName()
   */
  @Override
  public String getRepositoryName() throws Exception {
    return repository;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getWorkspaceName()
   */
  public String getWorkspaceName() throws Exception {
    return workspace;
  }

  public String getDownloadLink(Node node) throws Exception {
    return Utils.getDownloadLink(node);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getTemplatePath()
   */
  @Override
  public String getTemplatePath() throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.getTemplatePath(getOriginalNode(), false);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.portal.webui.portal.UIPortalComponent#getTemplate()
   */
  public String getTemplate() {
    try {
      return getTemplatePath();
    } catch (Exception e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  @Deprecated
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    try {
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
      resourceResolver = new JCRResourceResolver(workspace);
    } catch (Exception e) {
      Utils.createPopupMessage(this,
                               "UIMessageBoard.msg.get-template-resource",
                               null,
                               ApplicationMessage.ERROR);
    }
    return resourceResolver;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver() {
    try {
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
      resourceResolver = new JCRResourceResolver(workspace);
    } catch (Exception e) {
      Utils.createPopupMessage(this,
                               "UIMessageBoard.msg.get-template-resource",
                               null,
                               ApplicationMessage.ERROR);
    }
    return resourceResolver;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getNodeType()
   */
  public String getNodeType() throws Exception {
    return getOriginalNode().getPrimaryNodeType().getName();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#isNodeTypeSupported()
   */
  public boolean isNodeTypeSupported() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#setNode(javax.jcr.Node)
   */
  public void setNode(Node node) {
    contentNodeLocation = NodeLocation.make(node);
  }

  /**
   * Sets the repository.
   *
   * @param repository the new repository
   */
  public void setRepository(String repository) {
    this.repository = repository;
  }

  /**
   * Sets the workspace.
   *
   * @param workspace the new workspace
   */
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getCommentComponent()
   */
  public UIComponent getCommentComponent() {
    return null;
  }

  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null, "DocumentInfoRemoveAttach");
    ((AbstractActionComponent)uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPCVContainer.class}));
    return uicomponent;
  }

  public UIComponent getRemoveComment() throws Exception {
    return null;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getUIComponent(mimeType, this);
  }
}
