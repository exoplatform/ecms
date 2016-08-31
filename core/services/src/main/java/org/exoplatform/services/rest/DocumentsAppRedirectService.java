package org.exoplatform.services.rest;

import java.io.IOException;
import java.net.URI;

import javax.annotation.security.RolesAllowed;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Web service to redirect to the Documents app to display the document with the given workspace and the given id
 */
@Path("/documents/view")
public class DocumentsAppRedirectService implements ResourceContainer {

  private static final Log LOG  = ExoLogger.getLogger(DocumentsAppRedirectService.class);

  private SessionProviderService sessionProviderService;
  private RepositoryService repositoryService;
  private DocumentService documentService;

  public DocumentsAppRedirectService(SessionProviderService sessionProviderService, RepositoryService repositoryService, DocumentService documentService) {
    this.sessionProviderService = sessionProviderService;
    this.repositoryService = repositoryService;
    this.documentService = documentService;
  }

  @GET
  @RolesAllowed("users")
  @Path("/{workspaceName}/{docId}")
  public Response redirect(@Context HttpServletRequest request,
                           @PathParam("workspaceName") String workspaceName,
                           @PathParam("docId") String docId) throws IOException {

    if(StringUtils.isEmpty(workspaceName) || StringUtils.isEmpty(docId)) {
      return Response.serverError().entity("Parameters workspaceName and docId are mandatory").build();
    }

    LOG.debug("Requesting Documents app redirection for doc with id {0}", docId);

    try {
      SessionProvider systemSessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ExtendedSession session = (ExtendedSession) systemSessionProvider.getSession(workspaceName,  repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(docId);
      if(node != null) {
        String linkInDocumentsApp = documentService.getLinkInDocumentsApp(node.getPath());
        URI redirectUrl = new URI(getURLPrefix(request) + linkInDocumentsApp);
        return Response.temporaryRedirect(redirectUrl).build();
      }
    } catch(ItemNotFoundException e) {
      LOG.error("Cannot get node " + docId + " : " + e.getMessage(), e);
      return Response.status(404).build();
    } catch(Exception e) {
      LOG.error("Cannot get node " + docId + " : " + e.getMessage(), e);
      return Response.serverError().build();
    }

    return Response.serverError().build();
  }

  protected String getURLPrefix(HttpServletRequest request) {
    StringBuilder result = new StringBuilder();
    result.append(request.getScheme())
            .append("://")
            .append(request.getServerName());

    int port = request.getServerPort();
    if (port != 80) {
      result.append(':')
              .append(port);
    }

    return result.toString();
  }

}
