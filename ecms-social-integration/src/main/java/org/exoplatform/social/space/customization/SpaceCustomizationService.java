package org.exoplatform.social.space.customization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
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

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.
   * exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deployContentToSpaceDrive(SessionProvider sessionProvider,
                                        String spaceId,
                                        DeploymentDescriptor deploymentDescriptor)
                                                                                   throws Exception {

    String sourcePath = deploymentDescriptor.getSourcePath();
    LOG.info("Deploying '" + sourcePath + "'content to '" + spaceId + "' Space JCR location");

    // sourcePath should start with: war:/, jar:/, classpath:/, file:/
    Boolean cleanupPublication = deploymentDescriptor.getCleanupPublication();

    InputStream inputStream = configurationManager.getInputStream(sourcePath);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(deploymentDescriptor.getTarget().getWorkspace(), repository);
    String targetNodePath = deploymentDescriptor.getTarget().getNodePath();
    if (targetNodePath.indexOf("/") == 0) {
      targetNodePath = targetNodePath.replaceFirst("/", "");
    }
    if (targetNodePath.lastIndexOf("/") == targetNodePath.length() - 1) {
      targetNodePath = targetNodePath.substring(0, targetNodePath.lastIndexOf("/"));
    }
    // if target path contains folders, then create them
    if (!targetNodePath.equals("")) {
      Node spaceRootNode = (Node) session.getItem(groupsPath + spaceId);
      Utils.makePath(spaceRootNode, targetNodePath, NodetypeConstant.NT_UNSTRUCTURED);
    }
    String fullTargetNodePath = groupsPath + spaceId + "/" + targetNodePath;
    Node parentTargetNode = (Node) session.getItem(fullTargetNodePath);
    NodeIterator nodeIterator = parentTargetNode.getNodes();
    List<String> initialChildNodesUUID = new ArrayList<String>();
    List<String> initialChildNodesNames = new ArrayList<String>();
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      String uuid = null;
      try {
        uuid = node.getUUID();
      } catch (Exception exception) {
        // node is not referenceable
        continue;
      }
      initialChildNodesUUID.add(uuid);
      initialChildNodesNames.add(node.getName());
    }

    session.importXML(fullTargetNodePath, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

    parentTargetNode = (Node) session.getItem(fullTargetNodePath);
    nodeIterator = parentTargetNode.getNodes();
    List<ExtendedNode> newChildNodesUUID = new ArrayList<ExtendedNode>();
    while (nodeIterator.hasNext()) {
      ExtendedNode childNode = (ExtendedNode) nodeIterator.nextNode();
      String uuid = null;
      try {
        uuid = childNode.getUUID();
      } catch (Exception exception) {
        // node is not referenceable
        continue;
      }
      // determines wether this is a new node or not
      if (!initialChildNodesUUID.contains(uuid)) {
        if (initialChildNodesNames.contains(childNode.getName())) {
          LOG.info(childNode.getName() + " already exists under " + fullTargetNodePath + ". This node will not be imported!");
          childNode.remove();
        } else {
          newChildNodesUUID.add(childNode);
        }
      }
    }
    String spaceMembershipManager = userACL.getAdminMSType() + ":" + spaceId;
    for (ExtendedNode extendedNode : newChildNodesUUID) {
      if (extendedNode.isNodeType(NodetypeConstant.EXO_PRIVILEGEABLE)) {
        extendedNode.clearACL();
      } else if (extendedNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
        extendedNode.addMixin("exo:privilegeable");
        extendedNode.clearACL();
      } else {
        throw new IllegalStateException("Can't change permissions on node imported to the added Space.");
      }
      extendedNode.setPermission(IdentityConstants.ANY, new String[] { PermissionType.READ });
      extendedNode.setPermission(spaceMembershipManager, PermissionType.ALL);
      if (cleanupPublication) {

        /**
         * This code allows to cleanup the publication lifecycle in the target
         * folder after importing the data. By using this, the publication live
         * revision property will be re-initialized and the content will be set
         * as published directly. Thus, the content will be visible in front
         * side.
         */
        if (extendedNode.hasProperty("publication:liveRevision") && extendedNode.hasProperty("publication:currentState")) {
          LOG.info("\"" + extendedNode.getName() + "\" publication lifecycle has been cleaned up");
          extendedNode.setProperty("publication:liveRevision", "");
          extendedNode.setProperty("publication:currentState", "published");
        }
      }
    }

    session.save();
    session.logout();
    LOG.info(deploymentDescriptor.getSourcePath() + " is deployed succesfully into " + fullTargetNodePath);
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
