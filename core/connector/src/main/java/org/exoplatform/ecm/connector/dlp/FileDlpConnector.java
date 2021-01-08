package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.domain.DlpPositiveItemEntity;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import java.util.Calendar;

/**
 * Dlp Connector for Files
 */
public class FileDlpConnector extends DlpServiceConnector {
  
  public static final String  TYPE                 = "file";
  
  public static final String DLP_SECURITY_FOLDER   = "Security";

  private static final Log    LOGGER               = ExoLogger.getExoLogger(FileDlpConnector.class);

  private static final String COLLABORATION_WS     = "collaboration";

  private static final String DLP_KEYWORDS_PARAM   = "dlp.keywords";
  
  private RepositoryService   repositoryService;
  
  private IndexingService indexingService;

  private String              dlpKeywords;

  public FileDlpConnector(InitParams initParams, RepositoryService repositoryService, IndexingService indexingService) {
    super(initParams);
    ValueParam dlpKeywordsParam = initParams.getValueParam(DLP_KEYWORDS_PARAM);
    this.dlpKeywords = dlpKeywordsParam.getValue();
    this.repositoryService = repositoryService;
    this.indexingService = indexingService;
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
      long startTime = System.currentTimeMillis();
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Workspace workspace = session.getWorkspace();
      Node node = session.getNodeByIdentifier(entityId);
      Node dlpSecurityNode = (Node) session.getItem("/" + DLP_SECURITY_FOLDER);
      String fileName = node.getName();
      if (!dlpSecurityNode.hasNode(fileName)) {
        workspace.move(node.getPath(), "/" + DLP_SECURITY_FOLDER + "/" + fileName);
        indexingService.unindex(TYPE, entityId);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        LOGGER.info("service={} operation={} parameters=\"fileName:{}\" status=ok " + "duration_ms={}",
                 DlpOperationProcessor.DLP_FEATURE,
                 DLP_POSITIVE_DETECTION,
                 fileName,
                 totalTime);
        saveDlpItem(node);
      }
    } catch (Exception e) {
      LOGGER.error("Error while treating file dlp connector item", e);
    }
  }

  private void saveDlpItem(Node node) throws RepositoryException {
    DlpPositiveItemService dlpPositiveItemService = CommonsUtils.getService(DlpPositiveItemService.class);
    DlpPositiveItemEntity dlpPositiveItemEntity = new DlpPositiveItemEntity();
    dlpPositiveItemEntity.setUuid(node.getUUID());
    dlpPositiveItemEntity.setDetectionDate(Calendar.getInstance());
    // to be updated with detected keyword
    dlpPositiveItemEntity.setKeywords(dlpKeywords);
    dlpPositiveItemService.addDlpItem(dlpPositiveItemEntity);

  }
}
