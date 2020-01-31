package org.exoplatform.wcm.ext.component.activity;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIPreviewCommentArea.gtmpl",
        events = {
                @EventConfig(listeners = UIPreviewCommentArea.RefreshCommentsActionListener.class),
                @EventConfig(listeners = UIPreviewCommentArea.RemoveCommentActionListener.class),
                @EventConfig(listeners = UIPreviewCommentArea.LikeActivityActionListener.class)

        }
)
public class UIPreviewCommentArea extends UIComponent {

  public static final String REFRESH_COMMENTS = "RefreshComments";
  public static final String REMOVE_COMMENT = "RemoveComment";

  private static final String DEFAULT_ACTIVITY = "DEFAULT_ACTIVITY";
  private static final String LINK_ACTIVITY = "LINK_ACTIVITY";
  private static final String DOC_ACTIVITY = "DOC_ACTIVITY";

  private Node getOriginalNode() throws Exception {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getOriginalNode();
  }

  private String[] getDisplayedIdentityLikes() throws Exception {
    return this.getBaseUIActivity().getDisplayedIdentityLikes();
  }

  private BaseUIActivity getBaseUIActivity() {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getBaseUIActivity();
  }

  private ExoSocialActivity getActivity() {
    return this.getBaseUIActivity().getActivity();
  }

  private String getActivityId() {
    return this.getActivity().getId();
  }

  public String getActivityStatus() {
    Map<String, String> activityParams = this.getActivity().getTemplateParams();
    if (activityParams.get(FileUIActivity.MESSAGE) == null) {
      if (this.getEmbedHtml() != null) {
        return activityParams.get("comment");
      }
      return activityParams.get(FileUIActivity.ACTIVITY_STATUS);
    } else {
      return null;
    }
  }

  private boolean isCommentDeletable(String activityUserId) throws SpaceException {
    return this.getBaseUIActivity().isCommentDeletable(activityUserId);
  }

  private boolean isLiked() throws Exception {
    return this.getBaseUIActivity().isLiked();
  }

  private List<ExoSocialActivity> getAllComments() {
    return this.getBaseUIActivity().getAllComments();
  }

  private String getCommentMessage(Map<String, String> activityParams) {
    String[] systemComment = Utils.getSystemCommentBundle(activityParams);
    StringBuffer commentBuffer = new StringBuffer();
    if (systemComment != null && systemComment.length > 0) {
      String[] systemCommentTitle = Utils.getSystemCommentTitle(activityParams);
      for (int count = 0; count < systemComment.length; count++) {
        String commentMessage = Utils.getBundleValue(systemComment[count]);
        if (systemCommentTitle != null && systemCommentTitle.length > count) {
          String[] titles = systemCommentTitle[count].split(ActivityCommonService.METADATA_VALUE_SEPERATOR);
          for (int i = 0; i < titles.length; i++) {
            commentMessage = commentMessage.replace("{" + i + "}", titles[i]);
            commentMessage = org.exoplatform.wcm.ext.component.activity.listener.Utils.getFirstSummaryLines(commentMessage);
          }
        }

        commentBuffer.append("<p class=\"ContentBlock\">").append(commentMessage).append("</p>");
      }
    }
    return commentBuffer.toString();
  }

  private String getEmbedHtml() {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getEmbedHtml();
  }

  private String getRelativeTimeLabel(WebuiBindingContext webuiBindingContext, long postedTime) {
    return this.getBaseUIActivity().getRelativeTimeLabel(webuiBindingContext, postedTime);
  }

  private String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return Utils.getSystemCommentTitle(activityParams);
  }

  private String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return Utils.getSystemCommentBundle(activityParams);
  }

  private boolean isNoLongerExisting(String activityId, Event<UIPreviewCommentArea> event) {
    ExoSocialActivity existingActivity = org.exoplatform.social.webui.Utils.getActivityManager().getActivity
            (activityId);
    if (existingActivity == null) {
      UIApplication uiApplication = event.getRequestContext().getUIApplication();
      uiApplication.addMessage(new ApplicationMessage("BaseUIActivity.msg.info.Activity_No_Longer_Exist",
              null,
              ApplicationMessage.INFO));
      return true;
    }
    return false;
  }

  public static class RefreshCommentsActionListener extends EventListener<UIPreviewCommentArea> {
    public void execute(Event<UIPreviewCommentArea> event) throws Exception {
      UIPreviewCommentArea uiPreviewCommentArea = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPreviewCommentArea);
    }
  }

  public static class RemoveCommentActionListener extends EventListener<UIPreviewCommentArea> {
    public void execute(Event<UIPreviewCommentArea> event) throws Exception {
      UIPreviewCommentArea uiPreviewCommentArea = event.getSource();
      String activityId = uiPreviewCommentArea.getActivityId();
      String commentId = event.getRequestContext().getRequestParameter(OBJECTID);

      if (uiPreviewCommentArea.isNoLongerExisting(activityId, event) ||
              uiPreviewCommentArea.isNoLongerExisting(commentId, event)) {
        return;
      }

      org.exoplatform.social.webui.Utils.getActivityManager().deleteComment(activityId, commentId);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPreviewCommentArea);
    }
  }

  public static class LikeActivityActionListener extends EventListener<UIPreviewCommentArea> {
    @Override
    public void execute(Event<UIPreviewCommentArea> event) throws Exception {
      UIPreviewCommentArea uiPreviewCommentArea = event.getSource();
      String activityId = uiPreviewCommentArea.getActivityId();
      BaseUIActivity uiActivity = uiPreviewCommentArea.getBaseUIActivity();

      if (uiPreviewCommentArea.isNoLongerExisting(activityId, event)) {
        return;
      }

      WebuiRequestContext requestContext = event.getRequestContext();
      String isLikedStr = requestContext.getRequestParameter(OBJECTID);
      uiActivity.setLike(Boolean.parseBoolean(isLikedStr));
      //
      JavascriptManager jm = requestContext.getJavascriptManager();
      //jm.require("SHARED/social-ui-activity", "activity").addScripts("activity.displayLike('#ContextBox" + activityId + "');");

      requestContext.addUIComponentToUpdateByAjax(uiPreviewCommentArea);
    }
  }
}
