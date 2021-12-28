/**
 * 
 */
package org.exoplatform.services.wcm.search;

import java.util.*;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchFilter;
import org.exoplatform.commons.search.es.ElasticSearchFilterType;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;

import io.swagger.annotations.*;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.favorite.FavoriteService;

/**
 * @author Ayoub Zayati
 */
@Path("search/documents")
@Api(tags = "search/documents", value = "search/documents", description = "Managing search documents")
public class FileSearchRestService implements ResourceContainer {

  private FileSearchServiceConnector fileSearchServiceConnector;
  
  private NodeHierarchyCreator nodeHierarchyCreator;

  private static final int DEFAULT_LIMIT = 20;

  public FileSearchRestService(FileSearchServiceConnector fileSearchServiceConnector, NodeHierarchyCreator nodeHierarchyCreator) {
    this.fileSearchServiceConnector = fileSearchServiceConnector;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
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
  public Response searchRecentDocuments(@ApiParam(value = "Query string", required = false) @QueryParam("q") String query,
                                        @ApiParam(value = "My work", required = false, defaultValue = "false") @QueryParam("myWork") boolean myWork,
                                        @ApiParam(value = "Sort field", required = false, defaultValue = "date") @QueryParam("sort") String sortField,
                                        @ApiParam(value = "Sort direction", required = false, defaultValue = "desc") @QueryParam("direction") String sortDirection,
                                        @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit,
                                        @ApiParam(value = "favorites", required = false, defaultValue = "false") @QueryParam("favorites") boolean favorites) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    if (StringUtils.isBlank(sortField)) {
      sortField = "date";
    }
    if (StringUtils.isBlank(sortDirection)) {
      sortDirection = "desc";
    }
    List<ElasticSearchFilter> recentFilters = new ArrayList<>();
    if (myWork) {
      recentFilters.add(filterMyWorkingDocuments());
    }
    if (favorites) {
      Map<String, List<String>> metadataFilters = buildMetadataFilter();
      String metadataQuery = buildMetadataQueryStatement(metadataFilters);
      StringBuilder recentFilter = new StringBuilder();
      recentFilter.append(metadataQuery);
      recentFilters.add(new ElasticSearchFilter(ElasticSearchFilterType.FILTER_MATADATAS, "", recentFilter.toString()));
    }
    recentFilters.add(getFileTypesFilter(myWork));
    UserACL userACL = PortalContainer.getInstance().getComponentInstanceOfType(UserACL.class);
    if (!userACL.isSuperUser() && !userACL.isUserInGroup(userACL.getAdminGroups())) {
      recentFilters.add(getPathsFilter(Arrays.asList(Utils.SPACES_NODE_PATH, getUserPrivateNode().getPath())));
    }
    if (StringUtils.isNotBlank(query)) {
      query = query.replace("#", " ")
                   .replace("$", " ")
                   .replace("_", " ")
                   .replace(".", " ");
    }
    Collection<SearchResult> recentDocuments = fileSearchServiceConnector.filteredSearch(null, query, recentFilters, null, 0, limit, sortField, sortDirection);
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
    Collection<SearchResult> recentSpacesDocuments = fileSearchServiceConnector.filteredSearch(null, null, recentSpacesFilters, null, 0, limit, "date", "desc");
    return Response.ok(recentSpacesDocuments).build();
  }

  private ElasticSearchFilter getFileTypesFilter(boolean myWork) {
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
    if (!myWork) {
      fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"image/jpeg\" }\n }");
      fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"image/png\" }\n }");
      fileTypesFilter.append(",{\n \"term\" : { \"fileType\" : \"image/gif\" }\n }");
    }
    return new ElasticSearchFilter(ElasticSearchFilterType.FILTER_CUSTOM, "fileType", fileTypesFilter.toString());
  }

  private ElasticSearchFilter getFileTypesFilter() {
    return getFileTypesFilter(false);
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

  private ElasticSearchFilter filterMyWorkingDocuments() {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    StringBuilder recentFilter = new StringBuilder();
    recentFilter.append("\"should\" : {\n \"term\" : { \"author\" : \"" + userId + "\" }\n },\n");
    recentFilter.append("\"must\" : {\n \"term\" : { \"lastModifier\" : \"" + userId + "\" }\n }");
    return new ElasticSearchFilter(ElasticSearchFilterType.FILTER_MY_WORK_DOCS, "", recentFilter.toString());
  }

  private Node getUserPrivateNode() throws Exception {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
      return (Node) userNode.getNode(Utils.PRIVATE);
    } finally {
      sessionProvider.close();
    }
  }

  private String buildMetadataQueryStatement(Map<String, List<String>> metadataFilters) {
    StringBuilder metadataQuerySB = new StringBuilder();
    Set<Map.Entry<String, List<String>>> metadataFilterEntries = metadataFilters.entrySet();
    for (Map.Entry<String, List<String>> metadataFilterEntry : metadataFilterEntries) {
      metadataQuerySB.append("{\"terms\":{\"metadatas.")
              .append(metadataFilterEntry.getKey())
              .append(".metadataName.keyword")
              .append("\": [\"")
              .append(org.apache.commons.lang.StringUtils.join(metadataFilterEntry.getValue(), "\",\""))
              .append("\"]}},");
    }
    return metadataQuerySB.toString();
  }

  private Map<String, List<String>> buildMetadataFilter() {
    Identity viewerIdentity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId());
    Map<String, List<String>> metadataFilters = new HashMap<>();
    metadataFilters.put(FavoriteService.METADATA_TYPE.getName(), Collections.singletonList(viewerIdentity.getId()));
    return metadataFilters;
  }

}
