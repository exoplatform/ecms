package org.exoplatform.services.attachments.plugins;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.attachments.service.AttachmentEntityTypePlugin;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;

import javax.jcr.*;
import java.util.Set;

import static org.exoplatform.services.attachments.utils.Utils.EXO_SYMLINK_UUID;
import static org.exoplatform.services.wcm.core.NodetypeConstant.*;

/**
 * Attachment
 */
public class TaskAttachmentEntityTypePlugin extends AttachmentEntityTypePlugin {

  private static final Log           LOG                      = ExoLogger.getExoLogger(TaskAttachmentEntityTypePlugin.class);

  private final TaskService          taskService;

  private final ProjectService       projectService;

  private final NodeHierarchyCreator nodeHierarchyCreator;

  private final SessionProviderService sessionProviderService;

  private final RepositoryService repositoryService;

  public static final String         DOCUMENTS_NODE           = "Documents";

  private static final String        DEFAULT_GROUPS_HOME_PATH = "/Groups";

  public static final String         GROUPS_PATH_ALIAS        = "groupsPath";

  public TaskAttachmentEntityTypePlugin(TaskService taskService,
                                        ProjectService projectService,
                                        NodeHierarchyCreator nodeHierarchyCreator,
                                        SessionProviderService sessionProviderService,
                                        RepositoryService repositoryService) {
    this.taskService = taskService;
    this.projectService = projectService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  @Override
  public String getAttachmentOrLinkId(String entityType, long entityId, String attachmentId) {

    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    try {
      TaskDto task = taskService.getTask(entityId);
      Set<String> taskPermittedIdentities = projectService.getParticipator(task.getStatus().getProject().getId());

      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session userSession = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);

      // check if content is still there
      Node attachmentNode = getNodeByIdentifier(userSession, attachmentId);
      if (attachmentNode == null) {
        return attachmentId;
      }

      // Check if the content is symlink, then get the original content
      if (attachmentNode.isNodeType(EXO_SYMLINK)) {
        String sourceNodeId = attachmentNode.getProperty(EXO_SYMLINK_UUID).getString();
        Node originalNode = getNodeByIdentifier(userSession, sourceNodeId);
        if (originalNode != null) {
          attachmentNode = originalNode;
        }
      }

      Node linkNode = null;
      for (String permittedIdentity : taskPermittedIdentities) {
        // set read permission
        if (attachmentNode.canAddMixin(EXO_PRIVILEGEABLE)) {
          attachmentNode.addMixin(EXO_PRIVILEGEABLE);
        }
        ((ExtendedNode) attachmentNode).setPermission(permittedIdentity, new String[] { PermissionType.READ });
        attachmentNode.save();

        // Create a symlink in Document app of the space if the task belongs to a
        // project of a space
        if (permittedIdentity.contains(":/spaces/")) {
          String groupId = permittedIdentity.split(":")[1];
          Node rootNode = getGroupNode(nodeHierarchyCreator, userSession, groupId);
          if (rootNode != null) {
            Node parentNode = getDestinationFolder(rootNode, task.getId());
            linkNode = Utils.createSymlink(attachmentNode, parentNode, permittedIdentity);
          }
        }
      }
      return linkNode != null ? ((ExtendedNode) linkNode).getIdentifier() : attachmentId;
    } catch (EntityNotFoundException e) {
      LOG.error("Could not find task with ID {}", entityId, e);
    } catch (Exception e) {
      LOG.error("Error updating shared document {}", attachmentId, e);
    }
    return attachmentId;
  }

  @Override
  public String getEntityType() {
    return "task";
  }

  public Node getDestinationFolder(Node rootNode, Long entityId) {
    Node parentNode;
    try {
      if (rootNode.hasNode("task")) {
        parentNode = rootNode.getNode("task");
      } else {
        parentNode = rootNode.addNode("task", NT_FOLDER);
        rootNode.save();
      }
      if (parentNode.hasNode(String.valueOf(entityId))) {
        return parentNode.getNode(String.valueOf(entityId));
      } else {
        Node taskNode = parentNode.addNode(String.valueOf(entityId), NT_FOLDER);
        parentNode.save();
        return taskNode;
      }
    } catch (RepositoryException repositoryException) {
      LOG.error("Could not create and return parent folder for task {} under root folder {}",
                entityId,
                rootNode,
                repositoryException);
      return rootNode;
    }
  }

  private static Node getGroupNode(NodeHierarchyCreator nodeHierarchyCreator,
                                   Session session,
                                   String groupId) throws RepositoryException {
    String groupsHomePath = getGroupsPath(nodeHierarchyCreator);
    String groupPath = groupsHomePath + groupId + "/" + DOCUMENTS_NODE;
    if (session.itemExists(groupPath)) {
      return (Node) session.getItem(groupPath);
    }
    return null;
  }

  private static String getGroupsPath(NodeHierarchyCreator nodeHierarchyCreator) {
    String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_PATH_ALIAS);
    if (StringUtils.isBlank(groupsPath)) {
      groupsPath = DEFAULT_GROUPS_HOME_PATH;
    }
    return groupsPath;
  }

  private static Node getNodeByIdentifier(Session session, String nodeId) {
    try {
      return ((ExtendedSession) session).getNodeByIdentifier(nodeId);
    } catch (PathNotFoundException e) {
      LOG.info("Node with identifier {} is not found. Ignore search result.", nodeId);
    } catch (RepositoryException e) {
      LOG.debug("Error retrieving node with identifier {}. Will attempt to retrieve it by path", nodeId, e);
    }
    return null;
  }
}
