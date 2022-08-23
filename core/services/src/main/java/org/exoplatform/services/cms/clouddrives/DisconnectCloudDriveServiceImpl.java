package org.exoplatform.services.cms.clouddrives;

import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

import javax.jcr.*;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisconnectCloudDriveServiceImpl implements DisconnectCloudDriveService {

  private DocumentFileService    documentFileService;

  private SessionProviderService sessionProviders;

  private NodeFinder             finder;

  private RepositoryService      jcrService;

  private IdentityManager        identityManager;

  private FavoriteService        favoriteService;

  public DisconnectCloudDriveServiceImpl(DocumentFileService documentFileService,
                                         SessionProviderService sessionProviders,
                                         NodeFinder finder,
                                         RepositoryService jcrService,
                                         IdentityManager identityManager,
                                         FavoriteService favoriteService) {
    this.documentFileService = documentFileService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.jcrService = jcrService;
    this.identityManager = identityManager;
    this.favoriteService = favoriteService;
  }

  @Override
  public void disconnectCloudDrive(String workspace, String path, String providerId) {
    long userIdentityId = getCurrentUserIdentityId();
    String userName = getCurrentUserIdentity().getUserId();
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = null;
    Item item = null;
    List<Node> nodes = new ArrayList<Node>();
    long delay = 6;
    try {
      userSession = sp.getSession(workspace, jcrService.getCurrentRepository());
      item = finder.findItem(userSession, path);
      Node userNode = (Node) item;
      StringBuilder queryStr = new StringBuilder().append("select * from ").append(JCRLocalCloudDrive.ECD_CLOUDDRIVE).
              append(" where ecd:localUserName='").append(userName).append("' AND ecd:provider='").append(providerId).append("'");
      Query q = userNode.getSession().getWorkspace().getQueryManager().createQuery(queryStr.toString(), Query.SQL);
      NodeIterator r = q.execute().getNodes();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCurrentUserIdentityId() {
    String currentUser = getCurrentUserIdentity().getUserId();
    org.exoplatform.social.core.identity.model.Identity identity =
                                                                 identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                     currentUser);
    return identity == null ? 0 : Long.parseLong(identity.getId());
  }

  public Identity getCurrentUserIdentity() {
    return ConversationState.getCurrent().getIdentity();
  }
}
