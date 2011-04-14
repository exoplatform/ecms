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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public class XJavascriptService implements Startable {

  /** The SHARE d_ j s_ query. */
  private static String           SHARED_JS_QUERY     = "select * from exo:jsFile "
                                                          + "where jcr:path like '{path}/%' "
                                                          + "and exo:active='true' and exo:sharedJS='true' "
                                                          + "order by exo:priority ASC".intern();

  private static String           WEBCONTENT_JS_QUERY = "select * from exo:jsFile "
                                                          + "where jcr:path like '{path}/%' and exo:active='true' "
                                                          + "order by exo:priority ASC".intern();

  /** The MODUL e_ name. */
  final private String MODULE_NAME = "eXo.WCM.Live".intern();

  /** The PATH. */
  final private String PATH = "/javascript/eXo/{portalName}/live".intern();

  /** The js config service. */
  private JavascriptConfigService jsConfigService ;

  /** The configuration service. */
  private WCMConfigurationService configurationService;

  /** The schema config service. */
  private WebSchemaConfigService schemaConfigService;

  /** The s context. */
  private ServletContext sContext ;

  /** The log. */
  private Log log = ExoLogger.getLogger("wcm:XJavascriptService");

  /**
   * Instantiates a new x javascript service.
   *
   * @param repositoryService the repository service
   * @param jsConfigService the js config service
   * @param servletContext the servlet context
   * @param configurationService the configuration service
   * @param contentInitializerService the content initializer service
   *
   * @throws Exception the exception
   */
  public XJavascriptService() throws Exception{
    this.jsConfigService = WCMCoreUtils.getService(JavascriptConfigService.class);
    this.sContext = WCMCoreUtils.getService(ServletContext.class);
    this.schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    this.configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
  }

  /**
   * Get active java script.
   *
   * @param home the home
   * @return Code of all js file in home node.
   * @throws Exception the exception
   */
  public String getActiveJavaScript(Node webcontent) throws Exception {
    StringBuffer buffer = new StringBuffer();
    String jsQuery = StringUtils.replaceOnce(WEBCONTENT_JS_QUERY, "{path}", webcontent.getPath());

    // Need re-login to get session because this node is get from template and the session is not live anymore.
    NodeLocation webcontentLocation = NodeLocation.make(webcontent);
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
      buffer.append(registeredJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                    .getProperty(NodetypeConstant.JCR_DATA)
                                    .getString());
    }
    session.logout();
    return buffer.toString();
  }

  /**
   * Update and merged all Java Script in all portal when content of js file is modified.
   *
   * @param jsFile the js file
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void updatePortalJSOnModify(Node portalNode, Node jsFile) throws Exception {
    String repository = ((ManageableRepository) portalNode.getSession().getRepository()).getConfiguration()
                                                                                        .getName();
    String sharedPortalName = configurationService.getSharedPortalName(repository);
    if(sharedPortalName.equals(portalNode.getName())) {
      addSharedPortalJavascript(portalNode, jsFile, false);
    }else {
      addPortalJavascript(portalNode, jsFile, false);
    }
  }

  /**
   * Update and merged all Java Script in all portal when content of js file is modified.
   *
   * @param jsFile the js file
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void updatePortalJSOnRemove(Node portalNode, Node jsFile) throws Exception {
    String repository = ((ManageableRepository) portalNode.getSession().getRepository()).getConfiguration()
                                                                                        .getName();
    String sharedPortalName = configurationService.getSharedPortalName(repository);
    if(sharedPortalName.equals(portalNode.getName())) {
      addSharedPortalJavascript(portalNode, jsFile, false);
    }else {
      addPortalJavascript(portalNode, jsFile, false);
    }
  }

  /**
   * Adds the javascript.
   *
   * @param jsData the js data
   */
  private void addPortalJavascript(Node portalNode, Node jsFile, boolean isStartup) throws Exception {
    String javascriptPath = StringUtils.replaceOnce(PATH, "{portalName}", portalNode.getName());
    String jsData = mergeJSData(portalNode, jsFile, isStartup);
    if(jsConfigService.isModuleLoaded(MODULE_NAME)) {
      jsConfigService.removeExtendedJavascript(MODULE_NAME, javascriptPath, sContext) ;
    }
    jsConfigService.addExtendedJavascript(MODULE_NAME, javascriptPath, sContext, jsData) ;
  }

  /**
   * Adds the javascript.
   *
   * @param jsData the js data
   */
  private void addSharedPortalJavascript(Node portalNode, Node jsFile, boolean isStartup) throws Exception {
    String javascriptPath = StringUtils.replaceOnce(PATH, "{portalName}", portalNode.getName());
    String jsData = mergeJSData(portalNode, jsFile, isStartup);
    if(jsConfigService.isModuleLoaded(MODULE_NAME)) {
      jsConfigService.removeExtendedJavascript(MODULE_NAME, javascriptPath, sContext) ;
    }
    jsConfigService.addExtendedJavascript(MODULE_NAME, javascriptPath, sContext, jsData) ;
  }

  /**
   * Gets the jS data by sql query.
   *
   * @param session the session
   * @param queryStatement the query statement
   * @param exceptPath the except path
   *
   * @return the jS data by sql query
   *
   * @throws Exception the exception
   */
  private String mergeJSData(Node portalNode, Node newJSFile, boolean isStartup) throws Exception {
    StringBuffer buffer = new StringBuffer();

    // Get all js by query
    Node jsFolder = schemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class)
                                       .getJSFolder(portalNode);
    String statement = StringUtils.replaceOnce(SHARED_JS_QUERY, "{path}", jsFolder.getPath());
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    NodeLocation portalNodeLocation = NodeLocation.make(portalNode);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(portalNodeLocation.getWorkspace(), repository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    QueryResult queryResult = null;
    Query query = queryManager.createQuery(statement, Query.SQL);
    queryResult = query.execute();
    NodeIterator iterator = queryResult.getNodes();

    if (isStartup) {
      while(iterator.hasNext()) {
        Node registeredJSFile = iterator.nextNode();
        buffer.append(registeredJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                      .getProperty(NodetypeConstant.JCR_DATA)
                                      .getString());
      }
    } else {
      boolean isAdded = false;
      while(iterator.hasNext()) {
        Node registeredJSFile = iterator.nextNode();
        // Add new
        long newJSFilePriority = newJSFile.getProperty(NodetypeConstant.EXO_PRIORITY).getLong();
        long registeredJSFilePriority = registeredJSFile.getProperty(NodetypeConstant.EXO_PRIORITY).getLong();
        if (!isAdded && newJSFilePriority < registeredJSFilePriority) {
          buffer.append(newJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                 .getProperty(NodetypeConstant.JCR_DATA)
                                 .getString());
          isAdded = true;
          continue;
        }
        // Modify
        if (newJSFile.getPath().equals(registeredJSFile.getPath())) {
          buffer.append(newJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                 .getProperty(NodetypeConstant.JCR_DATA)
                                 .getString());
          continue;
        }
        buffer.append(registeredJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                      .getProperty(NodetypeConstant.JCR_DATA)
                                      .getString());
      }
    }

    return buffer.toString();
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
      Node sharedPortal = livePortalManagerService.getLiveSharedPortal(sessionProvider);
      addSharedPortalJavascript(sharedPortal, null, true);
      List<Node> livePortals = livePortalManagerService.getLivePortals(sessionProvider);
      for(Node portal: livePortals) {
        addPortalJavascript(portal, null, true);
      }
    } catch (PathNotFoundException e) {
      log.warn("Exception when merging inside Portal : WCM init is not completed.");
    }catch (Exception e) {
      log.error("Exception when start XJavascriptService");
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }
}
