package org.exoplatform.ecms.listener.analytics;

import static org.exoplatform.analytics.utils.AnalyticsUtils.addSpaceStatistics;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import javax.jcr.*;
import javax.jcr.observation.Event;

import org.apache.commons.chain.Context;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class JCRNodeListener implements Action {

  private static final String                   GROUPS_SPACES_PARENT_FOLDER          = "/Groups/spaces/";

  private static final String                   DOCUMENT_NAME_PARAM                  = "documentName";

  private static final Log                      LOG                                  = ExoLogger.getLogger(JCRNodeListener.class);

  private static final String                   DEFAULT_CHARSET                      = Charset.defaultCharset().name();

  private static final ScheduledExecutorService SCHEDULER                            = new ScheduledThreadPoolExecutor(0);        // NOSONAR

  private static final Executor                 DEFAULT_DELAYED_EXECUTOR             = delayedExecutor(3, TimeUnit.SECONDS);

  private static final String                   FILE_EXTENSION_PARAM                 = "fileExtension";

  private static final String                   DOCUMENT_TYPE_PARAM                  = "documentType";

  private static final String                   UUID_PARAM                           = "uuid";

  private static final String                   FILE_SIZE_PARAM                      = "fileSize";

  private static final String                   FILE_MIME_TYPE_PARAM                 = "fileMimeType";

  private static final String                   DOCUMENT_UPDATED_OPERATION           = "documentUpdated";

  private static final String                   FILE_UPDATED_OPERATION               = "fileUpdated";

  private static final String                   DOCUMENT_CREATED_OPERATION           = "documentCreated";

  private static final String                   FILE_CREATED_OPERATION               = "fileCreated";

  private static final String                   DOCUMENT_MOVED_TO_TRASH_OPERATION    = "documentMovedToTrash";

  private static final String                   FILE_MOVED_TO_TRASH_OPERATION        = "fileMovedToTrash";

  private static final String                   MODULE_DOCUMENT                      = "Document";

  private static final String                   SUB_MODULE_CONTENT                   = "Content";

  private static final String                   MODULE_DRIVE                         = "Drive";

  private static final String                   SEPARATOR                            = "@@";

  private static final Set<String>              CURRENTLY_PROCESSING_NODE_PATH_QUEUE = new HashSet<>();

  private static final String                   EXO_USER_PREFERENCES                 = "exo:userPrefferences";

  private PortalContainer                       container;

  private TemplateService                       templateService;

  private SpaceService                          spaceService;

  private TrashService                          trashService;

  public JCRNodeListener() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public boolean execute(Context context) throws Exception { // NOSONAR
    try {
      String username = AnalyticsUtils.getUsername(ConversationState.getCurrent());
      boolean unkownUser = AnalyticsUtils.isUnkownUser(username);
      if (unkownUser) {
        return true;
      }

      Object item = context.get(InvocationContext.CURRENT_ITEM);
      Node node = (item instanceof Property) ? ((Property) item).getParent() : (Node) item;
      if (node == null) {
        return true;
      }
      Node managedNode = getManagedNodeFromParents(node);
      if (managedNode == null) {
        return true;
      }

      int eventType = (Integer) context.get(InvocationContext.EVENT);
      if (eventType == Event.NODE_ADDED && node.isNodeType(EXO_USER_PREFERENCES)) {
        return true;
      }
      String nodePath = managedNode.getPath();
      String queueKey = username + SEPARATOR + nodePath;

      if (CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(queueKey)) {
        // Ignore multiple action invocations when adding new node or updating a
        // node
        return true;
      }

      CURRENTLY_PROCESSING_NODE_PATH_QUEUE.add(queueKey);

      ManageableRepository repository = SessionProviderService.getRepository();
      String workspace = managedNode.getSession().getWorkspace().getName();

      boolean isNew = node.isNew();

      DEFAULT_DELAYED_EXECUTOR.execute(() -> {
        ExoContainerContext.setCurrentContainer(container);
        RequestLifeCycle.begin(container);
        SessionProvider systemProvider = SessionProvider.createSystemProvider();
        try {
          CURRENTLY_PROCESSING_NODE_PATH_QUEUE.remove(queueKey);

          Session session = systemProvider.getSession(workspace, repository);
          if (!session.itemExists(nodePath)) {
            return;
          }
          Node changedNode = (Node) session.getItem(nodePath);

          boolean isFile = changedNode.isNodeType(NodetypeConstant.NT_FILE);
          StatisticData statisticData = addModuleName(isFile);
          addOperationName(node, statisticData, isNew, isFile);
          addUUID(changedNode, statisticData);
          if (isFile) {
            addFileProperties(statisticData, changedNode);
          }
          statisticData.setUserId(AnalyticsUtils.getUserIdentityId(username));
          statisticData.addParameter(DOCUMENT_TYPE_PARAM, changedNode.getPrimaryNodeType().getName());
          addDocumentTitle(changedNode, statisticData);
          addSpaceStatistic(statisticData, nodePath);

          AnalyticsUtils.addStatisticData(statisticData);
        } catch (Exception e) {
          LOG.warn("Error computing jcr statistics", e);
        } finally {
          systemProvider.close();
          RequestLifeCycle.end();
          ExoContainerContext.setCurrentContainer(null);
        }
        return;
      });
    } catch (Exception e) {
      if (LOG.isDebugEnabled() || PropertyManager.isDevelopping()) {
        LOG.warn("Error computing jcr statistics", e);
      } else {
        LOG.warn("Error computing jcr statistics: {}", e.getMessage());
      }
    }
    return true;
  }

  private void addFileProperties(StatisticData statisticData, Node changedNode) throws RepositoryException {
    if (changedNode.hasNode(NodetypeConstant.JCR_CONTENT)) {
      Node fileMetadataNode = changedNode.getNode(NodetypeConstant.JCR_CONTENT);
      if (fileMetadataNode.hasProperty(NodetypeConstant.JCR_MIME_TYPE)) {
        statisticData.addParameter(FILE_MIME_TYPE_PARAM,
                                   fileMetadataNode.getProperty(NodetypeConstant.JCR_MIME_TYPE).getString());
      }
      if (fileMetadataNode.hasProperty(NodetypeConstant.JCR_DATA)) {
        statisticData.addParameter(FILE_SIZE_PARAM,
                                   fileMetadataNode.getProperty(NodetypeConstant.JCR_DATA).getLength());
      }
    }

    String nodeName = changedNode.getName();
    int index = nodeName.lastIndexOf('.');
    if (index != -1) {
      statisticData.addParameter(FILE_EXTENSION_PARAM, nodeName.substring(index + 1));
    }
  }

  private void addUUID(Node changedNode,
                       StatisticData statisticData) throws RepositoryException {
    if (changedNode.hasProperty(NodetypeConstant.JCR_UUID)) {
      statisticData.addParameter(UUID_PARAM,
                                 changedNode.getProperty(NodetypeConstant.JCR_UUID).getString());
    }
  }

  private StatisticData addModuleName(boolean isFile) {
    StatisticData statisticData = new StatisticData();
    statisticData.setModule(MODULE_DRIVE);
    if (isFile) {
      statisticData.setSubModule(MODULE_DOCUMENT);
    } else {
      statisticData.setSubModule(SUB_MODULE_CONTENT);
    }
    return statisticData;
  }

  private void addOperationName(Node node,
                                StatisticData statisticData,
                                boolean isNew,
                                boolean isFile) throws RepositoryException {
    boolean movedToTrash = getTrashService().isInTrash(node);

    String operation = null;
    if (movedToTrash) {
      if (isFile) {
        operation = FILE_MOVED_TO_TRASH_OPERATION;
      } else {
        operation = DOCUMENT_MOVED_TO_TRASH_OPERATION;
      }
    } else if (isNew) {
      if (!isFile) {
        operation = DOCUMENT_CREATED_OPERATION;
      }
    } else {
      if (isFile) {
        operation = FILE_UPDATED_OPERATION;
      } else {
        operation = DOCUMENT_UPDATED_OPERATION;
      }
    }
    statisticData.setOperation(operation);
  }

  private void addDocumentTitle(Node managedNode, StatisticData statisticData) throws RepositoryException,
                                                                               UnsupportedEncodingException {
    String title = null;
    if (managedNode.hasProperty(NodetypeConstant.EXO_TITLE)) {
      title = managedNode.getProperty(NodetypeConstant.EXO_TITLE).getString();
    } else if (managedNode.hasProperty(NodetypeConstant.EXO_NAME)) {
      title = managedNode.getProperty(NodetypeConstant.EXO_NAME).getString();
    } else {
      title = managedNode.getName();
    }
    title = URLDecoder.decode(URLDecoder.decode(title, DEFAULT_CHARSET), DEFAULT_CHARSET);
    statisticData.addParameter(DOCUMENT_NAME_PARAM, title);
  }

  private Node getManagedNodeFromParents(Node changedNode) throws RepositoryException {
    Node nodeIndex = changedNode;
    Node managedNode = getManagedNode(changedNode);
    do {
      if (StringUtils.equals("/", nodeIndex.getPath())) {
        break;
      } else {
        try {
          nodeIndex = nodeIndex.getParent();
          Node managedNodeTmp = getManagedNode(nodeIndex);
          if (managedNodeTmp != null) {
            // A parent node has Managed Template, thus, use it in Analytics
            // reference instead of node itself
            managedNode = managedNodeTmp;
          }
        } catch (AccessDeniedException e) {
          LOG.trace("User doesn't have access to parent node of '{}'", nodeIndex.getPath(), e);
          break;
        }
      }
    } while (true);
    return managedNode;
  }

  private Node getManagedNode(Node node) throws RepositoryException {
    String nodeType = node.getPrimaryNodeType().getName();
    if (!node.isNodeType(NodetypeConstant.NT_RESOURCE)
        && (node.isNodeType(NodetypeConstant.NT_FILE) || getTemplateService().isManagedNodeType(nodeType))) {
      // Found parent managed node
      return node;
    }
    return null;
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

  private TemplateService getTemplateService() {
    if (templateService == null) {
      templateService = this.container.getComponentInstanceOfType(TemplateService.class);
    }
    return templateService;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = this.container.getComponentInstanceOfType(SpaceService.class);
    }
    return spaceService;
  }

  private TrashService getTrashService() {
    if (trashService == null) {
      trashService = this.container.getComponentInstanceOfType(TrashService.class);
    }
    return trashService;
  }

  private static Executor delayedExecutor(long delay, TimeUnit unit) {
    return delayedExecutor(delay, unit, ForkJoinPool.commonPool());
  }

  private static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
    return r -> SCHEDULER.schedule(() -> executor.execute(r), delay, unit);
  }

}
