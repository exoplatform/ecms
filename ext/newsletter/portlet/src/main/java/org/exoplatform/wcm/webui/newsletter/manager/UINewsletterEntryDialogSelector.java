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
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterTemplateHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 12, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/newsletter/NewsletterManager/UINewsletterEntryDialogSelector.gtmpl",
                 events = {
    @EventConfig(listeners = UINewsletterEntryDialogSelector.UpdateNewsletterActionListener.class),
    @EventConfig(listeners = UINewsletterEntryDialogSelector.OpenWebcontentSelectorFormActionListener.class),
    @EventConfig(listeners = UINewsletterEntryDialogSelector.ChangeTemplateActionListener.class),
    @EventConfig(listeners = UINewsletterEntryDialogSelector.ChangeCategoryActionListener.class) })
public class UINewsletterEntryDialogSelector extends UIForm {

  /** The Constant NEWSLETTER_ENTRY_TEMPLATE. */
  public static final String NEWSLETTER_ENTRY_TEMPLATE = "UINewsletterEntryTemplate";

  /** The Constant NEWSLETTER_ENTRY_SEND_DATE. */
  public static final String NEWSLETTER_ENTRY_SEND_DATE = "UINewsletterEntrySendDate";

  /** The dialog. */
  private String dialog = "dialog1";

  /**
   * Gets the dialog.
   *
   * @return the dialog
   */
  public String getDialog() {
    return dialog;
  }

  /**
   * Sets the dialog.
   *
   * @param dialog the new dialog
   */
  public void setDialog(String dialog) {
    this.dialog = dialog;
  }

  /**
   * Instantiates a new uI newsletter entry dialog selector.
   *
   * @throws Exception the exception
   */
  public UINewsletterEntryDialogSelector() throws Exception {
    this.setActions(new String[]{"UpdateNewsletter"});
    UIFormSelectBox newsletterEntryTemplate = new UIFormSelectBox(NEWSLETTER_ENTRY_TEMPLATE,
                                                                  NEWSLETTER_ENTRY_TEMPLATE,
                                                                  new ArrayList<SelectItemOption<String>>());
    newsletterEntryTemplate.setOnChange("ChangeTemplate");
    addChild(newsletterEntryTemplate);
    addUIFormInput(new UIFormDateTimeInput(NEWSLETTER_ENTRY_SEND_DATE, NEWSLETTER_ENTRY_SEND_DATE, null, true));
  }

  /**
   * Inits the.
   *
   * @param categoryName the category name
   * @param subScriptionName the sub scription name
   *
   * @throws Exception the exception
   */
  public void init(String categoryName, String subScriptionName) throws Exception{
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    NewsletterCategoryHandler newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    String portalName = NewsLetterUtil.getPortalName();
    List<NewsletterCategoryConfig> newsletterCategoryConfigs = newsletterCategoryHandler.getListCategories(portalName,
                                                                                                           sessionProvider);
    List<SelectItemOption<String>> categories = new ArrayList<SelectItemOption<String>>();
    for (NewsletterCategoryConfig newsletterCategoryConfig : newsletterCategoryConfigs) {
      categories.add(new SelectItemOption<String>(newsletterCategoryConfig.getTitle(), newsletterCategoryConfig.getName()));
    }
    UIFormSelectBox categorySelectBox = new UIFormSelectBox(UINewsletterConstant.ENTRY_CATEGORY_SELECTBOX,
                                                            UINewsletterConstant.ENTRY_CATEGORY_SELECTBOX, categories);
    categorySelectBox.setOnChange("ChangeCategory");
    if(categoryName != null && categoryName.trim().length() > 0){
      categorySelectBox.setValue(categoryName);
      categorySelectBox.setDisabled(true);
    }

    if(categoryName == null && categories.size() > 0) categoryName = categories.get(0).getValue();
    NewsletterSubscriptionHandler newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    List<NewsletterSubscriptionConfig> listSubscriptions = newsletterSubscriptionHandler.
        getSubscriptionsByCategory(sessionProvider, portalName, categoryName);
    List<SelectItemOption<String>> subscriptions = new ArrayList<SelectItemOption<String>>();
    for (NewsletterSubscriptionConfig newsletterSubscriptionConfig : listSubscriptions) {
      subscriptions.add(new SelectItemOption<String>(newsletterSubscriptionConfig.getTitle(),
                                                     newsletterSubscriptionConfig.getName()));
    }
    UIFormSelectBox subscriptionSelectBox = new UIFormSelectBox(UINewsletterConstant.ENTRY_SUBSCRIPTION_SELECTBOX,
                                                                UINewsletterConstant.ENTRY_SUBSCRIPTION_SELECTBOX,
                                                                subscriptions);
    if(subScriptionName != null && subScriptionName.trim().length() > 0){
      subscriptionSelectBox.setValue(subScriptionName);
      subscriptionSelectBox.setDisabled(true);
    }

    addChild(categorySelectBox);
    addChild(subscriptionSelectBox);
    NewsletterCategoryConfig categoryConfig = getAncestorOfType(UINewsletterEntryContainer.class).getCategoryConfig();
    if(categoryConfig == null && newsletterCategoryConfigs.size() > 0)
      categoryConfig = newsletterCategoryConfigs.get(0);
    updateTemplateSelectBox(categoryConfig);
  }

