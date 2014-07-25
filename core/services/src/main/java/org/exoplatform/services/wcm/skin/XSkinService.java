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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinKey;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.SkinVisitor;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS Author : Hoa.Pham hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public class XSkinService implements Startable {
  /* Don't use "-", "_", ":" "#" "%" */
  final static String     SEPARATOR             = "/"; 
  final static String     MODULE_NAME_PATTERN   = "{repositoryName}"+SEPARATOR+"{portalName}";
  final static String     MODULE_NAME_REGEXP    = "(.*)"+SEPARATOR+"(.*)";
  final static String     MODULE_PARAM          = "moduleName";
  final static String     SKIN_PARAM            = "skinName";
  final static String     CONTEXT_PARAM         = "context";
  final static String     SITENAME_PARAM        = "siteName";
  
  /** The Constant SKIN_PATH_REGEXP. */
  public final static String      SKIN_PATH_REGEXP     = "/(.*)/css/jcr/"+MODULE_NAME_REGEXP+"/(.*)/(.*).css";

  /** The Constant SKIN_PATH_PATTERN. */
  private final static String     SKIN_PATH_PATTERN    = "/{docBase}/css/jcr/{moduleName}/(.*)/Stylesheet.css";  

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(XSkinService.class.getName());

  /** The configuration service. */
  private WCMConfigurationService configurationService;

  /** The skin service. */
  private SkinService skinService ;

  /** The servlet context. */
  private ServletContext servletContext;

  /**
   * Instantiates a new extended skin service to manage skin for web content.
   *
   * @param skinService the skin service
   * @param initializerService the content initializer service. this param makes
   *          sure that the service started after the content initializer
   *          service is started
   * @param schemaConfigService the schema config service
   * @param configurationService the configuration service
   * @param servletContext the servlet context
   * @param repositoryService the repository service
   * @throws Exception the exception
   */
  public XSkinService(LivePortalManagerService livePortalService) throws Exception {
    this.skinService = WCMCoreUtils.getService(SkinService.class);
    this.skinService.addResourceResolver(new WCMSkinResourceResolver(this.skinService, livePortalService));
    this.configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    this.servletContext = WCMCoreUtils.getService(ServletContext.class);
  }

  /**
   * Gets the active style sheet of the specified web content node
   * 
   * @param webcontent the web content node to get style sheet
   * @return the active style sheet
   * @throws Exception the exception
   */
  public String getActiveStylesheet(Node webcontent) throws Exception {
    return WCMCoreUtils.getActiveStylesheet(webcontent);
  }

  /**
   * Update portal skin on modify.
   *
   * @param portal the portal
   * @param cssFile the css file
   *
   * @throws Exception the exception
   */
  public void updatePortalSkinOnModify(Node portal, Node cssFile) throws Exception {
    String sharedPortalName = configurationService.getSharedPortalName();
    if (sharedPortalName.equals(portal.getName())) {
      addSharedPortalSkin(portal);
    } else {
      addPortalSkin(portal);
    }
  }

  /**
   * Update portal skin on remove.
   *
   * @param portal the portal
   * @param cssFile the css file
   *
   * @throws Exception the exception
   */
  public void updatePortalSkinOnRemove(Node portal, Node cssFile) throws Exception {
    updatePortalSkinOnModify(portal, cssFile);
  }

  /**
   * Adds the portal skin.
   *
   * @param portalNode the portal
   * @param cssFile the css file
   * @param isStartup the flag to decide whether this situation is startup or not
   *
   * @throws Exception the exception
   */
  private void addPortalSkin(Node portalNode) throws Exception {
    String moduleName = createModuleName(portalNode.getName());
    String skinPath = StringUtils.replaceOnce(SKIN_PATH_PATTERN, "{moduleName}",moduleName)
                                 .replaceFirst("\\{docBase\\}",
                                               servletContext.getServletContextName());
    Iterator<String> iterator = skinService.getAvailableSkinNames().iterator();
    if (iterator.hasNext() == false) {
      skinPath = StringUtils.replaceOnce(skinPath,"(.*)", "Default");
      skinService.invalidateCachedSkin(skinPath);
      skinService.addSkin(moduleName, "Default", skinPath);
    } else {
      while (iterator.hasNext()) {
        String skinName = iterator.next();
        skinPath = StringUtils.replaceOnce(skinPath,"(.*)",skinName);
        skinService.invalidateCachedSkin(skinPath);        
        skinService.addSkin(moduleName, skinName, skinPath);
      }
    }
  }

  /**
   * Adds the shared portal skin.
   *
   * @param portalNode the portal
   *
   * @throws Exception the exception
   */
  private void addSharedPortalSkin(Node portalNode) throws Exception {
    String moduleName = createModuleName(portalNode.getName());
    String skinPath = StringUtils.replaceOnce(SKIN_PATH_PATTERN, "{moduleName}", moduleName)
                                 .replaceFirst("\\{docBase\\}",
                                               servletContext.getServletContextName());
    for (Iterator<String> iterator = skinService.getAvailableSkinNames().iterator(); iterator.hasNext();) {
      String skinName = iterator.next();
      skinPath = StringUtils.replaceOnce(skinPath, "(.*)", skinName);
      skinService.invalidateCachedSkin(skinPath);
      skinService.addPortalSkin(moduleName, skinName, skinPath);
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
      List<Node> livePortals = livePortalManagerService.getLivePortals(sessionProvider);
      for (Node portal : livePortals) {
        addPortalSkin(portal);
      }      
      Node sharedPortal = livePortalManagerService.getLiveSharedPortal(sessionProvider);
      addSharedPortalSkin(sharedPortal);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when start XSkinService", e);
      }
    } finally {
      sessionProvider.close();
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }

  static String createModuleName(String siteName){
    String repoName;
    try{
      repoName = WCMCoreUtils.getRepository().getConfiguration().getName();
    }catch(NullPointerException e){
      repoName = WCMCoreUtils.getService(RepositoryService.class).getConfig().getDefaultRepositoryName();
    }
    String moduleName = StringUtils.replaceOnce(MODULE_NAME_PATTERN, "{repositoryName}", repoName);
    moduleName = StringUtils.replaceOnce(moduleName,"{portalName}",siteName);
    return moduleName;
  }
  
  static Map<String,String> getSkinParams(String path){
    if (!path.matches(SKIN_PATH_REGEXP)) return null;
    String moduleName;
    String skinName;
    String siteName;
    String context;
    String[] elements = path.split("/");
    if (XSkinService.SEPARATOR.equals("/")){
      context = elements[4];
      siteName = elements[5];
      skinName = elements[6];      
    }else{
      context = elements[4].split(XSkinService.SEPARATOR)[0];
      siteName = elements[4].split(XSkinService.SEPARATOR)[1];
      skinName = elements[5];
    }
    
    moduleName = StringUtils.replaceOnce(MODULE_NAME_PATTERN, "{repositoryName}", context);
    moduleName = StringUtils.replaceOnce(moduleName,"{portalName}",siteName);
    Map<String,String> params = new HashMap<String,String>();
    params.put(MODULE_PARAM, moduleName);
    params.put(SKIN_PARAM, skinName);
    params.put(SITENAME_PARAM, siteName);
    params.put(CONTEXT_PARAM, context);    
    return params;  
  }
  
  
}
