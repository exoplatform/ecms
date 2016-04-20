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
package org.exoplatform.clouddrive.ecms.rest;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * REST service providing access to UI resources. Created by The eXo
 * Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ResourceService.java 00000 Apr 20, 2016 pnedonosko $
 */
@Path("/clouddrive/resource")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceService implements ResourceContainer {

  protected static final Log LOG = ExoLogger.getLogger(ResourceService.class);

  public static class LocaleBundle {
    protected final Map<String, String> data;

    protected final String              language;

    protected final String              country;

    protected LocaleBundle(String language, String country, Map<String, String> data) {
      super();
      this.language = language;
      this.country = country;
      this.data = data;
    }

    /**
     * @return the data
     */
    public Map<String, String> getData() {
      return data;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
      return language;
    }

    /**
     * @return the country
     */
    public String getCountry() {
      return country;
    }

  }

  protected final ResourceBundleService resourceService;

  /**
   * Constructor.
   * 
   * @param {@link ResourceBundleService} resourceService
   */
  public ResourceService(ResourceBundleService resourceService) {
    this.resourceService = resourceService;
  }

  /**
   * Return provider by its id.
   * 
   * @param providerId - provider name see more in {@link CloudProvider}
   * @return response with asked {@link CloudProvider} json
   */
  @GET
  @RolesAllowed("users")
  @Path("/bundle")
  public Response getBundle(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
    Locale locale = request.getLocale();
    try {
      // TODO should we use user locale (for current user)?
      ResourceBundle bundle = resourceService.getResourceBundle("locale.clouddrive.CloudDrive", locale);
      if (bundle != null) {
        Map<String, String> bundleMap = new HashMap<String, String>();
        for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
          String key = keys.nextElement();
          String value = bundle.getString(key);
          bundleMap.put(key, value);
        }
        return Response.ok().entity(new LocaleBundle(locale.getLanguage(), locale.getCountry(), bundleMap)).build();
      } else {
        return Response.status(Status.NOT_FOUND).entity("Bundle not found for " + locale.toLanguageTag()).build();
      }
    } catch (Throwable e) {
      LOG.error("Error returning locale bundle for " + locale + ": " + e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity("Error getting bundle for " + locale.toLanguageTag())
                     .build();
    }
  }

}
