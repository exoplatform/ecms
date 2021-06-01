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
package org.exoplatform.wcm.ext.component.document.service.rest;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.groovyscript.text.BindingContext;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

/**
 *
 */
@Path("/contentviewer")
public class ContentViewerRESTService implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(ContentViewerRESTService.class.getName());

  private WebAppController webAppController;

  private RepositoryService repositoryService;

  private LinkManager linkManager;

  public ContentViewerRESTService(WebAppController webAppController, RepositoryService repositoryService, LinkManager linkManager) throws Exception {
    this.webAppController = webAppController;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
  }

  /**
   * Returns a pdf file for a PDF document.
   *
   * @param repoName The repository name.
   * @param workspaceName   The workspace name.
   * @param uuid     The identifier of the document.
   * @return Response inputstream.
   * @throws Exception The exception
   * @anchor PDFViewerRESTService.getPDFFile
   */
  @GET
  @Path("/{repoName}/{workspaceName}/{uuid}/")
    public Response getContent(@Context HttpServletRequest request,
                               @Context HttpServletResponse response,
                             @PathParam("repoName") String repoName,
                             @PathParam("workspaceName") String workspaceName,
                             @PathParam("uuid") String uuid) throws Exception {
    String content = null;
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = getSystemProvider().getSession(workspaceName, repository);
      Node contentNode = session.getNodeByUUID(uuid);

      if(contentNode != null && contentNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        contentNode = linkManager.getTarget(contentNode);
      }

      StringWriter writer = new StringWriter();

      UIDocViewer uiDocViewer = new UIDocViewer();
      uiDocViewer.docPath = contentNode.getPath();
      uiDocViewer.repository = repository.getConfiguration().getName();
      uiDocViewer.workspace = workspaceName;
      uiDocViewer.setOriginalNode(contentNode);
      uiDocViewer.setNode(contentNode);

      ControllerContext controllerContext = new ControllerContext(webAppController, webAppController.getRouter(), request, response, null);
      PortalApplication application = webAppController.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
      PortalRequestContext requestContext = new PortalRequestContext(application, controllerContext, org.exoplatform.portal.mop.SiteType.PORTAL.toString(), "", "", null);
      WebuiRequestContext.setCurrentInstance(requestContext);
      UIPortalApplication uiApplication = new UIPortalApplication();
      uiApplication.setCurrentSite(new UIPortal());
      requestContext.setUIApplication(uiApplication);
      requestContext.setWriter(writer);

      uiDocViewer.processRender(requestContext);

      content = writer.toString();

    } catch (Exception e) {
      LOG.error("Cannot render content of document " + repoName + "/" + workspaceName + "/" + uuid, e);
    }

    return Response.ok(content).build();
  }

  private SessionProvider getSystemProvider() {
    SessionProviderService service = WCMCoreUtils.getService(SessionProviderService.class);
    return service.getSystemSessionProvider(null);
  }

  public String getTemplate(Node contentNode) {
    if(contentNode == null) {
      return null;
    }
    TemplateService templateService = CommonsUtils.getService(TemplateService.class);
    String userName = ConversationState.getCurrent().getIdentity().getUserId();
    try {
      String nodeType = contentNode.getPrimaryNodeType().getName();
      if(templateService.isManagedNodeType(nodeType)) {
        return templateService.getTemplatePathByUser(false, nodeType, userName);
      }
    }catch (RepositoryException re){
      if (LOG.isDebugEnabled() || LOG.isWarnEnabled())
        LOG.error("Get template catch RepositoryException: ", re);
    }
    catch (Exception e) {
      LOG.warn(e.getMessage(), e);
    }

    return null;
  }

  public void processRender(String template, UIComponent uiComponent,  Writer writer) throws Exception {
    if(template == null) {
      throw new IllegalStateException();
    } else {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      ResourceResolver resolver = new JCRResourceResolver("dms-system");
      BindingContext bcontext = new BindingContext(resolver, writer);
      bcontext.put("_ctx", bcontext);
      bcontext.put("uicomponent", uiComponent);
      bcontext.put(uiComponent.getUIComponentName(), uiComponent);
      bcontext.put("locale", new Locale("en"));
      org.exoplatform.groovyscript.text.TemplateService service = container.getComponentInstanceOfType(org.exoplatform.groovyscript.text.TemplateService.class);
      service.merge(template, bcontext);
    }
  }

}
