/*
 * Copyright (C) 2003-2021 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.document.service.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.entity.ActivityEntity;
import org.exoplatform.social.service.rest.api.models.SharedActivityRestIn;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

@Path("/document/activities")
public class ShareDocumentRestService implements ResourceContainer {

  private static final Log      LOG                       = ExoLogger.getLogger(ShareDocumentRestService.class.getName());


  private IShareDocumentService shareDocumentService;
  
  private ActivityManager activityManager;

  public ShareDocumentRestService(ActivityManager activityManager,
                                  IShareDocumentService shareDocumentService) throws Exception {
    this.activityManager = activityManager;
    this.shareDocumentService = shareDocumentService;
  }

  @POST
  @Path("{id}/share")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Shares a specific document activity to specific spaces",
      httpMethod = "POST",
      response = Response.class,
      notes = "This shares the given activity to the target spaces if the authenticated user has permissions to post to the target spaces")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response shareDocumentActivityOnSpaces(@Context UriInfo uriInfo,
                                                @ApiParam(value = "Activity id", required = true) @PathParam("id") String activityId,
                                                @ApiParam(value = "Asking for a full representation of a specific subresource, ex: comments or likes", required = false) @QueryParam("expand") String expand,
                                                @ApiParam(value = "Share target spaces", required = true) SharedActivityRestIn sharedActivityRestIn) throws Exception {
    if (activityManager.getActivity(activityId) == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    if (sharedActivityRestIn == null || sharedActivityRestIn.getTargetSpaces() == null
        || sharedActivityRestIn.getTargetSpaces().isEmpty() || sharedActivityRestIn.getType() == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    IdentityManager identityManager = ExoContainerContext.getService(IdentityManager.class);
    Identity currentUser = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser);

    List<ActivityEntity> sharedActivitiesEntities = new ArrayList<ActivityEntity>();
    for (String targetSpaceName : sharedActivityRestIn.getTargetSpaces()) {
      ExoSocialActivity sharedActivity = shareDocumentService.shareDocumentActivityToSpace(targetSpaceName, activityId, sharedActivityRestIn.getTitle(), sharedActivityRestIn.getType());
      if (sharedActivity != null) {
        ActivityEntity sharedActivityEntity = EntityBuilder.buildEntityFromActivity(sharedActivity, currentUser, uriInfo.getPath(), expand);
        sharedActivitiesEntities.add(sharedActivityEntity);
        LOG.info("service=activity operation=share parameters=\"activity_type:{},activity_id:{},space_name:{},user_id:{}\"",
                     sharedActivity.getType(),
                     sharedActivity.getId(),
                     targetSpaceName,
                     sharedActivity.getPosterId());
      }
    }
    return EntityBuilder.getResponse(sharedActivitiesEntities, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
}
