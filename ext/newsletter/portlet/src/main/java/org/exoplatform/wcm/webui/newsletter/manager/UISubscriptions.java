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

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterManageUserHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * ha.mai@exoplatform.com
 * Jun 5, 2009
 */

@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/newsletter/NewsletterManager/UISubscriptions.gtmpl",
                 events = {
                   @EventConfig(listeners = UISubscriptions.AddEntryActionListener.class),
                   @EventConfig(listeners = UISubscriptions.BackToCategoriesActionListener.class),
                   @EventConfig(listeners = UISubscriptions.AddSubcriptionActionListener.class),
                   @EventConfig(listeners = UISubscriptions.EditSubscriptionActionListener.class),
                   @EventConfig(listeners = UISubscriptions.DeleteSubscriptionActionListener.class,
                                confirm = "UISubscription.msg.confirmDeleteSubscription"),
                   @EventConfig(listeners = UISubscriptions.DeleteCategoryActionListener.class,
                                confirm = "UISubscription.msg.confirmDeleteCategory"),
                   @EventConfig(listeners = UISubscriptions.OpenSubscriptionActionListener.class),
                   @EventConfig(listeners = UISubscriptions.EditCategoryActionListener.class),
                   @EventConfig(listeners = UISubscriptions.ManagerUsersActionListener.class),
                   @EventConfig(listeners = UISubscriptions.SelectSubscriptionActionListener.class),
                   @EventConfig(listeners = UISubscriptions.SelectSubscriptionCheckboxActionListener.class),
                   @EventConfig(listeners = UISubscriptions.SelectSubscriptionCheckboxHeaderActionListener.class)
                 }
               )
public class UISubscriptions extends UIForm {
  /** The subscription handler. */
  NewsletterSubscriptionHandler      subscriptionHandler;

  /** The category handler. */
  NewsletterCategoryHandler          categoryHandler;

  /** The category config. */
  NewsletterCategoryConfig           categoryConfig;

  /** The user handler. */
  NewsletterManageUserHandler        userHandler                      = null;

  String                             selectSubscriptionCheckboxHeader = "SelectSubscriptionCheckboxHeader";

  String                             selectSubscriptionCheckbox       = "SelectSubscriptionCheckbox";

  String                             uiSubscriptionsCheckAll          = "UISubscriptionsCheckAll";

  List<NewsletterSubscriptionConfig> listSubs                         = null;

  /** The portal name. */
  String                             portalName;

  String                             userId;

  /**
   * Instantiates a new uI subscriptions.
   *
   * @throws Exception the exception
   */
  public UISubscriptions()throws Exception{
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    subscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    categoryHandler = newsletterManagerService.getCategoryHandler();
    userHandler = newsletterManagerService.getManageUserHandler();
    portalName = NewsLetterUtil.getPortalName();
    userId = NewsLetterUtil.getCurrentUser();
  }

  /**
   * Sets the category.
   *
   * @param categoryConfig the new category
   */
  public void setCategory(NewsletterCategoryConfig categoryConfig){
    this.categoryConfig = categoryConfig;
  }

  /**
   * Inits the.
   *
   * @param listSubScritpions the list subscriptions
   */
  public void init() {
    this.getChildren().clear();

    updateListSubscription();
    UIFormCheckBoxInput<Boolean> checkboxHeader = new UIFormCheckBoxInput<Boolean>(uiSubscriptionsCheckAll,
                                                                                   uiSubscriptionsCheckAll,
                                                                                   false);
    checkboxHeader.setOnChange(selectSubscriptionCheckboxHeader);
    this.addChild(checkboxHeader);

    UIFormCheckBoxInput<Boolean> checkBoxInput = null;
    for(NewsletterSubscriptionConfig subscription : listSubs){
      checkBoxInput = new UIFormCheckBoxInput<Boolean>(subscription.getName(), subscription.getName(), false);
      checkBoxInput.setOnChange(selectSubscriptionCheckbox);
      this.addChild(checkBoxInput);
    }
  }

  /**
   * Gets the number of user.
   *
   * @param subscriptionName the subscription name
   *
   * @return the number of user
   */
  public int getNumberOfUser(String subscriptionName){
    return userHandler.getQuantityUserBySubscription(WCMCoreUtils.getUserSessionProvider(),
                                                     portalName,
                                                     this.categoryConfig.getName(),
                                                     subscriptionName);
  }

