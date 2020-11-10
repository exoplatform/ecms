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
package org.exoplatform.services.wcm.javascript;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.handler.SiteJavascriptHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public class XJavascriptService implements Startable {

  private static String           WEBCONTENT_JS_QUERY = "select * from exo:jsFile "
                                                          + "where jcr:path like '{path}/%' and exo:active='true' "
                                                          + "order by exo:priority ASC";

  /** The MODUL e_ name. */
  final private String MODULE_NAME = "eXo.WCM.Live";

  public final static String JS_PATH_REGEXP = "/(.*)/javascript/eXo/(.*)/live";

  /** The PATH. */
  final private String PATH = "/javascript/eXo/{portalName}/live";

  /** The js config service. */
  private JavascriptConfigService jsConfigService ;

  /** The configuration service. */
  private WCMConfigurationService configurationService;

  private LivePortalManagerService livePortalManagerService_;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(XJavascriptService.class.getName());

  private Set<String> loadedJSModule = new HashSet<String>();
  private Set<String> loadedSharedJSModule = new HashSet<String>();

  private ExoCache<String, Object> jsCache_;

  private long                     jsLastModifiedDate   = System.currentTimeMillis();

  /**
   * Instantiates a new x javascript service.
   *
   * @param livePortalService the livePortal service
   *
   * @throws Exception the exception
   */
  public XJavascriptService(WCMConfigurationService wcmConfigurationService, JavascriptConfigService javascriptConfigService, LivePortalManagerService livePortalService) throws Exception{
    this.livePortalManagerService_ = livePortalService;
    this.jsConfigService = javascriptConfigService;
    this.configurationService = wcmConfigurationService;

    jsCache_ = WCMCoreUtils.getService(CacheService.class).getCacheInstance(SiteJavascriptHandler.CACHE_REGION);

    this.jsConfigService.addResourceResolver(new WCMJavascriptResourceResolver(livePortalManagerService_, jsConfigService));
  }

  /**
   * Get active java script.
   *
   * @param webcontent the webcontent's node
   * @return Code of all js file in home node.
   * @throws Exception the exception
   */
  public String getActiveJavaScript(Node webcontent) throws Exception {
    StringBuffer buffer = new StringBuffer();
    String jsQuery = StringUtils.replaceOnce(WEBCONTENT_JS_QUERY, "{path}", webcontent.getPath());

    // Need re-login to get session because this node is get from template and the session is not live anymore.
    NodeLocation webcontentLocation = NodeLocation.getNodeLocationByNode(webcontent);
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = null;
    if (webcontentLocation.getPath().startsWith("/jcr:system"))
      session = repository.getSystemSession(repository.getConfiguration().getSystemWorkspaceName());
    else {
      session = repository.getSystemSession(webcontentLocation.getWorkspace());
    }

    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(jsQuery, Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator iterator = queryResult.getNodes();
    while(iterator.hasNext()) {
      Node registeredJSFile = iterator.nextNode();
      buffer.append(getActivedJSData(registeredJSFile));
    }
    session.logout();
    return buffer.toString();
  }

  /**
   * Update and merged all Java Script in all portal when content of js file is modified.
   *
   * @param portalNode the portal node
   * @param jsFile the js file
   *
   * @throws Exception the exception
   */
  public void updatePortalJSOnModify(Node portalNode, Node jsFile) throws Exception {
    String sharedPortalName = configurationService.getSharedPortalName();
    if(sharedPortalName.equals(portalNode.getName())) {
      addSharedPortalJavascript(portalNode, jsFile, false);
    }else {
      addPortalJavascript(portalNode, jsFile, false);
    }
  }

  /**
   * Update and merged all Java Script in all portal when content of js file is modified.
   *
   * @param portalNode the portal node
   * @param jsFile the js file
   *
   * @throws Exception the exception
   */
  public void updatePortalJSOnRemove(Node portalNode, Node jsFile) throws Exception {
    String sharedPortalName = configurationService.getSharedPortalName();
    if(sharedPortalName.equals(portalNode.getName())) {
      addSharedPortalJavascript(portalNode, jsFile, false);
    }else {
      addPortalJavascript(portalNode, jsFile, false);
    }
  }

  /**
   * Adds the javascript.
   *
   * @param portalNode the portal node
   * @param jsFile the js data
   * @param isStartup
   */
  private void addPortalJavascript(Node portalNode, Node jsFile, boolean isStartup) throws Exception {
    String javascriptPath = StringUtils.replaceOnce(PATH, "{portalName}", portalNode.getName());

    String moduleName = MODULE_NAME + '.' + portalNode.getName();
//    jsConfigService.invalidateCachedJScript("/" + servletContext.getServletContextName() +
//                                            javascriptPath);
    if (!loadedJSModule.contains(moduleName)) {
      loadedJSModule.add(moduleName);
//      jsConfigService.addPortalJScript(
//        new PortalJScript(moduleName, javascriptPath,"/" + servletContext.getServletContextName(),
//                          10, portalNode.getName()));
    }
    jsCache_.clearCache();
    jsLastModifiedDate = System.currentTimeMillis();
  }

  /**
   * Adds the javascript.
   *
   * @param portalNode the portal node
   * @param jsFile the js data
   * @param isStartup
   */
  private void addSharedPortalJavascript(Node portalNode, Node jsFile, boolean isStartup) throws Exception {
    if (portalNode != null) {
      String moduleName = MODULE_NAME + '.' + portalNode.getName();
      if (!loadedSharedJSModule.contains(moduleName)) {
        loadedSharedJSModule.add(moduleName);
      }
      jsCache_.clearCache();
      jsLastModifiedDate = System.currentTimeMillis();
    }
  }

  private String getActivedJSData(Node jsFile) throws ValueFormatException,
                                          RepositoryException,
                                          PathNotFoundException {
    if (jsFile != null && !jsFile.isNodeType("exo:restoreLocation")
        && jsFile.hasNode(NodetypeConstant.JCR_CONTENT)
        && jsFile.getNode(NodetypeConstant.JCR_CONTENT).hasProperty(NodetypeConstant.JCR_DATA)
        && jsFile.hasProperty(NodetypeConstant.EXO_ACTIVE)
        && jsFile.getProperty(NodetypeConstant.EXO_ACTIVE).getBoolean() == true) {

      return jsFile.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_DATA).getString();
    }
    return "";
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
      List<Node> livePortals = livePortalManagerService.getLivePortals(sessionProvider);
      for(Node portal: livePortals) {
        addPortalJavascript(portal, null, true);
      }
      Node sharedPortal = livePortalManagerService.getLiveSharedPortal(sessionProvider);
      addSharedPortalJavascript(sharedPortal, null, true);
    } catch (PathNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Exception when merging inside Portal : WCM init is not completed.");
      }
    } catch (Exception e) {
      LOG.error("Exception when start XJavascriptService", e);
    } finally {
      sessionProvider.close();
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }

  public long getJsLastModifiedDate() {
    return jsLastModifiedDate;
  }
}