  /**
   * Update template select box.
   *
   * @param categoryConfig the category config
   *
   * @throws Exception the exception
   */
  public void updateTemplateSelectBox(NewsletterCategoryConfig categoryConfig) throws Exception {
    List<SelectItemOption<String>> templates = new ArrayList<SelectItemOption<String>>();
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    NewsletterTemplateHandler newsletterTemplateHandler = newsletterManagerService.getTemplateHandler();
    List<Node> templateNodes = newsletterTemplateHandler.getTemplates(WCMCoreUtils.getUserSessionProvider(),
                                                                      NewsLetterUtil.getPortalName(),
                                                                      categoryConfig);
    for (Node template : templateNodes) {
      templates.add(new SelectItemOption<String>(template.getProperty("exo:title").getString(), template.getName()));
    }
    getUIFormSelectBox(NEWSLETTER_ENTRY_TEMPLATE).setOptions(templates);
  }

  /**
   * The listener interface for receiving changeTemplateAction events.
   * The class that is interested in processing a changeTemplateAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeTemplateActionListener<code> method. When
   * the changeTemplateAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeTemplateActionEvent
   */
  public static class ChangeTemplateActionListener extends EventListener<UINewsletterEntryDialogSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryDialogSelector> event) throws Exception {
      UINewsletterEntryDialogSelector newsletterEntryDialogSelector = event.getSource();
      UIFormSelectBox newsletterEntryTemplate = newsletterEntryDialogSelector.getChildById(NEWSLETTER_ENTRY_TEMPLATE);
      String templateName = newsletterEntryTemplate.getValue();
      newsletterEntryDialogSelector.setDialog(templateName) ;
      NewsletterManagerService newsletterManagerService = newsletterEntryDialogSelector.
          getApplicationComponent(NewsletterManagerService.class);
      NewsletterTemplateHandler newsletterTemplateHandler = newsletterManagerService.getTemplateHandler();
      UINewsletterEntryContainer newsletterEntryContainer = newsletterEntryDialogSelector.
          getAncestorOfType(UINewsletterEntryContainer.class);
      UINewsletterEntryForm newsletterEntryForm = newsletterEntryContainer.getChild(UINewsletterEntryForm.class);
      newsletterEntryForm.setNodePath(newsletterTemplateHandler.getTemplate(WCMCoreUtils.getUserSessionProvider(),
                                                                            NewsLetterUtil.getPortalName(),
                                                                            newsletterEntryContainer.getCategoryConfig(),
                                                                            templateName)
                                                               .getPath());
      newsletterEntryForm.getChildren().clear();
      newsletterEntryForm.resetProperties();
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterEntryContainer) ;
      newsletterEntryTemplate.setValue(templateName);
    }
  }

  /**
   * The listener interface for receiving openWebcontentSelectorFormAction events.
   * The class that is interested in processing a openWebcontentSelectorFormAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addOpenWebcontentSelectorFormActionListener<code> method. When
   * the openWebcontentSelectorFormAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see OpenWebcontentSelectorFormActionEvent
   */
  public static class OpenWebcontentSelectorFormActionListener extends EventListener<UINewsletterEntryDialogSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryDialogSelector> event) throws Exception {
      UINewsletterEntryDialogSelector newsletterEntryDialogSelector = event.getSource();
      UINewsletterEntryWebcontentSelectorForm newsletterEntryWebcontentSelector = newsletterEntryDialogSelector.
          createUIComponent(UINewsletterEntryWebcontentSelectorForm.class, null, null);
      Utils.createPopupWindow(newsletterEntryDialogSelector,
                              newsletterEntryWebcontentSelector,
                              UINewsletterConstant.WEBCONTENT_SELECTOR_FORM_POPUP_WINDOW,
                              300);
    }
  }

  /**
   * The listener interface for receiving changeCategoryAction events.
   * The class that is interested in processing a changeCategoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeCategoryActionListener<code> method. When
   * the changeCategoryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeCategoryActionEvent
   */
  public static class ChangeCategoryActionListener extends EventListener<UINewsletterEntryDialogSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryDialogSelector> event) throws Exception {
      UINewsletterEntryDialogSelector newsletterEntryDialogSelector = event.getSource();

      UINewsletterEntryContainer newsletterEntryContainer = newsletterEntryDialogSelector.
          getAncestorOfType(UINewsletterEntryContainer.class);

      UIFormSelectBox categorySelectBox = newsletterEntryDialogSelector.
          getChildById(UINewsletterConstant.ENTRY_CATEGORY_SELECTBOX);
      List<SelectItemOption<String>> subscriptions = new ArrayList<SelectItemOption<String>>();
      NewsletterManagerService newsletterManagerService = newsletterEntryDialogSelector.
          getApplicationComponent(NewsletterManagerService.class);
      NewsletterSubscriptionHandler newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
      String portalName = NewsLetterUtil.getPortalName();
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      NewsletterCategoryConfig categoryConfig = newsletterManagerService.getCategoryHandler()
                                                                        .getCategoryByName(sessionProvider,
                                                                                           portalName,
                                                                                           categorySelectBox.getValue());
      List<NewsletterSubscriptionConfig> newsletterSubscriptionConfigs =
          newsletterSubscriptionHandler.getSubscriptionsByCategory(sessionProvider, portalName, categorySelectBox.getValue());
      for (NewsletterSubscriptionConfig newsletterSubscriptionConfig : newsletterSubscriptionConfigs) {
        subscriptions.add(new SelectItemOption<String>(newsletterSubscriptionConfig.getTitle(),
                                                       newsletterSubscriptionConfig.getName()));
      }

      UIFormSelectBox subscriptionSelectBox = newsletterEntryDialogSelector.
          getChildById(UINewsletterConstant.ENTRY_SUBSCRIPTION_SELECTBOX);
      subscriptionSelectBox.setOptions(subscriptions);
      newsletterEntryDialogSelector.updateTemplateSelectBox(categoryConfig);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterEntryContainer);
    }
  }

  /**
   * The listener interface for receiving updateNewsletterAction events.
   * The class that is interested in processing a updateNewsletterAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addUpdateNewsletterActionListener<code> method. When
   * the updateNewsletterAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see UpdateNewsletterActionEvent
   */
  public static class UpdateNewsletterActionListener extends EventListener<UINewsletterEntryDialogSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryDialogSelector> event) throws Exception {
      UINewsletterEntryDialogSelector newsletterEntryDialogSelector = event.getSource();
      UIFormSelectBox newsletterEntryTemplate = newsletterEntryDialogSelector.getChildById(NEWSLETTER_ENTRY_TEMPLATE);
      String templateName = newsletterEntryTemplate.getValue();
      UIFormDateTimeInput formDateTimeInput = newsletterEntryDialogSelector.getChild(UIFormDateTimeInput.class);
      Calendar calendar = formDateTimeInput.getCalendar();

      UIFormSelectBox categorySelectBox = newsletterEntryDialogSelector.getChildById("UINewsletterEntryCategorySelectBox");
      UIFormSelectBox subcriptionSelectBox = newsletterEntryDialogSelector.
          getChildById("UINewsletterEntrySubscriptionSelectBox");

      UINewsletterEntryContainer entryContainer = newsletterEntryDialogSelector.
          getAncestorOfType(UINewsletterEntryContainer.class);
      UIApplication uiApp = entryContainer.getAncestorOfType(UIApplication.class);
      if (calendar == null || formDateTimeInput.getValue().trim().length() < 1) {
        if (formDateTimeInput.getValue() == null
            || formDateTimeInput.getValue().trim().length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.DateTimeIsNotNull",
                                                  null,
                                                  ApplicationMessage.WARNING));
        } else {
          uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.DateTimeIsInvalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
        }
        
        return;
      }
      if (categorySelectBox.getValue() == null || subcriptionSelectBox.getValue() == null
          || categorySelectBox.getValue().length() == 0
          || subcriptionSelectBox.getValue().length() == 0) {
        Utils.createPopupMessage(newsletterEntryDialogSelector,
                                 "UINewsletterEntryDialogSelector.msg.subcriptionIsNotEmpty",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      }

      entryContainer.setUpdated(true);
      formDateTimeInput.setCalendar(calendar);
      uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.UpdateInformationSuccessful",
                                              null,
                                              ApplicationMessage.INFO));
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterEntryDialogSelector) ;
      newsletterEntryTemplate.setValue(templateName);
    }
  }
}
