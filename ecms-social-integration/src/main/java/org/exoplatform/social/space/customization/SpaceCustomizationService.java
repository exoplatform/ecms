package org.exoplatform.social.space.customization;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.spi.SpaceService;

public class SpaceCustomizationService {

  private static final Log       LOG                          = ExoLogger.getExoLogger(SpaceCustomizationService.class);

  private static final String    GROUPS_PATH                  = "groupsPath";

  private static final String    ACTIVITY_FOLDER_UPLOAD_NAME  = "Activity Stream Documents";

  private SessionProviderService sessionProviderService;

  private NodeHierarchyCreator   nodeHierarchyCreator         = null;

  private DMSConfiguration       dmsConfiguration             = null;

  private RepositoryService      repositoryService            = null;

  private ConfigurationManager   configurationManager         = null;

  private SpaceService           spaceService                 = null;

  private UserACL                userACL                      = null;

  private String                 groupsPath;

  public SpaceCustomizationService(SessionProviderService sessionProviderService,
                                   NodeHierarchyCreator nodeHierarchyCreator,
                                   DMSConfiguration dmsConfiguration,
                                   RepositoryService repositoryService,
                                   ConfigurationManager configurationManager,
                                   UserACL userAcl) {
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.dmsConfiguration = dmsConfiguration;
    this.repositoryService = repositoryService;
    this.userACL = userAcl;
    this.configurationManager = configurationManager;
    this.sessionProviderService = sessionProviderService;
    groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_PATH);
    if (groupsPath.lastIndexOf("/") == groupsPath.length() - 1) {
      groupsPath = groupsPath.substring(0, groupsPath.lastIndexOf("/"));
    }
  }

  public void editSpaceDriveViewPermissions(String viewNodeName, String permission) throws RepositoryException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Trying to add permission " + permission + " for ECMS view " + viewNodeName);
    }
    String viewsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWS_PATH);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    Session session = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
    Node viewHomeNode = (Node) session.getItem(viewsPath);
    if (viewHomeNode.hasNode(viewNodeName)) {
      Node contentNode = viewHomeNode.getNode(viewNodeName);
      String contentNodePermissions = contentNode.getProperty("exo:accessPermissions").getString();
      contentNode.setProperty("exo:accessPermissions", contentNodePermissions.concat(",").concat(permission));
      viewHomeNode.save();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Permission " + permission + " added with success to ECMS view " + viewNodeName);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can not find view node: " + viewNodeName);
      }
    }
  }

  public SpaceService getSpaceService() {
    if (this.spaceService == null) {
      this.spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    }
    return this.spaceService;
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
      parentNode.addNode(ACTIVITY_FOLDER_UPLOAD_NAME);
      session.save();
    }
  }

}
