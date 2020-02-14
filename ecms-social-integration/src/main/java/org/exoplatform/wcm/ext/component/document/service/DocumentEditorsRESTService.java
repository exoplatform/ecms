/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.document.service;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

@Path("/documents/editors")
public class DocumentEditorsRESTService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(DocumentEditorsRESTService.class);

  @POST
  @Path("/preffered/{fileId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@PathParam("fileId") String fileId, String data) {

    JSONParser parser = new JSONParser();
    Object obj = null;
    try {
      obj = parser.parse(data);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    JSONObject jsonObj = (JSONObject) obj;

    String provider = (String) jsonObj.get("provider");
    String userId = (String) jsonObj.get("userId");
    LOG.info("REQUEST HANDLED: " + fileId + " " + userId + " " + provider);
    return Response.ok().build();
  }
}
