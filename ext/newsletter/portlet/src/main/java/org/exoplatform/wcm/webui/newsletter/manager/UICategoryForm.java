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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
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
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.NameValidator;
import org.exoplatform.webui.form.validator.SpecialCharacterValidator;

/**
 * The Class UICategoryForm.
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class ,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UICategoryForm.SaveActionListener.class),
      @EventConfig(listeners = UICategoryForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICategoryForm.DeleteModeratorActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICategoryForm.SelectUserActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICategoryForm.SelectMemberActionListener.class, phase = Phase.DECODE)
    }
)
public class UICategoryForm extends UIForm implements UIPopupComponent, UISelectable {

  /** The category config. */
  private NewsletterCategoryConfig categoryConfig;

  /** The popup id. */
  private String popupId;

  /** The Constant INPUT_CATEGORY_NAME. */
  public static final String INPUT_CATEGORY_NAME           = "CategoryName";

  /** The Constant INPUT_CATEGORY_TITLE. */
  public static final String INPUT_CATEGORY_TITLE          = "CategoryTitle";

  /** The Constant INPUT_CATEGORY_DESCRIPTION. */
  public static final String INPUT_CATEGORY_DESCRIPTION    = "CategoryDescription";

  /** The Constant FORM_CATEGORY_MODERATOR. */
  public static final String FORM_CATEGORY_MODERATOR       = "FormCategoryModerator";

  /** The Constant INPUT_CATEGORY_MODERATOR. */
  public static final String INPUT_CATEGORY_MODERATOR      = "CategoryModerator";

  private boolean isRemove;

  public boolean isRemove() {
    return isRemove;
  }

  public void setRemove(boolean isRemove) {
    this.isRemove = isRemove;
  }

  /**
   * Instantiates a new uI category form.
   *
   * @throws Exception the exception
   */
  public UICategoryForm() throws Exception{
    UIFormStringInput inputCateName = new UIFormStringInput(INPUT_CATEGORY_NAME, null, null);
    UIFormStringInput inputCateTitle = new UIFormStringInput(INPUT_CATEGORY_TITLE, null, null);
    this.getChildren().clear();
    UIFormTextAreaInput inputCateDescription = new UIFormTextAreaInput(INPUT_CATEGORY_DESCRIPTION, null, null);
    UIFormStringInput inputModerator = new UIFormStringInput(INPUT_CATEGORY_MODERATOR, INPUT_CATEGORY_MODERATOR, null);
    inputModerator.setEditable(false);
    UIFormInputSetWithAction formCategoryModerator = new UIFormInputSetWithAction(FORM_CATEGORY_MODERATOR);
    formCategoryModerator.addChild(inputModerator);
    formCategoryModerator.setActionInfo(INPUT_CATEGORY_MODERATOR, new String[] {"SelectUser", "SelectMember", "DeleteModerator"});
    formCategoryModerator.showActionInfo(true);

    inputCateName.addValidator(MandatoryValidator.class).addValidator(NameValidator.class)
                  .addValidator(SpecialCharacterValidator.class);;
    inputCateTitle.addValidator(MandatoryValidator.class).addValidator(NoneHTMLValidator.class);
    inputModerator.addValidator(MandatoryValidator.class);
    inputCateDescription.addValidator(NoneHTMLValidator.class);

    addChild(inputCateName);
    addChild(inputCateTitle);
    addChild(inputCateDescription);
    addChild(formCategoryModerator);
    setActions(new String[]{"Save", "Cancel"});
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    UIFormInputSetWithAction formCategoryModerator = getChildById(FORM_CATEGORY_MODERATOR);
    UIFormStringInput stringInput = formCategoryModerator.getChildById(selectField);
    List<String> values = new ArrayList<String>();
    String oldValue = stringInput.getValue();
    if (oldValue != null && oldValue.length() > 0) {
      values.addAll(Arrays.asList(oldValue.split(",")));
    }
    if (!values.contains((String) value)) {
      values.add((String) value);
    }
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

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#activate()
   */
  public void activate() throws Exception {}

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#deActivate()
   */
  public void deActivate() throws Exception {}

  /**
   * Sets the category info.
   *
   * @param categoryConfig the new category info
   */
  public void setCategoryInfo(NewsletterCategoryConfig categoryConfig){
    if(categoryConfig == null) return;
    this.categoryConfig = categoryConfig;
    UIFormStringInput inputCateName = this.getChildById(INPUT_CATEGORY_NAME);
    inputCateName.setValue(categoryConfig.getName());
    inputCateName.setEditable(false);
    UIFormStringInput inputCateTitle = this.getChildById(INPUT_CATEGORY_TITLE);
    inputCateTitle.setValue(categoryConfig.getTitle());
    UIFormTextAreaInput inputCateDescription = this.getChildById(INPUT_CATEGORY_DESCRIPTION);
    inputCateDescription.setValue(categoryConfig.getDescription());
    UIFormInputSetWithAction inputSetWithAction = getChildById(FORM_CATEGORY_MODERATOR);
    UIFormStringInput inputModerator = inputSetWithAction.getChildById(INPUT_CATEGORY_MODERATOR);
    inputModerator.setValue(categoryConfig.getModerator());

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
  static  public class SaveActionListener extends EventListener<UICategoryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm uiCategoryForm = event.getSource();
      NewsletterManagerService newsletterManagerService =
        (NewsletterManagerService)PortalContainer.getInstance().getComponentInstanceOfType(NewsletterManagerService.class) ;
      NewsletterCategoryConfig categoryConfig = null;
      if(uiCategoryForm.categoryConfig == null) {
        categoryConfig = new NewsletterCategoryConfig();
        categoryConfig.setName(((UIFormStringInput)uiCategoryForm.getChildById(INPUT_CATEGORY_NAME)).getValue());
      }else{
        categoryConfig = uiCategoryForm.categoryConfig;
      }
      categoryConfig.setTitle(((UIFormStringInput)uiCategoryForm.getChildById(INPUT_CATEGORY_TITLE)).getValue());
      categoryConfig.setDescription(((UIFormTextAreaInput)uiCategoryForm.getChildById(INPUT_CATEGORY_DESCRIPTION)).getValue());

      UIFormInputSetWithAction inputSetWithAction = uiCategoryForm.getChildById(FORM_CATEGORY_MODERATOR);
      UIFormStringInput inputModerator = inputSetWithAction.getChildById(INPUT_CATEGORY_MODERATOR);

      String inputModeratorValue = inputModerator.getValue();
      if (("".equals(inputModeratorValue)) || (inputModeratorValue == null)) {
        UIApplication uiApp = uiCategoryForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UICategoryForm.msg.inputModeratorEmpty", null, ApplicationMessage.WARNING));
        
        return;
      }
      categoryConfig.setModerator(inputModeratorValue);

      // Update access permission into newsletter manager page for moderators
      NewsletterConstant.updateAccessPermission(inputModeratorValue.split(","));
      UIApplication uiApp = uiCategoryForm.getAncestorOfType(UIApplication.class);
      NewsletterCategoryHandler categoryHandler = newsletterManagerService.getCategoryHandler();
      categoryHandler.setRemove(uiCategoryForm.isRemove());
      String portalName = NewsLetterUtil.getPortalName();
      try{
        SessionProvider sessionProvider = Utils.getSessionProvider();
        if(uiCategoryForm.categoryConfig == null){ // if add new category then check cateogry's name is already exist or not
          if(categoryHandler.getCategoryByName(sessionProvider, portalName, categoryConfig.getName()) != null){
            uiApp.addMessage(new ApplicationMessage("UICategoryForm.msg.categoryNameIsAlreadyExist",
                                                    null,
                                                    ApplicationMessage.WARNING));
            
            return;
          }else{
            categoryHandler.add(sessionProvider, portalName, categoryConfig);
          }
        }else{ // Edit a category is already exist
          categoryHandler.edit(sessionProvider, portalName, categoryConfig);
        }
      }catch(Exception ex){
        Utils.createPopupMessage(uiCategoryForm, "UICategoryForm.msg.error-add-category", null, ApplicationMessage.ERROR);
      }
      Utils.closePopupWindow(uiCategoryForm, UINewsletterConstant.CATEGORY_FORM_POPUP_WINDOW);
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
  static  public class CancelActionListener extends EventListener<UICategoryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm uiCategoryForm = event.getSource();
      uiCategoryForm.setRemove(false);
      Utils.closePopupWindow(uiCategoryForm, UINewsletterConstant.CATEGORY_FORM_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving selectUserAction events.
   * The class that is interested in processing a selectUserAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectUserActionListener<code> method. When
   * the selectUserAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectUserActionEvent
   */
  public static class SelectUserActionListener extends EventListener<UICategoryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm categoryForm = event.getSource();
      categoryForm.setRemove(false);
      UIUserMemberSelector userMemberSelector = categoryForm.createUIComponent(UIUserMemberSelector.class, null, null);
      userMemberSelector.setMulti(false);
      userMemberSelector.setShowSearch(true);
      userMemberSelector.setSourceComponent(categoryForm, new String[] {INPUT_CATEGORY_MODERATOR});
      userMemberSelector.init();
      Utils.createPopupWindow(categoryForm, userMemberSelector, UINewsletterConstant.USER_SELECTOR_POPUP_WINDOW, 750);
      categoryForm.setPopupId(UINewsletterConstant.USER_SELECTOR_POPUP_WINDOW);
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
  public static class SelectMemberActionListener extends EventListener<UICategoryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm categoryForm = event.getSource();
      categoryForm.setRemove(false);
      UIGroupMemberSelector groupMemberSelector = categoryForm.createUIComponent(UIGroupMemberSelector.class, null, null);
      groupMemberSelector.setShowAnyPermission(false);
      groupMemberSelector.setSourceComponent(categoryForm, new String[] {INPUT_CATEGORY_MODERATOR});
      Utils.createPopupWindow(categoryForm, groupMemberSelector, UINewsletterConstant.GROUP_SELECTOR_POPUP_WINDOW, 540);
      categoryForm.setPopupId(UINewsletterConstant.GROUP_SELECTOR_POPUP_WINDOW);
    }
  }

  public static class DeleteModeratorActionListener extends EventListener<UICategoryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm categoryForm = event.getSource();
      UIFormInputSetWithAction formCategoryModerator = categoryForm.getChildById(FORM_CATEGORY_MODERATOR);
      UIFormStringInput stringInput = formCategoryModerator.getChildById(INPUT_CATEGORY_MODERATOR);
      if(stringInput.getValue() == null || stringInput.getValue().trim().length() < 1) {
        UIApplication uiApp = categoryForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UICategoryForm.msg.doNotHaveModerators", null, ApplicationMessage.WARNING));
        
        return;
      };
      UIRemoveModerators removeModerators = categoryForm.createUIComponent(UIRemoveModerators.class, null, null);
      removeModerators.init(((UIFormStringInput)((UIFormInputSetWithAction)categoryForm.
                            getChildById(FORM_CATEGORY_MODERATOR)).getChildById(INPUT_CATEGORY_MODERATOR)).getValue());
      categoryForm.setRemove(true);
      Utils.createPopupWindow(categoryForm, removeModerators, UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW, 480);
      categoryForm.setPopupId(UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW);
    }
  }
}
