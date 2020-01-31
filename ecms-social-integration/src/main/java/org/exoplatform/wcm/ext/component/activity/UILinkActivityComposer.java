/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity;

import java.util.*;

import org.exoplatform.commons.embedder.ExoMedia;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.service.rest.LinkShare;
import org.exoplatform.social.webui.composer.UIActivityComposer;
import org.exoplatform.social.webui.composer.UIComposer;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.social.webui.profile.UIUserActivitiesDisplay;
import org.exoplatform.social.webui.space.UISpaceActivitiesDisplay;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.*;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * UIComposerLinkExtension.java
 * <p>
 * an ui component to attach link, gets link information and displays; changes link title,
 * description content inline.
 * </p>
 *
 * @author    <a href="http://hoatle.net">hoatle</a>
 * @since     Apr 19, 2010
 */
@ComponentConfig(
  template = "war:/groovy/ecm/social-integration/plugin/link/UILinkActivityComposer.gtmpl",
  events = {
    @EventConfig(listeners = UILinkActivityComposer.AttachActionListener.class),
    @EventConfig(listeners = UILinkActivityComposer.ChangeLinkContentActionListener.class),
    @EventConfig(listeners = UILinkActivityComposer.RemoveLinkActionListener.class),
    @EventConfig(listeners = UIActivityComposer.CloseActionListener.class),
    @EventConfig(listeners = UIActivityComposer.SubmitContentActionListener.class),
    @EventConfig(listeners = UIActivityComposer.ActivateActionListener.class)
  }
)
public class UILinkActivityComposer extends UIActivityComposer {
  public static final String LINK_PARAM = "link";
  public static final String IMAGE_PARAM = "image";
  public static final String TITLE_PARAM = "title";
  public static final String DESCRIPTION_PARAM = "description";
  public static final String COMMENT_PARAM = "comment";
  public static final String HTML_PARAM = "html";

  private static final String MSG_ERROR_INVALID_LINK = "UILinkComposerPlugin.msg.error.Attach_Link";
  
  private LinkShare linkShare_;
  private boolean linkInfoDisplayed_ = false;
  private Map<String, String> templateParams;
  private boolean isDisplayed;
  
  /** Html attribute title. */
  private static final String HTML_ATTRIBUTE_TITLE   = "title";
  
  /**
   * constructor
   */
  public UILinkActivityComposer() {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    ResourceBundle resourceBundle = requestContext.getApplicationResourceBundle();
    setReadyForPostingActivity(false);
    UIFormStringInput inputLink = new UIFormStringInput("InputLink", "InputLink", null);
    inputLink.setHTMLAttribute(HTML_ATTRIBUTE_TITLE, resourceBundle.getString("UILinkComposerPlugin.label.InputLink"));
    addChild(inputLink);
  }

  /**
   * Gets displayed information of component.
   * 
   * @return the isDisplayed
   */
  @Override
  public boolean isDisplayed() {
    return isDisplayed;
  }

  /**
   * Sets displayed information of component.
   * 
   * @param isDisplayed the isDisplayed to set
   */
  @Override
  public void setDisplayed(boolean isDisplayed) {
    this.isDisplayed = isDisplayed;
  }

  public void setLinkInfoDisplayed(boolean displayed) {
    linkInfoDisplayed_ = displayed;
  }

  public boolean isLinkInfoDisplayed() {
    return linkInfoDisplayed_;
  }

  public void setTemplateParams(Map<String, String> tempParams) {
    templateParams = tempParams;
  }

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public void clearLinkShare() {
    linkShare_ = null;
  }

  public LinkShare getLinkShare() {
    return linkShare_;
  }

