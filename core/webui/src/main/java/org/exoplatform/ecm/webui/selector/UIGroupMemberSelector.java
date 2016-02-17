/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 10, 2008
 */

@ComponentConfigs( {
  @ComponentConfig(
      template = "classpath:groovy/ecm/webui/UIMemberSelector.gtmpl",
      events = {
        @EventConfig(listeners = UIGroupMemberSelector.ChangeNodeActionListener.class),
        @EventConfig(listeners = UIGroupMemberSelector.SelectMembershipActionListener.class),
        @EventConfig(listeners = UIGroupMemberSelector.SelectPathActionListener.class),
        @EventConfig(listeners = UIGroupMemberSelector.AddAnyPermissionActionListener.class)
      }),
  @ComponentConfig(
      type = UITree.class,
      id = "UITreeMembershipSelector",
      template = "system:/groovy/webui/core/UITree.gtmpl",
      events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)),
  @ComponentConfig(
      type = UIBreadcumbs.class,
      id = "BreadcumbMembershipSelector",
      template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
      events = @EventConfig(listeners = UIBreadcumbs.SelectPathActionListener.class))
  }
)

public class UIGroupMemberSelector extends UIContainer implements ComponentSelector{

  /** The Constant defaultValue. */
  final static public String defaultValue    = "/admin";

  /** The ui component. */
  private UIComponent        uiComponent;

  /** The return field name. */
  private String             returnFieldName = null;

  /** The is selected membership. */
  private boolean            isSelectedMembership = true;

  /** The is selected user. */
  private boolean            isSelectedUser;

  /** The is use popup. */
  private boolean             isUsePopup      = true;

  /** Show/hide Add AnyPermission form   */
  private boolean             isShowAnyPermission = true;

  private Group selectGroup_;

  private List<String> listMemberhip;

  public UIGroupMemberSelector() throws Exception {
    addChild(UIAnyPermission.class, null, "UIQueriesAnyPermission");
    UIBreadcumbs uiBreadcumbs = addChild(UIBreadcumbs.class, "BreadcumbMembershipSelector", "BreadcumbMembershipSelector") ;
    UITree tree = addChild(UITree.class, "UITreeMembershipSelector", "TreeMembershipSelector");
    OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
    Collection<?> sibblingsGroup = service.getGroupHandler().findGroups(null);

    Collection<?> collection = service.getMembershipTypeHandler().findMembershipTypes();
    listMemberhip  = new ArrayList<String>(5);
    for(Object obj : collection){
      listMemberhip.add(((MembershipType)obj).getName());
    }
    if (!listMemberhip.contains("*")) listMemberhip.add("*");
    Collections.sort(listMemberhip);
    tree.setSibbling((List)sibblingsGroup);
    tree.setIcon("GroupAdminIcon");
    tree.setSelectedIcon("PortalIcon");
    tree.setBeanIdField("id");
    tree.setBeanLabelField("label");
    uiBreadcumbs.setBreadcumbsStyle("UIExplorerHistoryPath") ;
  }

  public Group getCurrentGroup() { return selectGroup_ ; }

  public List<String> getListMemberhip() { return listMemberhip; }

  public void changeGroup(String groupId) throws Exception {
    OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
    UIBreadcumbs uiBreadcumb = getChild(UIBreadcumbs.class);
    uiBreadcumb.setPath(getPath(null, groupId)) ;

    UITree tree = getChild(UITree.class);
    Collection<?> sibblingGroup;

    if(groupId == null) {
      sibblingGroup = service.getGroupHandler().findGroups(null);
      tree.setSibbling((List)sibblingGroup);
      tree.setChildren(null);
      tree.setSelected(null);
      selectGroup_ = null;
      return;
    }

    selectGroup_ = service.getGroupHandler().findGroupById(groupId);
    String parentGroupId = null;
    if(selectGroup_ != null) parentGroupId = selectGroup_.getParentId();
    Group parentGroup = null ;
    if(parentGroupId != null) parentGroup = service.getGroupHandler().findGroupById(parentGroupId);

    Collection childrenGroup = service.getGroupHandler().findGroups(selectGroup_);
    sibblingGroup = service.getGroupHandler().findGroups(parentGroup);

    tree.setSibbling((List)sibblingGroup);
    tree.setChildren((List)childrenGroup);
    tree.setSelected(selectGroup_);
    tree.setParentSelected(parentGroup);
  }

  private List<LocalPath> getPath(List<LocalPath> list, String id) throws Exception {
    if(list == null) list = new ArrayList<LocalPath>(5);
    if(id == null) return list;
    OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
    Group group = service.getGroupHandler().findGroupById(id);
    if(group == null) return list;
    list.add(0, new LocalPath(group.getId(), group.getLabel()));
    getPath(list, group.getParentId());
    return list ;
  }

