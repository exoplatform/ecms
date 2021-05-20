package org.exoplatform.wcm.ext.component.activity.listener;

import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ActivityFile;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.List;

public class ActivityListener extends ActivityListenerPlugin {

  private static final Log             LOG           = ExoLogger.getLogger(ActivityListener.class);

  /** The constant COMMENT. */
  private static final String          COMMENT       = "";

  /** The constant POST_ACTIVITY. */
  private static final Boolean         POST_ACTIVITY = false;
  /** The constant ACTIVITY PARAMS. */
  private static final String          SEPARATOR_REGEX     = "\\|@\\|";
  private static final String          NODE_UUID_PARAM     =  "id";


  private final SpaceService           spaceService;

  private final SessionProviderService sessionProviderService;

  private final RepositoryService      repositoryService;

  private final IShareDocumentService  shareDocumentService;

  private final OrganizationService    organizationService;

  public ActivityListener(SpaceService spaceService,
                          SessionProviderService sessionProviderService,
                          RepositoryService repositoryService,
                          IShareDocumentService shareDocumentService,
                          OrganizationService organizationService) {
    this.spaceService = spaceService;
    this.sessionProviderService = sessionProviderService;
    this.repositoryService = repositoryService;
    this.shareDocumentService = shareDocumentService;
    this.organizationService = organizationService;
  }

  @Override
  public void saveActivity(ActivityLifeCycleEvent activityLifeCycleEvent) {
    shareActivityFilesToSpace(activityLifeCycleEvent);
  }

  private void shareActivityFilesToSpace(ActivityLifeCycleEvent activityLifeCycleEvent) {
    ExoSocialActivity activity = activityLifeCycleEvent.getActivity();
    List<ActivityFile> filesToShare = activity.getFiles();
    String[] uuidNodes = activity.getTemplateParams().get(NODE_UUID_PARAM).split(SEPARATOR_REGEX);

    String streamOwner = activity.getStreamOwner();
    Space targetSpace = spaceService.getSpaceByPrettyName(streamOwner);

    if (targetSpace != null) {
      Node node;
      String[] permissions = new String[] { PermissionType.READ };
      String organizationalIdentity;

      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository currentRepository;
      String workspaceName;
      Session session = null;

      try {
        currentRepository = repositoryService.getCurrentRepository();
        workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();
        session = sessionProvider.getSession(workspaceName, currentRepository);

      if (session != null) {
        Collection<Membership> memberships = null;
        String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
        try {
          memberships = organizationService.getMembershipHandler()
                                           .findMembershipsByUserAndGroup(currentUserId, targetSpace.getGroupId());
        } catch (Exception e) {
          LOG.warn("Error getting memberships by user (" + currentUserId + ") and group (" + targetSpace.getGroupId() + ")", e);
        }
        if (memberships != null) {
          for (String nodeUuid : uuidNodes) {
            try {
              node = session.getNodeByUUID(nodeUuid);

              for (Membership membership : memberships) {
                organizationalIdentity = new MembershipEntry(membership.getGroupId(), membership.getMembershipType()).toString();

                if (!PermissionUtil.hasPermissions(node, organizationalIdentity, permissions)) {
                  // publish to shared folder
                  shareDocumentService.publishDocumentToSpace(targetSpace.getGroupId(),
                                                              node,
                                                              COMMENT,
                                                              PermissionType.READ,
                                                              POST_ACTIVITY);
                  break;
                }
              }
            } catch (RepositoryException e) {
              LOG.error("Error while sharing document to space. Node uuid: " + nodeUuid, e);
            }
          }
        }
      }
      } catch (RepositoryException e) {
        LOG.warn("Error while getting session for sharing files to the space: " + targetSpace.getGroupId(), e);
      } finally {
        sessionProvider.close();
      }
    }
  }

  @Override
  public void updateActivity(ActivityLifeCycleEvent activityLifeCycleEvent) {
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent activityLifeCycleEvent) {
  }

  @Override
  public void updateComment(ActivityLifeCycleEvent activityLifeCycleEvent) {
  }

  @Override
  public void likeActivity(ActivityLifeCycleEvent activityLifeCycleEvent) {
  }

  @Override
  public void likeComment(ActivityLifeCycleEvent activityLifeCycleEvent) {
  }
}
