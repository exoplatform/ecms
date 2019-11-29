package org.exoplatform.social.ckeditor.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.service.rest.RestChecker;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@Path("/composer/image")
public class ComposerImageControllerRest implements ResourceContainer {
  private static final Log LOG = ExoLogger.getLogger(ComposerImageControllerRest.class);

  private UploadService    uploadService;

  private ActivityManager  activityManager;

  public ComposerImageControllerRest(UploadService uploadService, ActivityManager activityManager, InitParams params) {
    this.uploadService = uploadService;
    this.activityManager = activityManager;
  }

  @GET
  @Path("new")
  @RolesAllowed("users")
  public Response createNewUploadId(@QueryParam("uploadId") String uploadId) {
    RestChecker.checkAuthenticatedRequest();
    uploadService.addUploadLimit(uploadId, activityManager.getMaxUploadSize());
    return Response.ok(String.valueOf(activityManager.getMaxUploadSize())).build();
  }

  @GET
  @Path("thumbnail")
  @RolesAllowed("users")
  public Response getUploadedImageThumbnail(@Context Request request, @QueryParam("uploadId") String uploadId) {
    RestChecker.checkAuthenticatedRequest();
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    if (uploadResource == null) {
      return Response.status(404).build();
    }

    EntityTag eTag = new EntityTag(uploadId);
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);

    if (builder == null) {
      String storeLocation = uploadResource.getStoreLocation();
      File file = new File(storeLocation);
      if (!file.exists()) {
        LOG.warn("File doesn't exists " + storeLocation);
        return Response.serverError().build();
      }
      InputStream stream = null;
      try {
        stream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        LOG.warn("File doesn't exists " + storeLocation);
        return Response.serverError().build();
      }
      builder = Response.ok(stream, uploadResource.getMimeType());
    }
    builder.tag(eTag);
    CacheControl cc = new CacheControl();
    cc.setMaxAge(86400);
    return builder.cacheControl(cc).build();
  }
}
