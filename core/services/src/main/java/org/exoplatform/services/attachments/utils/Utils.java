package org.exoplatform.services.attachments.utils;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;

public class Utils {

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

  public static Node getTargetNode(Session session, NodeFinder nodeFinder, String path) throws Exception {
    return (Node) nodeFinder.getItem(session, path, true);
  }

  public static Session getSession(SessionProviderService sessionProviderService,
                                   RepositoryService repositoryService) throws RepositoryException {
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    return sessionProvider.getSession(getCurrentWorkspace(repositoryService), repositoryService.getCurrentRepository());
  }

  public static String getCurrentWorkspace(RepositoryService repositoryService) throws RepositoryException {
    return repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
  }
}
