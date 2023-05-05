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
package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.*;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 16, 2009
 * 10:02:04 AM
 */
public class FavoriteServiceImpl implements org.exoplatform.services.cms.documents.FavoriteService {

  private IdentityManager identityManager;
  private FavoriteService favoriteService;
  private MetadataService metadataService;
  private SessionProviderService sessionProviderService;
  private RepositoryService repositoryService;

  private static final Log LOG = ExoLogger.getLogger(FavoriteServiceImpl.class);

  public static final MetadataType METADATA_TYPE = new MetadataType(1, "favorites");

  public FavoriteServiceImpl(IdentityManager identityManager, FavoriteService favoriteService, MetadataService metadataService, SessionProviderService sessionProviderService, RepositoryService repositoryService) {
    this.identityManager = identityManager;
    this.favoriteService = favoriteService;
    this.metadataService = metadataService;
    this.sessionProviderService = sessionProviderService;
    this.repositoryService = repositoryService;
  }

  /**
   * {@inheritDoc}
   */
  public void addFavorite(Node node, String userName) throws Exception {
    Identity identity = identityManager.getOrCreateUserIdentity(userName);
    if (identity != null) {
      Favorite favorite = new Favorite("file", node.getUUID(), "", Long.parseLong(identity.getId()));
      favoriteService.createFavorite(favorite);
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllFavoriteNodesByUser(String userName, int limit) throws Exception {
    
    List<Node> ret = new ArrayList<Node>();
    Identity identity = identityManager.getOrCreateUserIdentity(userName);
    if(identity != null){
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Session session = sessionProvider.getSession(repositoryService.getCurrentRepository()
              .getConfiguration()
              .getDefaultWorkspaceName(), repositoryService.getCurrentRepository());
    List<MetadataItem> favorites = metadataService.getMetadataItemsByMetadataNameAndTypeAndObject(String.valueOf(identity.getId()),METADATA_TYPE.getName(),"file",0,limit);
    for(MetadataItem favorite : favorites){
      try {
        Node node =  session.getNodeByUUID(favorite.getObjectId());
        if(node != null){
          ret.add(node);
        }
      } catch (RepositoryException repositoryException) {
        LOG.warn("Can't get Favorite Node with UUID {}",favorite.getObjectId());
      }
    }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  public void removeFavorite(Node node, String userName) throws Exception {
    Identity identity = identityManager.getOrCreateUserIdentity(userName);
    Favorite favorite = new Favorite("file", node.getUUID(), "", Long.parseLong(identity.getId()));
    favoriteService.deleteFavorite(favorite);
  }

  public boolean isFavoriter(String userName, Node node) throws Exception {
    try {
      Identity identity = identityManager.getOrCreateUserIdentity(userName);
      if(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
        Favorite favorite = new Favorite("file", node.getUUID(), "", Long.parseLong(identity.getId()));
        return favoriteService.isFavorite(favorite);
      }
      return false;
    } catch (Exception e) {
      LOG.warn("Cannot get the identifier of the node", e.getMessage());
      return false;
    }
  }

}
