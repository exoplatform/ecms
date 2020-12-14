package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;

import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Dlp Connector for Files
 */
public class FileDlpConnector extends DlpServiceConnector {

  private static final Log    LOGGER               = ExoLogger.getExoLogger(FileDlpConnector.class);

  public static final String  TYPE                 = "file";

  private static final String DLP_KEYWORD_PROPERTY = "exo.dlp.keywords";

  private static final String COLLABORATION_WS     = "collaboration";
  
  private static final String DLP_KEYWORDS_PARAM     = "dlp.keywords";

  private TrashService        trashService;

  private RepositoryService   repositoryService;
  
  private String   dlpKeywords;

  public FileDlpConnector(InitParams initParams, TrashService trashService, RepositoryService repositoryService) {
    super(initParams);
    ValueParam dlpKeywordsParam = initParams.getValueParam(DLP_KEYWORDS_PARAM);
    this.dlpKeywords = dlpKeywordsParam.getValue();
    this.trashService = trashService;
    this.repositoryService = repositoryService;
  }

  @Override
  public boolean processItem(String entityId) {
    if (!isIndexedByEs()) {
      return false;
    }
    else if (matchKeyword()) {
      treatItem(entityId);
    }
    return true;
  }

  private boolean isIndexedByEs() {
    // TODO
    return true;
  }

  private boolean matchKeyword() {
    // TODO
    return dlpKeywords != null && !dlpKeywords.isEmpty();
  }

  private void treatItem(String entityId) {
    ExtendedSession session = null;
    try {
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS,
                                                                                     repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(entityId);
      trashService.moveToTrash(node, WCMCoreUtils.getSystemSessionProvider());
    } catch (Exception e) {
      LOGGER.error("Error while treating file dlp connector item", e);
    }
  }
}
