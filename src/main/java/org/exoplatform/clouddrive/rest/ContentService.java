/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.rest;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.features.CloudDriveFeatures;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.clouddrive.viewer.ContentReader;
import org.exoplatform.clouddrive.viewer.DocumentNotFoundException;
import org.exoplatform.clouddrive.viewer.ViewerStorage;
import org.exoplatform.clouddrive.viewer.ViewerStorage.ContentFile;
import org.exoplatform.clouddrive.viewer.ViewerStorage.PDFFile;
import org.exoplatform.clouddrive.viewer.ViewerStorage.PDFFile.ImageFile;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.security.RolesAllowed;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

/**
 * RESTful service to access file content in Cloud Drive operations.<br>
 * 
 */
@Path("/clouddrive/content")
public class ContentService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(ContentService.class);

  /**
   * REST service URL path.
   */
  public static String       SERVICE_PATH;

  static {
    Path restPath = ContentService.class.getAnnotation(Path.class);
    SERVICE_PATH = restPath.value();
  }

  /** The features. */
  protected final CloudDriveFeatures     features;

  /** The cloud drives. */
  protected final CloudDriveService      cloudDrives;

  /** The viewer storage. */
  protected final ViewerStorage          viewerStorage;

  /** The jcr service. */
  protected final RepositoryService      jcrService;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /**
   * Constructor.
   *
   * @param cloudDrives the cloud drives
   * @param features the features
   * @param viewerStorage the viewer storage
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   */
  public ContentService(CloudDriveService cloudDrives,
                        CloudDriveFeatures features,
                        ViewerStorage viewerStorage,
                        RepositoryService jcrService,
                        SessionProviderService sessionProviders) {
    this.cloudDrives = cloudDrives;
    this.features = features;
    this.viewerStorage = viewerStorage;

    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
  }

  /**
   * Create a link to get this file content via eXo REST service as a proxy to remote drive. It is a path
   * relative to the current server.
   *
   * @param workspace {@link String}
   * @param path {@link String}
   * @param fileId {@link String}
   * @return {@link String}
   * @see #get(String, String, String)
   */
  public static String contentLink(String workspace, String path, String fileId) {
    StringBuilder linkPath = new StringBuilder();
    linkPath.append(PortalContainer.getCurrentPortalContainerName());
    linkPath.append('/');
    linkPath.append(PortalContainer.getCurrentRestContextName());
    linkPath.append(SERVICE_PATH);
    linkPath.append('/');
    linkPath.append(workspace);
    // path already starts with slash
    // XXX we want escape + in file names as ECMS does
    try {
      String encodedPath = URLEncoder.encode(path, "UTF-8");
      encodedPath = encodedPath.replaceAll("%2F", "/");
      linkPath.append(encodedPath);
    } catch (UnsupportedEncodingException e1) {
      linkPath.append(path);
    }

    StringBuilder link = new StringBuilder();
    link.append('/');

    link.append(linkPath);
    // add query
    link.append('?');
    link.append("contentId=");
    link.append(fileId);

    return link.toString();
  }
  
  /**
   * Create a link to get this file representation in PDF form. It is a path
   * relative to the current server.
   *
   * @param contentLink {@link String}
   * @return {@link String}
   * @see #get(String, String, String)
   */
  public static String pdfLink(String contentLink) {
    return contentLink.replace(ContentService.SERVICE_PATH, ContentService.SERVICE_PATH + "/pdf");
  }
  
  /**
   * Create a link to get this file representation in PDF pages converted to images. It is a path
   * relative to the current server.
   *
   * @param contentLink {@link String}
   * @return {@link String}
   * @see #get(String, String, String)
   */
  public static String pdfPageLink(String contentLink) {
    return contentLink.replace(ContentService.SERVICE_PATH, ContentService.SERVICE_PATH + "/pdf/page");
  }

  /**
   * Return file content reading it from cloud side.<br>
   *
   * @param workspace the workspace
   * @param path the path
   * @param contentId the content id
   * @return the response
   */
  @GET
  @Path("/{workspace}/{path:.*}")
  @RolesAllowed("users")
  public Response get(@PathParam("workspace") String workspace,
                      @PathParam("path") String path,
                      @QueryParam("contentId") String contentId) {
    // TODO support for range and if-modified, if-match... in WebDAV fashion, for browser players etc.
    if (workspace != null) {
      if (path != null) {
        path = normalizePath(path);
        if (contentId != null) {
          try {
            CloudDrive drive = cloudDrives.findDrive(workspace, path);
            if (drive != null) {
              ContentReader content = ((CloudDriveStorage) drive).getFileContent(contentId);
              if (content != null) {
                ResponseBuilder resp = Response.ok().entity(content.getStream());
                long len = content.getLength();
                if (len >= 0) {
                  resp.header("Content-Length", len);
                }
                resp.type(content.getMimeType());
                String typeMode = content.getTypeMode();
                if (typeMode != null && typeMode.length() > 0) {
                  resp.header(ExtendedMimeTypeResolver.X_TYPE_MODE, typeMode);
                }
                return resp.build();
              }
            }
            return Response.status(Status.BAD_REQUEST).entity("No cloud file or content not available.").build();
          } catch (LoginException e) {
            LOG.warn("Error login to read cloud file content " + workspace + ":" + path + ": " + e.getMessage());
            return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
          } catch (NotFoundException e) {
            LOG.warn("File not found to read its content " + workspace + ":" + path, e);
            return Response.status(Status.NOT_FOUND).entity("File not found. " + e.getMessage()).build();
          } catch (CloudDriveException e) {
            LOG.warn("Error reading file content " + workspace + ":" + path, e);
            return Response.status(Status.BAD_REQUEST).entity("Error reading file content. " + e.getMessage()).build();
          } catch (RepositoryException e) {
            LOG.error("Error reading file content " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading file content: storage error.")
                           .build();
          } catch (Throwable e) {
            LOG.error("Error reading file content " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading file content: runtime error.")
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST).entity("Null fileId.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  /**
   * Return image (PNG) representation of cloud file content (page) reading it from local PDF storage. File
   * should be previously created in the storage to be successfully returned by this method, an empty response
   * (204 No Content) will be returned otherwise.<br>
   *
   * @param workspace the workspace
   * @param path the path
   * @param contentId the content id
   * @param strPage the str page
   * @param strRotation the str rotation
   * @param strScale the str scale
   * @return the page image
   */
  @GET
  @Path("/pdf/page/{workspace}/{path:.*}")
  @RolesAllowed("users")
  public Response getPageImage(@PathParam("workspace") String workspace,
                               @PathParam("path") String path,
                               @QueryParam("contentId") String contentId,
                               @DefaultValue("1") @QueryParam("page") String strPage,
                               @DefaultValue("0") @QueryParam("rotation") String strRotation,
                               @DefaultValue("1.0") @QueryParam("scale") String strScale) {
    if (workspace != null) {
      if (path != null) {
        path = normalizePath(path);
        if (contentId != null) {
          try {
            CloudDrive drive = cloudDrives.findDrive(workspace, path);
            if (drive != null) {
              String repository = jcrService.getCurrentRepository().getConfiguration().getName();
              ContentFile viewFile = viewerStorage.getFile(repository, workspace, drive, contentId);
              if (viewFile != null && viewFile.isPDF()) {
                PDFFile pdfFile = viewFile.asPDF();
                // save page capture to file.
                float scale;
                try {
                  scale = Float.parseFloat(strScale);
                  // maximum scale support is 300%
                  if (scale > 3.0f) {
                    scale = 3.0f;
                  }
                } catch (NumberFormatException e) {
                  scale = 1.0f;
                }
                float rotation;
                try {
                  rotation = Float.parseFloat(strRotation);
                } catch (NumberFormatException e) {
                  rotation = 0.0f;
                }
                int maximumOfPage = pdfFile.getNumberOfPages();
                int page;
                try {
                  page = Integer.parseInt(strPage);
                } catch (NumberFormatException e) {
                  page = 1;
                }
                if (page >= maximumOfPage) {
                  page = maximumOfPage;
                } else if (page < 1) {
                  page = 1;
                }

                ImageFile image = pdfFile.getPageImage(page, rotation, scale);

                return Response.ok(image.getStream(), image.getType())
                               .header("Last-Modified", pdfFile.getLastModified())
                               .header("Content-Length", image.getLength())
                               .header("Content-Disposition", "inline; filename=\"" + image.getName() + "\"")
                               .build();
              } else {
                // PDF representation not available
                LOG.warn("PDF representation not available for " + workspace + ":" + path + " id:" + contentId);
                return Response.status(Status.NO_CONTENT).build();
              }
            }
            return Response.status(Status.BAD_REQUEST).entity("No cloud file or content not available.").build();
          } catch (DocumentNotFoundException e) {
            LOG.error("Error reading cloud file representation " + workspace + ":" + path + ": " + e.getMessage());
            return Response.status(Status.NOT_FOUND).entity("Cloud file representation not found.").build();
          } catch (LoginException e) {
            LOG.warn("Error login to read cloud file representation " + workspace + ":" + path + ": " + e.getMessage());
            return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
          } catch (CloudDriveException e) {
            LOG.warn("Error reading cloud file representation " + workspace + ":" + path, e);
            return Response.status(Status.BAD_REQUEST)
                           .entity("Error reading cloud file representation. " + e.getMessage())
                           .build();
          } catch (RepositoryException e) {
            LOG.error("Error reading cloud file representation " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading cloud file representation: storage error.")
                           .build();
          } catch (Throwable e) {
            LOG.error("Error reading file content " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading cloud file representation: runtime error.")
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST).entity("Null fileId.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  /**
   * Return cloud file representation reading it from local PDF storage. File
   * should be previously created in the storage to be successfully returned by this method, an empty response
   * (204 No Content) will be returned otherwise.<br>
   *
   * @param workspace the workspace
   * @param path the path
   * @param contentId the content id
   * @return the pdf
   */
  @GET
  @Path("/pdf/{workspace}/{path:.*}")
  @RolesAllowed("users")
  public Response getPDF(@PathParam("workspace") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("contentId") String contentId) {
    if (workspace != null) {
      if (path != null) {
        path = normalizePath(path);
        if (contentId != null) {
          try {
            CloudDrive drive = cloudDrives.findDrive(workspace, path);
            if (drive != null) {
              String repository = jcrService.getCurrentRepository().getConfiguration().getName();
              ContentFile viewFile = viewerStorage.getFile(repository, workspace, drive, contentId);
              if (viewFile != null && viewFile.isPDF()) {
                String filename = viewFile.getName();
                if (!filename.endsWith(".pdf")) {
                  filename = new StringBuilder(filename).append(".pdf").toString();
                }
                ResponseBuilder resp = Response.ok(viewFile.getStream(), viewFile.getMimeType())
                                               .header("Last-Modified", viewFile.getLastModified())
                                               .header("Content-Length", viewFile.getLength());
                resp.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                return resp.build();
              } else {
                // PDF representation not available
                LOG.warn("PDF representation not available for " + workspace + ":" + path + " id:" + contentId);
                return Response.status(Status.NO_CONTENT).build();
              }
            }
            return Response.status(Status.BAD_REQUEST).entity("No cloud file or content not available.").build();
          } catch (DocumentNotFoundException e) {
            LOG.error("Error reading cloud file PDF representation " + workspace + ":" + path + ": " + e.getMessage());
            return Response.status(Status.NOT_FOUND).entity("Cloud file PDF representation not found.").build();
          } catch (LoginException e) {
            LOG.warn("Error login to read cloud file PDF representation " + workspace + ":" + path + ": " + e.getMessage());
            return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
          } catch (CloudDriveException e) {
            LOG.warn("Error reading cloud file PDF representation " + workspace + ":" + path, e);
            return Response.status(Status.BAD_REQUEST)
                           .entity("Error reading cloud file PDF representation. " + e.getMessage())
                           .build();
          } catch (RepositoryException e) {
            LOG.error("Error reading cloud file representation " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading cloud file PDF representation: storage error.")
                           .build();
          } catch (Throwable e) {
            LOG.error("Error reading file content " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity("Error reading cloud file PDF representation: runtime error.")
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST).entity("Null fileId.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  /**
   * Normalize JCR path (as eXo WebDAV does).
   * 
   * @param path {@link String}
   * @return normalized path
   */
  protected String normalizePath(String path) {
    return path.length() > 0 && path.endsWith("/") ? "/" + path.substring(0, path.length() - 1) : "/" + path;
  }
}