  /**
   * sets link url to gets content
   * @param url
   * @throws Exception
   */
  private void setLink(String url, WebuiRequestContext requestContext) throws Exception {
    try {
      linkShare_ = LinkShare.getInstance(url);
    } catch (Exception e) {
      clearLinkShare();
      resetToDefault();
      displayErrorMessage(requestContext, MSG_ERROR_INVALID_LINK);
      return;
    }
    
    if (linkShare_ == null) {
      resetToDefault();
      displayErrorMessage(requestContext, MSG_ERROR_INVALID_LINK);
      return;
    }
    
    templateParams = new LinkedHashMap<String, String>();
    templateParams.put(LINK_PARAM, linkShare_.getLink());
    ExoMedia mediaObject = linkShare_.getMediaObject();
    String image = "";
    List<String> images = linkShare_.getImages();
    if (images != null && images.size() > 0) {
      image = images.get(0);
    }
    templateParams.put(IMAGE_PARAM, image);
    templateParams.put(TITLE_PARAM, mediaObject != null ? mediaObject.getTitle() : linkShare_.getTitle());
    templateParams.put(DESCRIPTION_PARAM, (mediaObject != null)
                                           ? mediaObject.getDescription(): linkShare_.getDescription());
    templateParams.put(HTML_PARAM, mediaObject != null ? mediaObject.getHtml() : null);
    
    setLinkInfoDisplayed(true);
  }

  private void resetToDefault() {
    setReadyForPostingActivity(false);
    setDisplayed(false);
    getActivityComposerManager().setDefaultActivityComposer();
  }
  
  /**
   * Add error message to UIApplication.
   * 
   * @param requestContext
   * @param errorMessage
   */
  private void displayErrorMessage(WebuiRequestContext requestContext, String errorMessage) {
    UIApplication uiApp = requestContext.getUIApplication();
    uiApp.addMessage(new ApplicationMessage(errorMessage, null, ApplicationMessage.WARNING));
    ((PortalRequestContext) requestContext.getParentAppRequestContext()).ignoreAJAXUpdateOnPortlets(true);
  }

  static public class AttachActionListener extends EventListener<UILinkActivityComposer> {

    @Override
    public void execute(Event<UILinkActivityComposer> event) throws Exception {
      WebuiRequestContext requestContext = event.getRequestContext();
      UILinkActivityComposer uiComposerLinkExtension = event.getSource();
      String url = requestContext.getRequestParameter(OBJECTID).trim();

      uiComposerLinkExtension.setLink(url, requestContext);
      if (uiComposerLinkExtension.linkShare_ != null) {
        uiComposerLinkExtension.getActivityComposerManager().setCurrentActivityComposer(uiComposerLinkExtension);
        requestContext.addUIComponentToUpdateByAjax(uiComposerLinkExtension);
        event.getSource().setReadyForPostingActivity(true);
      } else {
        uiComposerLinkExtension.getActivityComposerManager().setDefaultActivityComposer();
        requestContext.addUIComponentToUpdateByAjax(uiComposerLinkExtension);
      }
    }
  }

  static public class ChangeLinkContentActionListener extends EventListener<UILinkActivityComposer> {
    @Override
    public void execute(Event<UILinkActivityComposer> event) throws Exception {
      WebuiRequestContext requestContext = event.getRequestContext();
      UILinkActivityComposer uiComposerLinkExtension = event.getSource();
      Map<String, String> tempParams = new LinkedHashMap<String, String>();
      tempParams.put(LINK_PARAM, requestContext.getRequestParameter(LINK_PARAM));
      tempParams.put(IMAGE_PARAM, requestContext.getRequestParameter(IMAGE_PARAM));
      tempParams.put(TITLE_PARAM, requestContext.getRequestParameter(TITLE_PARAM));
      tempParams.put(DESCRIPTION_PARAM, requestContext.getRequestParameter(DESCRIPTION_PARAM));
      uiComposerLinkExtension.setTemplateParams(tempParams);
      uiComposerLinkExtension.setLinkInfoDisplayed(true);
      requestContext.addUIComponentToUpdateByAjax(uiComposerLinkExtension);
      UIComponent uiParent = uiComposerLinkExtension.getParent();
      if (uiParent != null) {
        uiParent.broadcast(event, event.getExecutionPhase());
      }
    }
  }

