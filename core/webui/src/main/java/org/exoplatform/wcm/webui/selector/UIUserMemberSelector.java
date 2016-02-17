/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.selector;

import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * June 10, 2009
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UIUserMemberSelector.AddUserActionListener.class)}
)
public class UIUserMemberSelector extends UIContainer implements ComponentSelector  {

  /** The ui component. */
  private UIComponent uiComponent;

  /** The return field. */
  private String returnField;

  /** The is use popup. */
  private boolean isUsePopup = true;

  /** The is multi. */
  private boolean isMulti = true;

  /** The is show search group. */
  private boolean isShowSearchGroup = true;

  /** The is show search user. */
  private boolean isShowSearchUser = true;

  /** The is show search. */
  private boolean isShowSearch;

  /**
   * Instantiates a new uIWCM user container.
   */
  public UIUserMemberSelector() {}

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  public void init() throws Exception {
    UIUserSelector uiUserSelector = getChild(UIUserSelector.class);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, null);
    }
    uiUserSelector.setMulti(isMulti);
    uiUserSelector.setShowSearchGroup(isShowSearchGroup);
    uiUserSelector.setShowSearchUser(isShowSearchUser);
    uiUserSelector.setShowSearch(isShowSearch);
  }

  /**
   * Checks if is use popup.
   *
   * @return true, if is use popup
   */
  public boolean isUsePopup() {
    return isUsePopup;
  }

  /**
   * Sets the use popup.
   *
   * @param isUsePopup the new use popup
   */
  public void setUsePopup(boolean isUsePopup) {
    this.isUsePopup = isUsePopup;
  }

  /**
   * Checks if is multi.
   *
   * @return true, if is multi
   */
  public boolean isMulti() {
    return isMulti;
  }

  /**
   * Sets the multi.
   *
   * @param isMulti the new multi
   */
  public void setMulti(boolean isMulti) {
    this.isMulti = isMulti;
  }

  /**
   * Checks if is show search group.
   *
   * @return true, if is show search group
   */
  public boolean isShowSearchGroup() {
    return isShowSearchGroup;
  }

  /**
   * Sets the show search group.
   *
   * @param isShowSearchGroup the new show search group
   */
  public void setShowSearchGroup(boolean isShowSearchGroup) {
    this.isShowSearchGroup = isShowSearchGroup;
  }

  /**
   * Checks if is show search user.
   *
   * @return true, if is show search user
   */
  public boolean isShowSearchUser() {
    return isShowSearchUser;
  }

  /**
   * Sets the show search user.
   *
   * @param isShowSearchUser the new show search user
   */
  public void setShowSearchUser(boolean isShowSearchUser) {
    this.isShowSearchUser = isShowSearchUser;
  }

  /**
   * Checks if is show search.
   *
   * @return true, if is show search
   */
  public boolean isShowSearch() {
    return isShowSearch;
  }

  /**
   * Sets the show search.
   *
   * @param isShowSearch the new show search
   */
  public void setShowSearch(boolean isShowSearch) {
    this.isShowSearch = isShowSearch;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.ComponentSelector#getSourceComponent()
   */
  public UIComponent getSourceComponent() {
    return uiComponent;
  }

  /**
   * Gets the return field.
   *
   * @return the return field
   */
  public String getReturnField() {
    return returnField;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.ecm.webui.selector.ComponentSelector#setSourceComponent
   * (org.exoplatform.webui.core.UIComponent, java.lang.String[])
   */
  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    uiComponent = uicomponent;
    if (initParams == null || initParams.length == 0)
      return;
    for (int i = 0; i < initParams.length; i++) {
      if (initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=");
        returnField = array[1];
        break;
      }
      returnField = initParams[0];
    }
  }

  /**
   * The listener interface for receiving addUserAction events.
   * The class that is interested in processing a addUserAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddUserActionListener</code> method. When
   * the addUserAction event occurs, that object's appropriate
   * method is invoked.
   */
  static  public class AddUserActionListener extends EventListener<UIUserMemberSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIUserMemberSelector> event) throws Exception {
      UIUserMemberSelector userMemberSelector = event.getSource();
      UIUserSelector userSelector = userMemberSelector.getChild(UIUserSelector.class);
      String returnField = userMemberSelector.getReturnField();
      ((UISelectable)userMemberSelector.getSourceComponent()).doSelect(returnField, userSelector.getSelectedUsers());
      if (userMemberSelector.isUsePopup) {
        UIPopupWindow uiPopup = userMemberSelector.getParent();
        uiPopup.setShow(false);
        UIComponent uicomp = userMemberSelector.getSourceComponent().getParent();
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
        if (!uiPopup.getId().equals("PopupComponent"))
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(
            userMemberSelector.getSourceComponent());
      }
    }
  }
}
