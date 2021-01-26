package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import com.google.common.annotations.VisibleForTesting;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.domain.DlpPositiveItemEntity;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.dlp.queue.QueueDlpService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dlp Connector for Files
 */
public class FileDlpConnector extends DlpServiceConnector {

  public static final String TYPE = "file";

  public static final String DLP_QUARANTINE_FOLDER = "Quarantine";

  private static final Log LOGGER = ExoLogger.getExoLogger(FileDlpConnector.class);

  private static final String COLLABORATION_WS = "collaboration";

  private static final String DLP_KEYWORDS_PARAM = "dlp.keywords";

  private static final String TITLE = "exo:title";

  private static final String OWNER = "exo:owner";

  private RepositoryService repositoryService;

  private static final Pattern PATTERN             = Pattern.compile("<em>(.*?)</em>", Pattern.DOTALL);

  private IndexingService indexingService;

  private String dlpKeywords;

  private FileSearchServiceConnector fileSearchServiceConnector;

  private QueueDlpService queueDlpService;

  public FileDlpConnector(InitParams initParams, FileSearchServiceConnector fileSearchServiceConnector,
                          RepositoryService repositoryService, IndexingService indexingService, QueueDlpService queueDlpService) {
    super(initParams);
    ValueParam dlpKeywordsParam = initParams.getValueParam(DLP_KEYWORDS_PARAM);
    this.dlpKeywords = dlpKeywordsParam.getValue();
    if (dlpKeywords != null) {
      dlpKeywords = dlpKeywords.replace(",", " ");
    }
    this.repositoryService = repositoryService;
    this.indexingService = indexingService;
    this.fileSearchServiceConnector = fileSearchServiceConnector;
    this.queueDlpService = queueDlpService;
  }

  @Override
  public boolean processItem(String entityId) {
    if (!isIndexedByEs(entityId)) {
      return false;
    } else {
      checkMatchKeywordAndTreatItem(entityId);
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

  private void checkMatchKeywordAndTreatItem(String entityId) {
    SearchContext searchContext = null;
    if (dlpKeywords != null
        && !dlpKeywords.isEmpty()) {
      try {
        searchContext = new SearchContext(new Router(new ControllerDescriptor()), "");
        Collection<SearchResult> searchResults = fileSearchServiceConnector.dlpSearch(searchContext, dlpKeywords, entityId);
        if (searchResults.size()>0) {
          treatItem(entityId,searchResults);
        }
      } catch (Exception ex) {
        LOGGER.error("Can not create SearchContext", ex);
      }
    }
  }

  @VisibleForTesting
  protected void treatItem(String entityId, Collection<SearchResult> searchResults) {
    ExtendedSession session = null;
    try {
      long startTime = System.currentTimeMillis();
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Workspace workspace = session.getWorkspace();
      Node node = session.getNodeByIdentifier(entityId);
      String fileName = node.getName();
      if (!node.getPath().startsWith("/" + DLP_QUARANTINE_FOLDER + "/")) {
        workspace.move(node.getPath(), "/" + DLP_QUARANTINE_FOLDER + "/" + fileName);
        indexingService.unindex(TYPE, entityId);
        saveDlpPositiveItem(node,searchResults);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        LOGGER.info("service={} operation={} parameters=\"fileName:{}\" status=ok " + "duration_ms={}",
                 DlpOperationProcessor.DLP_FEATURE,
                 DLP_POSITIVE_DETECTION,
                 fileName,
                 totalTime);
      }
    } catch (Exception e) {
      LOGGER.error("Error while treating file dlp connector item", e);
    }
  }

  private void saveDlpPositiveItem(Node node, Collection<SearchResult> searchResults) throws RepositoryException {
    DlpPositiveItemService dlpPositiveItemService = CommonsUtils.getService(DlpPositiveItemService.class);
    DlpPositiveItemEntity dlpPositiveItemEntity = new DlpPositiveItemEntity();
    dlpPositiveItemEntity.setReference(node.getUUID());
    if (node.hasProperty(TITLE)) {
      String title = node.getProperty(TITLE).getString();
      dlpPositiveItemEntity.setTitle(title);
    }
    if (node.hasProperty(OWNER)) {
      String author = node.getProperty(OWNER).getString();
      dlpPositiveItemEntity.setAuthor(author);
    }
    dlpPositiveItemEntity.setType(TYPE);
    dlpPositiveItemEntity.setDetectionDate(Calendar.getInstance());
    dlpPositiveItemEntity.setKeywords(getDetectedKeywords(searchResults, dlpKeywords));
    dlpPositiveItemService.addDlpPositiveItem(dlpPositiveItemEntity);
  }
  
  @Override
  public void removePositiveItem(String itemReference) {
    ExtendedSession session = null;
    try {
      session =
          (ExtendedSession) WCMCoreUtils.getSystemSessionProvider()
                                        .getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Node dlpQuarantineNode = (Node) session.getItem("/" + DLP_QUARANTINE_FOLDER);
      Node node = session.getNodeByIdentifier(itemReference);

      if (node != null && dlpQuarantineNode != null) {
        node.remove();
        dlpQuarantineNode.save();
      }
    } catch (Exception e) {
      LOGGER.error("Error while deleting dlp file item", e);
    }
  }

  @Override
  public boolean checkExternal(String userId) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
    return identity.getProfile().getProperty("external") != null &&  identity.getProfile().getProperty("external").equals("true");
  }
  
  @Override
  public String getItemUrl(String itemReference) {
    ExtendedSession session = null;
    try {
      session =
          (ExtendedSession) WCMCoreUtils.getSystemSessionProvider()
                                        .getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(itemReference);
      return WCMCoreUtils.getLinkInDocumentsApplication(node.getPath());
    } catch (Exception e) {
      LOGGER.error("Error while getting dlp item url", e);
    }
    return null;
  }

  private String getDetectedKeywords(Collection<SearchResult> searchResults, String dlpKeywords) {
    List<String> detectedKeywords = new ArrayList<>();
    List<String> dlpKeywordsList = Arrays.asList(dlpKeywords.split(" "));
    searchResults.stream()
                 .map(searchResult -> searchResult.getExcerpts())
                 .map(stringListMap -> stringListMap.values())
                 .filter(excerptValue -> excerptValue != null && !excerptValue.isEmpty())
                 .flatMap(lists -> lists.stream())
                 .flatMap(strings -> strings.stream())
                 .forEach(s -> {
                   Matcher matcher = PATTERN.matcher(s);
                   while (matcher.find()) {
                     String keyword = dlpKeywordsList.stream().filter(key -> matcher.group(1).contains(key)).findFirst().orElse(null);
                     if (keyword != null && !detectedKeywords.contains(keyword)) {
                       detectedKeywords.add(keyword);
                     }
                   }
                 });
    return detectedKeywords.stream().collect(Collectors.joining(", "));
  }
}
