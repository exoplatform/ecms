/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.newsletter.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterManageUserHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterPublicUserHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.newsletter.manager.NewsLetterUtil;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SAS Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com May 25, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/newsletter/NewsletterViewer/UINewsletterListViewer.gtmpl",
                 events = {
    @EventConfig(listeners = UINewsletterViewerForm.SubcribeActionListener.class),
    @EventConfig(listeners = UINewsletterViewerForm.ForgetEmailActionListener.class),
    @EventConfig(listeners = UINewsletterViewerForm.ChangeSubcriptionsActionListener.class) })
public class UINewsletterViewerForm extends UIForm {

  public static String SUBJECT_KEY = "UINewsletterViewerForm.Email.ConfirmUser.Subject";

  public static String CONTENT_KEY = "UINewsletterViewerForm.Email.ConfirmUser.Content";

  /** The user code. */
  public String userCode;

  /** The input email. */
  public UIFormStringInput inputEmail;

  /** The user mail. */
  public String userMail = "";

  /** The is updated. */
  public boolean isUpdated = false;

  /** The subcription handler. */
  public NewsletterSubscriptionHandler subcriptionHandler ;

  /** The public user handler. */
  public NewsletterPublicUserHandler publicUserHandler ;

  /** The check box input. */
  private UIFormCheckBoxInput<Boolean> checkBoxInput;

  /** The category handler. */
  private NewsletterCategoryHandler categoryHandler ;

  /** The manager user handler. */
  private NewsletterManageUserHandler managerUserHandler ;

  /** The newsletter manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The link to send mail. */
  private String linkToSendMail;

  /** The list ids. */
  private List<String> listIds;

  /**
   * Instantiates a new uI newsletter viewer form.
   *
   * @throws Exception the exception
   */
  public UINewsletterViewerForm() throws Exception {
    newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    categoryHandler = newsletterManagerService.getCategoryHandler();
    subcriptionHandler = newsletterManagerService.getSubscriptionHandler();
    publicUserHandler = newsletterManagerService.getPublicUserHandler();
    managerUserHandler = newsletterManagerService.getManageUserHandler();

    // get email when user have login
    String username = Util.getPortalRequestContext().getRemoteUser();
    if (username != null) {
      OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
      User useraccount = service.getUserHandler().findUserByName(username);
      inputEmail = new UIFormStringInput("inputEmail", "Email", useraccount.getEmail());
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      List<String> Ids = new ArrayList<String>();
      List<NewsletterSubscriptionConfig> listSubscriptions = this.subcriptionHandler.
          getSubscriptionIdsByPublicUser(sessionProvider, NewsLetterUtil.getPortalName(), useraccount.getEmail());
      for (NewsletterSubscriptionConfig subscriptionConfig : listSubscriptions) {
        Ids.add(subscriptionConfig.getCategoryName() + "#" + subscriptionConfig.getName());
      }
      if (Ids.size() > 0) {
        this.setListIds(Ids);
        this.userMail = useraccount.getEmail();
        this.inputEmail.setRendered(false);
        this.setActionAgain();
      } else {        
        boolean isExistedEmail = this.managerUserHandler.checkExistedEmail(WCMCoreUtils.getUserSessionProvider(),
                                                                           NewsLetterUtil.getPortalName(),
                                                                           useraccount.getEmail());
        if(isExistedEmail) {
          this.userMail = useraccount.getEmail();
          this.inputEmail.setRendered(false);
          this.setActionAgain();       
        } else {
          this.inputEmail.setRendered(true);
          this.inputEmail.setValue(useraccount.getEmail());
          this.setActions(new String[] { "Subcribe" });  
          this.isUpdated = false;
        }
      }

    } else {
      this.setActions(new String[] { "Subcribe" });
      inputEmail = new UIFormStringInput("inputEmail", "Email", null);
    }

    inputEmail.addValidator(MandatoryValidator.class).addValidator(UINewsletterViewerEmailAddressValidator.class);
    this.addChild(inputEmail);

  }

  /**
   * Sets the list ids.
   *
   * @param listIds the new list ids
   */
  public void setListIds(List<String> listIds){
    this.listIds = listIds;
  }

