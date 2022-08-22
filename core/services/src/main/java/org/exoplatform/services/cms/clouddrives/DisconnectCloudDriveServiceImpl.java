package org.exoplatform.services.cms.clouddrives;

import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisconnectCloudDriveServiceImpl implements DisconnectCloudDriveService {

  private DocumentFileService          documentFileService;

  private SessionProviderService       sessionProviders;

  private NodeFinder                   finder;

  private RepositoryService            jcrService;

  public DisconnectCloudDriveServiceImpl(DocumentFileService documentFileService,
                                         SessionProviderService sessionProviders,
                                         NodeFinder finder,
                                         RepositoryService jcrService) {
    this.documentFileService = documentFileService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.jcrService = jcrService;
  }

  @Override
  public void disconnectCloudDrive(String workspace, String path) {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = null;
    Item item = null;
    List<Node> nodes = new ArrayList<Node>();
    try {
      userSession = sp.getSession(workspace, jcrService.getCurrentRepository());
      item = finder.findItem(userSession, path);
      Node userNode = (Node) item;
      Node childNode = null;
      NodeIterator nodeIterator = userNode.getNodes();
      while (nodeIterator.hasNext()) {
        childNode = nodeIterator.nextNode();
        if (childNode.hasProperty("ecd:connected")) {
          nodes.add(childNode);
        }
      }
      Node driveNode = nodes.get(0);
      String folderId = ((NodeImpl) driveNode).getIdentifier();
      documentFileService.deleteDocument(driveNode.getPath(), folderId, false, 6, 1);
    } catch (RepositoryException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