  /**
   * Gets the list subscription.
   *
   * @return the list subscription
   */
  public List<NewsletterSubscriptionConfig> getListSubscription(){
    return listSubs;
  }

  /**
   * Gets the list subscription.
   *
   * @return the list subscription
   */
  public void updateListSubscription() {
    listSubs = new ArrayList<NewsletterSubscriptionConfig>();
    try {
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      if (userHandler.isAdministrator(portalName, userId)
          || userHandler.isModerator(userId, categoryConfig)) {
        listSubs = subscriptionHandler.getSubscriptionsByCategory(sessionProvider,
                                                                  portalName,
                                                                  categoryConfig.getName());
      } else {
        listSubs = subscriptionHandler.getSubscriptionByRedactor(portalName,
                                                                 categoryConfig.getName(),
                                                                 userId,
                                                                 sessionProvider);
      }
    } catch (Exception e) {
      Utils.createPopupMessage(this,
                               "UISubscription.msg.get-list-subscriptions",
                               null,
                               ApplicationMessage.ERROR);
    }
  }

  /**
   * Gets the number of waiting newsletter.
   *
   * @param subscriptionName the subscription name
   *
   * @return the number of waiting newsletter
   */
  public long getNumberOfWaitingNewsletter(String subscriptionName){
    try{
      return subscriptionHandler.getNumberOfNewslettersWaiting(WCMCoreUtils.getUserSessionProvider(),
                                                               portalName,
                                                               this.categoryConfig.getName(),
                                                               subscriptionName);
    }catch(Exception ex){
      return 0;
    }
  }

  /**
   * Gets the checked.
   *
   * @return the checked
   */
  @SuppressWarnings("unchecked")
  public String getChecked(){
    String subscriptionId = null;
    UIFormCheckBoxInput<Boolean> checkbox = null;
    for(UIComponent component : this.getChildren()){
      try{
        checkbox = (UIFormCheckBoxInput<Boolean>)component;
        if(checkbox.getId().equals(uiSubscriptionsCheckAll)) {
          continue;
        }
        if(checkbox.isChecked()){
          if(subscriptionId == null)subscriptionId = checkbox.getName();
          else return null;
        }
      }catch(Exception e){
        // You shouldn't throw popup message, because some exception often rise here.
      }
    }
    return subscriptionId;
  }

  public boolean isAdministrator() {
    return userHandler.isAdministrator(portalName, userId);
  }

  public boolean isModerator() {
    return userHandler.isModerator(userId, categoryConfig);
  }

