package org.exoplatform.social.space.customization;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.spi.SpaceService;

public class SpaceCustomizationService {

  private static final String    GROUPS_PATH                  = "groupsPath";

  private static final String    ACTIVITY_FOLDER_UPLOAD_NAME  = "Activity Stream Documents";

  private SessionProviderService sessionProviderService;

  private NodeHierarchyCreator   nodeHierarchyCreator         = null;

  private RepositoryService      repositoryService            = null;

  private SpaceService           spaceService                 = null;

  private String                 groupsPath;

  public SpaceCustomizationService(SessionProviderService sessionProviderService,
                                   NodeHierarchyCreator nodeHierarchyCreator,
                                   RepositoryService repositoryService) {
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_PATH);
    if (StringUtils.isNotBlank(groupsPath) && groupsPath.lastIndexOf("/") == groupsPath.length() - 1) {
      groupsPath = groupsPath.substring(0, groupsPath.lastIndexOf("/"));
    }
  }

  public void createSpaceDefaultFolders(String groupId) throws Exception {
    Node parentNode;
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    String workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();
    Session session = sessionProvider.getSession(workspaceName, currentRepository);
    String groupPath = nodeHierarchyCreator.getJcrPath("groupsPath");
    String spaceParentPath = groupPath + groupId + "/Documents";
    if (!session.itemExists(spaceParentPath)) {
      throw new IllegalStateException("Root node of space '" + spaceParentPath + "' doesn't exist");
    }
    parentNode = (Node) session.getItem(spaceParentPath);

    if (!parentNode.hasNode(ACTIVITY_FOLDER_UPLOAD_NAME)) {
      Node activityFolder = parentNode.addNode(ACTIVITY_FOLDER_UPLOAD_NAME);
      activityFolder.addMixin(NodetypeConstant.EXO_HIDDENABLE);
      session.save();
    }
  }

  public SpaceService getSpaceService() {
    if (this.spaceService == null) {
      this.spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    }
    return this.spaceService;
  }

}
