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

import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          maivanha1610@gmail.com
 * Sep 22, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/newsletter/NewsletterManager/UIRemoveModerators.gtmpl",
                 events = {
    @EventConfig(listeners = UIRemoveModerators.RemoveModeratorsActionListener.class),
    @EventConfig(listeners = UIRemoveModerators.CancelActionListener.class) })

public class UIRemoveModerators extends UIForm {
  private boolean setForCategoryForm = true;
  private List<String> listModerators = new ArrayList<String>();
  public void init(String input){
    listModerators.clear();
    listModerators.addAll(Arrays.asList(input.split(",")));
    this.removeChild(UIFormCheckBoxInput.class);
    for(String str : listModerators){
      this.addChild(new UIFormCheckBoxInput<Boolean>(str, str, false ));
    }
  }

  public void permissionForSubscriptionForm(){
    this.setForCategoryForm = false;
  }

  static  public class RemoveModeratorsActionListener extends EventListener<UIRemoveModerators> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIRemoveModerators> event) throws Exception {
      UIRemoveModerators removeModerators = event.getSource();
      UIFormCheckBoxInput<Boolean> checkBoxInput;
      StringBuffer sbResult = new StringBuffer();
      boolean isChecked = false;
      // Get all permissions which are not checked to remove
      for(String str : removeModerators.listModerators){
        checkBoxInput = removeModerators.getChildById(str);
        if(!checkBoxInput.isChecked()){
          if(sbResult.length() > 0) sbResult.append(",");
          sbResult.append(str);
        } else {
          isChecked = true;
        }
      }
      UIApplication uiApp = removeModerators.getAncestorOfType(UIApplication.class);

      // if untick any permission then view waring  and stop processing
      if (!isChecked) {
        uiApp.addMessage(new ApplicationMessage("UIRemoveModeratorsFormPopupWindow.msg.checkToRemove",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }

      // if current user is not admin of newsletterManager portle and in list permision don't have him selt
      // then view waring and stop processing
//      if(!removeModerators.isAdmin){
//        boolean havePermission = false;
//        List<String> listGrouptMembers = NewsLetterUtil.getAllGroupAndMembershipOfCurrentUser();
//        for(String str : result.split(",")){
//          if(listGrouptMembers.contains(str)){
//            havePermission = true;
//            break;
//          }
//        }
//        if(havePermission == false){
      // uiApp.addMessage(new
      // ApplicationMessage("UIRemoveModeratorsFormPopupWindow.msg.donotMoveYourSelt",
      // null, ApplicationMessage.WARNING));
//          
//          return;
//        }
//      }

      UIPopupContainer popupContainer = (UIPopupContainer)removeModerators.getAncestorOfType(UIPopupContainer.class);
      UIFormInputSetWithAction formInputSetWithAction;
      String inputId;

      //If now is modifying for Category then update for category form
      if (removeModerators.setForCategoryForm) {
        UICategoryForm componentForm = popupContainer.findFirstComponentOfType(UICategoryForm.class);
        formInputSetWithAction = (UIFormInputSetWithAction) componentForm.getChildById(UICategoryForm.FORM_CATEGORY_MODERATOR);
        inputId = UICategoryForm.INPUT_CATEGORY_MODERATOR;

        // if now is modifying for subscription then update for subscriptionForm
      } else {
        UISubcriptionForm componentForm = popupContainer.findFirstComponentOfType(UISubcriptionForm.class);
        formInputSetWithAction = (UIFormInputSetWithAction) componentForm.
            getChildById(UISubcriptionForm.FORM_SUBSCRIPTION_REDACTOR);
        inputId = UISubcriptionForm.SELECT_REDACTOR;
      }
      UIFormStringInput formStringInput = (UIFormStringInput)formInputSetWithAction.getChildById(inputId);
      formStringInput.setValue(sbResult.toString());
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      Utils.closePopupWindow(removeModerators, UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW);
    }
  }

  static  public class CancelActionListener extends EventListener<UIRemoveModerators> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIRemoveModerators> event) throws Exception {
      UIRemoveModerators removeModerators = event.getSource();
      UIPopupContainer popupContainer = (UIPopupContainer) removeModerators.getAncestorOfType(UIPopupContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      Utils.closePopupWindow(removeModerators,
                             UINewsletterConstant.REMOVE_MODERATORS_FORM_POPUP_WINDOW);
    }
  }
}
