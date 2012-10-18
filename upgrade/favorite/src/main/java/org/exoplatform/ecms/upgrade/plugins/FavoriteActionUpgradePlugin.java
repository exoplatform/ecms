/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.plugins;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Add exo:addToFavoriteAction action to all favorite node of all user which have not that action yet
 *
 * Author : eXoPlatform
 *          dongpd@exoplatform.com
 * Oct 4, 2011
 */
public class FavoriteActionUpgradePlugin extends UpgradeProductPlugin {
  private static final Log LOG = ExoLogger.getLogger(FavoriteActionUpgradePlugin.class.getName());

  private static final String FAVORITE_ALIAS = "userPrivateFavorites";
  private static final String ADD_TO_FAVORITE_ACTION = "addToFavorite";
  private static final String NODE_TYPE_ADD_TO_FAVORITE_ACTION = "exo:addToFavoriteAction";
  private static final String FILE_NAME_ADD_TO_FAVORITE_ACTION = "AddToFavoriteScript.groovy";
  private static final String SCRIPT_PATH_ADD_TO_FAVORITE_ACTION
    = "war:/conf/dms-extension/dms/artifacts/scripts/ecm-explorer/action/AddToFavoriteScript.groovy";
  private static final String NT_UNSTRUCTURED = "nt:unstructured";
  private static final String EXO_FAVORITEFOLDER = "exo:favoriteFolder";
  private static final String EXO_PRIVILEGEABLE = "exo:privilegeable";

  private ActionServiceContainer actionServiceContainer;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private OrganizationService organizationService;  
  private DMSConfiguration dmsConfiguration;
  private RepositoryService repoService;
  private ScriptService scriptService;
  private ConfigurationManager configurationManager;

  public FavoriteActionUpgradePlugin(RepositoryService repoService,
                                     DMSConfiguration dmsConfiguration,
                                     ScriptService scriptService,
                                     ConfigurationManager configurationManager,
                                     ActionServiceContainer actionServiceContainer,
                                     NodeHierarchyCreator nodeHierarchyCreator,
                                     OrganizationService organizationService,
                                     InitParams initParams) {
    super(initParams);

    // Get services
    this.nodeHierarchyCreator =  nodeHierarchyCreator;
    this.organizationService = organizationService;    
    this.actionServiceContainer = actionServiceContainer;
    this.repoService = repoService;
    this.dmsConfiguration = dmsConfiguration;
    this.scriptService = scriptService;
    this.configurationManager = configurationManager;
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Start " + this.getClass().getName() + ".............");
      }
      RequestLifeCycle.begin(PortalContainer.getInstance());

      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession(dmsConfiguration.getConfig().getSystemWorkspace(),
                                                   repoService.getCurrentRepository());

      // Register Favorite Script if necessary
      String scriptPath = nodeHierarchyCreator.getJcrPath(BasePath.ECM_ACTION_SCRIPTS) +
                          "/" + FILE_NAME_ADD_TO_FAVORITE_ACTION;
      Node currScriptNode = null;
      String lastestScriptContent = getLastestFavoriteScriptContent();
      try {
        currScriptNode = (Node)session.getItem(scriptPath);
      }
      catch (PathNotFoundException pne) {
        registerFavoriteScript(sessionProvider, lastestScriptContent);
      }

      // Update Favorite script to lastest version if it has a new version
      if (currScriptNode != null) {
        String currScriptContent =
            currScriptNode.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_DATA).getString();
        if (!currScriptContent.equals(lastestScriptContent)) {
          currScriptNode.remove();
          session.save();
          registerFavoriteScript(sessionProvider, lastestScriptContent);
        }
      }

      // Register Node Type exo:addToFavoriteAction if neccessary
      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
      try {
        nodeTypeManager.getNodeType(NODE_TYPE_ADD_TO_FAVORITE_ACTION);
      } catch (NoSuchNodeTypeException e) {
        nodeTypeManager.registerNodeTypes(
          configurationManager.getURL("classpath:/conf/portal/AddToFavoriteAction_NodeType_Definition.xml").openStream(),
          ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
          NodeTypeDataManager.TEXT_XML);
      }

      // Get all users and remove exo:addToFavoriteAction action for favorite folder
      ListAccess<User> userListAccess = organizationService.getUserHandler().findAllUsers();
      Node favoriteNode = null;
      for (User user : userListAccess.load(0, userListAccess.getSize())) {
        String userName = user.getUserName();
        Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userName);
        String favoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);

        try {
          favoriteNode = userNode.getNode(favoritePath);
        }
        catch (PathNotFoundException pne) {
          favoriteNode = createFavoriteFolder(userName);
        }

        if (favoriteNode != null) {
          if (actionServiceContainer.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION) != null) {
            actionServiceContainer.removeAction(favoriteNode, ADD_TO_FAVORITE_ACTION,
                                                repoService.getCurrentRepository().getConfiguration().getName());
          }
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("End " + this.getClass().getName() + ".............");
      }
    }
    catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(this.getClass().getName() + " failed:", e);
      }
    }
    finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * Register Favorite Script.
   *
   * @param sessionProvider
   * @param scriptContent
   * @throws Exception
   */
  private void registerFavoriteScript(SessionProvider sessionProvider, String scriptContent) throws Exception {
    scriptService.addScript("ecm-explorer/action/" + FILE_NAME_ADD_TO_FAVORITE_ACTION, scriptContent, sessionProvider);
  }

  /**
   * Get lastest addtofavorite.groovy script
   *
   * @return lastest content
   * @throws Exception
   */
  private String getLastestFavoriteScriptContent() throws Exception {
    StringWriter writer = new StringWriter();
    IOUtils.copy(configurationManager.getURL(SCRIPT_PATH_ADD_TO_FAVORITE_ACTION).openStream(), writer);
    return writer.toString();
  }
  /**
   * Create Favorite Folder.
   *
   * @param userName UserName
   * @return Favorite Node
   * @throws Exception
   */
  private Node createFavoriteFolder(String userName) throws Exception {
    Node userFavoriteNode = null;
    try {
      // Get default favorite path
      Node userNode =
        nodeHierarchyCreator.getUserNode(WCMCoreUtils.getSystemSessionProvider(), userName);
      String userFavoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);

      // Create favorite path
      userFavoriteNode = userNode.addNode(userFavoritePath, NT_UNSTRUCTURED);

      // Add Mixin types
      userFavoriteNode.addMixin(EXO_PRIVILEGEABLE);
      userFavoriteNode.addMixin(EXO_FAVORITEFOLDER);

      // Add permission
      Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
      permissionsMap.put(userName, PermissionType.ALL);
      ((ExtendedNode)userFavoriteNode).setPermissions(permissionsMap);

      userNode.getSession().save();

    } catch (PathNotFoundException pne) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Private Folder of User " + userName + " not found");
      }
    }
    return userFavoriteNode;
  }

}
