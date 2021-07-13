package org.exoplatform.ecms.activity.listener;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

/**
 * A listener to share documents from original Activity to the shared space
 * location
 */
public class FileActivityListener extends ActivityListenerPlugin {

  public static final String    DOCPATH       = "DOCPATH";

  public static final String    NODEPATH_NAME = "nodePath";

  private static final Log      LOG           = ExoLogger.getLogger(FileActivityListener.class);

  private IShareDocumentService shareDocumentService;

  private ActivityManager       activityManager;

  public FileActivityListener(ActivityManager activityManager, IShareDocumentService shareDocumentService) {
    this.shareDocumentService = shareDocumentService;
    this.activityManager = activityManager;
  }

  @Override
  public void shareActivity(ActivityLifeCycleEvent event) {
    ExoSocialActivity sharedActivity = event.getActivity();
    if (sharedActivity != null && sharedActivity.getTemplateParams() != null
        && sharedActivity.getTemplateParams().containsKey("originalActivityId")) {
      String originalActivityId = sharedActivity.getTemplateParams().get("originalActivityId");
      ExoSocialActivity originalActivity = activityManager.getActivity(originalActivityId);
      if (originalActivity != null && originalActivity.getTemplateParams() != null
          && !sharedActivity.isComment() && sharedActivity.getActivityStream() != null
          && (originalActivity.getTemplateParams().containsKey(DOCPATH)
              || originalActivity.getTemplateParams().containsKey(NODEPATH_NAME))) {
        try {
          shareDocumentService.shareDocumentActivityToSpace(sharedActivity);
        } catch (Exception e) {
          LOG.error("Error while sharing files of activity {}", sharedActivity.getId(), e);
        }
      }
    }
  }

}
