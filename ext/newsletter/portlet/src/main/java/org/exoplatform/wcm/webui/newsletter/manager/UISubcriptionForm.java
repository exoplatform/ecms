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
package org.exoplatform.wcm.webui.newsletter.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.NoneHTMLValidator;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.wcm.webui.selector.UIUserMemberSelector;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.NameValidator;
import org.exoplatform.webui.form.validator.SpecialCharacterValidator;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Jun 4, 2009
 */
@ComponentConfig(
lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
    @EventConfig(listeners = UISubcriptionForm.SaveActionListener.class),
    @EventConfig(listeners = UISubcriptionForm.CancelActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UISubcriptionForm.DeleteModeratorActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UISubcriptionForm.SelectUserActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UISubcriptionForm.SelectMemberActionListener.class, phase = Phase.DECODE) }
)
public class UISubcriptionForm extends UIForm implements UIPopupComponent, UISelectable{

  /** The Constant INPUT_SUBCRIPTION_NAME. */
  private static final String          INPUT_SUBCRIPTION_NAME        = "SubcriptionName";

  /** The Constant INPUT_SUBCRIPTION_DESCRIPTION. */
  private static final String          INPUT_SUBCRIPTION_DESCRIPTION = "SubcriptionDescription";

  /** The Constant INPUT_SUBCRIPTION_TITLE. */
  private static final String          INPUT_SUBCRIPTION_TITLE       = "SubcriptionTitle";

  /** The Constant SELECT_CATEGORIES_NAME. */
  private static final String          SELECT_CATEGORIES_NAME        = "CategoryName";

  public static final String          SELECT_REDACTOR               = "UIWCMSubscriptionRedactor";
  public static final String          FORM_SUBSCRIPTION_REDACTOR    = "UIWCMFormSubscriptionRedactor";

  /** The category handler. */
  private NewsletterCategoryHandler    categoryHandler               = null;

  /** The subscription config. */
  private NewsletterSubscriptionConfig subscriptionConfig            = null;

  private String popupId;

  private boolean isRemove;

  public boolean isRemove() {
    return isRemove;
  }

  public void setRemove(boolean isRemove) {
    this.isRemove = isRemove;
  }

  /**
   * Instantiates a new uI subcription form.
   *
   * @throws Exception the exception
   */
  public UISubcriptionForm() throws Exception{

    setActions(new String[]{"Save", "Cancel"});
    List<NewsletterCategoryConfig> categories = getListCategories();
    List<SelectItemOption<String>> listCategoriesName = new ArrayList<SelectItemOption<String>>();
    SelectItemOption<String> option = null;
    for (NewsletterCategoryConfig category: categories) {
      option = new SelectItemOption<String>(category.getTitle(), category.getName());
      listCategoriesName.add(option);
    }

    UIFormStringInput inputSubcriptionName = new UIFormStringInput(INPUT_SUBCRIPTION_NAME, null);
    inputSubcriptionName.addValidator(MandatoryValidator.class).addValidator(NameValidator.class)
    .addValidator(SpecialCharacterValidator.class);

    UIFormStringInput inputSubcriptionTitle = new UIFormStringInput(INPUT_SUBCRIPTION_TITLE, null);
    inputSubcriptionTitle.addValidator(MandatoryValidator.class).addValidator(NoneHTMLValidator.class);

    UIFormStringInput inputModerator = new UIFormStringInput(SELECT_REDACTOR, SELECT_REDACTOR, null);
    inputModerator.setEditable(false);
    inputModerator.addValidator(MandatoryValidator.class);
    UIFormInputSetWithAction formSubscriptionDeractor = new UIFormInputSetWithAction(FORM_SUBSCRIPTION_REDACTOR);
    formSubscriptionDeractor.addChild(inputModerator);
    formSubscriptionDeractor.setActionInfo(SELECT_REDACTOR, new String[] { "SelectUser",
        "SelectMember", "DeleteModerator" });
    formSubscriptionDeractor.showActionInfo(true);
    UIFormTextAreaInput descriptionInput = new UIFormTextAreaInput(INPUT_SUBCRIPTION_DESCRIPTION, null, null);
    descriptionInput.addValidator(NoneHTMLValidator.class);

    addChild(new UIFormSelectBox(SELECT_CATEGORIES_NAME, SELECT_CATEGORIES_NAME, listCategoriesName));
    addChild(inputSubcriptionName);
    addChild(inputSubcriptionTitle);
    addChild(descriptionInput);
    addChild(formSubscriptionDeractor);
  }

