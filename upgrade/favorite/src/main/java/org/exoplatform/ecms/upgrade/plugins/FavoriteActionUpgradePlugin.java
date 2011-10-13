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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.templates.TemplateService;
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
 *          exo@exoplatform.com
 * Oct 4, 2011
 */
public class FavoriteActionUpgradePlugin extends UpgradeProductPlugin {
  private Log log = ExoLogger.getLogger(this.getClass());

  private static final String  FAVORITE_ALIAS = "userPrivateFavorites";
  private static final String ADD_TO_FAVORITE_ACTION = "addToFavorite";

  private ActionServiceContainer actionServiceContainer;
  private TemplateService templateService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private OrganizationService organizationService;

  public FavoriteActionUpgradePlugin(ActionServiceContainer actionServiceContainer,
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
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    try {
      log.info("Start " + this.getClass().getName() + ".............");

      RequestLifeCycle.begin(PortalContainer.getInstance());

      // Get all users and apply exo:addToFavoriteAction action for favorite folder
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      ListAccess<User> userListAccess = organizationService.getUserHandler().findAllUsers();
      List<User> userList = WCMCoreUtils.getAllElementsOfListAccess(userListAccess);
      for (User user : userList) {
        Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, user.getUserName());
        String favoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);
        Node favoriteNode = userNode.getNode(favoritePath);
        if (actionServiceContainer.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION) == null)
          applyAddToFavoriteAction(favoriteNode);
      }
      log.info("End " + this.getClass().getName() + ".............");
    }
    catch (Exception e) {
      log.error(this.getClass().getName() + " failed:", e);
    }
    finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * Apply AddToFavoriteAction action to favorite Node.
   * When user create new document node in favorite Node, it will be add favorite too.
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
    actionNode.setProperty("exo:affectedNodeTypeNames", templateService.getAllDocumentNodeTypes().toArray(new String[0]));
    actionNode.save();
  }
}