  /**
   * The listener interface for receiving backToCategoriesAction events.
   * The class that is interested in processing a backToCategoriesAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addBackToCategoriesActionListener<code> method. When
   * the backToCategoriesAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see BackToCategoriesActionEvent
   */
  static  public class BackToCategoriesActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      UINewsletterManagerPortlet newsletterManagerPortlet = subsriptions.getAncestorOfType(UINewsletterManagerPortlet.class);
      UICategories categories = newsletterManagerPortlet.getChild(UICategories.class);
      subsriptions.setCategory(null);
      categories.setRendered(true);
      subsriptions.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving editCategoryAction events.
   * The class that is interested in processing a editCategoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addEditCategoryActionListener<code> method. When
   * the editCategoryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see EditCategoryActionEvent
   */
  static  public class EditCategoryActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      UICategoryForm categoryForm = subsriptions.createUIComponent(UICategoryForm.class, null, null);
      categoryForm.setCategoryInfo(subsriptions.categoryConfig);
      Utils.createPopupWindow(subsriptions, categoryForm, UINewsletterConstant.CATEGORY_FORM_POPUP_WINDOW, 480);
    }
  }

  /**
   * The listener interface for receiving deleteCategoryAction events.
   * The class that is interested in processing a deleteCategoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteCategoryActionListener<code> method. When
   * the deleteCategoryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DeleteCategoryActionEvent
   */
  static  public class DeleteCategoryActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
  public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      NewsletterManagerService newsletterManagerService = subsriptions.getApplicationComponent(NewsletterManagerService.class);
      NewsletterCategoryHandler categoryHandler = newsletterManagerService.getCategoryHandler();
      categoryHandler.delete(WCMCoreUtils.getUserSessionProvider(),
                             NewsLetterUtil.getPortalName(),
                             subsriptions.categoryConfig.getName());
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class) ;
      WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
      DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
      Page page = dataStorage.getPage(Util.getUIPortal().getSelectedUserNode().getPageRef());
      List<String> pageAccessPermissions = new ArrayList<String>(Arrays.asList(page.getAccessPermissions()));
      
      String membership = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.NEWSLETTER_MANAGE_MEMBERSHIP);
      Group group = organizationService.getGroupHandler().findGroupById(membership.split(":")[1]);
      MembershipHandler membershipHandler = organizationService.getMembershipHandler();
      MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.split(":")[0]);
      for(String userName : subsriptions.categoryConfig.getModerator().split(","))
      {
        if(pageAccessPermissions.contains(userName))
        {
          //Remove a membership record with  - a relation of the user ,group and membership type
          pageAccessPermissions.remove(userName);  
          Membership member = membershipHandler.findMembershipByUserGroupAndType(userName,group.getId(),membershipType.getName());
          if(member!=null)  membershipHandler.removeMembership(member.getId(), true);
        }
      }    
      UINewsletterManagerPortlet newsletterManagerPortlet = subsriptions.getAncestorOfType(UINewsletterManagerPortlet.class);
      UICategories categories = newsletterManagerPortlet.getChild(UICategories.class);
      subsriptions.setCategory(null);
      categories.setRendered(true);
      subsriptions.setRendered(false);
      page.setAccessPermissions(pageAccessPermissions.toArray(new String[]{}));
      dataStorage.save(page);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving addSubcriptionAction events.
   * The class that is interested in processing a addSubcriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddSubcriptionActionListener<code> method. When
   * the addSubcriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddSubcriptionActionEvent
   */
  static  public class AddSubcriptionActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      UISubcriptionForm subcriptionForm = subsriptions.createUIComponent(UISubcriptionForm.class, null, null);
      UIFormSelectBox selectedCategoryName = subcriptionForm.getChildById("CategoryName");
      selectedCategoryName.setValue(subsriptions.categoryConfig.getName());
      selectedCategoryName.setDisabled(true);
      Utils.createPopupWindow(subsriptions, subcriptionForm, UINewsletterConstant.SUBSCRIPTION_FORM_POPUP_WINDOW, 500);
      subcriptionForm.setSubscriptionInfor(null);
    }
  }

  /**
   * The listener interface for receiving editSubscriptionAction events.
   * The class that is interested in processing a editSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addEditSubscriptionActionListener<code> method. When
   * the editSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see EditSubscriptionActionEvent
   */
  static  public class EditSubscriptionActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings("deprecation")
  public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      String subId = subsriptions.getChecked();
      if (subId == null) {
        UIApplication uiApp = subsriptions.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISubscription.msg.checkOnlyOneSubScriptionToEdit",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      UISubcriptionForm subcriptionForm = subsriptions.createUIComponent(UISubcriptionForm.class, null, null);
      NewsletterSubscriptionConfig subscriptionConfig = subsriptions.subscriptionHandler.
                                                                            getSubscriptionsByName(Utils.getSessionProvider(),
                                                                                NewsLetterUtil.getPortalName(),
                                                                                subsriptions.categoryConfig.getName(),
                                                                                subId);
      subcriptionForm.setSubscriptionInfor(subscriptionConfig);
      Utils.createPopupWindow(subsriptions,
                              subcriptionForm,
                              UINewsletterConstant.SUBSCRIPTION_FORM_POPUP_WINDOW,
                              500);
    }
  }

  /**
   * The listener interface for receiving deleteSubscriptionAction events.
   * The class that is interested in processing a deleteSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteSubscriptionActionListener<code> method. When
   * the deleteSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DeleteSubscriptionActionEvent
   */
  static  public class DeleteSubscriptionActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions subsriptions = event.getSource();
      UIFormCheckBoxInput<Boolean> checkbox = null;
      String portalName = NewsLetterUtil.getPortalName();
      for(UIComponent component : subsriptions.getChildren()){
        if(component.getName().equals(subsriptions.uiSubscriptionsCheckAll)) {
          continue;
        }
        checkbox = (UIFormCheckBoxInput<Boolean>)component;
        if (checkbox.isChecked() == true) {
          SessionProvider sessionProvider = Utils.getSessionProvider();
          NewsletterSubscriptionConfig subscriptionConfig = subsriptions.subscriptionHandler.
              getSubscriptionsByName(sessionProvider, portalName, subsriptions.categoryConfig.getName(), checkbox.getName());
          if (subscriptionConfig != null) {
            subsriptions.subscriptionHandler.delete(sessionProvider,
                                                    NewsLetterUtil.getPortalName(),
                                                    subsriptions.categoryConfig.getName(),
                                                    subscriptionConfig);
            OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class) ;
            WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
            DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
            Page page = dataStorage.getPage(Util.getUIPortal().getSelectedUserNode().getPageRef());
            List<String> pageAccessPermissions = new ArrayList<String>(Arrays.asList(page.getAccessPermissions()));
            
            String membership = 
              wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.NEWSLETTER_MANAGE_MEMBERSHIP);
            Group group = organizationService.getGroupHandler().findGroupById(membership.split(":")[1]);
            MembershipHandler membershipHandler = organizationService.getMembershipHandler();
            MembershipType membershipType = 
              organizationService.getMembershipTypeHandler().findMembershipType(membership.split(":")[0]);
            for(String userName : subscriptionConfig.getRedactor().split(","))
            {
              if(pageAccessPermissions.contains(userName))
              {
                pageAccessPermissions.remove(userName);
                //Remove a membership record with  - a relation of the user ,group and membership type
                Membership member = 
                  membershipHandler.findMembershipByUserGroupAndType(userName,group.getId(),membershipType.getName());
                if(member!=null) membershipHandler.removeMembership(member.getId(), true);
              }
            }   
            page.setAccessPermissions(pageAccessPermissions.toArray(new String[]{}));
            dataStorage.save(page);
          } else {
            UIApplication uiApp = subsriptions.getAncestorOfType(UIApplication.class);
            uiApp.addMessage(new ApplicationMessage("UISubscription.msg.subscriptionNotfound",
                                                    null,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
        }
      }
      subsriptions.init();
    }
  }

  /**
   * The listener interface for receiving openSubscriptionAction events.
   * The class that is interested in processing a openSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addOpenSubscriptionActionListener<code> method. When
   * the openSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see OpenSubscriptionActionEvent
   */
  static  public class OpenSubscriptionActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
  public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions uiSubscription = event.getSource();
      String subId = uiSubscription.getChecked();
      if(subId == null){
        UIApplication uiApp = uiSubscription.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISubscription.msg.checkOnlyOneSubScriptionToOpen",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }

      UINewsletterManagerPortlet newsletterManagerPortlet = uiSubscription.getAncestorOfType(UINewsletterManagerPortlet.class);
      UINewsletterEntryManager newsletterManager = newsletterManagerPortlet.getChild(UINewsletterEntryManager.class);
      newsletterManager.setRendered(true);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      newsletterManager.setCategoryConfig(
                        uiSubscription.categoryHandler.getCategoryByName(
                                                                         sessionProvider,
                                                                         uiSubscription.portalName,
                                                                         uiSubscription.categoryConfig.getName()));
      newsletterManager.setSubscriptionConfig(
                        uiSubscription.subscriptionHandler.getSubscriptionsByName(
                                                                                  sessionProvider,
                                                                                  uiSubscription.portalName,
                                                                                  uiSubscription.categoryConfig.getName(),
                                                                                  subId));
      newsletterManager.init();
      newsletterManagerPortlet.getChild(UICategories.class).setRendered(false);
      newsletterManagerPortlet.getChild(UISubscriptions.class).setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving selectSubscriptionAction events.
   * The class that is interested in processing a selectSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectSubscriptionActionListener<code> method. When
   * the selectSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectSubscriptionActionEvent
   */
  public static class SelectSubscriptionActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions uiSubscriptions = event.getSource();
      String subscriptionId = event.getRequestContext().getRequestParameter(OBJECTID);
      UINewsletterManagerPortlet newsletterManagerPortlet = uiSubscriptions.getAncestorOfType(UINewsletterManagerPortlet.class);
      UINewsletterEntryManager newsletterManager = newsletterManagerPortlet.getChild(UINewsletterEntryManager.class);
      newsletterManager.setRendered(true);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      newsletterManager.setCategoryConfig(
                        uiSubscriptions.categoryHandler.getCategoryByName(
                                                                          sessionProvider,
                                                                         uiSubscriptions.portalName,
                                                                         uiSubscriptions.categoryConfig.getName()));
      newsletterManager.setSubscriptionConfig(
                        uiSubscriptions.subscriptionHandler.getSubscriptionsByName(
                                                                                   sessionProvider,
                                                                                   uiSubscriptions.portalName,
                                                                                   uiSubscriptions.categoryConfig.getName(),
                                                                                   subscriptionId));
      newsletterManager.init();
      newsletterManagerPortlet.getChild(UICategories.class).setRendered(false);
      newsletterManagerPortlet.getChild(UISubscriptions.class).setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving selectSubscriptionAction events.
   * The class that is interested in processing a selectSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectSubscriptionActionListener<code> method. When
   * the selectSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectSubscriptionActionEvent
   */
  public static class SelectSubscriptionCheckboxActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings("unchecked")
  public void execute(Event<UISubscriptions> event) throws Exception {

      UISubscriptions uiSubscriptions = event.getSource();

      boolean checked = true;
      List<UIComponent> listSubscriptions = uiSubscriptions.getChildren();
      for(UIComponent subscriptionCheckbox : listSubscriptions) {
        UIFormCheckBoxInput<Boolean> objCheckbox = (UIFormCheckBoxInput<Boolean>) subscriptionCheckbox;
        if(objCheckbox.getName().equals("UISubscriptionsCheckAll")) {
          continue;
        }
        if( objCheckbox.isChecked() == false) {
          checked = false;
        }
      }
      uiSubscriptions.getUIFormCheckBoxInput("UISubscriptionsCheckAll").setChecked(checked);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiSubscriptions);

    }
  }

  /**
   * The listener interface for receiving selectSubscriptionAction events.
   * The class that is interested in processing a selectSubscriptionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectSubscriptionActionListener<code> method. When
   * the selectSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectSubscriptionActionEvent
   */
  public static class SelectSubscriptionCheckboxHeaderActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
  public void execute(Event<UISubscriptions> event) throws Exception {

      UISubscriptions uiSubscriptions = event.getSource();

      List<UIComponent> listSubscriptions = uiSubscriptions.getChildren();
      UIFormCheckBoxInput<Boolean> uiSubscriptionsCheckAll = uiSubscriptions.getChildById("UISubscriptionsCheckAll");
      boolean checked = uiSubscriptionsCheckAll.isChecked();

      for(UIComponent subscriptionCheckbox : listSubscriptions) {
        if(subscriptionCheckbox.getName().equals("UISubscriptionsCheckAll")) {
          continue;
        }
         uiSubscriptions.getUIFormCheckBoxInput(subscriptionCheckbox.getName()).setChecked(checked);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSubscriptions);
    }
  }

  /**
   * The listener interface for receiving managerUsersAction events.
   * The class that is interested in processing a managerUsersAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addManagerUsersActionListener<code> method. When
   * the managerUsersAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ManagerUsersActionEvent
   */
  static  public class ManagerUsersActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions uiSubscription = event.getSource();
      UIManagerUsers managerUsers = uiSubscription.createUIComponent(UIManagerUsers.class, null, null);
      managerUsers.setInfor(uiSubscription.categoryConfig.getName(), null);
      Utils.createPopupWindow(uiSubscription, managerUsers, UINewsletterConstant.MANAGER_USERS_POPUP_WINDOW, 550);
    }
  }

  /**
   * The listener interface for receiving addEntryAction events.
   * The class that is interested in processing a addEntryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddEntryActionListener<code> method. When
   * the addEntryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddEntryActionEvent
   */
  public static class AddEntryActionListener extends EventListener<UISubscriptions> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISubscriptions> event) throws Exception {
      UISubscriptions uiSubscriptions = event.getSource();
      UINewsletterEntryContainer entryContainer = uiSubscriptions.createUIComponent(UINewsletterEntryContainer.class,
                                                                                    null,
                                                                                    null);
      entryContainer.setCategoryConfig(uiSubscriptions.categoryConfig);
      entryContainer.getChild(UINewsletterEntryDialogSelector.class)
                    .init(uiSubscriptions.categoryConfig.getName(), null);
      Utils.createPopupWindow(uiSubscriptions,
                              entryContainer,
                              UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW,
                              800, false);
    }
  }
}