  public void doSelect(String selectField, Object value) throws Exception {
    UIFormInputSetWithAction formCategoryModerator = getChildById(FORM_SUBSCRIPTION_REDACTOR);
    UIFormStringInput stringInput = formCategoryModerator.getChildById(selectField);
    List<String> values = new ArrayList<String>();
    String oldValue = stringInput.getValue();
    if(oldValue != null && oldValue.length() > 0)
      values.addAll(Arrays.asList(oldValue.split(",")));
    if(!values.contains((String)value)) values.add((String)value);
    StringBuffer sbResult = new StringBuffer();
    for (String str : values) {
      if (sbResult.toString().trim().length() > 0) {
        sbResult.append(",");
      }
      sbResult.append(str);
    }
    stringInput.setValue(sbResult.toString());
    Utils.closePopupWindow(this, popupId);
  }

  /**
   * Gets the popup id.
   *
   * @return the popup id
   */
  public String getPopupId() {
    return popupId;
  }

  /**
   * Sets the popup id.
   *
   * @param popupId the new popup id
   */
  public void setPopupId(String popupId) {
    this.popupId = popupId;
  }

  /**
   * Sets the subscription infor.
   *
   * @param subscriptionConfig the new subscription infor
   */
  public void setSubscriptionInfor(NewsletterSubscriptionConfig subscriptionConfig){
    if(subscriptionConfig == null) return;
    this.subscriptionConfig = subscriptionConfig;

    UIFormStringInput inputName = this.getChildById(INPUT_SUBCRIPTION_NAME);

    inputName.setValue(subscriptionConfig.getName());
    inputName.setEditable(false);

    ((UIFormStringInput)this.getChildById(INPUT_SUBCRIPTION_TITLE)).setValue(subscriptionConfig.getTitle());
    ((UIFormTextAreaInput)this.getChildById(INPUT_SUBCRIPTION_DESCRIPTION)).setValue(subscriptionConfig.getDescription());
    UIFormSelectBox formSelectBox = this.getChildById(SELECT_CATEGORIES_NAME);
    formSelectBox.setValue(subscriptionConfig.getCategoryName());
    formSelectBox.setDisabled(true);

    UIFormInputSetWithAction inputSetWithAction = getChildById(FORM_SUBSCRIPTION_REDACTOR);
    UIFormStringInput inputModerator = inputSetWithAction.getChildById(SELECT_REDACTOR);
    inputModerator.setValue(subscriptionConfig.getRedactor());
  }

