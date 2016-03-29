/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.collaboration;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Gets the bundle that is based on the key and the locale.
 *
 * @LevelAPI Provisional
 *
 * @anchor ResourceBundleConnector
 */
@Path("/bundle/")
public class ResourceBundleConnector implements ResourceContainer {

  /**
  * Gets the bundle that is based on the key and the locale.
   *
  * @param key The key used to get the bundle.
  * @param locale  The locale used to get the bundle.
  * 
  * @anchor ResourceBundleConnector.getBundle
  */

  private static DocumentBuilder DB;

  static {
    try {
      DB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
  @GET
  @Path("/getBundle/")
  public Response getBundle (
      @QueryParam("key") String key,
      @QueryParam("locale") String locale) {
    try {
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      String resourceBundleNames[] = resourceBundleService.getSharedResourceBundleNames();
      Document document = DB.newDocument();
      Element bundles = document.createElement("bundles");
      bundles.setAttribute("locale", locale);
      String keys[] = key.split(",");
      Set<String> remainingKeys = new LinkedHashSet<String>(keys.length + 1, 1f);
      Collections.addAll(remainingKeys, keys);
      loop : for ( String resourceBundleName : resourceBundleNames) {
        ResourceBundle resourceBundle = null;
        if(locale.indexOf("_") > 0) {
            resourceBundle = resourceBundleService.getResourceBundle(resourceBundleName, new Locale(
                locale.substring(0, locale.lastIndexOf("_")), 
                locale.substring(locale.lastIndexOf("_") + 1, locale.length()))); 
            
        } else {
          resourceBundle = resourceBundleService.getResourceBundle(resourceBundleName, new Locale(locale));
        }
        
        for (Iterator<String> it = remainingKeys.iterator(); it.hasNext();) {
           String oneKey = it.next();
          try {
            String value = resourceBundle.getString(oneKey);
            Element element = document.createElement(oneKey);
            element.setAttribute("value", value);
            bundles.appendChild(element);
            it.remove();
            if (remainingKeys.isEmpty()) {
              break loop;
            }
          } catch (MissingResourceException e) {
            continue;
          }
        }
      }
      document.appendChild(bundles);

      CacheControl cacheControl = new CacheControl();
      cacheControl.setNoCache(true);
      cacheControl.setNoStore(true);
      return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
    } catch (Exception e) {
      return Response.serverError().build();
    }
  }

}
