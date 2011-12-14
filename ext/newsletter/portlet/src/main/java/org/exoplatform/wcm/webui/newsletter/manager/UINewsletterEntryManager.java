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

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.config.NewsletterManagerConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterEntryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterTemplateHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * ngoc.tran@exoplatform.com
 * Jun 9, 2009
 */
@SuppressWarnings("deprecation")
@ComponentConfig(
     lifecycle = UIFormLifecycle.class,
     template = "app:/groovy/webui/newsletter/NewsletterManager/UINewsletterEntryManager.gtmpl",
     events = {
         @EventConfig(listeners = UINewsletterEntryManager.AddEntryActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.BackToSubcriptionsActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.BackToCategoriesActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.OpenNewsletterActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.ConvertTemplateActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.EditNewsletterEntryActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.DeleteNewsletterEntryActionListener.class),
         @EventConfig(listeners = UINewsletterEntryManager.SelectNewsletterActionListener.class)
     }
 )
public class UINewsletterEntryManager extends UIForm {

  /** The check box input. */
  private UIFormCheckBoxInput<Boolean>  checkBoxInput;

  /** The subscription config. */
  private NewsletterSubscriptionConfig subscriptionConfig;

  /** The category config. */
  private NewsletterCategoryConfig categoryConfig;

  /** The list newsletter config. */
  private List<NewsletterManagerConfig> listNewsletterConfig;

  /** The newsletter entry handler. */
  private NewsletterEntryHandler newsletterEntryHandler ;

  /** The PAGEITERATO r_ id. */
  private String PAGEITERATOR_ID = "NewsletterEntryManagerPageIterator";

  /** The ui page iterator_. */
  private UIPageIterator uiPageIterator_;

  /**
   * Instantiates a new uI newsletter entry manager.
   *
   * @throws Exception the exception
   */
  public UINewsletterEntryManager() throws Exception {
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    newsletterEntryHandler = newsletterManagerService.getEntryHandler();
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public void init() throws Exception {
    ListAccess<NewsletterManagerConfig> newsletterList = new ListAccessImpl<NewsletterManagerConfig>(
        NewsletterManagerConfig.class, setListNewsletterEntries());
    LazyPageList<NewsletterManagerConfig> dataPageList = new LazyPageList<NewsletterManagerConfig>(newsletterList, 10);
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, PAGEITERATOR_ID);
    addChild(uiPageIterator_);
    uiPageIterator_.setPageList(dataPageList);
  }

  /**
   * Sets the list newsletter entries.
   *
   * @return the list< newsletter manager config>
   */
  private List<NewsletterManagerConfig> setListNewsletterEntries(){
    this.getChildren().clear();
    listNewsletterConfig = new ArrayList<NewsletterManagerConfig>();
    try {
      listNewsletterConfig.addAll(newsletterEntryHandler.getNewsletterEntriesBySubscription(WCMCoreUtils.getUserSessionProvider(),
                                                                                            NewsLetterUtil.getPortalName(),
                                                                                            categoryConfig.getName(),
                                                                                            subscriptionConfig.getName()));
      for (NewsletterManagerConfig newletter : listNewsletterConfig) {
        checkBoxInput = new UIFormCheckBoxInput<Boolean>(newletter.getNewsletterName(),
                                                         newletter.getNewsletterName(),
                                                         false);
        this.addChild(checkBoxInput);
      }
    } catch (Exception ex) {
      Utils.createPopupMessage(this,
                               "UINewsletterEntryManager.msg.set-list-newsletter",
                               null,
                               ApplicationMessage.ERROR);
    }
    return listNewsletterConfig;
  }

  /**
   * Gets the list newsletter entries.
   *
   * @return the list newsletter entries
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public List getListNewsletterEntries() throws Exception {
    if(uiPageIterator_ != null)return uiPageIterator_.getCurrentPageData() ;
    else return new ArrayList<NewsletterManagerConfig>();
  }

  /**
   * Gets the checked.
   *
   * @return the checked
   */
  @SuppressWarnings("unchecked")
  public List<String> getChecked() {
    List<String> newsletterId = new ArrayList<String>();
    UIFormCheckBoxInput<Boolean> checkbox = null;
    for (UIComponent component : this.getChildren()) {
      try {
        checkbox = (UIFormCheckBoxInput<Boolean>) component;
        if (checkbox.isChecked()) {
          newsletterId.add(checkbox.getName());
        }
      } catch (Exception e) {
        // You shouldn't throw popup message, because some exception often rise
        // here.
      }
    }
    return newsletterId;
  }