  /**
   * Gets the list categories.
   *
   * @return the list categories
   */
  private List<NewsletterCategoryConfig> getListCategories(){
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    categoryHandler = newsletterManagerService.getCategoryHandler();
    try{
      return categoryHandler.getListCategories(NewsLetterUtil.getPortalName(), Utils.getSessionProvider());
    }catch(Exception e){
      return new ArrayList<NewsletterCategoryConfig>();
    }
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  static  public class SaveActionListener extends EventListener<UISubcriptionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubcriptionForm> event) throws Exception {

      UISubcriptionForm uiSubcriptionForm = event.getSource();

      UINewsletterManagerPortlet newsletterPortlet = uiSubcriptionForm.getAncestorOfType(UINewsletterManagerPortlet.class);
      NewsletterManagerService newsletterManagerService = (NewsletterManagerService)
          newsletterPortlet.getApplicationComponent(NewsletterManagerService.class);

      UIFormSelectBox formSelectBox = uiSubcriptionForm.getChildById(SELECT_CATEGORIES_NAME);
      String categoryName = formSelectBox.getSelectedValues()[0].toString();
      String subcriptionTitle = ((UIFormStringInput)uiSubcriptionForm.getChildById(INPUT_SUBCRIPTION_TITLE)).getValue();
      String subcriptionName = uiSubcriptionForm.getUIStringInput(INPUT_SUBCRIPTION_NAME).getValue();
      String subcriptionDecription = ((UIFormTextAreaInput) uiSubcriptionForm.
          getChildById(INPUT_SUBCRIPTION_DESCRIPTION)).getValue();

      UIApplication uiApp = uiSubcriptionForm.getAncestorOfType(UIApplication.class);

      NewsletterSubscriptionHandler subscriptionHandler = newsletterManagerService.getSubscriptionHandler();
      subscriptionHandler.setRemove(uiSubcriptionForm.isRemove());
      NewsletterSubscriptionConfig newsletterSubscriptionConfig = null;
      SessionProvider sessionProvider = Utils.getSessionProvider();
      UIFormInputSetWithAction inputSetWithAction = uiSubcriptionForm.getChildById(FORM_SUBSCRIPTION_REDACTOR);
      UIFormStringInput inputModerator = inputSetWithAction.getChildById(SELECT_REDACTOR);

      String inputRedactorValue = inputModerator.getValue();
      if (("".equals(inputRedactorValue)) || (inputRedactorValue == null)) {
        uiApp.addMessage(new ApplicationMessage("UISubcriptionForm.msg.selectRedactorPermission",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }

      // Update Redactors into access permissions of newsletter manager page
      NewsletterConstant.updateAccessPermission(inputRedactorValue.split(","));

      if(uiSubcriptionForm.subscriptionConfig == null) {
        newsletterSubscriptionConfig = subscriptionHandler.getSubscriptionsByName(sessionProvider,
                                                                                  NewsLetterUtil.getPortalName(),
                                                                                  categoryName,
                                                                                  subcriptionName);
        if (newsletterSubscriptionConfig != null) {

          uiApp.addMessage(new ApplicationMessage("UISubcriptionForm.msg.subcriptionNameIsAlreadyExist",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }

        newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();

        newsletterSubscriptionConfig.setName(subcriptionName);
        newsletterSubscriptionConfig.setCategoryName(categoryName);
        newsletterSubscriptionConfig.setDescription(subcriptionDecription);
        newsletterSubscriptionConfig.setTitle(subcriptionTitle);

        newsletterSubscriptionConfig.setRedactor(inputRedactorValue);
        subscriptionHandler.add(sessionProvider, NewsLetterUtil.getPortalName(), newsletterSubscriptionConfig);
      } else {
        newsletterSubscriptionConfig = uiSubcriptionForm.subscriptionConfig;
        newsletterSubscriptionConfig.setCategoryName(categoryName);
        newsletterSubscriptionConfig.setDescription(subcriptionDecription);
        newsletterSubscriptionConfig.setTitle(subcriptionTitle);
        newsletterSubscriptionConfig.setRedactor(inputRedactorValue);
        subscriptionHandler.edit(sessionProvider, NewsLetterUtil.getPortalName(), newsletterSubscriptionConfig);
      }
      UISubscriptions uiSubscriptions = uiSubcriptionForm.getAncestorOfType(UINewsletterManagerPortlet.class)
                                                         .getChild(UISubscriptions.class);
      if (uiSubscriptions != null) {
        if (uiSubscriptions.categoryConfig != null) {
          uiSubscriptions.init();
        }
      }
      Utils.closePopupWindow(uiSubcriptionForm, UINewsletterConstant.SUBSCRIPTION_FORM_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CancelActionEvent
   */
  static  public class CancelActionListener extends EventListener<UISubcriptionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubcriptionForm> event) throws Exception {
      UISubcriptionForm uiSubcriptionForm = event.getSource();
      if (uiSubcriptionForm.getSubmitAction().equals("Cancel")) {
        uiSubcriptionForm.getChildren().clear();
      }
      uiSubcriptionForm.setRemove(false);
      Utils.closePopupWindow(uiSubcriptionForm, UINewsletterConstant.SUBSCRIPTION_FORM_POPUP_WINDOW);
    }
  }

public static class SelectUserActionListener extends EventListener<UISubcriptionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubcriptionForm> event) throws Exception {
      UISubcriptionForm uiSubcriptionForm = event.getSource();
      uiSubcriptionForm.setRemove(false);
      UIUserMemberSelector userMemberSelector = uiSubcriptionForm.createUIComponent(UIUserMemberSelector.class, null, null);
      userMemberSelector.setMulti(false);
      userMemberSelector.setShowSearch(true);
      userMemberSelector.setSourceComponent(uiSubcriptionForm, new String[] {SELECT_REDACTOR});
      userMemberSelector.init();
      Utils.createPopupWindow(uiSubcriptionForm, userMemberSelector, UINewsletterConstant.USER_SELECTOR_POPUP_WINDOW, 750);
      uiSubcriptionForm.setPopupId(UINewsletterConstant.USER_SELECTOR_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving selectMemberAction events.
   * The class that is interested in processing a selectMemberAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectMemberActionListener<code> method. When
   * the selectMemberAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectMemberActionEvent
   */
  public static class SelectMemberActionListener extends EventListener<UISubcriptionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubcriptionForm> event) throws Exception {
      UISubcriptionForm uiSubcriptionForm = event.getSource();
      uiSubcriptionForm.setRemove(false);
      UIGroupMemberSelector groupMemberSelector = uiSubcriptionForm.createUIComponent(UIGroupMemberSelector.class, null, null);
      groupMemberSelector.setShowAnyPermission(false);
      groupMemberSelector.setSourceComponent(uiSubcriptionForm, new String[] {SELECT_REDACTOR});
      Utils.createPopupWindow(uiSubcriptionForm, groupMemberSelector, UINewsletterConstant.GROUP_SELECTOR_POPUP_WINDOW, 540);
      uiSubcriptionForm.setPopupId(UINewsletterConstant.GROUP_SELECTOR_POPUP_WINDOW);
    }
  }

  public static class DeleteModeratorActionListener extends EventListener<UISubcriptionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubcriptionForm> event) throws Exception {
      UISubcriptionForm subscriptionForm = event.getSource();
      subscriptionForm.setRemove(true);
      UIFormInputSetWithAction formCategoryModerator = subscriptionForm.getChildById(FORM_SUBSCRIPTION_REDACTOR);
      UIFormStringInput stringInput = formCategoryModerator.getChildById(SELECT_REDACTOR);
      if(stringInput.getValue() == null || stringInput.getValue().trim().length() < 1) {
        UIApplication uiApp = subscriptionForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UICategoryForm.msg.doNotHaveModerators", null, ApplicationMessage.WARNING));
        
        return;
      };
      UIRemoveModerators removeModerators = subscriptionForm.createUIComponent(UIRemoveModerators.class, null, null);
      removeModerators.permissionForSubscriptionForm();
      removeModerators.init(((UIFormStringInput) ((UIFormInputSetWithAction) subscriptionForm.
          getChildById(FORM_SUBSCRIPTION_REDACTOR)).getChildById(SELECT_REDACTOR)).getValue());
      Utils.createPopupWindow(subscriptionForm,
                              removeModerators,
                              UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW,
                              480);
      subscriptionForm.setPopupId(UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW);
    }
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {
  }
}
