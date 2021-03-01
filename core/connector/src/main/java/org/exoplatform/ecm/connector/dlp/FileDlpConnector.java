package org.exoplatform.ecm.connector.dlp;

import javax.jcr.*;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.domain.DlpPositiveItemEntity;
import org.exoplatform.commons.dlp.domain.RestoredDlpItemEntity;
import org.exoplatform.commons.dlp.dto.RestoredDlpItem;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.dlp.service.RestoredDlpItemService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.dlp.queue.QueueDlpService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Calendar;

/**
 * Dlp Connector for Files
 */
public class FileDlpConnector extends DlpServiceConnector {

  public static final String TYPE = "file";

  public static final String DLP_QUARANTINE_FOLDER = "Quarantine";
  
  public static final String EXO_CURRENT_PROVIDER = "exo:currentProvider";

  private static final Log LOGGER = ExoLogger.getExoLogger(FileDlpConnector.class);

  private static final String COLLABORATION_WS = "collaboration";

  private RepositoryService repositoryService;

  final static public String EXO_RESTORE_LOCATION = "exo:restoreLocation";

  final static public String RESTORE_PATH = "exo:restorePath";

  private IndexingService indexingService;

  private FileSearchServiceConnector fileSearchServiceConnector;

  private RestoredDlpItemService restoredDlpItemService;

  private DlpOperationProcessor dlpOperationProcessor;

  public FileDlpConnector(InitParams initParams, FileSearchServiceConnector fileSearchServiceConnector,
                          RepositoryService repositoryService, IndexingService indexingService, DlpOperationProcessor dlpOperationProcessor, RestoredDlpItemService restoredDlpItemService) {
    super(initParams);
    this.repositoryService = repositoryService;
    this.indexingService = indexingService;
    this.fileSearchServiceConnector = fileSearchServiceConnector;
    this.restoredDlpItemService = restoredDlpItemService;
    this.dlpOperationProcessor = dlpOperationProcessor;
  }

  @Override
  public boolean processItem(String entityId) {
    if (!isIndexedByEs(entityId) || editorOpened(entityId)) {
      return false;
    } else {
      checkMatchKeywordAndTreatItem(entityId);
    }
    return true;
  }

