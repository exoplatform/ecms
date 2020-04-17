/**
 * 
 */
package org.exoplatform.services.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.entity.Document;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * @author Ayoub Zayati
 */
@Path("/documents")
@Api(tags = "/documents", value = "/documents", description = "Managing documents")
public class DocumentsRestService implements ResourceContainer {

  private LinkManager         linkManager;

  private RepositoryService   repositoryService;

  private SpaceService        spaceService;

  private static final String COLLABORATION         = "collaboration";

  private static final int    DEFAULT_LIMIT         = 20;

  private static final String SEPARATOR             = "/";

  private static final String USER_SPACES_NODE_PATH = "/Groups/spaces";

  private enum DocumentType {
    FAVORITE,
    RECENT,
    SHARED,
  }

  public DocumentsRestService(RepositoryService repositoryService, LinkManager linkManager, SpaceService spaceService) {
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
    this.spaceService = spaceService;
  }

  @GET
  @Path("/query")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets documents by quey",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns documents by quey")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response getDocumentsByQuery(@ApiParam(value = "Query from which documents to be retrieved", required = true) @QueryParam("query") String query,
                                      @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (query == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    return Response.ok(getDocuments(query, limit)).build();
  }

  @GET
  @Path("/folder")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets documents by folder",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns documents by folder")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response getDocumentsByFolder(@ApiParam(value = "Folder from which documents to be retrieved", required = true) @QueryParam("folder") String folder,
                                       @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (folder == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    String query = "select * from nt:base where jcr:path like '" + folder
        + "/%' and (exo:primaryType = 'nt:file' or jcr:primaryType = 'nt:file') order by exo:dateModified DESC";
    return Response.ok(getDocuments(query, limit)).build();
  }

  @GET
  @Path("/type")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets documents by type",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns documents by type")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getDocumentsByType(@ApiParam(value = "Type of documents to be retrieved", required = true, defaultValue = "all") @QueryParam("type") String type,
                                     @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (type == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    DocumentType documentType;
    try {
      documentType = DocumentType.valueOf(type.toUpperCase());
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    Node userPrivateNode = (Node) userNode.getNode(Utils.PRIVATE);
    Node userDocumentsNode = (Node) userPrivateNode.getNode(NodetypeConstant.DOCUMENTS);
    String folder = null;
    switch (documentType) {
    case FAVORITE: {
      if (userPrivateNode.hasNode(NodetypeConstant.FAVORITE)) {
        folder = ((Node) userPrivateNode.getNode(NodetypeConstant.FAVORITE)).getPath();
      }
      break;
    }
    case RECENT: {
      // TODO elastic search for all user documents
      folder = userDocumentsNode.getPath();
      // folder = USER_SPACES_NODE_PATH;
      break;
    }
    case SHARED: {
      if (userDocumentsNode.hasNode(NodetypeConstant.SHARED)) {
        folder = ((Node) userDocumentsNode.getNode(NodetypeConstant.SHARED)).getPath();
      }
      break;
    }
    default:
      break;
    }
    String query = folder != null ? "select * from nt:base where jcr:path like '" + folder
        + "/%' and (exo:primaryType = 'nt:file' or jcr:primaryType = 'nt:file') order by exo:dateModified DESC" : null;
    return Response.ok(getDocuments(query, limit)).build();
  }

  private List<Document> getDocuments(String query, long limit) throws Exception {
    List<Document> documents = new ArrayList<Document>();
    if (query != null) {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(COLLABORATION, manageableRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      QueryImpl documentsQuery = (QueryImpl) queryManager.createQuery(query, Query.SQL);
      documentsQuery.setLimit(limit);
      QueryResult queryResult = documentsQuery.execute();
      NodeIterator documentsIterator = queryResult.getNodes();
      while (documentsIterator.hasNext()) {
        Node documentNode = documentsIterator.nextNode();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
        if (documentNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
          documentNode = linkManager.getTarget(documentNode);
        }
        String documentNodePath = documentNode.getPath();
        Document document = new Document(documentNode.getUUID(),
                                         Utils.getTitle(documentNode),
                                         documentNodePath,
                                         documentNodePath.contains(Utils.PRIVATE) ? Utils.PRIVATE : getSpace(documentNodePath),
                                         Utils.getFileType(documentNode),
                                         simpleDateFormat.format(Utils.getDate(documentNode).getTime()));
        documents.add(document);
      }
      session.logout();
    }
    return documents;
  }

  private String getSpace(String nodePath) {
    if (nodePath.startsWith(USER_SPACES_NODE_PATH)) {
      String[] splittedNodePath = nodePath.split(SEPARATOR);
      if (splittedNodePath.length > 3) {
        Space space = spaceService.getSpaceByPrettyName(splittedNodePath[3]);
        if (space != null) {
          return space.getDisplayName();
        }
      }
    }
    return "";
  }
}
