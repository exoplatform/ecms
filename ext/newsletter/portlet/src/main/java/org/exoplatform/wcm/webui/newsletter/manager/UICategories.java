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
import java.util.List;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * The Class UICategories.
 */
@ComponentConfig(template = "app:/groovy/webui/newsletter/NewsletterManager/UICategories.gtmpl", events = {
    @EventConfig(listeners = UICategories.AddEntryActionListener.class),
    @EventConfig(listeners = UICategories.AddCategoryActionListener.class),
    @EventConfig(listeners = UICategories.OpenCategoryActionListener.class),
    @EventConfig(listeners = UICategories.AddSubcriptionActionListener.class),
    @EventConfig(listeners = UICategories.ManagerUsersActionListener.class),
    @EventConfig(listeners = UICategories.SelectSubscriptionActionListener.class) })
public class UICategories extends UIContainer {

  /** The category handler. */
  NewsletterCategoryHandler categoryHandler = null;

  /** The subscription handler. */
  NewsletterSubscriptionHandler subscriptionHandler = null;

  /** The user handler. */
  NewsletterManageUserHandler userHandler = null;

  /** The portal name. */
  String portalName;

  String userId;

  /**
   * Instantiates a new uI categories.
   *
   * @throws Exception
   *             the exception
   */
  public UICategories() throws Exception {
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    categoryHandler = newsletterManagerService.getCategoryHandler();
    subscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    userHandler = newsletterManagerService.getManageUserHandler();
    portalName = NewsLetterUtil.getPortalName();
    userId = NewsLetterUtil.getCurrentUser();
  }

  /**
   * Gets the number of waiting newsletter.
   *
   * @param categoryName
   *            the category name
   * @param subscriptionName
   *            the subscription name
   *
   * @return the number of waiting newsletter
   */
  public long getNumberOfWaitingNewsletter(String categoryName,
      String subscriptionName) {
    try {
      return subscriptionHandler.getNumberOfNewslettersWaiting(Utils.getSessionProvider(),
                                                               portalName,
                                                               categoryName,
                                                               subscriptionName);
    } catch (Exception ex) {
      return 0;
    }
  }

  /**
   * Gets the number of user.
   *
   * @param categoryName
   *            the category name
   * @param subscriptionName
   *            the subscription name
   *
   * @return the number of user
   */
  public int getNumberOfUser(String categoryName, String subscriptionName) {
    return userHandler.getQuantityUserBySubscription(Utils.getSessionProvider(),
                                                     portalName,
                                                     categoryName,
                                                     subscriptionName);
  }

  /**
   * Gets the list categories.
   *
   * @return the list categories
   */
  public List<NewsletterCategoryConfig> getListCategories() {
    List<NewsletterCategoryConfig> listCategories = new ArrayList<NewsletterCategoryConfig>();
    try {
      SessionProvider sessionProvider = WCMCoreUtils
          .getUserSessionProvider();
      if (userHandler.isAdministrator(portalName, userId))
        listCategories = categoryHandler.getListCategories(portalName,
            sessionProvider);
      else
        listCategories = categoryHandler.getListCategoriesCanView(
            portalName, userId, sessionProvider);
    } catch (Exception e) {
      Utils.createPopupMessage(this,
          "UICategories.msg.get-list-categories", null,
          ApplicationMessage.ERROR);
    }
    return listCategories;
  }

  public boolean isAdministrator() {
    return userHandler.isAdministrator(portalName, userId);
  }

  /**
   * Gets the list subscription.
   *
   * @param categoryName
   *            the category name
   *
   * @return the list subscription
   */
  public List<NewsletterSubscriptionConfig> getListSubscription(
      String categoryName) {
    List<NewsletterSubscriptionConfig> listSubscription = new ArrayList<NewsletterSubscriptionConfig>();
    try {
      SessionProvider sessionProvider = WCMCoreUtils
          .getUserSessionProvider();
      NewsletterCategoryConfig categoryConfig = categoryHandler
          .getCategoryByName(sessionProvider, portalName,
              categoryName);
      if (userHandler.isAdministrator(portalName, userId)
          || userHandler.isModerator(userId, categoryConfig))
        listSubscription = subscriptionHandler
            .getSubscriptionsByCategory(sessionProvider,
                portalName, categoryName);
      else {
        listSubscription = subscriptionHandler
            .getSubscriptionByRedactor(portalName, categoryName,
                userId, sessionProvider);
      }
    } catch (Exception e) {
      Utils.createPopupMessage(this,
          "UICategories.msg.get-list-subscriptions", null,
          ApplicationMessage.ERROR);
    }
    return listSubscription;
  }

