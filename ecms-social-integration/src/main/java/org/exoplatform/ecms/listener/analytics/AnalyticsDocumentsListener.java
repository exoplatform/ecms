package org.exoplatform.ecms.listener.analytics;

import static io.meeds.analytics.utils.AnalyticsUtils.*;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import io.meeds.analytics.model.StatisticData;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.impl.DocumentServiceImpl;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

@Asynchronous
public class AnalyticsDocumentsListener extends Listener<String, Node> {

  public static final String UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME  = "documentUploadedNewApp";

  public static final String UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME  = "documentUploadedOldApp";

  public static final String CREATE_DOCUMENT_OPERATION_NAME          = "documentCreated";

  private static final String UPLOAD_DOCUMENT_NEW_APP_EVENT_NAME     = "exo.upload.doc.newApp";

  private static final String                   GROUPS_SPACES_PARENT_FOLDER          = "/Groups/spaces/";

  private IdentityManager     identityManager;

  private SpaceService        spaceService;

  @Override
  public void onEvent(Event<String, Node> event) throws Exception {
    Node data = event.getData();
    String operation = operationOf(event.getEventName());
    long userId = 0;
    Identity identity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, event.getSource());
    if (identity != null) {
      userId = Long.parseLong(identity.getId());
    }
    StatisticData statisticData = new StatisticData();

    statisticData.setModule("documents");
    statisticData.setSubModule("event");
    statisticData.setOperation(operation);
    statisticData.setUserId(userId);
    String nodePath = data.getPath();
    statisticData.addKeyword("documentsName", data.getName());
    statisticData.addKeyword("documentsPath", nodePath);
    statisticData.addKeyword("documentsOwner", readOwner(data.getSession().getWorkspace().getName(), nodePath));
    addSpaceStatistic(statisticData, nodePath);
    addStatisticData(statisticData);
  }

  /**
   * The operation recorded for each event this listener is registered on: a
   * document created from a template, uploaded in the Documents app, or
   * uploaded in the old explorer.
   *
   * @param eventName the name of the broadcast event
   * @return the operation name
   */
  static String operationOf(String eventName) {
    if (DocumentServiceImpl.CREATE_DOC_NEW_APP.equals(eventName)) {
      return CREATE_DOCUMENT_OPERATION_NAME;
    } else if (UPLOAD_DOCUMENT_NEW_APP_EVENT_NAME.equals(eventName)) {
      return UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME;
    } else {
      return UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME;
    }
  }

  /**
   * The owner of the node, read through a system session: this listener runs
   * after the request that broadcast the event, whose session may already be
   * closed.
   *
   * @param workspace the workspace of the node
   * @param nodePath the path of the node
   * @return the owner, or null when the node no longer exists
   * @throws RepositoryException when the node cannot be read
   */
  String readOwner(String workspace, String nodePath) throws RepositoryException {
    SessionProvider systemProvider = createSystemProvider();
    try {
      Session session = systemProvider.getSession(workspace, SessionProviderService.getRepository());
      if (!session.itemExists(nodePath)) {
        return null;
      }
      return ((NodeImpl) session.getItem(nodePath)).getACL().getOwner();
    } finally {
      systemProvider.close();
    }
  }

  SessionProvider createSystemProvider() {
    return SessionProvider.createSystemProvider();
  }

  public IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = ExoContainerContext.getService(IdentityManager.class);
    }
    return identityManager;
  }

  public SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = ExoContainerContext.getService(SpaceService.class);
    }
    return spaceService;
  }

  private void addSpaceStatistic(StatisticData statisticData, String nodePath) {
    if (nodePath.startsWith(GROUPS_SPACES_PARENT_FOLDER)) {
      String[] nodePathParts = nodePath.split("/");

      if (nodePathParts.length > 3) {
        String groupId = "/spaces/" + nodePathParts[3];
        Space space = getSpaceService().getSpaceByGroupId(groupId);
        addSpaceStatistics(statisticData, space);
      }
    }
  }

}
