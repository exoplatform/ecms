/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.skin;

import java.io.Reader;
import java.io.StringReader;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.resource.Resource;
import org.exoplatform.portal.resource.ResourceResolver;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.phamvu@exoplatform.com
 * Nov 25, 2008
 */
public class WCMSkinResourceResolver implements ResourceResolver {
  private SkinService skinService;
  private LivePortalManagerService livePortalService;
  
  /** The log. */
  private static Log              log                  = ExoLogger.getLogger("wcm:WCMSkinResourceResolver");
  
  /** The SHARE d_ cs s_ query. */
  private static String           SHARED_CSS_QUERY     = "select * from exo:cssFile where jcr:path like '{path}/%' "
                                                           + "and exo:active='true' and exo:sharedCSS='true' "
                                                           + "and jcr:mixinTypes <> 'exo:restoreLocation' "
                                                           + "order by exo:priority ASC".intern();
  
  
  public WCMSkinResourceResolver(SkinService skinService, LivePortalManagerService livePortalService) {
    this.skinService = skinService;
    this.livePortalService = livePortalService;
  }

  public Resource resolve(String path) {
    if(!path.matches(XSkinService.SKIN_PATH_REGEXP)) return null;
    String[] elements = path.split("/");
    String portalName = elements[4];
    String skinName = elements[5];
    String skinModule = portalName;
    String cssPath = null;
    SkinConfig portalSkinConfig = skinService.getSkin(portalName,skinName);
    if(portalSkinConfig != null) {
      cssPath = portalSkinConfig.getCSSPath();
    }
    //get css for shared portal if the portalName is shared Portal
    if(cssPath == null) {
      for(SkinConfig skinConfig: skinService.getPortalSkins(skinName)) {
        if(skinConfig.getModule().equals(skinModule)) {
          cssPath = skinConfig.getCSSPath();
          break;
        }
      }
    }
    try {
      Node portalNode = livePortalService.getLivePortal(WCMCoreUtils.getSystemSessionProvider(), portalName);  
      final String cssData = WCMCoreUtils.getActiveStylesheet(portalNode);
      if(cssData == null)
        return null;
      return new Resource(cssPath) {
        public Reader read() {
          return new StringReader(cssData);
        }
      };
    } catch(Exception e) {
      log.error("Unexpected error happens", e);
    }
    return null;
  }
  
  /**
   * Merge the registered css and new css file
   * 
   * @param portalNode the portal
   * @param newCSSFile new css file
   * @param isStartup flag to decide whether this situation is startup or not
   * @return the merged css data as result of registered css and new css
   * @throws Exception the exception
   */
  private String mergeCSSData(Node portalNode) throws Exception {
    StringBuffer buffer = new StringBuffer();
    WebSchemaConfigService schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    // Get all css by query
    Node cssFolder = schemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class)
                                        .getCSSFolder(portalNode);
    String statement = StringUtils.replaceOnce(SHARED_CSS_QUERY, "{path}", cssFolder.getPath());
    try {
      QueryManager queryManager = portalNode.getSession().getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(statement, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();
      while (iterator.hasNext()) {
        Node registeredCSSFile = iterator.nextNode();
        buffer.append(getActivedCSSData(registeredCSSFile));
      }
      buffer.append(WCMCoreUtils.getActiveStylesheet(portalNode));
    } catch(Exception e) {
      log.error("Unexpected problem happen when merge CSS data", e);
    }   
    return buffer.toString();
  }
  
  private String getActivedCSSData(Node cssFile) throws ValueFormatException,
                                              RepositoryException,
                                              PathNotFoundException {
    if (!cssFile.isNodeType("exo:restoreLocation") && cssFile.hasNode(NodetypeConstant.JCR_CONTENT)
        && cssFile.getNode(NodetypeConstant.JCR_CONTENT).hasProperty(NodetypeConstant.JCR_DATA)
        && cssFile.hasProperty(NodetypeConstant.EXO_ACTIVE)
        && cssFile.getProperty(NodetypeConstant.EXO_ACTIVE).getBoolean() == true) {

      return cssFile.getNode(NodetypeConstant.JCR_CONTENT)
                   .getProperty(NodetypeConstant.JCR_DATA)
                   .getString();
    }
    return "";
  }  

}
