package org.exoplatform.services.rest;

import java.io.IOException;
import java.net.URI;

import javax.annotation.security.RolesAllowed;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web service to redirect to the Documents app to display the document with the given workspace and the given id
 */
@Path("/documents/view")
public class DocumentsAppRedirectService implements ResourceContainer {

  private static final Log LOG  = ExoLogger.getLogger(DocumentsAppRedirectService.class);

  private RepositoryService repositoryService;
  private DocumentService documentService;

  public DocumentsAppRedirectService(RepositoryService repositoryService, DocumentService documentService) {
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
      return Response.serverError().entity("Parameters workspaceName and docId are mandatory").cacheControl(noStore()).build();
    }
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState == null || conversationState.getIdentity() == null) {
      return Response.status(Response.Status.UNAUTHORIZED).cacheControl(noStore()).build();
    }

    LOG.debug("Requesting Documents app redirection for doc with id {}", docId);

    // The node is read as the caller, so a document they cannot read is neither located nor redirected to
    SessionProvider sessionProvider = new SessionProvider(new ConversationState(conversationState.getIdentity()));
    try {
      ExtendedSession session = (ExtendedSession) sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(docId);
      if(node != null) {
        String linkInDocumentsApp = documentService.getLinkInDocumentsApp(node.getPath());
        URI redirectUrl = new URI(getURLPrefix(request) + linkInDocumentsApp);
        return Response.temporaryRedirect(redirectUrl).cacheControl(noStore()).build();
      }
    } catch (AccessDeniedException | ItemNotFoundException | NoSuchWorkspaceException e) {
      LOG.debug("Document {} not found or not readable by the current user", docId, e);
      return Response.status(Response.Status.NOT_FOUND).cacheControl(noStore()).build();
    } catch(Exception e) {
      LOG.error("Cannot get node {}", docId, e);
      return Response.serverError().cacheControl(noStore()).build();
    } finally {
      sessionProvider.close();
    }

    return Response.status(Response.Status.NOT_FOUND).cacheControl(noStore()).build();
  }

  private static CacheControl noStore() {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate(true);
    cacheControl.setNoStore(true);
    return cacheControl;
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
