/**
 * 
 */
package org.exoplatform.services.wcm.search;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.CollectionUtils;
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

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.rest.api.RestUtils;

/**
 * @author Ayoub Zayati
 */
@Path("search/documents")
@Tag(name = "search/documents", description = "Managing search documents")
public class FileSearchRestService implements ResourceContainer {

  private FileSearchServiceConnector fileSearchServiceConnector;
  
  private NodeHierarchyCreator nodeHierarchyCreator;
  private final IdentityManager     identityManager;

  private final FavoriteService     favoriteService;

  private final TagService     tagService;

  private static final int DEFAULT_LIMIT = 20;

  public FileSearchRestService(FileSearchServiceConnector fileSearchServiceConnector, NodeHierarchyCreator nodeHierarchyCreator, IdentityManager  identityManager, FavoriteService  favoriteService, TagService tagService) {
    this.fileSearchServiceConnector = fileSearchServiceConnector;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.identityManager = identityManager;
    this.favoriteService = favoriteService;
    this.tagService = tagService;
  }

  @GET
  @Path("/recent")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Gets recent documents",
          method = "GET",
          description = "This returns recent documents")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public Response searchRecentDocuments(@Parameter(description = "Query string") @QueryParam("q") String query,
                                        @Parameter(description = "My work") @Schema(defaultValue = "false") @QueryParam("myWork") boolean myWork,
                                        @Parameter(description = "Sort field") @Schema(defaultValue = "date") @QueryParam("sort") String sortField,
                                        @Parameter(description = "Sort direction") @Schema(defaultValue = "desc") @QueryParam("direction") String sortDirection,
                                        @Parameter(description = "Limit") @Schema(defaultValue = "20") @QueryParam("limit") int limit,
                                        @Parameter(description = "favorites") @Schema(defaultValue = "false") @QueryParam("favorites") boolean favorites,
                                        @Parameter(description = "Tag names list") @Schema(defaultValue = "false") @QueryParam("tags") List<String> tagNames
                                        ) throws Exception {

    if (StringUtils.isBlank(query) && !favorites && CollectionUtils.isEmpty(tagNames)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("'query' parameter is mandatory").build();
    }
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
    String userid = String.valueOf(RestUtils.getCurrentUserIdentityId());
    if (myWork) {
      recentFilters.add(filterMyWorkingDocuments());
    }
    Map<String, List<String>> metadataFilters = new HashMap<>();
    if (favorites) {
      StringBuilder recentFilter = new StringBuilder();
      metadataFilters.put(FavoriteService.METADATA_TYPE.getName(), Collections.singletonList(userid));
      String favoriteQuery = buildFavoriteQueryStatement(metadataFilters.get(FavoriteService.METADATA_TYPE.getName()));
      recentFilter.append(favoriteQuery);
      recentFilters.add(new ElasticSearchFilter(ElasticSearchFilterType.FILTER_MATADATAS, "", recentFilter.toString()));
    }
    if (!CollectionUtils.isEmpty(tagNames)) {
      StringBuilder recentFilter = new StringBuilder();
      metadataFilters.put(TagService.METADATA_TYPE.getName(), tagNames);
      String tagsQuery = buildTagsQueryStatement(metadataFilters.get(TagService.METADATA_TYPE.getName()));
      recentFilter.append(tagsQuery);
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
  @Operation(
          summary = "Gets recent spaces documents",
          method = "GET",
          description = "This returns recent spaces documents")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public Response searchRecentSpacesDocuments(@Parameter(description = "Limit") @Schema(defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
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

  private String buildFavoriteQueryStatement(List<String> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    return new StringBuilder().append("{\"terms\":{")
            .append("\"metadatas.favorites.metadataName.keyword\": [\"")
            .append(StringUtils.join(values, "\",\""))
            .append("\"]}},")
            .toString();
  }
  private String buildTagsQueryStatement(List<String> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    List<String> tagsQueryParts = values.stream()
            .map(value -> new StringBuilder().append("{\"term\": {\n")
                    .append("            \"metadatas.tags.metadataName.keyword\": {\n")
                    .append("              \"value\": \"")
                    .append(value)
                    .append("\",\n")
                    .append("              \"case_insensitive\":true\n")
                    .append("            }\n")
                    .append("          }}")
                    .toString())
            .collect(Collectors.toList());
    return new StringBuilder()
            .append(StringUtils.join(tagsQueryParts, ","))
            .append("  ,\n")
            .toString();
  }

}
