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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.entity.ActivityEntity;
import org.exoplatform.social.service.rest.api.models.SharedActivityRestIn;
import org.exoplatform.social.webui.activity.UILinkActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

@Path("/document/activities")
public class ShareDocumentRestService implements ResourceContainer {

  private static final Log      LOG             = ExoLogger.getLogger(ShareDocumentRestService.class.getName());

  private SpaceService          spaceService;

  private ActivityManager       activityManager;

  private IdentityManager       identityManager;

  private IShareDocumentService shareDocumentService;
  
  private static final String TEMPLATE_PARAMS_SEPARATOR = "|@|";

  public ShareDocumentRestService(SpaceService spaceService,
                                  ActivityManager activityManager,
                                  IdentityManager identityManager,
                                  IShareDocumentService shareDocumentService) throws Exception {
    this.spaceService = spaceService;
    this.activityManager = activityManager;
    this.shareDocumentService = shareDocumentService;
    this.identityManager = identityManager;
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
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (activityManager.getActivity(activityId) == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    if (sharedActivityRestIn == null || sharedActivityRestIn.getTargetSpaces() == null
        || sharedActivityRestIn.getTargetSpaces().isEmpty() || sharedActivityRestIn.getType() == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    Identity authenticatedUserIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser);
    List<ActivityEntity> sharedActivitiesEntities = new ArrayList<ActivityEntity>();
    for (String targetSpaceName : sharedActivityRestIn.getTargetSpaces()) {
      Space targetSpace = spaceService.getSpaceByPrettyName(targetSpaceName);
      if (SpaceUtils.isSpaceManagerOrSuperManager(authenticatedUser, targetSpace.getGroupId())
          || (spaceService.isMember(targetSpace, authenticatedUser) && SpaceUtils.isRedactor(authenticatedUser,
                                                                                             targetSpace.getGroupId()))) {
        Map<String, String> originalActivityTemplateParams = activityManager.getActivity(activityId).getTemplateParams();
        String[] originalActivityFilesRepositories = getParameterValues(originalActivityTemplateParams, UIDocActivity.REPOSITORY);
        String[] originalActivityFilesWorkspaces = getParameterValues(originalActivityTemplateParams, UIDocActivity.WORKSPACE);
        String[] originalActivityFilesPaths = getParameterValues(originalActivityTemplateParams, UIDocActivity.DOCPATH);
        Map<String, String> templateParams = new HashMap<>();
        concatenateParam(templateParams, "originalActivityId", activityId);
        if (originalActivityFilesPaths != null && originalActivityFilesPaths.length > 0) {
          for (int i = 0; i < originalActivityFilesPaths.length; i++)  {
            String originalActivityFileRepository = "repository";
            if (originalActivityFilesRepositories != null && originalActivityFilesRepositories.length == originalActivityFilesPaths.length && StringUtils.isNotBlank(originalActivityFilesRepositories[i])) {
              originalActivityFileRepository = originalActivityFilesRepositories[i];
            }
            String originalActivityFileWorkspace = "collaboration";
            if (originalActivityFilesWorkspaces != null && originalActivityFilesWorkspaces.length == originalActivityFilesPaths.length && StringUtils.isNotBlank(originalActivityFilesWorkspaces[i])) {
              originalActivityFileWorkspace = originalActivityFilesWorkspaces[i];
            }
            String originalActivityFilePath = originalActivityFilesPaths[i];

            NodeLocation originalActivityFileNodeLocation = new NodeLocation(originalActivityFileRepository, originalActivityFileWorkspace, originalActivityFilePath);
            Node originalActivityFileNode = NodeLocation.getNodeByLocation(originalActivityFileNodeLocation);
            String targetSpaceFileNodeUUID = shareDocumentService.publishDocumentToSpace(targetSpace.getGroupId(),
                                                        originalActivityFileNode,
                                                        "",
                                                        PermissionType.READ,
                                                        false);
            Node targetSpaceFileNode = originalActivityFileNode.getSession().getNodeByUUID(targetSpaceFileNodeUUID);
            concatenateParam(templateParams, FileUIActivity.ID, targetSpaceFileNodeUUID);
            concatenateParam(templateParams, UIDocActivity.REPOSITORY, "repository");
            concatenateParam(templateParams, UIDocActivity.WORKSPACE, "collaboration");
            
            concatenateParam(templateParams, FileUIActivity.CONTENT_LINK, Utils.getContentLink(targetSpaceFileNode));
            String state;
            try {
              state = targetSpaceFileNode.hasProperty(Utils.CURRENT_STATE_PROP) ? targetSpaceFileNode.getProperty(Utils.CURRENT_STATE_PROP)
                  .getValue()
                  .getString() : "";
            } catch (Exception e) {
              state="";
            }
            concatenateParam(templateParams, FileUIActivity.STATE, state);
            concatenateParam(templateParams, FileUIActivity.AUTHOR, "");//TODO
            
            /** The date formatter. */
            DateFormat dateFormatter = null;
            dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
            String strDateCreated = "";
            if (targetSpaceFileNode.hasProperty(NodetypeConstant.EXO_DATE_CREATED)) {
              Calendar dateCreated = targetSpaceFileNode.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
              strDateCreated = dateFormatter.format(dateCreated.getTime());
              concatenateParam(templateParams, FileUIActivity.DATE_CREATED, strDateCreated);
            }
            String strLastModified = "";
            if (targetSpaceFileNode.hasNode(NodetypeConstant.JCR_CONTENT)) {
              Node contentNode = targetSpaceFileNode.getNode(NodetypeConstant.JCR_CONTENT);
              if (contentNode.hasProperty(NodetypeConstant.JCR_LAST_MODIFIED)) {
                Calendar lastModified = contentNode.getProperty(NodetypeConstant.JCR_LAST_MODIFIED)
                                                   .getDate();
                strLastModified = dateFormatter.format(lastModified.getTime());
                concatenateParam(templateParams, FileUIActivity.LAST_MODIFIED, strLastModified);
              }
            }

            concatenateParam(templateParams, FileUIActivity.MIME_TYPE, Utils.getMimeType(originalActivityFileNode));
            concatenateParam(templateParams, FileUIActivity.IMAGE_PATH, Utils.getIllustrativeImage(targetSpaceFileNode));
            String nodeTitle;
            try {
              nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(targetSpaceFileNode);
            } catch (Exception e1) {
              nodeTitle ="";
            }
            concatenateParam(templateParams, FileUIActivity.DOCUMENT_TITLE, nodeTitle);
            concatenateParam(templateParams, FileUIActivity.DOCUMENT_VERSION, "");
            concatenateParam(templateParams, FileUIActivity.DOCUMENT_SUMMARY, Utils.getFirstSummaryLines(Utils.getSummary(targetSpaceFileNode), Utils.MAX_SUMMARY_CHAR_COUNT));
            concatenateParam(templateParams, UIDocActivity.DOCPATH, targetSpaceFileNode.getPath());
            concatenateParam(templateParams, UILinkActivity.LINK_PARAM, "");//to check if necessary
            concatenateParam(templateParams, UIDocActivity.IS_SYMLINK, "true");
          }
        }

        // create activity
        ExoSocialActivity sharedActivity = new ExoSocialActivityImpl();
        sharedActivity.setTitle(sharedActivityRestIn.getTitle());
        sharedActivity.setType(sharedActivityRestIn.getType());
        sharedActivity.setUserId(authenticatedUserIdentity.getId());
        sharedActivity.setTemplateParams(templateParams);
        Identity targetSpaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, targetSpaceName);
        if (targetSpaceIdentity != null) {
          activityManager.saveActivityNoReturn(targetSpaceIdentity, sharedActivity);
          ActivityEntity sharedActivityEntity = EntityBuilder.buildEntityFromActivity(sharedActivity, uriInfo.getPath(), expand);
          sharedActivitiesEntities.add(sharedActivityEntity);
          LOG.info("service=activity operation=share parameters=\"activity_type:{},activity_id:{},space_id:{},user_id:{}\"",
                   sharedActivity.getType(),
                   sharedActivity.getId(),
                   targetSpace.getId(),
                   sharedActivity.getPosterId());
        }
      }
    }
    return EntityBuilder.getResponse(sharedActivitiesEntities, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }

  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if (value == null) {
      value = activityParams.get(paramName.toLowerCase());
    }
    if (value != null) {
      values = value.split(FileUIActivity.SEPARATOR_REGEX);
    }
    return values;
  }
  
  private void concatenateParam(Map<String, String> activityParams, String paramName, String paramValue) {
    String oldParamValue = activityParams.get(paramName);
    if (StringUtils.isBlank(oldParamValue)) {
      activityParams.put(paramName, paramValue);
    } else {
      activityParams.put(paramName, oldParamValue + TEMPLATE_PARAMS_SEPARATOR + paramValue);
    }
  }

}
