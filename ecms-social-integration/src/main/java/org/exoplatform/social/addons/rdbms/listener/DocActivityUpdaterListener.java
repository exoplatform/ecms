package org.exoplatform.social.addons.rdbms.listener;

import static org.exoplatform.social.plugin.doc.UIDocActivityBuilder.ACTIVITY_TYPE;

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

public class DocActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(DocActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (ACTIVITY_TYPE.equals(activity.getType())) {
      String workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE);
      if(workspace == null) {
        workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE.toLowerCase());
      }
      String docId = activity.getTemplateParams().get(UIDocActivity.ID);
      Node docNode = getDocNode(workspace, activity.getUrl(), docId);
      if (docNode != null && docNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          ActivityTypeUtils.attachActivityId(docNode, event.getData());
          docNode.save();
        } catch (RepositoryException e) {
          LOG.warn("Updates the document activity is unsuccessful!");
          LOG.debug("Updates the document activity is unsuccessful!", e);
        }
      }
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