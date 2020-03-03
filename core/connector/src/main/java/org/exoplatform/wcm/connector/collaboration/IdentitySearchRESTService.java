package org.exoplatform.wcm.connector.collaboration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class IdentitySearchRESTService.
 */
@Path("/identity/search")
public class IdentitySearchRESTService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log    LOG             = ExoLogger.getLogger(IdentitySearchRESTService.class);

  /** The max result size. */
  protected static final int    MAX_RESULT_SIZE = 20;

  /** The Constant USER_TYPE. */
  protected static final String USER_TYPE       = "user";

  /** The Constant SPACE_TYPE. */
  protected static final String SPACE_TYPE      = "space";

  /** The Constant GROUP_TYPE. */
  protected static final String GROUP_TYPE      = "group";

  /** The identity manager. */
  protected IdentityManager     identityManager;

  /** The organization service. */
  protected OrganizationService organization;

  /** The space service. */
  protected SpaceService        spaceService;

  /**
   * Instantiates a new SuggestionRESTService.
   *
   * @param identityManager the identity manager
   * @param organization the organization
   * @param spaceService the space service
   */
  public IdentitySearchRESTService(IdentityManager identityManager, OrganizationService organization, SpaceService spaceService) {
    this.identityManager = identityManager;
    this.organization = organization;
    this.spaceService = spaceService;
  }

  /**
   * Gets the suggestions.
   *
   * @param uriInfo the uri info
   * @param name the name
   * @return the suggestions
   */
  @GET
  @RolesAllowed("administrators")
  @Path("/{suggestedName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSuggestions(@Context UriInfo uriInfo, @PathParam("suggestedName") String name) {
    try {
      String json = new JsonGeneratorImpl().createJsonArray(getSuggestions(name)).toString();
      return Response.status(Status.OK).entity("{\"identities\":" + json + "}").build();
    } catch (Exception e) {
      LOG.error("Cannot get identities with suggested name: {}, error: {}", name, e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets the suggestions.
   *
   * @param name the name
   * @return the suggestions
   * @throws Exception the exception
   */
  protected List<SearchResult> getSuggestions(String name) throws Exception {
    List<SearchResult> suggestions = findUsers(name, MAX_RESULT_SIZE);
    int remain = MAX_RESULT_SIZE - suggestions.size();
    if (remain > 0) {
      suggestions.addAll(findGroups(name, remain));
    }

    Collections.sort(suggestions, new Comparator<SearchResult>() {
      public int compare(SearchResult s1, SearchResult s2) {
        return s1.getDisplayName().compareTo(s2.getDisplayName());
      }
    });

    return suggestions;
  }

  /**
   * Gets the users suggestions.
   *
   * @param name the name
   * @param count the count
   * @return the users suggestions
   * @throws Exception the exception
   */
  protected List<SearchResult> findUsers(String name, int count) throws Exception {
    List<SearchResult> results = new ArrayList<>();
    ProfileFilter identityFilter = new ProfileFilter();
    identityFilter.setName(name);
    ListAccess<Identity> identitiesList = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                                       identityFilter,
                                                                                       false);
    int size = identitiesList.getSize() >= count ? count : identitiesList.getSize();
    if (size > 0) {
      Identity[] identities = identitiesList.load(0, size);
      for (Identity id : identities) {
        Profile profile = id.getProfile();
        String fullName = profile.getFullName();
        String userName = (String) profile.getProperty(Profile.USERNAME);
        String avatarUrl = profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
        results.add(new SearchResult(userName, fullName, USER_TYPE, avatarUrl));
      }
    }
    return results;
  }

  /**
   * Gets the groups sugesstions.
   *
   * @param name the name
   * @param count the count
   * @return the groups sugesstions
   * @throws Exception the exception
   */
  protected List<SearchResult> findGroups(String name, int count) throws Exception {
    List<SearchResult> results = new ArrayList<>();
    ListAccess<Group> groupsAccess = organization.getGroupHandler().findGroupsByKeyword(name);
    int size = groupsAccess.getSize() >= count ? count : groupsAccess.getSize();
    if (size > 0) {
      Group[] groups = groupsAccess.load(0, size);
      for (Group group : groups) {
        Space space = spaceService.getSpaceByGroupId(group.getId());
        if (space != null) {
          String avatarUrl = space.getAvatarUrl() != null ? space.getAvatarUrl() : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
          results.add(new SearchResult(space.getGroupId(), space.getDisplayName(), SPACE_TYPE, avatarUrl));
        } else {
          results.add(new SearchResult(group.getId(), group.getLabel(), GROUP_TYPE, LinkProvider.SPACE_DEFAULT_AVATAR_URL));
        }
      }
    }
    return results;
  }

  /**
   * The Class Suggestion.
   */
  public static class SearchResult {

    /** The name. */
    private String name;

    /** The displayName. */
    private String displayName;

    /** The type. */
    private String type;

    /** The avatar url. */
    private String avatarUrl;

    /**
     * Instantiates a new SearchResult.
     *
     * @param name the name
     * @param displayName the display name
     * @param type the type
     * @param avatarUrl the avatar url
     */
    public SearchResult(String name, String displayName, String type, String avatarUrl) {
      this.name = name;
      this.displayName = displayName;
      this.type = type;
      this.avatarUrl = avatarUrl;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets the displayName.
     *
     * @return the displayName
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Sets the displayName.
     *
     * @param displayName the new displayName
     */
    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
      return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
      this.type = type;
    }

    /**
     * Gets the avatar url.
     *
     * @return the avatar url
     */
    public String getAvatarUrl() {
      return avatarUrl;
    }

    /**
     * Sets the avatar url.
     *
     * @param avatarUrl the new avatar url
     */
    public void setAvatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl;
    }
  }

}
