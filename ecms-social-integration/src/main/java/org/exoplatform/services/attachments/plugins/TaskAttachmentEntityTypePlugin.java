/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.attachments.plugins;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.attachments.service.AttachmentEntityTypePlugin;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;

import javax.jcr.*;
import java.util.*;

import static org.exoplatform.services.attachments.utils.Utils.EXO_SYMLINK_UUID;
import static org.exoplatform.services.wcm.core.NodetypeConstant.*;

/**
 * Plugin to define how and where files attached to tasks are stored
 */
public class TaskAttachmentEntityTypePlugin extends AttachmentEntityTypePlugin {

  private static final Log           LOG                      = ExoLogger.getExoLogger(TaskAttachmentEntityTypePlugin.class);

  private final TaskService          taskService;

  private final ProjectService       projectService;

  private final NodeHierarchyCreator nodeHierarchyCreator;

  private final SessionProviderService sessionProviderService;

  private final RepositoryService repositoryService;

  private final IdentityRegistry     identityRegistry;

  private final Authenticator        authenticator;

  public static final String         DOCUMENTS_NODE           = "Documents";

  private static final String        DEFAULT_GROUPS_HOME_PATH = "/Groups"; //NOSONAR

  public static final String         GROUPS_PATH_ALIAS        = "groupsPath";

  public TaskAttachmentEntityTypePlugin(TaskService taskService,
                                        ProjectService projectService,
                                        NodeHierarchyCreator nodeHierarchyCreator,
                                        SessionProviderService sessionProviderService,
                                        RepositoryService repositoryService,
                                        IdentityRegistry identityRegistry,
                                        Authenticator authenticator) {
    this.taskService = taskService;
    this.projectService = projectService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.identityRegistry = identityRegistry;
    this.authenticator = authenticator;
  }

  @Override
  public List<String> getlinkedAttachments(String entityType, long entityId, String attachmentId) {

    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    try {
      TaskDto task = taskService.getTask(entityId);
      // The task creator views the task's attachments (TaskAttachmentACLPlugin) even
      // outside the project's space, so the file is made readable for them as for the
      // project participants, when no participant identity already covers them: a
      // creator who reads through a participant group keeps reading through it, and
      // no personal entry outlives that membership. The assignee and the coworkers,
      // admitted by the same rule, are not granted here: they change over the task's
      // life, and a grant taken when the file is linked would have to be revoked with
      // them.
      Set<String> taskPermittedIdentities = new HashSet<>(projectService.getParticipator(task.getStatus().getProject().getId()));
      String taskCreator = task.getCreatedBy();
      if (StringUtils.isBlank(taskCreator) || isCoveredBy(taskCreator, taskPermittedIdentities)) {
        taskCreator = null;
      } else {
        taskPermittedIdentities.add(taskCreator);
      }

      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session userSession = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);

      // check if content is still there
      Node attachmentNode = Utils.getNodeByIdentifier(userSession, attachmentId);
      if (attachmentNode == null) {
        return Collections.singletonList(attachmentId);
      }

      // Check if the content is symlink, then get the original content
      if (attachmentNode.isNodeType(EXO_SYMLINK)) {
        String sourceNodeId = attachmentNode.getProperty(EXO_SYMLINK_UUID).getString();
        Node originalNode = Utils.getNodeByIdentifier(userSession, sourceNodeId);
        if (originalNode != null) {
          attachmentNode = originalNode;
        }
      }

      List<String> linkNodes = new ArrayList<>();
      for (String permittedIdentity : taskPermittedIdentities) {
        if (permittedIdentity.contains(":/spaces/")) {
          String groupId = permittedIdentity.split(":")[1];
          if (attachmentNode.getPath().contains(groupId + "/") && !linkNodes.contains(attachmentId)) {
            linkNodes.add(attachmentId);
          } else {
            // Create a symlink in Document app of the space if the task belongs to a
            // project of a space
            Node rootNode = getGroupNode(nodeHierarchyCreator, userSession, groupId);
            if (rootNode != null) {
              Node parentNode = getDestinationFolder(rootNode, task.getId());
              // We won't add the file symlink as attachment if another file exists with the same name
              if(!parentNode.hasNode(attachmentNode.getName())) {
                Node linkNode = Utils.createSymlink(attachmentNode, parentNode, permittedIdentity);
                if (linkNode != null) {
                  linkNodes.add(((ExtendedNode) linkNode).getIdentifier());
                  // The symlink is readable by the space only: a creator outside it reads it too
                  if (taskCreator != null) {
                    grantReadPermission(linkNode, taskCreator);
                  }
                }
              }
            }
          }
        }
        // set read permission for users or groups different from spaces
        grantReadPermission(attachmentNode, permittedIdentity);
      }
      if(linkNodes.isEmpty()) {
        return Collections.singletonList(attachmentId);
      }
      return linkNodes;
    } catch (Exception e) {
      LOG.error("Error getting linked documents {}", attachmentId, e);
    }
    return Collections.singletonList(attachmentId);
  }

  @Override
  public String getEntityType() {
    return "task";
  }

  /**
   * Checks whether a user already reads what a set of identities reads: the
   * user is one of them, or holds one of the memberships among them
   *
   * @param user a user name
   * @param identities user names, 'type:/group' memberships or '/group' ids
   * @return true when one of the identities covers the user; false when none
   *         does, or when the user's identity can't be resolved
   */
  private boolean isCoveredBy(String user, Set<String> identities) {
    if (identities.contains(user)) {
      return true;
    }
    Identity identity = identityRegistry.getIdentity(user);
    if (identity == null) {
      try {
        identity = authenticator.createIdentity(user);
      } catch (Exception e) {
        LOG.warn("Can't resolve the memberships of user {}", user, e);
      }
    }
    if (identity == null) {
      return false;
    }
    for (String permittedIdentity : identities) {
      MembershipEntry entry = permittedIdentity.startsWith("/") ? new MembershipEntry(permittedIdentity)
                                                                : MembershipEntry.parse(permittedIdentity);
      if (entry != null && identity.isMemberOf(entry)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Grants the read permission on a node to an identity that has none yet
   *
   * @param node the attachment node, or the symlink pointing at it
   * @param identity a user name or a membership expression
   * @throws RepositoryException when the node's ACL can't be read or saved
   */
  private static void grantReadPermission(Node node, String identity) throws RepositoryException {
    if (node.canAddMixin(EXO_PRIVILEGEABLE)) {
      node.addMixin(EXO_PRIVILEGEABLE);
    }
    AccessControlList permsList = ((ExtendedNode) node).getACL();
    if (permsList == null || permsList.getPermissions(identity).isEmpty()) {
      ((ExtendedNode) node).setPermission(identity, new String[] { PermissionType.READ });
    }
    node.save();
  }

  private Node getDestinationFolder(Node rootNode, Long entityId) {
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
}
