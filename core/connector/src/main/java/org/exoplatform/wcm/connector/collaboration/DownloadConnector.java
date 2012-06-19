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
import org.exoplatform.doc.gen.annotation.LevelAPI;
import org.exoplatform.doc.gen.annotation.LevelAPI.LevelType;

/**
 * DownloadConnector
 *
 * Enable downloading the content of nt:file.
 * 
 * See methods for more api details.
 * GET: /contents//download/{workspace}/{path:.*}/ 
 * 
 * @copyright  eXo Platform SEA
 * 
 * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.DownloadConnector
 */
@Path("/contents/")
@LevelAPI(LevelType.PLATFORM)
public class DownloadConnector implements ResourceContainer{
  
  /**
   * Return to browser a stream got from jcr:content/jcr:data for downloading the content of the node.
   * 
   * @param workspace : the workspace where stores the document node  
   * @param path      : the path of the document node 
   * @return
   * @throws Exception
   * @Objective : Return to browser a stream for download content of a node. Stream got from jcr:content/jcr:data
   * @Author    : Nguyen The Vinh from ECM of eXoPlatform
   *              nguyenthevinhbk@gmail.com
   *              
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.DownloadConnector.download
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