  /**
   * Inits the.
   *
   * @param listNewsletterSubcription the list newsletter subcription
   * @param categoryName the category name
   * @throws Exception the exception
   */
  public void init(List<NewsletterSubscriptionConfig> listNewsletterSubcription, String categoryName) throws Exception {
    if ((listIds != null && listIds.size() > 0)
        || (userCode != null && userCode.trim().length() > 0)) { // run when
                                                                 // confirm user
                                                                 // code
      String subcriptionPattern;
      for (NewsletterSubscriptionConfig newsletterSubcription : listNewsletterSubcription) {
        subcriptionPattern = categoryName + "#" + newsletterSubcription.getName();
        if (this.getChildById(subcriptionPattern) != null) this.removeChildById(subcriptionPattern);
        checkBoxInput = new UIFormCheckBoxInput<Boolean>(subcriptionPattern, subcriptionPattern, false);
        if(listIds.contains(subcriptionPattern)) checkBoxInput.setChecked(true);
        else checkBoxInput.setChecked(false);
        this.addChild(checkBoxInput);
      }
    } else {
      String subcriptionPattern;
      for (NewsletterSubscriptionConfig newsletterSubcription : listNewsletterSubcription) {
        subcriptionPattern = categoryName + "#" + newsletterSubcription.getName();
        if (this.getChildById(subcriptionPattern) != null) continue;
        else{
          checkBoxInput = new UIFormCheckBoxInput<Boolean>(subcriptionPattern, subcriptionPattern, false);
          this.addChild(checkBoxInput);
        }
      }
    }
  }

  /**
   * Sets the infor confirm.
   *
   * @param userEmail the user email
   * @param userCode the user code
   */
  public void setInforConfirm(String userEmail, String userCode){
    this.userMail = userEmail;
    this.userCode = userCode;
  }

  /**
   * Sets the action again.
   */
  private void setActionAgain(){
    this.setActions(new String[] { "ForgetEmail", "ChangeSubcriptions" });
    this.isUpdated = true;
  }

  /**
   * List subscription checked.
   *
   * @return the list< string>
   */
  @SuppressWarnings({ "unchecked" })
  private List<String> listSubscriptionChecked(){
    List<String> listSubscription = new ArrayList<String>();
    for(UIComponent component : this.getChildren()){
      try{
        if(((UIFormCheckBoxInput<Boolean>) component).isChecked())
          listSubscription.add(component.getName());
      }catch(ClassCastException ex){
        continue;
        // You shouldn't throw popup message, because some exception often rise here.
      };
    }
    return listSubscription;
  }

  /**
   * Gets the list categories.
   *
   * @return the list categories
   */
  public List<NewsletterCategoryConfig> getListCategories() {
    try {
      return categoryHandler.getListCategories(NewsLetterUtil.getPortalName(), WCMCoreUtils.getUserSessionProvider());
    } catch (Exception e) {
      return new ArrayList<NewsletterCategoryConfig>();
    }
  }

  /**
   * Gets the list subscription.
   *
   * @param categoryName the category name
   *
   * @return the list subscription
   */
  public List<NewsletterSubscriptionConfig> getListSubscription(String categoryName) {
    try {
      List<NewsletterSubscriptionConfig> listSubscription = subcriptionHandler.
          getSubscriptionsByCategory(WCMCoreUtils.getUserSessionProvider(), NewsLetterUtil.getPortalName(), categoryName);
      this.init(listSubscription, categoryName);
      return listSubscription;
    } catch (Exception e) {
      return new ArrayList<NewsletterSubscriptionConfig>();
    }
  }

  /**
   * Sets the link.
   *
   * @param url the new link
   */
  public void setLink(String url) throws Exception {
    this.linkToSendMail = NewsLetterUtil.generateLink(url);
  }

