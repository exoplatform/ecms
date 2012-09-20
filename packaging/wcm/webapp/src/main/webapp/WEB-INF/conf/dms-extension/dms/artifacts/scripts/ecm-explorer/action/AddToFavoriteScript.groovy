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

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
/**
 * Created by The eXo Platform SAS
 * Author : dongpd@exoplatform.com
 *        : phamdong@gmail.com
 * Sep 22, 2011
 */
public class AddToFavoriteScript implements CmsScript {

  private static Log        LOG = ExoLogger.getLogger("AddToFavoriteScript");
  
  private static final String  FAVORITE_ALIAS = "userPrivateFavorites";

  private RepositoryService repositoryService_;

  private FavoriteService   favoriteService_;
  
  private NodeHierarchyCreator nodeHierarchyCreator_;
  
  private TemplateService templateService_;

  public AddToFavoriteScript(RepositoryService repositoryService, FavoriteService favoriteService,
                             NodeHierarchyCreator nodeHierarchyCreator, TemplateService templateService) {
    repositoryService_ = repositoryService;
    favoriteService_ = favoriteService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    templateService_ = templateService;
  }

  public void execute(Object context) {
    Map variables = (Map) context;
    String nodePath = (String) variables.get("nodePath");
    String workspace = (String) variables.get("srcWorkspace");
    Session session = null;
    try {
      // Get new added node
      session = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, repositoryService_.getCurrentRepository());
      Node addedNode = (Node) session.getItem(nodePath);
      if (ConversationState.getCurrent() == null || 
          ConversationState.getCurrent().getIdentity() == null || 
          ConversationState.getCurrent().getIdentity().getUserId() == null) {
        return;
      }
      String userID = ConversationState.getCurrent().getIdentity().getUserId();
      Node userNode = nodeHierarchyCreator_.getUserNode(WCMCoreUtils.getSystemSessionProvider(), userID);
      String favoritePath = nodeHierarchyCreator_.getJcrPath(FAVORITE_ALIAS);
      Node favoriteNode = userNode.getNode(favoritePath);
      if (nodePath.startsWith(favoriteNode.getPath()) && 
          templateService_.getAllDocumentNodeTypes().contains(addedNode.getPrimaryNodeType().getName())) {
        // Add new node to favorite
        favoriteService_.addFavorite(addedNode, userID);
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
