/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.browsecontent;
import javax.jcr.Node;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.EmailAddressValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@gmail.com
 * Jan 30, 2007
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UICBCommentForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UICBCommentForm.CancelActionListener.class)
    }
)

public class UICBCommentForm extends UIForm implements UIPopupComponent {
  final public static String DEFAULT_LANGUAGE = "default";
  final private static String FIELD_EMAIL = "email";
  final private static String FIELD_WEBSITE = "website";
  final private static String FIELD_COMMENT = "comment";

  private Node docNode_;

  private boolean edit;

  private String nodeCommentPath;

  public boolean isEdit() {
    return edit;
  }

  public void setEdit(boolean edit) {
    this.edit = edit;
  }

  public String getNodeCommentPath() {
    return nodeCommentPath;
  }

  public void setNodeCommentPath(String nodeCommentPath) {
    this.nodeCommentPath = nodeCommentPath;
  }



  public UICBCommentForm() throws Exception {
    setActions(new String[] {"Save", "Cancel"});
  }

  public Node getDocument() { return docNode_;}
  public void setDocument(Node node) { docNode_ = node;}

  private void prepareFields() throws Exception{
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    String userName = requestContext.getRemoteUser();
    if(userName == null || userName.length() == 0){
      addUIFormInput(new UIFormStringInput(FIELD_EMAIL, FIELD_EMAIL, null).addValidator(EmailAddressValidator.class));
      addUIFormInput(new UIFormStringInput(FIELD_WEBSITE, FIELD_WEBSITE, null));
    }
    addUIFormInput(new UIFormWYSIWYGInput(FIELD_COMMENT, FIELD_COMMENT, null).addValidator(MandatoryValidator.class));
    if (isEdit()) {
      Node comment = getAncestorOfType(UIBrowseContentPortlet.class).findFirstComponentOfType(UIBrowseContainer.class)
                     .getNodeByPath(nodeCommentPath,getDocument().getSession().getWorkspace().getName());
      if(comment.hasProperty("exo:commentContent")){
        getChild(UIFormWYSIWYGInput.class).setValue(comment.getProperty("exo:commentContent").getString());
      }
    }
  }

  public static class CancelActionListener extends EventListener<UICBCommentForm>{
    public void execute(Event<UICBCommentForm> event) throws Exception {
      UICBCommentForm uiForm = event.getSource();
      uiForm.reset();
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);
      uiPopupAction.deActivate();
    }
  }

  public void activate() throws Exception {
    prepareFields();
  }
  public void deActivate() throws Exception { }

  public static class SaveActionListener extends EventListener<UICBCommentForm>{
    public void execute(Event<UICBCommentForm> event) throws Exception {
      UICBCommentForm uiForm = event.getSource();
      String comment = (String)uiForm.<UIFormInputBase>getUIInput(FIELD_COMMENT).getValue();
      UIBrowseContentPortlet uiPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      UIBrowseContainer uiBCContainer = uiPortlet.findFirstComponentOfType(UIBrowseContainer.class);
      UIDocumentDetail uiDocumentDetail = uiBCContainer.getChild(UIDocumentDetail.class);
      Node currentDoc = uiForm.getDocument();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if(comment == null || comment.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UICBCommentForm.msg.comment-required", null, ApplicationMessage.WARNING));
        UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
        
        return;
      }
      CommentsService commentsService = uiForm.getApplicationComponent(CommentsService.class);
      String lockToken = LockUtil.getLockToken(currentDoc);
      if (lockToken != null)
        currentDoc.getSession().addLockToken(lockToken);
      try {
        if (uiForm.isEdit()) {
          Node commentNode = uiBCContainer.getNodeByPath(uiForm.getNodeCommentPath(),
                                                         currentDoc.getSession()
                                                                   .getWorkspace()
                                                                   .getName());
          commentsService.updateComment(commentNode, comment);
        } else {
          String userName = event.getRequestContext().getRemoteUser();
          String website = null;
          String email = null;
          if(userName == null || userName.trim().length() == 0){
            userName = "anonymous";
            website = uiForm.getUIStringInput(FIELD_WEBSITE).getValue();
            email = uiForm.getUIStringInput(FIELD_EMAIL).getValue();
          } else {
            OrganizationService organizationService = uiForm.getApplicationComponent(OrganizationService.class);
            UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
            UserHandler userHandler = organizationService.getUserHandler();
            User user = userHandler.findUserByName(userName);
            UserProfile userProfile = profileHandler.findUserProfileByName(userName);
            website = userProfile.getUserInfoMap().get("user.business-info.online.uri");
            email = user.getEmail();
          }
          String language = uiDocumentDetail.getLanguage();
          if (DEFAULT_LANGUAGE.equals(language)) {
            if (!uiForm.getDocument().hasProperty(Utils.EXO_LANGUAGE)) {
              currentDoc.addMixin("mix:i18n");
              currentDoc.save();
              language = DEFAULT_LANGUAGE;
            } else {
              language = uiForm.getDocument().getProperty(Utils.EXO_LANGUAGE).getString();
            }
          }
          commentsService.addComment(uiForm.getDocument(), userName, email, website, comment, language);
        }
      } catch (LockException le) {
        uiApp.addMessage(new ApplicationMessage("UICBCommentForm.msg.locked-doc", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UICBCommentForm.msg.versioning-doc", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UICBCommentForm.msg.error-vote", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);
      uiPopupAction.deActivate();
      UIPopupContainer uiPopupContainer = uiPortlet.getChildById("UICBPopupAction");
      if (uiPopupContainer.isRendered()) event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiBCContainer);
    }
  }
}