  /**
   * The listener interface for receiving forgetEmailAction events.
   * The class that is interested in processing a forgetEmailAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addForgetEmailActionListener<code> method. When
   * the forgetEmailAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ForgetEmailActionEvent
   */
  public static class ForgetEmailActionListener extends EventListener<UINewsletterViewerForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings("unchecked")
    public void execute(Event<UINewsletterViewerForm> event) throws Exception {
      UINewsletterViewerForm newsletterForm = event.getSource();
      newsletterForm.publicUserHandler.forgetEmail(WCMCoreUtils.getUserSessionProvider(),
                                                   NewsLetterUtil.getPortalName(),
                                                   newsletterForm.userMail);

      newsletterForm.setListIds(null);
      newsletterForm.inputEmail.setValue("");
      newsletterForm.inputEmail.setRendered(true);
      newsletterForm.userMail = "";
      for (UIComponent component : newsletterForm.getChildren()) {
        try {
          UIFormCheckBoxInput<Boolean> uiFormCheckBoxInput = (UIFormCheckBoxInput<Boolean>) component;
          if (uiFormCheckBoxInput.isChecked())
            uiFormCheckBoxInput.setChecked(false);
        } catch (ClassCastException ex) {
          continue;
          // You shouldn't throw popup message, because some exception often
          // rise here.
        }
      }
      newsletterForm.setActions(new String[] {"Subcribe"});
      newsletterForm.isUpdated = false;
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterForm);
    }
  }

  /**
   * The listener interface for receiving changeSubcriptionsAction events.
   * The class that is interested in processing a changeSubcriptionsAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeSubcriptionsActionListener<code> method. When
   * the changeSubcriptionsAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeSubcriptionsActionEvent
   */
  public static class ChangeSubcriptionsActionListener extends EventListener<UINewsletterViewerForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterViewerForm> event) throws Exception {
      UINewsletterViewerForm newsletterForm = event.getSource();
      List<String> listSubcriptionPattern = new ArrayList<String>();
      listSubcriptionPattern = newsletterForm.listSubscriptionChecked();
      newsletterForm.publicUserHandler.updateSubscriptions(WCMCoreUtils.getUserSessionProvider(),
                                                           NewsLetterUtil.getPortalName(),
                                                           newsletterForm.inputEmail.getValue(),
                                                           listSubcriptionPattern);
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      UIApplication uiApp = context.getUIApplication();
      uiApp.addMessage(new ApplicationMessage("UINewsletterViewerForm.msg.updateSuccess", null, ApplicationMessage.INFO));
      newsletterForm.setListIds(listSubcriptionPattern);
      newsletterForm.isUpdated = true;
      newsletterForm.userMail = newsletterForm.userMail;
      newsletterForm.inputEmail.setRendered(false);
      newsletterForm.setActions(new String[] {"ForgetEmail", "ChangeSubcriptions" });

      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterForm);
    }
  }

  /**
   * The listener interface for receiving subcribeAction events.
   * The class that is interested in processing a subcribeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSubcribeActionListener<code> method. When
   * the subcribeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SubcribeActionEvent
   */
  public static class SubcribeActionListener extends EventListener<UINewsletterViewerForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterViewerForm> event) throws Exception {
      UINewsletterViewerForm newsletterForm = event.getSource();
      String portalName = NewsLetterUtil.getPortalName();
      String userEmail = newsletterForm.inputEmail.getValue();
      List<String> listCategorySubscription = newsletterForm.listSubscriptionChecked();
      String contentOfMessage;
      boolean isExistedEmail = newsletterForm.managerUserHandler
        .checkExistedEmail(WCMCoreUtils.getUserSessionProvider(), portalName, userEmail);

      if (!isExistedEmail) {
        if(listCategorySubscription.size() < 1){
          contentOfMessage = "UINewsletterViewerForm.msg.checkSubscriptionToProcess";
        }else{
          WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
          ResourceBundle resourceBundle = context.getApplicationResourceBundle() ;
          String Subject = "";
          String Content = "";
          try {
            Subject = resourceBundle.getString(SUBJECT_KEY);
            Content = resourceBundle.getString(CONTENT_KEY);
          } catch (MissingResourceException e) {
            Subject = SUBJECT_KEY;
            Content = CONTENT_KEY;
          }
          // get email's content to create mail confirm
          String emailContent[] = new String[]{Subject, Content};
          try {
            newsletterForm.publicUserHandler.subscribe(WCMCoreUtils.getUserSessionProvider(),
                                                       portalName,
                                                       userEmail,
                                                       listCategorySubscription,
                                                       newsletterForm.linkToSendMail,
                                                       emailContent);
            newsletterForm.setListIds(listCategorySubscription);
            newsletterForm.inputEmail.setRendered(false);
            newsletterForm.userMail = userEmail;
            newsletterForm.isUpdated = true;
            newsletterForm.setActions(new String[] { "ForgetEmail", "ChangeSubcriptions" });
            contentOfMessage = "UINewsletterViewerForm.msg.subcribed";
          }catch(Exception ex){
            contentOfMessage = "UINewsletterViewerForm.msg.canNotSubcribed";
          }
        }
      } else {
        contentOfMessage = "UINewsletterViewerForm.msg.alreadyExistedEmail";
      }

      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      UIApplication uiApp = context.getUIApplication();
      uiApp.addMessage(new ApplicationMessage(contentOfMessage, null, ApplicationMessage.INFO));
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterForm);
    }
  }
}
