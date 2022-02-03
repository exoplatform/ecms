package org.exoplatform.ecms.listener.analytics;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.jcr.Node;

import static org.exoplatform.analytics.utils.AnalyticsUtils.addSpaceStatistics;

@Asynchronous
public class AnalyticsDocumentsListener extends Listener<String, Node> {

  private static final String UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME  = "documentUploadedNewApp";

  private static final String UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME  = "documentUploadedOldApp";

  private static final String                   GROUPS_SPACES_PARENT_FOLDER          = "/Groups/spaces/";

  private IdentityManager     identityManager;

  private SpaceService        spaceService;

  @Override
  public void onEvent(Event<String, Node> event) throws Exception {
    Node data = event.getData();
    String operation = event.getEventName().equals("exo.upload.doc.newApp") ? UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME : UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME;
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
    statisticData.addParameter("documentsName", data.getName());
    statisticData.addParameter("documentsPath", data.getPath());
    statisticData.addParameter("documentsOwner", ((NodeImpl) data).getACL().getOwner());
    String nodePath = data.getPath();
    addSpaceStatistic(statisticData, nodePath);
    AnalyticsUtils.addStatisticData(statisticData);
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