  /**
   * Gets the subscription config.
   *
   * @return the subscription config
   */
  public NewsletterSubscriptionConfig getSubscriptionConfig() {
    return subscriptionConfig;
  }

  /**
   * Sets the subscription config.
   *
   * @param subscriptionConfig the new subscription config
   */
  public void setSubscriptionConfig(NewsletterSubscriptionConfig subscriptionConfig) {
    this.subscriptionConfig = subscriptionConfig;
  }

  /**
   * Gets the category config.
   *
   * @return the category config
   */
  public NewsletterCategoryConfig getCategoryConfig() {
    return categoryConfig;
  }

  /**
   * Sets the category config.
   *
   * @param categoryConfig the new category config
   */
  public void setCategoryConfig(NewsletterCategoryConfig categoryConfig) {
    this.categoryConfig = categoryConfig;
  }

  /**
   * The listener interface for receiving backToSubcriptionsAction events.
   * The class that is interested in processing a backToSubcriptionsAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addBackToSubcriptionsActionListener<code> method. When
   * the backToSubcriptionsAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see BackToSubcriptionsActionEvent
   */
  static  public class BackToSubcriptionsActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      UINewsletterManagerPortlet newsletterManagerPortlet = uiNewsletterEntryManager.
          getAncestorOfType(UINewsletterManagerPortlet.class);
      UISubscriptions uiSubscriptions = newsletterManagerPortlet.getChild(UISubscriptions.class);
      newsletterManagerPortlet.getChild(UICategories.class).setRendered(false);
      uiSubscriptions.setRendered(true);
      uiSubscriptions.setCategory(uiNewsletterEntryManager.categoryConfig);
      uiNewsletterEntryManager.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
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
  static  public class BackToCategoriesActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager newsletter = event.getSource();
      UINewsletterManagerPortlet newsletterManagerPortlet = newsletter.getAncestorOfType(UINewsletterManagerPortlet.class);
      UICategories uiCategories = newsletterManagerPortlet.getChild(UICategories.class);
      UISubscriptions subcription = newsletterManagerPortlet.getChild(UISubscriptions.class);
      uiCategories.setRendered(true);
      subcription.setRendered(false);
      newsletter.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(newsletterManagerPortlet);
    }
  }

  /**
   * The listener interface for receiving openNewsletterAction events.
   * The class that is interested in processing a openNewsletterAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addOpenNewsletterActionListener<code> method. When
   * the openNewsletterAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see OpenNewsletterActionEvent
   */
  static public class OpenNewsletterActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      List<String> subIds = uiNewsletterEntryManager.getChecked();
      if (subIds == null) {
        Utils.createPopupMessage(uiNewsletterEntryManager,
                                 "UISubscription.msg.checkOneNewsletterToOpen",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      } else if (subIds.size() != 1) {
        Utils.createPopupMessage(uiNewsletterEntryManager,
                                 "UISubscription.msg.checkOnlyOneNewsletterToOpen",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      }
      UINewsletterManagerPopup newsletterManagerPopup = uiNewsletterEntryManager.createUIComponent(UINewsletterManagerPopup.class,
                                                                                                   null,
                                                                                                   null);
      newsletterManagerPopup.setNewsletterInfor(uiNewsletterEntryManager.categoryConfig.getName(),
                                                uiNewsletterEntryManager.subscriptionConfig.getName(),
                                                subIds.get(0));
      Utils.createPopupWindow(uiNewsletterEntryManager,
                              newsletterManagerPopup,
                              UINewsletterConstant.UIVIEW_ENTRY_PUPUP_WINDOW,
                              800);
    }
  }

  /**
   * The listener interface for receiving selectNewsletterAction events.
   * The class that is interested in processing a selectNewsletterAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectNewsletterActionListener<code> method. When
   * the selectNewsletterAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectNewsletterActionEvent
   */
  static  public class SelectNewsletterActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      String newsletterName = event.getRequestContext().getRequestParameter(OBJECTID);
      UINewsletterManagerPopup newsletterManagerPopup = uiNewsletterEntryManager.createUIComponent(UINewsletterManagerPopup.class,
                                                                                                   null,
                                                                                                   null);
      newsletterManagerPopup.setNewsletterInfor(uiNewsletterEntryManager.categoryConfig.getName(),
                                                uiNewsletterEntryManager.subscriptionConfig.getName(),
                                                newsletterName);
      Utils.createPopupWindow(uiNewsletterEntryManager,
                              newsletterManagerPopup,
                              UINewsletterConstant.UIVIEW_ENTRY_PUPUP_WINDOW,
                              800);
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
  public static class AddEntryActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      UINewsletterEntryContainer entryContainer = uiNewsletterEntryManager.createUIComponent(UINewsletterEntryContainer.class,
                                                                                             null,
                                                                                             null);
      entryContainer.setCategoryConfig(uiNewsletterEntryManager.categoryConfig);
      entryContainer.getChild(UINewsletterEntryDialogSelector.class)
                    .init(uiNewsletterEntryManager.categoryConfig.getName(),
                          uiNewsletterEntryManager.subscriptionConfig.getName());
      Utils.createPopupWindow(uiNewsletterEntryManager,
                              entryContainer,
                              UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW,
                              800, false);
    }
  }

  /**
   * The listener interface for receiving deleteNewsletterEntryAction events.
   * The class that is interested in processing a deleteNewsletterEntryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteNewsletterEntryActionListener<code> method. When
   * the deleteNewsletterEntryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DeleteNewsletterEntryActionEvent
   */
  public static class DeleteNewsletterEntryActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      List<String> subIds = uiNewsletterEntryManager.getChecked();
      if (subIds == null || subIds.size() == 0) {
        Utils.createPopupMessage(uiNewsletterEntryManager,
                                 "UISubscription.msg.checkOnlyOneNewsletterToDelete",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      }
      uiNewsletterEntryManager.newsletterEntryHandler.delete(WCMCoreUtils.getUserSessionProvider(),
                                                             NewsLetterUtil.getPortalName(),
                                                             uiNewsletterEntryManager.categoryConfig.getName(),
                                                             uiNewsletterEntryManager.subscriptionConfig.getName(),
                                                             subIds);
      uiNewsletterEntryManager.init();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiNewsletterEntryManager) ;
    }
  }

  /**
   * The listener interface for receiving editNewsletterEntryAction events.
   * The class that is interested in processing a editNewsletterEntryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addEditNewsletterEntryActionListener<code> method. When
   * the editNewsletterEntryAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see EditNewsletterEntryActionEvent
   */
  public static class EditNewsletterEntryActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager uiNewsletterEntryManager = event.getSource();
      List<String> subIds = uiNewsletterEntryManager.getChecked();
      if (subIds == null || subIds.size() == 0) {
        Utils.createPopupMessage(uiNewsletterEntryManager,
                                 "UISubscription.msg.checkOneNewsletterToEdit",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      } else if (subIds.size() != 1) {
        Utils.createPopupMessage(uiNewsletterEntryManager,
                                 "UISubscription.msg.checkOnlyOneNewsletterToEdit",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      } else {
        NewsletterManagerConfig newsletterName = uiNewsletterEntryManager.newsletterEntryHandler.
            getNewsletterEntry(WCMCoreUtils.getUserSessionProvider(),
                               NewsLetterUtil.getPortalName(),
                               uiNewsletterEntryManager.categoryConfig.getName(),
                               uiNewsletterEntryManager.getSubscriptionConfig().getName(),
                               subIds.get(0));
        if (newsletterName.getStatus().equals(NewsletterConstant.STATUS_SENT)) {
          Utils.createPopupMessage(uiNewsletterEntryManager,
                                   "UINewsletterEntryManager.msg.canNotEditNewsletter",
                                   null,
                                   ApplicationMessage.WARNING);
          return;
        }
      }
      UINewsletterEntryContainer entryContainer = uiNewsletterEntryManager.createUIComponent(UINewsletterEntryContainer.class,
                                                                                             null,
                                                                                             null);
      entryContainer.setNewsletterInfor(NewsletterConstant.generateCategoryPath(NewsLetterUtil.getPortalName())
          + "/"
          + uiNewsletterEntryManager.categoryConfig.getName()
          + "/"
          + uiNewsletterEntryManager.getSubscriptionConfig().getName() + "/" + subIds.get(0));
      UINewsletterEntryDialogSelector newsletterEntryDialogSelector = entryContainer.
          getChild(UINewsletterEntryDialogSelector.class);
      newsletterEntryDialogSelector.init(uiNewsletterEntryManager.categoryConfig.getName(),
                                         uiNewsletterEntryManager.subscriptionConfig.getName());
      UIFormSelectBox categorySelectBox = newsletterEntryDialogSelector.
          getChildById(UINewsletterConstant.ENTRY_CATEGORY_SELECTBOX);
      categorySelectBox.setValue(uiNewsletterEntryManager.categoryConfig.getName());
      categorySelectBox.setDisabled(true);
      UIFormSelectBox subscriptionSelectBox = newsletterEntryDialogSelector.
          getChildById(UINewsletterConstant.ENTRY_SUBSCRIPTION_SELECTBOX);
      subscriptionSelectBox.setValue(uiNewsletterEntryManager.subscriptionConfig.getName());
      subscriptionSelectBox.setDisabled(true);
      UINewsletterEntryForm newsletterEntryForm = entryContainer.getChild(UINewsletterEntryForm.class);
      newsletterEntryForm.addNew(false);
      entryContainer.setUpdated(true);
      Utils.createPopupWindow(uiNewsletterEntryManager,
                              entryContainer,
                              UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW,
                              800, false);
    }
  }

  /**
   * The listener interface for receiving convertTemplateAction events.
   * The class that is interested in processing a convertTemplateAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addConvertTemplateActionListener<code> method. When
   * the convertTemplateAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ConvertTemplateActionEvent
   */
  public static class ConvertTemplateActionListener extends EventListener<UINewsletterEntryManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryManager> event) throws Exception {
      UINewsletterEntryManager newsletterEntryManager = event.getSource();
      String categoryName = newsletterEntryManager.categoryConfig.getName();
      String subscriptionName = newsletterEntryManager.subscriptionConfig.getName();
      List<String> subIds = newsletterEntryManager.getChecked();
      String message;
      int messageType;
      if (subIds == null) {
        message = "UISubscription.msg.checkOneNewsletterToConvert";
        messageType = ApplicationMessage.WARNING;
      } else if (subIds.size() != 1) {
      message = "UISubscription.msg.checkOnlyOneNewsletterToConvert";
        messageType = ApplicationMessage.WARNING;
      } else {
        try{
          String newsletterName = subIds.get(0);
          NewsletterManagerService newsletterManagerService = newsletterEntryManager.
              getApplicationComponent(NewsletterManagerService.class);
          RepositoryService repositoryService = newsletterEntryManager.getApplicationComponent(RepositoryService.class);
          ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
          Session session = WCMCoreUtils.getUserSessionProvider()
                                 .getSession(newsletterManagerService.getWorkspaceName(),
                                             manageableRepository);
          String newsletterPath = NewsletterConstant.generateNewsletterPath(NewsLetterUtil.getPortalName(),
                                                                            categoryName,
                                                                            subscriptionName,
                                                                            newsletterName);
          Node newsletterNode = (Node) session.getItem(newsletterPath);
          NewsletterTemplateHandler newsletterTemplateHandler = newsletterManagerService.getTemplateHandler();
          newsletterTemplateHandler.convertAsTemplate(WCMCoreUtils.getUserSessionProvider(),
                                                      newsletterNode.getPath(),
                                                      NewsLetterUtil.getPortalName(),
                                                      categoryName);
          message = "UISubscription.msg.convertSuccessful";
          messageType = ApplicationMessage.INFO;
        } catch (Exception ex) {
          message = "UISubscription.msg.templateIsExist";
          messageType = ApplicationMessage.ERROR;
        }
      }
      UIApplication uiApp = newsletterEntryManager.getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage(message, null, messageType));
      
    }
  }
}