  public static class RemoveLinkActionListener extends EventListener<UILinkActivityComposer> {
    @Override
    public void execute(Event<UILinkActivityComposer> event) throws Exception {
      UILinkActivityComposer uiComposerLinkExtension = event.getSource();
      uiComposerLinkExtension.clearLinkShare();
      uiComposerLinkExtension.setLinkInfoDisplayed(false);
      uiComposerLinkExtension.setReadyForPostingActivity(false);
      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiComposerLinkExtension);
    }
  }
  
  @Override
  protected void onActivate(Event<UIActivityComposer> arg0) {
  }

  @Override
  protected void onClose(Event<UIActivityComposer> arg0) {
    setReadyForPostingActivity(false);
  }

  @Override
  protected void onSubmit(Event<UIActivityComposer> arg0) {
  }

  @Override
  public void onPostActivity(PostContext postContext, UIComponent source,
                             WebuiRequestContext requestContext, String postedMessage) throws Exception {
  }

  @Override
  protected ExoSocialActivity onPostActivity(UIComposer.PostContext postContext, String postedMessage) throws Exception {
    Map<String, String> templateParams = getTemplateParams();
    if (templateParams == null) return null;
    if (templateParams.size() == 0) {
      getAncestorOfType(UIPortletApplication.class)
        .addMessage(new ApplicationMessage("UIComposer.msg.error.Empty_Message", null, ApplicationMessage.WARNING));
      return null;
    }
    ActivityManager activityManager = getApplicationComponent(ActivityManager.class);
    IdentityManager identityManager = getApplicationComponent(IdentityManager.class);
    //
    String remoteUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteUser, true);

    templateParams.put(COMMENT_PARAM, postedMessage);
    templateParams.put(org.exoplatform.social.core.BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, COMMENT_PARAM ); 
    
    if(templateParams.get(IMAGE_PARAM) == null){
      templateParams.put(IMAGE_PARAM, "");
    }
    setTemplateParams(templateParams);

    String title = "${" + TITLE_PARAM + "}";
    ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(), UILinkActivity.ACTIVITY_TYPE, title, null);
    activity.setTemplateParams(templateParams);
    activity.setExternalId(UILinkActivity.ACTIVITY_TYPE);
    activity.setUrl(templateParams.get(LINK_PARAM).toString());

    if (postContext == PostContext.SPACE) {
      UISpaceActivitiesDisplay uiDisplaySpaceActivities = (UISpaceActivitiesDisplay) getActivityDisplay();
      Space space = uiDisplaySpaceActivities.getSpace();

      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);

      activityManager.saveActivityNoReturn(spaceIdentity, activity);

    } else if (postContext == PostContext.USER) {
      UIUserActivitiesDisplay uiUserActivitiesDisplay = (UIUserActivitiesDisplay) getActivityDisplay();
      String ownerName = uiUserActivitiesDisplay.getOwnerName();
      Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, ownerName, false);

      activityManager.saveActivityNoReturn(ownerIdentity, activity);

      if ((uiUserActivitiesDisplay.getSelectedDisplayMode() == UIUserActivitiesDisplay.DisplayMode.CONNECTIONS)
          || (uiUserActivitiesDisplay.getSelectedDisplayMode() == UIUserActivitiesDisplay.DisplayMode.MY_SPACE)) {
        uiUserActivitiesDisplay.setSelectedDisplayMode(UIUserActivitiesDisplay.DisplayMode.MY_ACTIVITIES);
      }
    }
    setTemplateParams(null);
    clearLinkShare();
    return activityManager.getActivity(activity.getId());
  }
  
  protected void clearComposerData() {
    clearLinkShare();
    resetToDefault();
    setLinkInfoDisplayed(false);
  }
}
