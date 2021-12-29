package org.exoplatform.wcm.connector.collaboration;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

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
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Enables downloading the content of _nt\:file_.
 *
 * @LevelAPI Provisional
 *
 * @anchor DownloadConnector
 */
@Path("/contents/")
public class DownloadConnector implements ResourceContainer{

  private static final Log LOG = ExoLogger.getLogger(DownloadConnector.class.getName());
  /**
   * Returns to browser a stream got from _jcr\:content_/_jcr\:data_ for downloading the content of the node.
   *
   * @param workspace The workspace where stores the document node.
   * @param path The path to the document node.
   * @param version The version name.
   * @return the instance of javax.ws.rs.core.Response.
   * @throws Exception The exception
   *
   * @anchor DownloadConnector.download
   */
  @GET
  @Path("/download/{workspace}/{path:.*}")
  public Response download(@PathParam("workspace") String workspace,
                           @PathParam("path") String path,
                           @QueryParam("version") String version) throws Exception {
    InputStream is = null;
    String mimeType = MediaType.TEXT_XML;
    Node node = null;
    String fileName = null;
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    try {
      node = (Node) session.getItem(path);
      fileName = node.getName();
      if (node.hasProperty("exo:title")){
        fileName = node.getProperty("exo:title").getString();
      }
      // decode the fileName in case the fileName is already encoded, for the old uploaded files.
      try {
        fileName = URLDecoder.decode(fileName, "UTF-8");
      }catch (Exception e){
        LOG.debug("The fileName is already decoded");
      }
      fileName = Text.unescapeIllegalJcrChars(fileName);
      fileName = URLEncoder.encode(fileName, "utf-8").replace("+", "%20");
      // In case version is specified, get file from version history
      if (version != null) {
        node = node.getVersionHistory().getVersion(version).getNode("jcr:frozenNode");
      }

      Node jrcNode = node.getNode("jcr:content");
      is = jrcNode.getProperty("jcr:data").getStream();
    }catch (PathNotFoundException pne) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    } catch (AccessDeniedException ade) {
      LOG.warn("You have not enough permissions on file: {}", fileName);
      return Response.status(HTTPStatus.NOT_FOUND).build();
    }
    if (node.isNodeType("nt:file") || (node.isNodeType("nt:frozenNode")) && node.getProperty("jcr:frozenPrimaryType").getValue().getString().equals("nt:file")) {
      mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    }
    return Response.ok(is, mimeType)
          .header("Content-Disposition","attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName)
            .build();
  }
}
