package org.exoplatform.ecm.connector.dlp;

import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;

public class PositiveFileDLPListener extends Listener<DlpPositiveItemService, String> {
  public static final String DLP_SECURITY_FOLDER = "Security";

  private static final String COLLABORATION_WS = "collaboration";

  private static final Log LOGGER = ExoLogger.getExoLogger(PositiveFileDLPListener.class);

  private RepositoryService repositoryService;

  public PositiveFileDLPListener() {
    this.repositoryService = CommonsUtils.getService(RepositoryService.class);
  }

  public void onEvent(Event<DlpPositiveItemService, String> event) throws Exception {

    if ("dlp.listener.event.delete.document".equals(event.getEventName())) {
      ExtendedSession session = null;
      try {
        String entityId = event.getData();
        session =
            (ExtendedSession) WCMCoreUtils.getSystemSessionProvider()
                                          .getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
        Node dlpSecurityNode = (Node) session.getItem("/" + DLP_SECURITY_FOLDER);
        Node node = session.getNodeByIdentifier(entityId);

        if (node != null && dlpSecurityNode != null) {
          node.remove();
          dlpSecurityNode.save();
        }
      } catch (Exception e) {
        LOGGER.error("Error while deleting dlp file item", e);
      }
    }
  }
}
