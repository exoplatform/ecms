package org.exoplatform.wcm.connector.collaboration;

import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

@Path("/contents/")
public class DownloadConnector implements ResourceContainer{
  
  /**
   * 
   * @param workspace : node's work space 
   * @param path      : node's full path from root
   * @return
   * @throws Exception
   * @Objective : Return to browser a stream for download content of a node. Stream got from jcr:content/jcr:data
   * @Author    : Nguyen The Vinh from ECM of eXoPlatform
   *              nguyenthevinhbk@gmail.com
   */
  @GET
  @Path("/download/{workspace}/{path:.*}/")
  public Response download(@PathParam("workspace") String workspace, @PathParam("path") String path) throws Exception {
    InputStream is=null;
    Node node = null;
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
      Node jrcNode = node.getNode("jcr:content");
      is = jrcNode.getProperty("jcr:data").getStream();
    }catch (PathNotFoundException pne) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    } catch (AccessDeniedException ade) {
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    }

    return Response.ok(is, MediaType.TEXT_XML)
          .header("Content-Disposition","attachment; filename=" + Text.unescapeIllegalJcrChars(node.getName()))
            .build();
  }
}