  @SuppressWarnings("unchecked")
  public List<String> getListGroup() throws Exception {
    OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
    List<String> listGroup = new ArrayList<String>();
    if(getCurrentGroup() == null) return null;
    Collection<Group> groups = service.getGroupHandler().findGroups(getCurrentGroup());
    if(groups.size() > 0) {
      for (Object child : groups) {
        Group childGroup = (Group)child;
        listGroup.add(childGroup.getId()) ;
      }
    }
    return listGroup;

  }
  /**
   * Gets the return component.
   *
   * @return the return component
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
    return returnFieldName;
  }

  public void setIsUsePopup(boolean isUsePopup) { this.isUsePopup = isUsePopup; }

  public boolean isUsePopup() { return isUsePopup; }

  /**
   * Sets the selected user.
   *
   * @param bool the new selected user
   */
  public void setSelectedUser(boolean bool) {
    isSelectedUser = bool;
  }

  /**
   * Checks if is selected user.
   *
   * @return true, if is selected user
   */
  public boolean isSelectedUser() {
    return isSelectedUser;
  }

  /**
   * Sets the selected membership.
   *
   * @param bool the new selected membership
   */
  public void setSelectedMembership(boolean bool) {
    isSelectedMembership = bool;
  }

  /**
   * Checks if is selected membership.
   *
   * @return true, if is selected membership
   */
  public boolean isSelectedMembership() {
    return isSelectedMembership;
  }

  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    uiComponent = uicomponent;
    if (initParams == null || initParams.length == 0)
      return;
    for (int i = 0; i < initParams.length; i++) {
      if (initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=");
        returnFieldName = array[1];
        break;
      }
      returnFieldName = initParams[0];
    }
  }

  public String event(String name, String beanId) throws Exception {
    UIForm uiForm = getAncestorOfType(UIForm.class) ;
    if(uiForm != null) return uiForm.event(name, getId(), beanId);
    return super.event(name, beanId);
  }

  static  public class ChangeNodeActionListener extends EventListener<UITree> {
    public void execute(Event<UITree> event) throws Exception {
      UIGroupMemberSelector uiGroupMemberSelector = event.getSource().getParent();
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiGroupMemberSelector.changeGroup(groupId);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupMemberSelector);
    }
  }

  static  public class SelectMembershipActionListener extends EventListener<UIGroupMemberSelector> {
    public void execute(Event<UIGroupMemberSelector> event) throws Exception {
      UIGroupMemberSelector uiGroupMemberSelector = event.getSource();
      if (uiGroupMemberSelector.getCurrentGroup() == null)
        return;
      String groupId = uiGroupMemberSelector.getCurrentGroup().getId();
      String permission = event.getRequestContext().getRequestParameter(OBJECTID);
      String value = "";
      if(uiGroupMemberSelector.isSelectedUser()) {
        value = permission;
      } else {
        value = permission + ":" + groupId;
      }
      String returnField = uiGroupMemberSelector.getReturnField();
      ((UISelectable) uiGroupMemberSelector.getSourceComponent()).doSelect(returnField, value);
      if (uiGroupMemberSelector.isUsePopup) {
        UIPopupWindow uiPopup = uiGroupMemberSelector.getParent();
        uiPopup.setShow(false);
        UIComponent uicomp = uiGroupMemberSelector.getSourceComponent().getParent();
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
        if (!uiPopup.getId().equals("PopupComponent"))
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(
            uiGroupMemberSelector.getSourceComponent());
      }
    }
  }

  /**
   * The listener interface for receiving selectPathAction events. The class
   * that is interested in processing a selectPathAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addSelectPathActionListener</code> method. When
   * the selectPathAction event occurs, that object's appropriate
   * method is invoked.
   */
  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiBreadcumbs.getParent();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiBreadcumbs.setSelectPath(objectId);
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId();
      uiGroupMemberSelector.changeGroup(selectGroupId);
      if (uiGroupMemberSelector.isUsePopup) {
        UIPopupWindow uiPopup = uiBreadcumbs.getAncestorOfType(UIPopupWindow.class);
        uiPopup.setShow(true);
        uiPopup.setShowMask(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupMemberSelector);
    }
  }

  static public class AddAnyPermissionActionListener extends EventListener<UIAnyPermission> {
    public void execute(Event<UIAnyPermission> event) throws Exception {
      UIAnyPermission uiAnyPermission = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiAnyPermission.getParent();
      String returnField = uiGroupMemberSelector.getReturnField();
      String value = IdentityConstants.ANY;
      ((UISelectable)uiGroupMemberSelector.getSourceComponent()).doSelect(returnField, value);
      if (uiGroupMemberSelector.isUsePopup()) {
        UIPopupWindow uiPopup = uiGroupMemberSelector.getParent();
        uiPopup.setShow(false);
        UIComponent uicomp = uiGroupMemberSelector.getSourceComponent().getParent();
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
        if (!uiPopup.getId().equals("PopupComponent"))
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(
            uiGroupMemberSelector.getSourceComponent());
      }
    }
  }

  /**
   * Check show/hide form to set any permission
   * @return
   */
  public boolean isShowAnyPermission() {
    return isShowAnyPermission;
  }

  /**
   * Set show/hide any permission form
   * @param isShowAnyPermission
   * isShowAnyPermission =  true: Show form  <br>
   * isShowAnyPermission =  false: Hide form
   */
  public void setShowAnyPermission(boolean isShowAnyPermission) {
    this.isShowAnyPermission = isShowAnyPermission;
  }
}
