package org.exoplatform.wcm.addons.rdbms.listener;

import javax.jcr.*;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.utils.ActivityTypeUtils;
import org.exoplatform.wcm.ext.component.activity.UILinkActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

public class WCMActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WCMActivityUpdaterListener.class);

  public WCMActivityUpdaterListener() {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity oldActivity = event.getSource();
    String type = (oldActivity.getType() == null) ? "" : oldActivity.getType();
    switch (type) {
    case UILinkActivity.ACTIVITY_TYPE:
      migrationLinkActivity(oldActivity, event.getData());
      break;
    case Utils.CONTENT_SPACES:
      migrationContentSpaceActivity(oldActivity, event.getData());
      break;
    case Utils.FILE_SPACES:
      migrationFileSpaceActivity(oldActivity, event.getData());
      break;
    default:
      break;
    }
  }

  private void migrationLinkActivity(ExoSocialActivity oldActivity, String newId) {
  }

  private void migrationContentSpaceActivity(ExoSocialActivity oldActivity, String newId) {
  }

  private void migrationFileSpaceActivity(ExoSocialActivity activity, String newId) throws RepositoryException {
    if (activity.isComment()) {
      // TODO: Needs to confirm with ECMS team about the comment type
      // Utils.CONTENT_SPACES = "contents:spaces" Asks ECMS team to update the comment
      // There is new mixin type define to keep the CommentId
      // private static String MIX_COMMENT = "exo:activityComment";
      // private static String MIX_COMMENT_ID = "exo:activityCommentID";
      LOG.info(String.format("Migration file-spaces comment '%s' with new id's %s", activity.getTitle(), newId));
      //
      migrationDoc(activity, newId);
    } else {
      LOG.info(String.format("Migration file-spaces activity '%s' with new id's %s", activity.getTitle(), newId));
      //
      migrationDoc(activity, newId);
    }
  }

  private void migrationDoc(ExoSocialActivity activity, String newId) throws RepositoryException {
    String workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE);
    if(workspace == null) {
      workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE.toLowerCase());
    }
    String docId = activity.getTemplateParams().get(UIDocActivity.ID);
    Node docNode = getDocNode(workspace, activity.getUrl(), docId);
    if (docNode != null && docNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      LOG.info("Migration doc: " + docNode.getPath());
      try {
        ActivityTypeUtils.attachActivityId(docNode, newId);
        docNode.getSession().save();
      } catch (RepositoryException e) {
        LOG.warn("Updates the file-spaces activity is unsuccessful!");
        LOG.debug("Updates the file-spaces activity is unsuccessful!", e);
      }
    } else {
      LOG.info(String.format("Missing document's path/Id on template-parameters. Do not migrate this file-spaces activity width old id %s - new id %s" , activity.getId(), newId));
    }
  }

  /**
   * This method is target to get the Document node.
   * 
   * @param workspace
   * @param path
   * @param nodeId
   * @return
   */
  private Node getDocNode(String workspace, String path, String nodeId) {
    if (workspace == null || (nodeId == null && path == null)) {
      return null;
    }
    try {
      Session session = SessionProviderService.getSystemSessionProvider().getSession(workspace, SessionProviderService.getRepository());
      try {
        return session.getNodeByUUID(nodeId);
      } catch (Exception e) {
        return (Node) session.getItem(path);
      }
    } catch (RepositoryException e) {
      return null;
    }
  }
}