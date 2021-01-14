package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import com.google.common.annotations.VisibleForTesting;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.domain.DlpPositiveItemEntity;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.dlp.queue.QueueDlpService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

import java.util.Collections;

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
  
  private FileSearchServiceConnector fileSearchServiceConnector;
  
  private QueueDlpService queueDlpService;

  public FileDlpConnector(InitParams initParams, FileSearchServiceConnector fileSearchServiceConnector,
                          RepositoryService repositoryService, IndexingService indexingService, QueueDlpService queueDlpService) {
    super(initParams);
    ValueParam dlpKeywordsParam = initParams.getValueParam(DLP_KEYWORDS_PARAM);
    this.dlpKeywords = dlpKeywordsParam.getValue();
    if (dlpKeywords != null) {
      dlpKeywords=dlpKeywords.replace(","," ");
    }
    this.repositoryService = repositoryService;
    this.indexingService = indexingService;
    this.fileSearchServiceConnector=fileSearchServiceConnector;
    this.queueDlpService=queueDlpService;
  }

  @Override
  public boolean processItem(String entityId) {
    if (!isIndexedByEs(entityId)) {
      return false;
    } else if (matchKeyword(entityId)) {
      treatItem(entityId);
    }
    return true;
  }

  private boolean isIndexedByEs(String entityId) {
    SearchContext searchContext = null;
    try {
      searchContext = new SearchContext(new Router(new ControllerDescriptor()), "");
      return fileSearchServiceConnector.isIndexed(searchContext, entityId);
    } catch (Exception ex) {
      LOGGER.error("Can not create SearchContext", ex);
    }
    return false;
  }

  private boolean matchKeyword(String entityId) {
    SearchContext searchContext = null;
    try {
      searchContext = new SearchContext(new Router(new ControllerDescriptor()), "");
      return dlpKeywords != null
          && !dlpKeywords.isEmpty()
          && fileSearchServiceConnector.dlpSearch(searchContext,dlpKeywords,entityId).size()>0;
    } catch (Exception ex) {
      LOGGER.error("Can not create SearchContext", ex);
    }
    return false;
  }
  
  @VisibleForTesting
  protected void treatItem(String entityId) {
    ExtendedSession session = null;
    try {
      long startTime = System.currentTimeMillis();
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Workspace workspace = session.getWorkspace();
      Node node = session.getNodeByIdentifier(entityId);
      Node dlpSecurityNode = (Node) session.getItem("/" + DLP_SECURITY_FOLDER);
      String fileName = node.getName();
      if (!node.getPath().startsWith("/"+DLP_SECURITY_FOLDER+"/")) {
        workspace.move(node.getPath(), "/" + DLP_SECURITY_FOLDER + "/" + fileName);
        indexingService.unindex(TYPE, entityId);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        LOGGER.info("service={} operation={} parameters=\"fileName:{}\" status=ok " + "duration_ms={}",
                 DlpOperationProcessor.DLP_FEATURE,
                 DLP_POSITIVE_DETECTION,
                 fileName,
                 totalTime);
        saveDlpPositiveItem(node);
      }
    } catch (Exception e) {
      LOGGER.error("Error while treating file dlp connector item", e);
    }
  }

  private void saveDlpPositiveItem(Node node) throws RepositoryException {
    DlpPositiveItemService dlpPositiveItemService = CommonsUtils.getService(DlpPositiveItemService.class);
    DlpPositiveItemEntity dlpPositiveItemEntity = new DlpPositiveItemEntity();
    dlpPositiveItemEntity.setReference(node.getUUID());
    dlpPositiveItemEntity.setType(TYPE);
    dlpPositiveItemEntity.setDetectionDate(Calendar.getInstance());
    // to be updated with detected keyword
    dlpPositiveItemEntity.setKeywords(dlpKeywords);
    dlpPositiveItemService.addDlpPositiveItem(dlpPositiveItemEntity);

  }
}
