/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import java.util.Map;


import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.IdentityConstants;
/**
 * Created by The eXo Platform SAS
 * Author : dongpd@exoplatform.com
 *        : phamdong@gmail.com
 * Sep 22, 2011
 */
public class AddToFavoriteScript implements CmsScript {

  private static Log        LOG = ExoLogger.getLogger("AddToFavoriteScript");
  
  private static final String  FAVORITE_ALIAS = "userPrivateFavorites";

  public AddToFavoriteScript() {
  }

  public void execute(Object context) {
    RepositoryService repositoryService_ = WCMCoreUtils.getService(RepositoryService.class);
    FavoriteService   favoriteService_ = WCMCoreUtils.getService(FavoriteService.class);
    NodeHierarchyCreator nodeHierarchyCreator_ = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    TemplateService templateService_ = WCMCoreUtils.getService(TemplateService.class);
    
    Map variables = (Map) context;
    String nodePath = (String) variables.get("nodePath");
    String workspace = (String) variables.get("srcWorkspace");
    String activateUser = null;
    if (variables.containsKey("userId")) {
      activateUser = (String) variables.get("userId");
    }
    try {
      if (ConversationState.getCurrent() == null) { 
        return;
      }
      String userID = ConversationState.getCurrent().getIdentity().getUserId();
      if(userID.equals(IdentityConstants.ANONIM) || userID.equals(IdentityConstants.SYSTEM) || (activateUser != null && activateUser.equals(IdentityConstants.SYSTEM))) return;
      ExtendedNode userNode = (ExtendedNode)nodeHierarchyCreator_.getUserNode(WCMCoreUtils.getSystemSessionProvider(), userID);
      String favoritePath = nodeHierarchyCreator_.getJcrPath(FAVORITE_ALIAS);
      ExtendedNode favoriteNode = (ExtendedNode)userNode.getNode(favoritePath);
      if (nodePath.startsWith(favoriteNode.getPath())) { 
        // Get new added node
        ExtendedNode addedNode = (ExtendedNode) WCMCoreUtils.getSystemSessionProvider().
        getSession(workspace, repositoryService_.getCurrentRepository()).getItem(nodePath);
        if (templateService_.getAllDocumentNodeTypes().contains(addedNode.getPrimaryNodeType().getName())) {
          // Add new node to favorite
          favoriteService_.addFavorite(addedNode, userID);
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Add Favorite failed", e);
      }
    }
  }

  public void setParams(String[] params) {
  }

}
