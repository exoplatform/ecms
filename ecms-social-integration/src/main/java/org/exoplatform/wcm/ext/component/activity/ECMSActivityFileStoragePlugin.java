package org.exoplatform.wcm.ext.component.activity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ActivityFile;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.ActivityFileStoragePlugin;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

/**
 * This plugin will store activity attachment files in JCR
 */
public class ECMSActivityFileStoragePlugin extends ActivityFileStoragePlugin {

  private static final String  USER_FOLDER_PUBLIC_ALIAS    = "userPublic";

  private static final String  ACTIVITY_FOLDER_UPLOAD_NAME = "Activity Stream Documents";

  private NodeHierarchyCreator nodeHierarchyCreator;

  private RepositoryService    repositoryService;

  private SpaceService         spaceService;

  private UploadService        uploadService;

  public ECMSActivityFileStoragePlugin(SpaceService spaceService,
                                       NodeHierarchyCreator nodeHierarchyCreator,
                                       RepositoryService repositoryService,
                                       UploadService uploadService,
                                       InitParams initParams) {
    super(initParams);
    this.spaceService = spaceService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.repositoryService = repositoryService;
    this.uploadService = uploadService;
  }

  @Override
  public void storeAttachments(ExoSocialActivity activity, Identity streamOwner, ActivityFile... attachments) throws Exception {
    if (attachments == null || attachments.length == 0) {
      return;
    }
    for (ActivityFile activityFile : attachments) {
      UploadResource uploadedResource = uploadService.getUploadResource(activityFile.getUploadId());
      if (uploadedResource == null) {
        throw new IllegalStateException("Cannot attach uploaded file " + activityFile.getUploadId() + ", it may not exist");
      }

      Node parentNode;

      SessionProvider sessionProvider = SessionProviderService.getSystemSessionProvider();
      ManageableRepository currentRepository = repositoryService.getCurrentRepository();
      String workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(workspaceName, currentRepository);

      if (streamOwner.getProviderId().equals(SpaceIdentityProvider.NAME)) {
        Space space = spaceService.getSpaceById(streamOwner.getId());
        String groupPath = nodeHierarchyCreator.getJcrPath("groupsPath");
        String spaceParentPath = groupPath + space.getGroupId() + "/Documents";
        if (!session.itemExists(spaceParentPath)) {
          throw new IllegalStateException("Root node of space '" + spaceParentPath + "' doesn't exist");
        }
        parentNode = (Node) session.getItem(spaceParentPath);
      } else {
        String remoteUser = ConversationState.getCurrent().getIdentity().getUserId();
        if (org.apache.commons.lang.StringUtils.isBlank(remoteUser)) {
          throw new IllegalStateException("Remote user is empty");
        }
        Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, remoteUser);
        String publicPath = nodeHierarchyCreator.getJcrPath(USER_FOLDER_PUBLIC_ALIAS);
        if (userNode == null || !userNode.hasNode(publicPath)) {
          throw new IllegalStateException("User '" + remoteUser + "' hasn't public folder");
        }
        parentNode = userNode.getNode(publicPath);

        if (!parentNode.hasNode(ACTIVITY_FOLDER_UPLOAD_NAME)) {
          parentNode.addNode(ACTIVITY_FOLDER_UPLOAD_NAME);
          session.save();
        }
      }

      Node parentUploadNode = parentNode.getNode(ACTIVITY_FOLDER_UPLOAD_NAME);
      Node node = parentUploadNode.addNode(uploadedResource.getFileName(), "nt:file");
      node.setProperty("exo:title", uploadedResource.getFileName());
      Node resourceNode = node.addNode("jcr:content", "nt:resource");
      resourceNode.setProperty("jcr:mimeType", uploadedResource.getMimeType());
      resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
      String fileDiskLocation = uploadedResource.getStoreLocation();
      try (InputStream inputStream = new FileInputStream(fileDiskLocation)) {
        resourceNode.setProperty("jcr:data", inputStream);
        session.save();
        node = (Node) session.getItem(node.getPath());
      }

      if (activity.getTemplateParams() == null) {
        activity.setTemplateParams(new HashMap<>());
      }
      concatenateParam(activity.getTemplateParams(), "REPOSITORY", "repository");
      concatenateParam(activity.getTemplateParams(), "WORKSPACE", "collaboration");
      concatenateParam(activity.getTemplateParams(), "DOCPATH", node.getPath());
      concatenateParam(activity.getTemplateParams(), "id", node.isNodeType("mix:referenceable") ? node.getUUID() : "");

      uploadService.removeUploadResource(activityFile.getUploadId());
    }
  }

}
