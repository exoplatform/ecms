/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.test.mock;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 6, 2012  
 */
@Path("/mock")
public class MockRestService implements ResourceContainer {
  
  private List<String>       guests = new ArrayList<String>();
  
  private final CacheControl cc;

  public MockRestService() {
    this.cc = new CacheControl();
    this.cc.setNoCache(true);
    this.cc.setNoStore(true);
  }

  @GET
  @Path("/guest")
  @Produces(MediaType.APPLICATION_XHTML_XML)
  public Response getGuest(@QueryParam("name") @DefaultValue("anonymous") String name) {
    if (!guests.contains(name)) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return Response.ok("Hello " + name, MediaType.APPLICATION_XHTML_XML).cacheControl(cc).build();
  }

  @POST
  @Path("/guest")
  @Produces(MediaType.APPLICATION_JSON)
  public Response register(@FormParam("name") @DefaultValue("anonymous") String name) {
    guests.add(name);
    return Response.ok("Registed " + name, MediaType.APPLICATION_JSON).cacheControl(cc).build();
  }

  @DELETE
  @Path("/guest")
  @Produces(MediaType.TEXT_PLAIN)
  public Response remove(@QueryParam("name") @DefaultValue("anonymous") String name) {
    if (!guests.contains(name)) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    guests.remove(name);
    return Response.ok("Removed " + name, MediaType.TEXT_PLAIN).cacheControl(cc).build();
  }
}