  /**
   * The listener interface for receiving addCategoryAction events. The class
   * that is interested in processing a addCategoryAction event implements
   * this interface, and the object created with that class is registered with
   * a component using the component's
   * <code>addAddCategoryActionListener<code> method. When
   * the addCategoryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddCategoryActionEvent
   */
  static public class AddCategoryActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategories = event.getSource();
      UICategoryForm categoryForm = uiCategories.createUIComponent(
          UICategoryForm.class, null, null);
      Utils.createPopupWindow(uiCategories, categoryForm,
          UINewsletterConstant.CATEGORY_FORM_POPUP_WINDOW, 450);
      categoryForm.setCategoryInfo(null);
    }
  }

  /**
   * The listener interface for receiving addSubcriptionAction events. The
   * class that is interested in processing a addSubcriptionAction event
   * implements this interface, and the object created with that class is
   * registered with a component using the component's
   * <code>addAddSubcriptionActionListener<code> method. When
   * the addSubcriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddSubcriptionActionEvent
   */
  static public class AddSubcriptionActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategories = event.getSource();
      UISubcriptionForm subcriptionForm = uiCategories.createUIComponent(
          UISubcriptionForm.class, null, null);
      Utils.createPopupWindow(uiCategories, subcriptionForm,
          UINewsletterConstant.SUBSCRIPTION_FORM_POPUP_WINDOW, 500);
      subcriptionForm.setSubscriptionInfor(null);
    }
  }

  /**
   * The listener interface for receiving openCategoryAction events. The class
   * that is interested in processing a openCategoryAction event implements
   * this interface, and the object created with that class is registered with
   * a component using the component's
   * <code>addOpenCategoryActionListener<code> method. When
   * the openCategoryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see OpenCategoryActionEvent
   */
  static public class OpenCategoryActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    @SuppressWarnings("deprecation")
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategories = event.getSource();
      String categoryName = event.getRequestContext()
          .getRequestParameter(OBJECTID);
      UINewsletterManagerPortlet newsletterManagerPortlet = uiCategories
          .getAncestorOfType(UINewsletterManagerPortlet.class);
      UISubscriptions subsriptions = newsletterManagerPortlet
          .getChild(UISubscriptions.class);
      subsriptions.setRendered(true);
      subsriptions.setCategory(uiCategories.categoryHandler.getCategoryByName(Utils.getSessionProvider(),
                                                                              NewsLetterUtil.getPortalName(),
                                                                              categoryName));
      subsriptions.init();
      newsletterManagerPortlet.getChild(UICategories.class).setRendered(
          false);
      event.getRequestContext().addUIComponentToUpdateByAjax(
          newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving managerUsersAction events. The class
   * that is interested in processing a managerUsersAction event implements
   * this interface, and the object created with that class is registered with
   * a component using the component's
   * <code>addManagerUsersActionListener<code> method. When
   * the managerUsersAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ManagerUsersActionEvent
   */
  static public class ManagerUsersActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategories = event.getSource();
      UIManagerUsers managerUsers = uiCategories.createUIComponent(
          UIManagerUsers.class, null, null);
      managerUsers.setInfor(null, null);
      Utils.createPopupWindow(uiCategories, managerUsers,
          UINewsletterConstant.MANAGER_USERS_POPUP_WINDOW, 600);
    }
  }

  /**
   * The listener interface for receiving addEntryAction events. The class
   * that is interested in processing a addEntryAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addAddEntryActionListener<code> method. When
   * the addEntryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddEntryActionEvent
   */
  public static class AddEntryActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategories = event.getSource();
      UINewsletterEntryContainer entryContainer = uiCategories
          .createUIComponent(UINewsletterEntryContainer.class, null,
              null);
      entryContainer.setCategoryConfig(null);
      entryContainer.getChild(UINewsletterEntryDialogSelector.class).init(null, null);
      Utils.createPopupWindow(uiCategories, entryContainer,
          UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW, 800, false);
    }
  }

  /**
   * The listener interface for receiving selectSubscriptionAction events. The
   * class that is interested in processing a selectSubscriptionAction event
   * implements this interface, and the object created with that class is
   * registered with a component using the component's
   * <code>addSelectSubscriptionActionListener<code> method. When
   * the selectSubscriptionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectSubscriptionActionEvent
   */
  public static class SelectSubscriptionActionListener extends
      EventListener<UICategories> {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UICategories> event) throws Exception {
      UICategories uiCategory = event.getSource();
      String categoryAndSubscription = event.getRequestContext()
          .getRequestParameter(OBJECTID);
      UINewsletterManagerPortlet newsletterManagerPortlet = uiCategory
          .getAncestorOfType(UINewsletterManagerPortlet.class);
      UINewsletterEntryManager newsletterManager = newsletterManagerPortlet
          .getChild(UINewsletterEntryManager.class);
      newsletterManager.setRendered(true);

      String categoryName = categoryAndSubscription.split("/")[0];
      String subscriptionName = categoryAndSubscription.split("/")[1];
      SessionProvider sessionProvider = Utils.getSessionProvider();
      newsletterManager.setCategoryConfig(uiCategory.categoryHandler
          .getCategoryByName(sessionProvider, uiCategory.portalName,
              categoryName));
      newsletterManager
          .setSubscriptionConfig(uiCategory.subscriptionHandler
              .getSubscriptionsByName(sessionProvider,
                  uiCategory.portalName, categoryName,
                  subscriptionName));
      newsletterManager.init();
      newsletterManagerPortlet.getChild(UICategories.class).setRendered(
          false);
      newsletterManagerPortlet.getChild(UISubscriptions.class)
          .setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(
          newsletterManagerPortlet);
    }
  }
}
