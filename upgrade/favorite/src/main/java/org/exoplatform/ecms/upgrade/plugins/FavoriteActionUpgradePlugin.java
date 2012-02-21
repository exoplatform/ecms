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

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
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
  private Log log = ExoLogger.getLogger(this.getClass());

  private static final String  FAVORITE_ALIAS = "userPrivateFavorites";
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
  private FavoriteService favoriteService;

  public FavoriteActionUpgradePlugin(RepositoryService repoService,
                                     DMSConfiguration dmsConfiguration,
                                     ScriptService scriptService,
                                     ConfigurationManager configurationManager,
                                     FavoriteService favoriteService,
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
    this.favoriteService = favoriteService;
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    try {
      if (log.isInfoEnabled()) {
        log.info("Start " + this.getClass().getName() + ".............");
      }
      RequestLifeCycle.begin(PortalContainer.getInstance());
      
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
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

      // Get all users and apply exo:addToFavoriteAction action for favorite folder
      ListAccess<User> userListAccess = organizationService.getUserHandler().findAllUsers();
      List<User> userList = WCMCoreUtils.getAllElementsOfListAccess(userListAccess);
      Node favoriteNode = null;
      for (User user : userList) {
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
          if (actionServiceContainer.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION) == null) {
            applyAddToFavoriteAction(favoriteNode);
          }
          setFavoritesForOldItems(favoriteNode, userName);
        }
      }
      if (log.isInfoEnabled()) {
        log.info("End " + this.getClass().getName() + ".............");
      }
    }
    catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(this.getClass().getName() + " failed:", e);
      }
    }
    finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * Set favorites for all document nodes which stay in specified folder.
   * 
   * @param node Specified Node
   * @param userName UserName
   * @throws Exception
   */
  private void setFavoritesForOldItems(Node node, String userName) throws Exception {
    NodeIterator iterrator = node.getNodes();
    while (iterrator.hasNext()) {
      Node child = (Node) iterrator.next();
      if (isDocument(child) && !favoriteService.isFavoriter(userName, child)) {
        favoriteService.addFavorite(child, userName);
      }
      setFavoritesForOldItems(child, userName);
    }
  }
  
  /**
   * Check if node is document.
   * 
   * @param node a Node
   * @return true: is document; false: is not document 
   * @throws Exception 
   */
  private boolean isDocument(Node node) throws Exception {
    NodeType nodeType = node.getPrimaryNodeType();
    return templateService.getDocumentTemplates().contains(nodeType.getName());
  }
  
  /**
   * Apply AddToFavoriteAction action to favorite Node.
   * When user create new document node in favorite Node, it will be add favorite too.
   * 
   * @param favoriteNode Favorite Node
   * @throws Exception
   */
  private void applyAddToFavoriteAction(Node favoriteNode) throws Exception {
    Map<String,JcrInputProperty> mappings = new HashMap<String,JcrInputProperty>();

    JcrInputProperty nodeTypeInputProperty = new JcrInputProperty();
    nodeTypeInputProperty.setJcrPath("/node");
    nodeTypeInputProperty.setValue(ADD_TO_FAVORITE_ACTION);
    mappings.put("/node", nodeTypeInputProperty);

    // Define name of action
    JcrInputProperty nameInputProperty = new JcrInputProperty();
    nameInputProperty.setJcrPath("/node/exo:name");
    nameInputProperty.setValue(ADD_TO_FAVORITE_ACTION);
    mappings.put("/node/exo:name", nameInputProperty);

    // Define lifecylePhase of action
    JcrInputProperty lifeCycleInputProperty = new JcrInputProperty();
    lifeCycleInputProperty.setJcrPath("/node/exo:lifecyclePhase");
    lifeCycleInputProperty.setValue(new String[]{"node_added"});
    mappings.put("/node/exo:lifecyclePhase", lifeCycleInputProperty);

    // Define isDeep property
    JcrInputProperty deepInputProperty = new JcrInputProperty();
    deepInputProperty.setJcrPath("/node/exo:isDeep");
    deepInputProperty.setValue(true);
    mappings.put("/node/exo:isDeep", deepInputProperty);

    // Define description of action
    JcrInputProperty descriptionInputProperty = new JcrInputProperty();
    descriptionInputProperty.setJcrPath("/node/exo:description");
    descriptionInputProperty.setValue("auto Add favorite when new document node created at favorite");
    mappings.put("/node/exo:description", descriptionInputProperty);

    // Add action
    actionServiceContainer.addAction(favoriteNode, "exo:addToFavoriteAction", mappings);

    // Specify affected Node Type Names
    Node actionNode = actionServiceContainer.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION);
    actionNode.addMixin("mix:affectedNodeTypes");
    actionNode.setProperty("exo:affectedNodeTypeNames",
                           templateService.getAllDocumentNodeTypes().toArray(new String[0])
                           );
    actionNode.save();
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
      if (log.isWarnEnabled()) {
        log.warn("Private Folder of User " + userName + " not found");
      }
    }
    return userFavoriteNode;
  }
}
