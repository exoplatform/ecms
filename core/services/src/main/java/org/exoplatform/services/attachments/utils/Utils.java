package org.exoplatform.services.attachments.utils;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.exoplatform.services.wcm.core.NodetypeConstant.*;
import static org.exoplatform.services.wcm.core.NodetypeConstant.EXO_PRIVILEGEABLE;

public class Utils {
  private static final Log LOG                      = ExoLogger.getExoLogger(Utils.class);

  public static final String QUARANTINE_FOLDER = "Quarantine";

  public static final String EMPTY_STRING = "";

  public static final String  EXO_SYMLINK_UUID         = "exo:uuid";

  public static void sortAttachmentsByDate(List<AttachmentContextEntity> attachments) {
    attachments.sort((attachment1, attachment2) -> ObjectUtils.compare(attachment2.getAttachedDate(),
                                                                       attachment1.getAttachedDate()));
  }

  /**
   * Gets the parent folder node.
   *
   * @param session jcr session
   * @param manageDriveService Managae drive service
   * @param nodeHierarchyCreator node hierarchy creator
   * @param nodeFinder node finder
   * @param driverName the driver name
   * @param currentFolder the current folder
   * @return the parent folder node
   * @throws Exception the exception
   */
  public static Node getParentFolderNode(Session session,
                                         ManageDriveService manageDriveService,
                                         NodeHierarchyCreator nodeHierarchyCreator,
                                         NodeFinder nodeFinder,
                                         String driverName,
                                         String currentFolder) throws Exception {
    DriveData driveData = manageDriveService.getDriveByName(driverName);
    StringBuilder parentPath = new StringBuilder("");
    if (driveData != null) {
      parentPath.append(driveData.getHomePath());

      if (driveData.getHomePath().startsWith(nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH) + "/${userId}")) {
        parentPath.setLength(0);
        parentPath.append(org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(driveData.getHomePath(),
                                                                                       ConversationState.getCurrent()
                                                                                                        .getIdentity()
                                                                                                        .getUserId()));
      }
    }

    if (StringUtils.isNotBlank(currentFolder)) {
      parentPath.append("/").append(currentFolder);
    }
    String parentPathStr = parentPath.toString().replace("//", "/");
    return getTargetNode(session, nodeFinder, parentPathStr);
  }

  /**
   * Creates a symlink for the file defined by attachmentNode inside the parentNode folder with setting the permission
   * @param attachmentNode the attached file
   * @param parentNode the folder where the attachment will be saved
   * @param permission the permission to set to the file
   * @return the node representing the link for the file
   */
  public static Node createSymlink(Node attachmentNode, Node parentNode, String permission) {
    Node linkNode;
    try {
      linkNode = parentNode.addNode(attachmentNode.getName(), EXO_SYMLINK);
      linkNode.setProperty(EXO_WORKSPACE, attachmentNode.getSession().getWorkspace().getName());
      linkNode.setProperty(EXO_PRIMARYTYPE, attachmentNode.getPrimaryNodeType().getName());
      linkNode.setProperty(EXO_SYMLINK_UUID, ((ExtendedNode) attachmentNode).getIdentifier());
      if (linkNode.canAddMixin(EXO_SORTABLE)) {
        linkNode.addMixin(EXO_SORTABLE);
      }
      if (attachmentNode.hasProperty(EXO_TITLE)) {
        linkNode.setProperty(EXO_TITLE, attachmentNode.getProperty(EXO_TITLE).getString());
      }
      linkNode.setProperty(EXO_NAME, attachmentNode.getName());
      String nodeMimeType = getMimeType(attachmentNode);
      linkNode.addMixin(MIX_FILE_TYPE);
      linkNode.setProperty(EXO_FILE_TYPE, nodeMimeType);
      if (linkNode.canAddMixin(EXO_PRIVILEGEABLE)) {
        linkNode.addMixin(EXO_PRIVILEGEABLE);
      }
      ((ExtendedNode) linkNode).setPermission(permission, new String[] { PermissionType.READ });
      parentNode.save();
      return linkNode;
    } catch (Exception e) {
      LOG.error("Error updating sharing of document {}", attachmentNode, e);
      return null;
    }
  }

  public static Node getTargetNode(Session session, NodeFinder nodeFinder, String path) throws Exception {
    return (Node) nodeFinder.getItem(session, path, true);
  }

  public static Session getSession(SessionProviderService sessionProviderService,
                                   RepositoryService repositoryService) throws RepositoryException {
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    return sessionProvider.getSession(getCurrentWorkspace(repositoryService), repositoryService.getCurrentRepository());
  }

  public static Session getSystemSession(SessionProviderService sessionProviderService,
                                   RepositoryService repositoryService) throws RepositoryException {
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    return sessionProvider.getSession(getCurrentWorkspace(repositoryService), repositoryService.getCurrentRepository());
  }

  public static String getCurrentWorkspace(RepositoryService repositoryService) throws RepositoryException {
    return repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
  }

  public static boolean isQuarantinedItem(Session systemSession, String attachmentId) throws RepositoryException {
    Node attachmentNode = ((ExtendedSession)systemSession).getNodeByIdentifier(attachmentId);
    return attachmentNode.getPath().startsWith("/" + QUARANTINE_FOLDER + "/");
  }

  /**
   * Get the MimeType
   *
   * @param node the node
   * @return the MimeType
   */
  public static String getMimeType(Node node) {
    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && node.hasNode(NodetypeConstant.JCR_CONTENT)) {
        return node.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_MIME_TYPE).getString();
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return "";
  }

  /**
   * Removes duplicated items and symlinks if their original file is present in the list
   * @param userSession JCR session for the user
   * @param attachments List of attachments
   * @return list of attachments with no duplicates
   */
  public static List<Attachment> removeDuplicatedAttachments(Session userSession, List<Attachment> attachments) {
    Map<String, Attachment> filteringAttachmentsMap = new HashMap<>();
    for(Attachment entity : attachments) {
      Node entityNode = getNodeByIdentifier(userSession, entity.getId());
      try {
        if (entityNode != null && entityNode.isNodeType(EXO_SYMLINK)) {
          String originalEntityNodeId = entityNode.getProperty(EXO_SYMLINK_UUID).getString();
          filteringAttachmentsMap.put(originalEntityNodeId, entity);
        } else {
          filteringAttachmentsMap.put(entity.getId(), entity);
        }
      } catch (Exception e) {
        filteringAttachmentsMap.put(entity.getId(), entity);
      }
    }
    return new ArrayList<>(filteringAttachmentsMap.values());
  }

  public static Node getNodeByIdentifier(Session session, String nodeId) {
    try {
      return ((ExtendedSession) session).getNodeByIdentifier(nodeId);
    } catch (PathNotFoundException e) {
      LOG.info("Node with identifier {} was not found !", nodeId);
    } catch (RepositoryException e) {
      LOG.debug("Error retrieving node with identifier {}", nodeId, e);
    }
    return null;
  }
}
