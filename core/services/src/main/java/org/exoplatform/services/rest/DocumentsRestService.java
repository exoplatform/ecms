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

/**
 * @author Ayoub Zayati
 */
@Path("/documents")
@Api(tags = "/documents", value = "/documents", description = "Managing documents")
public class DocumentsRestService implements ResourceContainer {

  private RepositoryService   repositoryService;

  private static final String COLLABORATION = "collaboration";

  private static final int    DEFAULT_LIMIT = 20;

  private enum DocumentType {
    ALL,
    FAVORITE,
    RECENT,
    SHARED,
  }

  public DocumentsRestService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @GET
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets a specific type of documents, from a specific folder or by quey",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns a specific type of documents, from a specific folder or by quey")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getDocuments(@ApiParam(value = "Folder path from which documents are retrieved", required = false) @QueryParam("folder") String folderPath,
                               @ApiParam(value = "Query from which documents are retrieved", required = false) @QueryParam("query") String query,
                               @ApiParam(value = "type", required = false, defaultValue = "all") @QueryParam("type") String type,
                               @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    DocumentType documentType;
    try {
      documentType = DocumentType.valueOf(type.toUpperCase());
    } catch (Exception e) {
      documentType = DocumentType.RECENT;
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    if (query == null) {
      if (folderPath == null) {
        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
        Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
        Node privateUserNode = (Node) userNode.getNode(Utils.PRIVATE);
        Node documentsUserNode = (Node) privateUserNode.getNode(NodetypeConstant.DOCUMENTS);
        switch (documentType) {
          case FAVORITE: {
            if (privateUserNode.hasNode(NodetypeConstant.FAVORITE)) {
              folderPath = ((Node) privateUserNode.getNode(NodetypeConstant.FAVORITE)).getPath();
            }
            break;
          }
          case RECENT: {
            // TODO elastic search for all user documents
            folderPath = documentsUserNode.getPath();
            break;
          }
          case SHARED: {
            if (documentsUserNode.hasNode(NodetypeConstant.SHARED)) {
              folderPath = ((Node) documentsUserNode.getNode(NodetypeConstant.SHARED)).getPath();
            }
            break;
          }
          default:
            break;
        }
      }
      if (folderPath != null) {
        query = "select * from nt:base where jcr:path like '" + folderPath + "/%' and (exo:primaryType = 'nt:file' or jcr:primaryType = 'nt:file') order by exo:dateModified DESC";
      }
    }
    List<Document> documents = new ArrayList<Document>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    if (query != null) {
      Session session = sessionProvider.getSession(COLLABORATION, manageableRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      QueryImpl documentsQuery = (QueryImpl) queryManager.createQuery(query, Query.SQL);
      documentsQuery.setLimit(limit);
      QueryResult queryResult = documentsQuery.execute();
      NodeIterator documentsIterator = queryResult.getNodes();
      while (documentsIterator.hasNext()) {
        Node documentNode = documentsIterator.nextNode();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
        Document document = new Document(documentNode.getName(),
                                         Utils.getTitle(documentNode),
                                         documentNode.getPath(),
                                         Utils.getTitle(documentNode.getParent()),
                                         Utils.getFileType(documentNode),
                                         simpleDateFormat.format(Utils.getDate(documentNode).getTime()));
        documents.add(document);
      }
      session.logout();
    }
    return Response.ok(documents).build();
  }
}
