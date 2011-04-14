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

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.resource.SkinService;
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
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS Author : Hoa.Pham hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public class XSkinService implements Startable {

  /** The SHARE d_ cs s_ query. */
  private static String           SHARED_CSS_QUERY     = "select * from exo:cssFile where jcr:path like '{path}/%' "
                                                           + "and exo:active='true' and exo:sharedCSS='true' "
                                                           + "order by exo:priority DESC".intern();

  private static String           WEBCONTENT_CSS_QUERY = "select * from exo:cssFile where jcr:path like '{path}/%' "
                                                           + "and exo:active='true' "
                                                           + "order by exo:priority DESC".intern();

  /** The Constant SKIN_PATH_REGEXP. */
  public final static String      SKIN_PATH_REGEXP     = "/(.*)/css/jcr/(.*)/(.*)/(.*).css".intern();

  /** The Constant SKIN_PATH_PATTERN. */
  private final static String     SKIN_PATH_PATTERN    = "/{docBase}/css/jcr/(.*)/(.*)/Stylesheet.css".intern();

  /** The log. */
  private static Log              log                  = ExoLogger.getLogger("wcm:XSkinService");

  /** The schema config service. */
  private WebSchemaConfigService schemaConfigService;

  /** The configuration service. */
  private WCMConfigurationService configurationService;

  private RepositoryService repositoryService;

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
   * @throws Exception the exception
   */
  public XSkinService() throws Exception {
    this.skinService = WCMCoreUtils.getService(SkinService.class);
    this.skinService.addResourceResolver(new WCMSkinResourceResolver(this.skinService));
    this.configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    this.schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    this.servletContext = WCMCoreUtils.getService(ServletContext.class);
    this.repositoryService = WCMCoreUtils.getService(RepositoryService.class);
  }

  /**
   * Gets the active stylesheet.
   *
   * @param home the home
   *
   * @return the active stylesheet
   *
   * @throws Exception the exception
   */
  public String getActiveStylesheet(Node webcontent) throws Exception {
    StringBuffer buffer = new StringBuffer();
    String cssQuery = StringUtils.replaceOnce(WEBCONTENT_CSS_QUERY, "{path}", webcontent.getPath());

    // Need re-login to get session because this node is get from template and the session is not live anymore.
    // If node is version (which is stored in system workspace) we have to login to system workspace to get data
    NodeLocation webcontentLocation = NodeLocation.make(webcontent);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = null;
    try {
      if (webcontentLocation.getPath().startsWith("/jcr:system"))
        session = repository.getSystemSession(repository.getConfiguration().getSystemWorkspaceName());
      else {
        session = repository.getSystemSession(webcontentLocation.getWorkspace());
      }

      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(cssQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();
      while (iterator.hasNext()) {
        Node registeredCSSFile = iterator.nextNode();
        buffer.append(registeredCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                       .getProperty(NodetypeConstant.JCR_DATA)
                                       .getString());
      }
    } catch(Exception e) {
      log.error("Unexpected problem happen when active stylesheet", e);
    } finally {
      if(session != null) session.logout();
    }
    return buffer.toString();
  }

  /**
   * Update portal skin on modify.
   *
   * @param cssFile the css file
   * @param portal the portal
   *
   * @throws Exception the exception
   */
  public void updatePortalSkinOnModify(Node portal, Node cssFile) throws Exception {
    String repository = ((ManageableRepository) portal.getSession().getRepository()).getConfiguration()
                                                                                    .getName();
    String sharedPortalName = configurationService.getSharedPortalName(repository);
    if (sharedPortalName.equals(portal.getName())) {
      addSharedPortalSkin(portal, cssFile, false);
    } else {
      addPortalSkin(portal, cssFile, false);
    }
  }

  /**
   * Update portal skin on modify.
   *
   * @param cssFile the css file
   * @param portal the portal
   *
   * @throws Exception the exception
   */
  public void updatePortalSkinOnRemove(Node portal, Node cssFile) throws Exception {
    String repository = ((ManageableRepository) portal.getSession().getRepository()).getConfiguration()
                                                                                    .getName();
    String sharedPortalName = configurationService.getSharedPortalName(repository);
    if (sharedPortalName.equals(portal.getName())) {
      addSharedPortalSkin(portal, cssFile, false);
    } else {
      addPortalSkin(portal, cssFile, false);
    }
  }

  /**
   * Adds the portal skin.
   *
   * @param portal the portal
   * @param preStatement the pre statement
   * @param exceptedPath the excepted path
   * @param appendedCSS the appended css
   * @param allowEmptyCSS the allow empty css
   *
   * @throws Exception the exception
   */
  private void addPortalSkin(Node portalNode, Node cssFile, boolean isStartup) throws Exception {
    String cssData = mergeCSSData(portalNode, cssFile, isStartup);
    String skinPath = StringUtils.replaceOnce(SKIN_PATH_PATTERN, "(.*)", portalNode.getName())
                                 .replaceFirst("\\{docBase\\}",
                                               servletContext.getServletContextName());
    Iterator<String> iterator = skinService.getAvailableSkinNames().iterator();
    if (iterator.hasNext() == false) {
      skinPath = StringUtils.replaceOnce(skinPath,"(.*)", "Default");
      skinService.addSkin(portalNode.getName(), "Default", skinPath, cssData);
    } else {
      while (iterator.hasNext()) {
        String skinName = iterator.next();
        skinPath = StringUtils.replaceOnce(skinPath,"(.*)",skinName);
        skinService.addSkin(portalNode.getName(), skinName, skinPath, cssData);
      }
    }
  }

  /**
   * Adds the shared portal skin.
   *
   * @param portal the portal
   * @param preStatement the pre statement
   * @param exceptedPath the excepted path
   * @param appendedCSS the appended css
   * @param allowEmptyCSS the allow empty css
   *
   * @throws Exception the exception
   */
  private void addSharedPortalSkin(Node portalNode, Node cssFile, boolean isAddNew) throws Exception {
    String cssData = mergeCSSData(portalNode, cssFile, isAddNew);
    String skinPath = StringUtils.replaceOnce(SKIN_PATH_PATTERN, "(.*)", portalNode.getName())
                                 .replaceFirst("\\{docBase\\}",
                                               servletContext.getServletContextName());
    for (Iterator<String> iterator = skinService.getAvailableSkinNames().iterator(); iterator.hasNext();) {
      String skinName = iterator.next();
      skinPath = StringUtils.replaceOnce(skinPath, "(.*)", skinName);
      skinService.addPortalSkin(portalNode.getName(), skinName, skinPath, cssData);
    }
  }

  /**
   * Gets the cSS data by sql query.
   *
   * @param session the session
   * @param statement the statement
   * @param exceptedPath the excepted path
   *
   * @return the cSS data by sql query
   *
   * @throws Exception the exception
   */
  private String mergeCSSData(Node portalNode, Node newCSSFile, boolean isStartup) throws Exception {
    StringBuffer buffer = new StringBuffer();

    // Get all css by query
    Node cssFolder = schemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class)
                                        .getCSSFolder(portalNode);
    String statement = StringUtils.replaceOnce(SHARED_CSS_QUERY, "{path}", cssFolder.getPath());
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    NodeLocation portalNodeLocation = NodeLocation.make(portalNode);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(portalNodeLocation.getWorkspace(), repository);
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(statement, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();

      if (isStartup) {
        while (iterator.hasNext()) {
          Node registeredCSSFile = iterator.nextNode();
          buffer.append(registeredCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                         .getProperty(NodetypeConstant.JCR_DATA)
                                         .getString());
        }
      } else {
        boolean isAdded = false;
        while(iterator.hasNext()) {
          Node registeredCSSFile = iterator.nextNode();
          // Add new
          long newCSSFilePriority = newCSSFile.getProperty(NodetypeConstant.EXO_PRIORITY).getLong();
          long registeredCSSFilePriority = registeredCSSFile.getProperty(NodetypeConstant.EXO_PRIORITY).getLong();
          if (!isAdded && newCSSFilePriority < registeredCSSFilePriority) {
            buffer.append(newCSSFile.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_DATA).getString());
            isAdded = true;
            continue;
          }
          // Modify
          if (newCSSFile.getPath().equals(registeredCSSFile.getPath())) {
            buffer.append(newCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                    .getProperty(NodetypeConstant.JCR_DATA)
                                    .getString());
            continue;
          }
          buffer.append(registeredCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                         .getProperty(NodetypeConstant.JCR_DATA)
                                         .getString());
        }
      }
    } catch(Exception e) {
      log.error("Unexpected problem happen when merge CSS data", e);
    } finally {
      session.logout();
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
      addSharedPortalSkin(sharedPortal, null, true);
      List<Node> livePortals = livePortalManagerService.getLivePortals(sessionProvider);
      for (Node portal : livePortals) {
        addPortalSkin(portal, null, true);
      }
    } catch (Exception e) {
      log.error("Exception when start XSkinService", e);
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }

}
