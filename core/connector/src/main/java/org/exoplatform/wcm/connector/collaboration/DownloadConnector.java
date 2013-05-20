package org.exoplatform.wcm.connector.collaboration;

import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Enable downloading the content of _nt\:file_.
 * 
 * @copyright  eXo Platform SEA
 * 
 * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.DownloadConnector
 */
@Path("/contents/")
public class DownloadConnector implements ResourceContainer{

  /**
   * Return to browser a stream got from _jcr\:content_/_jcr\:data_ for downloading the content of the node.
   * 
   * @param workspace The workspace where to store the document node
   * @param path The path to the document node
   * @param version The version name
   * @return the instance of javax.ws.rs.core.Response
   * @throws Exception
   * @Objective Return to browser a stream for downloading content of a node. The stream is got from _jcr\:content_/_jcr\:data_
   * @Author Nguyen The Vinh from ECM of eXoPlatform
   *              nguyenthevinhbk@gmail.com
   *              dongpd@exoplatform.com
   *              
   * @anchor CONTref.Devref.PublicRestAPIs.DownloadConnector.download
   */
  @GET
  @Path("/download/{workspace}/{path:.*}/")
  public Response download(@PathParam("workspace") String workspace,
                           @PathParam("path") String path,
                           @QueryParam("version") String version) throws Exception {
    InputStream is = null;
    String mimeType = MediaType.TEXT_XML;
    Node node = null;
    String fileName = null;
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    try {
      node = (Node) session.getItem(path);
      fileName = Text.unescapeIllegalJcrChars(node.getName());

      // In case version is specified, get file from version history
      if (version != null) {
        node = node.getVersionHistory().getVersion(version).getNode("jcr:frozenNode");
      }

      Node jrcNode = node.getNode("jcr:content");
      is = jrcNode.getProperty("jcr:data").getStream();
    }catch (PathNotFoundException pne) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    } catch (AccessDeniedException ade) {
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    }
    if (node.getPrimaryNodeType().getName().equals("nt:file")) {
      mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    }
    return Response.ok(is, mimeType)
          .header("Content-Disposition","attachment; filename=\"" + fileName+"\"")
            .build();
  }
}
