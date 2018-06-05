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

package org.exoplatform.clouddrive.exodrive.service.rest;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.clouddrive.exodrive.service.ExoDriveException;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveRepository;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveService;
import org.exoplatform.clouddrive.exodrive.service.FileStore;
import org.exoplatform.clouddrive.exodrive.service.NotFoundException;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * REST service with anonymous access to eXo Drive. Created by The eXo Platform
 * SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDrivePublicService.java 00000 Oct 17, 2012 pnedonosko $
 */
@Path("/exodrive")
public class ExoDrivePublicService implements ResourceContainer {

  protected static final Log        LOG = ExoLogger.getLogger(ExoDrivePublicService.class);

  protected final ExoDriveService   service;

  protected final RepositoryService jcrService;

  public ExoDrivePublicService(RepositoryService jcrService, ExoDriveService service) {
    this.service = service;
    this.jcrService = jcrService;
  }

  @GET
  @Path("/{user}/{filename}")
  @Produces(MediaType.TEXT_HTML)
  public Response get(@Context UriInfo uriInfo, @PathParam("user") String user, @PathParam("filename") String filename) {

    try {
      ExoDriveRepository repo = service.read(jcrService.getCurrentRepository().getConfiguration().getName());
      FileStore store = repo.read(user, filename);
      InputStream content = store.read();

      return Response.ok().entity(content).type(store.getType()).build();
    } catch (NotFoundException e) {
      LOG.warn("File not found for " + user + ". Asked file: " + filename, e);
      return Response.status(Status.NOT_FOUND).entity("Not found " + user + "/" + filename).build();
    } catch (ExoDriveException e) {
      LOG.error("Error read user " + user + " file " + filename, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity("Error read user " + user + " file " + filename + ": " + e.getMessage())
                     .build();
    } catch (Throwable e) {
      LOG.error("Error read user " + user + " file " + filename, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity("Error read user " + user + " file " + filename + ": " + e.getMessage())
                     .build();
    }

  }
}
