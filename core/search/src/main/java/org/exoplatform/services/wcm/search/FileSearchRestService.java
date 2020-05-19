/**
 * 
 */
package org.exoplatform.services.wcm.search;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchFilter;
import org.exoplatform.commons.search.es.ElasticSearchFilterType;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.connector.FileSearchRestServiceConnector;

/**
 * @author Ayoub Zayati
 */
@Path("search/documents")
@Api(tags = "search/documents", value = "search/documents", description = "Managing search documents")
public class FileSearchRestService implements ResourceContainer {

  private FileSearchRestServiceConnector fileSearchRestServiceConnector;
  
  private NodeHierarchyCreator nodeHierarchyCreator; 

  private static final int DEFAULT_LIMIT = 20;

  public FileSearchRestService(FileSearchRestServiceConnector fileSearchRestServiceConnector, NodeHierarchyCreator nodeHierarchyCreator) {
    this.fileSearchRestServiceConnector = fileSearchRestServiceConnector;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
  }

  @GET
  @Path("/favorite")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Search favorite documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns favorite documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response searchFavoriteDocuments(@Context UriInfo uriInfo,
                                       @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    Node userPrivateNode = getUserPrivateNode();
    Collection<SearchResult> favoriteDocuments = new ArrayList<SearchResult>();
    if (userPrivateNode.hasNode(NodetypeConstant.FAVORITE)) {
      List<ElasticSearchFilter> favoriteFilters = new ArrayList<ElasticSearchFilter>();
      favoriteFilters.add(getFileTypesFilter());
      favoriteFilters.add(getPathsFilter(Arrays.asList(((Node) userPrivateNode.getNode(NodetypeConstant.FAVORITE)).getPath())));
      favoriteDocuments = fileSearchRestServiceConnector.filteredSearch(null, null, favoriteFilters, null, 0, limit, "date", "desc");
    }
    return Response.ok(favoriteDocuments).build();
  }

  @GET
  @Path("/shared")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets shared documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns shared documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response searchSharedDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    Node userPrivateNode = getUserPrivateNode();
    Node userDocumentsNode = (Node) userPrivateNode.getNode(NodetypeConstant.DOCUMENTS);
    Collection<SearchResult> sharedDocuments = new ArrayList<SearchResult>();
    if (userDocumentsNode.hasNode(NodetypeConstant.SHARED)) {
      List<ElasticSearchFilter> sharedFilters = new ArrayList<ElasticSearchFilter>();
      sharedFilters.add(getFileTypesFilter());
      sharedFilters.add(getPathsFilter(Arrays.asList(((Node) userDocumentsNode.getNode(NodetypeConstant.SHARED)).getPath())));
      sharedDocuments = fileSearchRestServiceConnector.filteredSearch(null, null, sharedFilters, null, 0, limit, "date", "desc");
    }
    return Response.ok(sharedDocuments).build();
  }

  @GET
  @Path("/recent")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets recent documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns recent documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response searchRecentDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    List<ElasticSearchFilter> recentFilters = new ArrayList<ElasticSearchFilter>();
    recentFilters.add(getRecentFilter());
    recentFilters.add(getFileTypesFilter());
    recentFilters.add(getPathsFilter(Arrays.asList(Utils.SPACES_NODE_PATH, getUserPrivateNode().getPath())));
    Collection<SearchResult> recentDocuments = fileSearchRestServiceConnector.filteredSearch(null, null, recentFilters, null, 0, limit, "date", "desc");
    return Response.ok(recentDocuments).build();
  }
  
  @GET
  @Path("/recentSpaces")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets recent spaces documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns recent spaces documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response searchRecentSpacesDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    List<ElasticSearchFilter> recentSpacesFilters = new ArrayList<ElasticSearchFilter>();
    recentSpacesFilters.add(getFileTypesFilter());
    recentSpacesFilters.add(getPathsFilter(Arrays.asList(Utils.SPACES_NODE_PATH)));
    Collection<SearchResult> recentSpacesDocuments = fileSearchRestServiceConnector.filteredSearch(null, null, recentSpacesFilters, null, 0, limit, "date", "desc");
    return Response.ok(recentSpacesDocuments).build();
  }
  
  private ElasticSearchFilter getFileTypesFilter() {
    StringBuilder fileTypesFilter = new StringBuilder();
    fileTypesFilter.append("{\n \"term\" : { \"fileType\" : \"application/pdf\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.ms-excel\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"text/plain\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/rtf\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.openxmlformats-officedocument.presentationml.presentation\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.ms-powerpoint\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.oasis.opendocument.text\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/msword\" }\n }");
    fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\" }\n }");
    return new ElasticSearchFilter(ElasticSearchFilterType.FILTER_CUSTOM, "fileType", fileTypesFilter.toString());
  }
  
  private ElasticSearchFilter getPathsFilter(List<String> paths) {
    StringBuilder pathsFilter = new StringBuilder();
    int count = 0;
    for (String path : paths) {
      pathsFilter.append("{\n \"regexp\" : { \"path\" : \"" + path + ".*\" }\n }");
      count ++;
      if (count < paths.size()) {
        pathsFilter.append(",");
      }
    }
    return new ElasticSearchFilter(ElasticSearchFilterType.FILTER_CUSTOM, "path", pathsFilter.toString());
  }
  
  private ElasticSearchFilter getRecentFilter() {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    StringBuilder recentFilter = new StringBuilder();
    recentFilter.append("{\n \"term\" : { \"author\" : \"" + userId + "\" }\n }");
    recentFilter.append(",{\n \"term\" : { \"lastModifier\" : \"" + userId + "\" }\n }");
    return new ElasticSearchFilter(ElasticSearchFilterType.FILTER_CUSTOM, "", recentFilter.toString());
  }
  
  private Node getUserPrivateNode() throws Exception {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    return (Node) userNode.getNode(Utils.PRIVATE);
  }
}
