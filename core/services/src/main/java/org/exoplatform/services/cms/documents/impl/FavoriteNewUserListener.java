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
package org.exoplatform.services.cms.documents.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Listener to add exo:addToFavoriteAction action to favorite node
 * Created by The eXo Platform SAS
 * Author : dongpd@exoplatform.com
 *        : phamdong@gmail.com
 * Sep 22, 2011
 */
public class FavoriteNewUserListener extends UserEventListener {

  private static final String ADD_TO_FAVORITE_ACTION = "addToFavorite";

  private ActionServiceContainer actionServiceContainer_;

  private TemplateService templateService_;

  private NodeHierarchyCreator nodeHierarchyCreator_;

  private static final String  FAVORITE_ALIAS = "userPrivateFavorites";

  /**
   * @param driveService
   * @param nodeHierarchyCreatorService
   * @param params
   * @throws Exception
   */
  public FavoriteNewUserListener(NodeHierarchyCreator nodeHierarchyCreatorService,
                         FavoriteService favoriteService,
                         ActionServiceContainer actionServiceContainer,
                         TemplateService templateService) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreatorService;
    actionServiceContainer_ = actionServiceContainer;
    templateService_ = templateService;
  }

  public void postSave(User user, boolean isNew) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, user.getUserName());
    String favoritePath = nodeHierarchyCreator_.getJcrPath(FAVORITE_ALIAS);
    Node favoriteNode = userNode.getNode(favoritePath);
    applyAddToFavoriteAction(favoriteNode);
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
    actionServiceContainer_.addAction(favoriteNode, "exo:addToFavoriteAction", mappings);

    // Specify affected Node Type Names
    Node actionNode = actionServiceContainer_.getAction(favoriteNode, ADD_TO_FAVORITE_ACTION);
    actionNode.addMixin("mix:affectedNodeTypes");
    actionNode.setProperty("exo:affectedNodeTypeNames", templateService_.getAllDocumentNodeTypes().toArray(new String[0]));
    actionNode.save();
  }
}