  @Override
  public void restorePositiveItem(String itemReference) {
    ExtendedSession session = null;
    try {
      long startTime = System.currentTimeMillis();
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(itemReference);
      String fileName = node.getName();
      restoreFromQuarantine(node.getPath(), WCMCoreUtils.getUserSessionProvider());
      indexingService.reindex(TYPE, itemReference);
      saveRestoredDlpItem(node.getUUID());
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOGGER.info("service={} operation={} parameters=\"fileName:{}\" status=ok " + "duration_ms={}",
                  DlpOperationProcessor.DLP_FEATURE,
                  "restoreDLPItem",
                  fileName,
                  totalTime);
    } catch (Exception e) {
      LOGGER.error("Error while treating file dlp connector item", e);
    }
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
    String dlpKeywords = dlpOperationProcessor.getKeywords();
    if (dlpKeywords != null && !dlpKeywords.isEmpty()) {
      try {
        searchContext = new SearchContext(new Router(new ControllerDescriptor()), "");
        Collection<SearchResult> searchResults = fileSearchServiceConnector.dlpSearch(searchContext, dlpKeywords, entityId);
        if (searchResults.size() > 0) {
          treatItem(entityId, searchResults);
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
      String restorePath = fixRestorePath(node.getPath());
      RestoredDlpItem restoredDlpItem = findRestoredDlpItem(node.getUUID());
      if (!node.getPath().startsWith("/" + DLP_QUARANTINE_FOLDER + "/") && (restoredDlpItem == null ||  getNodeLastModifiedDate(node) > restoredDlpItem.getDetectionDate())) {
        workspace.move(node.getPath(), "/" + DLP_QUARANTINE_FOLDER + "/" + fileName);
        indexingService.unindex(TYPE, entityId);
        saveDlpPositiveItem(node,searchResults);
        addRestorePathInfo(node.getName(), restorePath, workspace.getName());
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

  private Long getNodeLastModifiedDate(Node node) throws Exception {
    Calendar calendar = node.hasProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE) ?
                        node.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate() :
                        node.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
    return calendar.getTimeInMillis();
  }

  private void saveDlpPositiveItem(Node node, Collection<SearchResult> searchResults) throws RepositoryException {
    DlpPositiveItemService dlpPositiveItemService = CommonsUtils.getService(DlpPositiveItemService.class);
    DlpPositiveItemEntity dlpPositiveItemEntity = new DlpPositiveItemEntity();
    dlpPositiveItemEntity.setReference(node.getUUID());
    if (node.hasProperty(NodetypeConstant.EXO_TITLE)) {
      String title = node.getProperty(NodetypeConstant.EXO_TITLE).getString();
      dlpPositiveItemEntity.setTitle(title);
    }
    if (node.hasProperty(NodetypeConstant.EXO_LAST_MODIFIER)) {
      String author = node.getProperty(NodetypeConstant.EXO_LAST_MODIFIER).getString();
      dlpPositiveItemEntity.setAuthor(author);
    }
    dlpPositiveItemEntity.setType(TYPE);
    dlpPositiveItemEntity.setDetectionDate(Calendar.getInstance());
    dlpPositiveItemEntity.setKeywords(getDetectedKeywords(searchResults, dlpOperationProcessor.getKeywords()));
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
    List<String> dlpKeywordsList = Arrays.asList(dlpKeywords.split(","));
    searchResults.stream()
                 .map(SearchResult::getExcerpts)
                 .map(Map::values)
                 .filter(excerptValue -> !excerptValue.isEmpty())
                 .flatMap(Collection::stream)
                 .flatMap(Collection::stream)
                 .forEach(s -> {
                   dlpKeywordsList.stream().filter(key -> removeAccents(s).contains(escapeSpecialCharacters(removeAccents(key)))).forEach(key -> {
                     if (!key.isEmpty() && !detectedKeywords.contains(key)) {
                       detectedKeywords.add(key);
                     }
                   });
                 });
    return detectedKeywords.stream().collect(Collectors.joining(", "));
  }

  private String escapeSpecialCharacters(String keyword) {
    List<String> keywordParts = Arrays.stream(keyword.split("[+\\-=&|><!(){}\\[\\]^\"*?:/ @$]+"))
                                      .distinct()
                                      .collect(Collectors.toList());
    for (String s : keywordParts) {
      keyword = keyword.replace(s, "<em>" + s + "</em>");
    }
    return keyword;
  }

  private void saveRestoredDlpItem(String nodeUID) {
    RestoredDlpItemEntity restoredDlpItemEntity = new RestoredDlpItemEntity();
    restoredDlpItemEntity.setReference(nodeUID);
    restoredDlpItemEntity.setDetectionDate(Calendar.getInstance());
    restoredDlpItemService.addRestoredDlpItem(restoredDlpItemEntity);
  }

  private RestoredDlpItem findRestoredDlpItem(String nodeUID) throws Exception {
    return restoredDlpItemService.getRestoredDlpItemByReference(nodeUID);
  }

  private void addRestorePathInfo(String nodeName, String restorePath, String nodeWorkspace) throws Exception {
    NodeIterator nodes = this.getSecurityHomeNode().getNodes(nodeName);
    Node node = null;
    while (nodes.hasNext()) {
      Node currentNode = nodes.nextNode();
      if (node == null) {
        node = currentNode;
      } else {
        if (node.getIndex() < currentNode.getIndex()) {
          node = currentNode;
        }
      }
    }
    if (node != null) {
      node.addMixin(EXO_RESTORE_LOCATION);
      node.setProperty(RESTORE_PATH, restorePath);
      node.save();
    }
  }

  private Node getSecurityHomeNode() {
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider()
                                    .getSession(COLLABORATION_WS,
                                                repositoryService.getCurrentRepository());
      return (Node) session.getItem("/" + DLP_SECURITY_FOLDER);
    } catch (Exception e) {
      return null;
    }

  }

  private String fixRestorePath(String path) {
    int leftBracket = path.lastIndexOf('[');
    int rightBracket = path.lastIndexOf(']');
    if (leftBracket == -1 || rightBracket == -1 ||
        (leftBracket >= rightBracket)) return path;

    try {
      Integer.parseInt(path.substring(leftBracket+1, rightBracket));
    } catch (Exception ex) {
      return path;
    }
    return path.substring(0, leftBracket);
  }

  private void restoreFromQuarantine(String securityNodePath,
                                SessionProvider sessionProvider) throws Exception {

    Node securityHomeNode = this.getSecurityHomeNode();
    Session restoreSession = securityHomeNode.getSession();
    Node securityNode = (Node) restoreSession.getItem(securityNodePath);
    String restorePath = securityNode.getProperty(RESTORE_PATH).getString();

    restoreSession.getWorkspace().move(securityNodePath, restorePath);
    removeRestorePathInfo(restoreSession, restorePath);
    securityHomeNode.save();
    restoreSession.save();
  }
  
  private void removeRestorePathInfo(Session session, String restorePath) throws Exception {
    Node sameNameNode = ((Node) session.getItem(restorePath));
    Node parent = sameNameNode.getParent();
    String name = sameNameNode.getName();
    NodeIterator nodeIter = parent.getNodes(name);
    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (node.isNodeType(EXO_RESTORE_LOCATION))
        node.removeMixin(EXO_RESTORE_LOCATION);
    }
  }

  private boolean editorOpened(String entityId) {
    Node node = null;
    try {
      ExtendedSession session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, repositoryService.getCurrentRepository());
      node = session.getNodeByIdentifier(entityId);
      return node.hasProperty(EXO_CURRENT_PROVIDER);
    } catch (RepositoryException e) {
      LOGGER.error("Error while checking editor status", e);
    }
    return false;
  }
  
  private String removeAccents(String string) {
    if (StringUtils.isNotBlank(string)) {
      string = Normalizer.normalize(string, Normalizer.Form.NFD);
      string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
    return string.toLowerCase();
  }
}
