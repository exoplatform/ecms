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

import com.google.gson.Gson;

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

/**
 * The Class SuggestionRESTService.
 */
@Path("/identities/suggestions")
public class SuggestionRESTService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log    LOG            = ExoLogger.getLogger(DocumentEditorsRESTService.class);

  /** The suggested size. */
  protected static final int    SUGGESTED_SIZE = 20;

  /** The Constant USER_TYPE. */
  protected static final String USER_TYPE      = "user";

  /** The Constant SPACE_TYPE. */
  protected static final String SPACE_TYPE     = "space";

  /** The Constant GROUP_TYPE. */
  protected static final String GROUP_TYPE     = "group";

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
  public SuggestionRESTService(IdentityManager identityManager, OrganizationService organization, SpaceService spaceService) {
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
      String json = new Gson().toJson(getSuggestions(name));
      return Response.status(Status.OK).entity("{\"suggestions\":" + json + "}").build();
    } catch (Exception e) {
      LOG.error("Cannot get suggestions with suggested name: {}, error: {}", name, e.getMessage());
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
  protected List<Suggestion> getSuggestions(String name) throws Exception {
    List<Suggestion> suggestions = getUsersSuggestions(name, SUGGESTED_SIZE);
    int remain = SUGGESTED_SIZE - suggestions.size();
    if (remain > 0) {
      suggestions.addAll(getGroupsSugesstions(name, remain));
    }

    Collections.sort(suggestions, new Comparator<Suggestion>() {
      public int compare(Suggestion s1, Suggestion s2) {
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
  protected List<Suggestion> getUsersSuggestions(String name, int count) throws Exception {
    List<Suggestion> suggestions = new ArrayList<>();
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
        suggestions.add(new Suggestion(userName, fullName, USER_TYPE, avatarUrl));
      }
    }
    return suggestions;
  }

  /**
   * Gets the groups sugesstions.
   *
   * @param name the name
   * @param count the count
   * @return the groups sugesstions
   * @throws Exception the exception
   */
  protected List<Suggestion> getGroupsSugesstions(String name, int count) throws Exception {
    List<Suggestion> suggestions = new ArrayList<>();
    ListAccess<Group> groupsAccess = organization.getGroupHandler().findGroupsByKeyword(name);
    int size = groupsAccess.getSize() >= count ? count : groupsAccess.getSize();
    if (size > 0) {
      Group[] groups = groupsAccess.load(0, size);
      for (Group group : groups) {
        Space space = spaceService.getSpaceByGroupId(group.getId());
        if (space != null) {
          String avatarUrl = space.getAvatarUrl() != null ? space.getAvatarUrl() : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
          suggestions.add(new Suggestion(space.getGroupId(), space.getDisplayName(), SPACE_TYPE, avatarUrl));
        } else {
          suggestions.add(new Suggestion(group.getId(), group.getLabel(), GROUP_TYPE, LinkProvider.SPACE_DEFAULT_AVATAR_URL));
        }
      }
    }
    return suggestions;
  }

  /**
   * The Class Suggestion.
   */
  public static class Suggestion {

    /** The name. */
    private String name;

    /** The displayName. */
    private String displayName;

    /** The type. */
    private String type;

    /** The avatar url. */
    private String avatarUrl;

    /**
     * Instantiates a new suggestion.
     *
     * @param name the name
     * @param displayName the display name
     * @param type the type
     * @param avatarUrl the avatar url
     */
    public Suggestion(String name, String displayName, String type, String avatarUrl) {
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
