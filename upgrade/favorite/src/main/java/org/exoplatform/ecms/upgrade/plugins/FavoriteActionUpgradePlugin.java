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
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
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
  private static final String USER_ALIAS = "usersPath";
  private static final String ADD_TO_FAVORITE_ACTION = "addToFavorite";
  private static final String NODE_TYPE_ADD_TO_FAVORITE_ACTION = "exo:addToFavoriteAction";
  private static final String FILE_NAME_ADD_TO_FAVORITE_ACTION = "AddToFavoriteScript.groovy";
  private static final String NT_UNSTRUCTURED = "nt:unstructured";
  private static final String EXO_FAVORITEFOLDER = "exo:favoriteFolder";
  private static final String EXO_PRIVILEGEABLE = "exo:privilegeable";

  private ActionServiceContainer actionServiceContainer;
  private TemplateService templateService;
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
                                     TemplateService templateService,
                                     NodeHierarchyCreator nodeHierarchyCreator,
                                     OrganizationService organizationService,
                                     InitParams initParams) {
    super(initParams);

    // Get services
    this.nodeHierarchyCreator =  nodeHierarchyCreator;
    this.organizationService = organizationService;
    this.actionServiceContainer = actionServiceContainer;
    this.templateService = templateService;
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
    SessionProvider sessionProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Start " + this.getClass().getName() + ".............");
      }
      RequestLifeCycle.begin(PortalContainer.getInstance());

      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(dmsConfiguration.getConfig().getSystemWorkspace(),
                                                   repoService.getCurrentRepository());

      // Register Script if necessary
      String scriptPath = nodeHierarchyCreator.getJcrPath(BasePath.ECM_ACTION_SCRIPTS) +
                          "/" + FILE_NAME_ADD_TO_FAVORITE_ACTION;
      try {
        session.getItem(scriptPath);
      }
      catch (PathNotFoundException pne) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(configurationManager.getURL("classpath:/script/AddToFavoriteScript.groovy").openStream(), writer);
        String scriptContent = writer.toString();
        scriptService.addScript("ecm-explorer/action/" + FILE_NAME_ADD_TO_FAVORITE_ACTION, scriptContent, sessionProvider);
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
//      ListAccess<User> userListAccess = organizationService.getUserHandler().findAllUsers();
//      List<User> userList = WCMCoreUtils.getAllElementsOfListAccess(userListAccess);
      session = sessionProvider.getSession(repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName(),
                                           repoService.getCurrentRepository());
      String usersNodePath = nodeHierarchyCreator.getJcrPath(USER_ALIAS);
      if (!usersNodePath.endsWith("/")) {
        usersNodePath += "/";
      }
      int count = 0;
      NodeIterator nodeIter = session.getWorkspace().getQueryManager().createQuery(
                                   "SELECT * FROM exo:favoriteFolder WHERE jcr:path like '" + usersNodePath + "%'", Query.SQL).
                                   execute().getNodes();
      while (nodeIter.hasNext()) {
        Node favoriteNode = nodeIter.nextNode();

        if (actionServiceContainer.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION) != null) {
          actionServiceContainer.removeAction(favoriteNode, ADD_TO_FAVORITE_ACTION, 
                                              repoService.getCurrentRepository().getConfiguration().getName());
          count++;
          if ((count) % 100 == 0) {
          if (LOG.isInfoEnabled()) {
        	StringBuilder infor = new StringBuilder(this.getClass().getSimpleName()).append(": ").append(count).append(" users done!");
        	LOG.info(infor.toString());
          }
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
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
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
