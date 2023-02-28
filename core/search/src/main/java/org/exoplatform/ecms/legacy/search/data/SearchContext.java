/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.ecms.legacy.search.data;

import org.apache.commons.collections.map.HashedMap;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;

import java.util.Map;

/**
 * Search Context contains a set of data needed for SearchService and all connectors.
 *  
 * @LevelAPI Experimental  
 * @deprecated Copied from commons-search to this module.
 *  Should be reworked to be more simple.
 */
@Deprecated(forRemoval = true, since = "6.0.0")
public class SearchContext {
  
  private static Log LOG = ExoLogger.getExoLogger(SearchContext.class);
  
  public static enum RouterParams {
   SITE_TYPE("sitetype"),
   SITE_NAME("sitename"),
   HANDLER("handler"),
   PATH("path"),
   LANG("lang");
   
   private final static String PREFIX = "gtn";
   private String paramName = null;
   
   /**
    * Constructor with paramName
    * @param paramName
    * @LevelAPI Experimental
    */
   private RouterParams(String paramName) {
     this.paramName = paramName;
   }
   
   /**
    * Create qualified name
    * @LevelAPI Experimental
    * @return QualifiedName
    * @LevelAPI Experimental 
    */
   public QualifiedName create() {
     return QualifiedName.create(PREFIX, paramName);
   }
   
  };
  /** */
  private Router router; // Gatein router, provides routing information for building resource URLs
  
  /** */
  private String siteName;
  
  /** */
  private Map<QualifiedName, String> params = null;
  
  /**
   * Get router
   * @return Router
   * @LevelAPI Experimental 
   */
  public Router getRouter() {
    return router;
  }

  /**
   * Set router
   * @param router
   * @LevelAPI Experimental 
   */
  public void setRouter(Router router) {
    this.router = router;
  }

  public String getParamValue(QualifiedName name) {
    return params.get(name);
  }

  /**
   * Get site name, e.g. intranet, acme, ..
   * @return String
   * @LevelAPI Experimental
   */
  public String getSiteName() {
    return siteName;
  }

  /**
   * Get site type
   * @return String
   * @LevelAPI Experimental
   */
  public String getSiteType() {
    return params.get(RouterParams.SITE_TYPE.create());
  }

  /**
   * Contructor to create a context for search service
   * @param router
   * @param siteName
   * @LevelAPI Experimental 
   */
  public SearchContext(Router router, String siteName) {
    this.router = router;
    this.siteName = siteName;
    params = new HashedMap();
  }
  
  /**
   * Puts Handler value into QualifiedName map
   * @param value
   * @return SearchContext
   * @LevelAPI Experimental  
   */
  public SearchContext handler(String value) {
    params.put(RouterParams.HANDLER.create(), value);
    return this;
  }
  
  /**
   * Puts Lang value into QualifiedName map
   * @param value
   * @return SearchContext
   * @LevelAPI Experimental 
   */
  public SearchContext lang(String value) {
    params.put(RouterParams.LANG.create(), value);
    return this;
  }
  
  /**
   * Puts Path value into QualifiedName map
   * @param value
   * @return SearchContext
   * @LevelAPI Experimental
   */
  public SearchContext path(String value) {
    params.put(RouterParams.PATH.create(), value);
    return this;
  }
  
  /**
   * Puts SiteType value into QualifiedName map
   * @param value
   * @return SearchContext
   * @LevelAPI Experimental
   */
  public SearchContext siteType(String value) {
    params.put(RouterParams.SITE_TYPE.create(), value);
    return this;
  }
  
  /**
   * Puts SiteType value into QualifiedName map
   * @param value
   * @return SearchContext
   * @LevelAPI Experimental
   */
  public SearchContext siteName(String value) {
    params.put(RouterParams.SITE_NAME.create(), value);
    return this;
  }
  
  /**
   * Render link base on router and {@literal Map<QualifiedName, String>}
   * @return String
   * @throws Exception
   * @LevelAPI Experimental
   */
  public String renderLink() throws Exception {
    //
    if (params.containsKey(RouterParams.LANG.create()) == false) {
      lang("");
    }
    
    //
    if (params.containsKey(RouterParams.HANDLER.create()) == false) {
      LOG.warn("Handler of QualifiedName not found!");
    }
    
    //
    if (params.containsKey(RouterParams.SITE_NAME.create()) == false) {
      LOG.warn("SiteName of QualifiedName not found!");
    }
    
    //
    if (params.containsKey(RouterParams.SITE_TYPE.create()) == false) {
      LOG.warn("SiteType of QualifiedName not found!");
    }
    
    //
    return router.render(params);
  }
  
}
