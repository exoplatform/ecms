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
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterUserInfor;
import org.exoplatform.services.wcm.newsletter.config.NewsletterUserConfig;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterManageUserHandler;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * ha.mai@exoplatform.com
 * Jun 10, 2009
 */

@SuppressWarnings("deprecation")
@ComponentConfig(
    //lifecycle = UIFormLifecycle.class ,
    template = "app:/groovy/webui/newsletter/NewsletterManager/UIManagerUsers.gtmpl",
    events = {
      @EventConfig(listeners = UIManagerUsers.UnBanUserActionListener.class),
      @EventConfig(listeners = UIManagerUsers.BanUserActionListener.class),
      @EventConfig(listeners = UIManagerUsers.DeleteUserActionListener.class),
      @EventConfig(listeners = UIManagerUsers.AddAdministratorActionListener.class),
      @EventConfig(listeners = UIManagerUsers.DeleteAdministratorActionListener.class),
      @EventConfig(listeners = UIManagerUsers.CloseActionListener.class)
    }
)
public class UIManagerUsers extends UITabPane {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UIManagerUsers.class);
  
  /** The TITL e_. */
  private String[] TITLE_  = {"Mail", "isBanned"};

  /** The ACTION s_. */
  private String[] ACTIONS_ = {"BanUser", "UnBanUser", "DeleteUser"};

  /** The MEMBE r_ titl e_. */
  private String[] MEMBER_TITLE_ = {"UserName", "FirstName", "LastName", "Email", "Role"};

  /** The MEMBE r_ action s_. */
  private String[] MEMBER_ACTIONS_ = {"AddAdministrator", "DeleteAdministrator"};

  /** The manager user handler. */
  private NewsletterManageUserHandler managerUserHandler = null;

  /** The category name. */
  private String categoryName ;

  /** The subscription name. */
  private String subscriptionName;

  /** The UIGRI d_ manage r_ users. */
  private String UIGRID_MANAGER_USERS = "UIManagerUsers";

  /** The UIGRI d_ manage r_ moderator. */
  private String UIGRID_MANAGER_MODERATOR = "UIManagerModerator";

  /** The is view tab. */
  public boolean isViewTab = false;

  /** The permissions. */
  private String[] permissions ;

  /**
   * Gets the list public user.
   *
   * @return the list public user
   */
  @SuppressWarnings("unchecked")
  public void getListPublicUser() {
    try {
      UIGrid uiGrid = getChildById(UIGRID_MANAGER_USERS);
      List<NewsletterUserConfig> userList = managerUserHandler.getUsers(WCMCoreUtils.getUserSessionProvider(),
                                                                        NewsLetterUtil.getPortalName(),
                                                                        categoryName,
                                                                        subscriptionName);
      ListAccess<NewsletterUserConfig> userConfigList = new ListAccessImpl<NewsletterUserConfig>(NewsletterUserConfig.class,
                                                                                                 userList);
      LazyPageList<NewsletterUserConfig> dataPageList = new LazyPageList<NewsletterUserConfig>(userConfigList,
                                                                                               5);
      uiGrid.getUIPageIterator().setPageList(dataPageList);
    } catch (Exception ex) {
      Utils.createPopupMessage(this,
                               "UIManagerUsers.msg.get-list-users",
                               null,
                               ApplicationMessage.ERROR);
    }
  }

  private void updateListUserInfor(UserHandler userHandler,
                                   List<NewsletterUserInfor> listUserInfor,
                                   List<String> listUser,
                                   String role) {
    NewsletterUserInfor userInfor;
    User user;
    for(String userName : listUser){
      try{
        user = (User) userHandler.findUserByName(userName);
        userInfor = new NewsletterUserInfor();
        userInfor.setEmail(user.getEmail());
        userInfor.setFirstName(user.getFirstName());
        userInfor.setLastName(user.getLastName());
        userInfor.setUserName(user.getUserName());
        userInfor.setRole(role);
        listUserInfor.add(userInfor);
      }catch(Exception ex){
        if (LOG.isWarnEnabled()) {
          LOG.warn(ex.getMessage());
        }
      }
    }
  }

  private void addArrayToList(List<String> list1, String[] list2){
    for(String str : list2){
      if(!list1.contains(str)) list1.add(str);
    }
  }

  private List<String> getAllAccesPermissions() throws Exception{
    UserPortalConfigService userService = (UserPortalConfigService) this.getApplicationComponent(UserPortalConfigService.class);
    Page page = userService.getPage(Util.getUIPortal().getSelectedUserNode().getPageRef());
    List<String> userGroupMembership = new ArrayList<String>();
    userGroupMembership.add(page.getOwnerId());
    addArrayToList(userGroupMembership, new String[]{page.getEditPermission()});
    addArrayToList(userGroupMembership, page.getAccessPermissions());
    return getAllUsersFromGroupMemebers(userGroupMembership);
  }

  private List<String> getAllEditPermission() throws Exception {
    List<String> userGroupMembership = new ArrayList<String>();
    userGroupMembership.add(WCMCoreUtils.getService(UserACL.class).getSuperUser());
    return getAllUsersFromGroupMemebers(userGroupMembership);
  }

  @SuppressWarnings("unchecked")
  private List<String> getAllUsersFromGroupMemebers(List<String> userGroupMembership) throws Exception{
    List<String> users = new ArrayList<String> () ;
    if (userGroupMembership == null || userGroupMembership.size() <= 0)
      return users;
    OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
    for (String str : userGroupMembership) {
      str = str.trim();
      if (str.indexOf("/") >= 0) {
        if (str.indexOf(":") >= 0) { // membership
          String[] array = str.split(":");
          List<User> userList = organizationService.getUserHandler()
                                                   .findUsersByGroup(array[1])
                                                   .getAll();
          if (array[0].length() > 1) {
            for (User user : userList) {
              if (!users.contains(user.getUserName())) {
                Collection<Membership> memberships = organizationService.getMembershipHandler()
                                                                        .findMembershipsByUser(user.getUserName());
                for (Membership member : memberships) {
                  if (member.getMembershipType().equals(array[0])) {
                    users.add(user.getUserName());
                    break;
                  }
                }
              }
            }
          } else {
            if (array[0].charAt(0) == 42) {
              for (User user : userList) {
                if (!users.contains(user.getUserName())) {
                  users.add(user.getUserName());
                }
              }
            }
          }
        } else { // group
          List<User> userList = organizationService.getUserHandler().findUsersByGroup(str).getAll();
          for (User user : userList) {
            if (!users.contains(user.getUserName())) {
              users.add(user.getUserName());
            }
          }
        }
      } else {// user
        if (!users.contains(str)) {
          users.add(str);
        }
      }
    }
    return users ;
  }

  /**
   * Update list user.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private void updateListUser() throws Exception{
    // get all administrator of newsletter
    List<String> listUserAccess = this.getAllAccesPermissions();
    List<String> listUserEdit = this.getAllEditPermission();
    List<String> listModerator = new ArrayList<String>();
    List<String> listRedactor = new ArrayList<String>();
    String portalName = NewsLetterUtil.getPortalName();
    LivePortalManagerService livePortalManagerService = getApplicationComponent(LivePortalManagerService.class);
    Node portalNode = livePortalManagerService.getLivePortal(Utils.getSessionProvider(), portalName);
    Session session = portalNode.getSession();
    // get list of moderator
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    NewsletterCategoryHandler categoryHandler = newsletterManagerService.getCategoryHandler();
    for (NewsletterCategoryConfig categoryConfig : categoryHandler.getListCategories(portalName,
                                                                                     Utils.getSessionProvider())) {
      for (String str : categoryConfig.getModerator().split(",")) {
        if (!listModerator.contains(str)) {
          listModerator.add(str);
        }
      }
    }
    listModerator = getAllUsersFromGroupMemebers(listModerator);

    // get list redactor from subscriptions
    listRedactor.addAll(getAllUsersFromGroupMemebers(NewsletterConstant.getAllRedactor(NewsLetterUtil.getPortalName(),
                                                                                       session)));

    // Remove all user who is administrator from moderators and accesspermission
    for (String uId : managerUserHandler.getAllAdministrator(Utils.getSessionProvider(),
                                                             NewsLetterUtil.getPortalName())) {
      if (!listUserEdit.contains(uId))
        listUserEdit.add(uId);
    }

    // Filter permission of users
    listModerator.removeAll(listUserEdit);
    listRedactor.removeAll(listUserEdit);
    listRedactor.removeAll(listModerator);
    listUserAccess.removeAll(listUserEdit);
    listUserAccess.removeAll(listModerator);
    listUserAccess.removeAll(listRedactor);

    // Set permission for user to view in UI
    List<NewsletterUserInfor> userInfors = new ArrayList<NewsletterUserInfor>();
    UserHandler userHandler = getApplicationComponent(OrganizationService.class).getUserHandler();
    updateListUserInfor(userHandler, userInfors, listUserEdit, permissions[0]);
    updateListUserInfor(userHandler, userInfors, listModerator, permissions[1]);
    updateListUserInfor(userHandler, userInfors, listRedactor, permissions[2]);
    updateListUserInfor(userHandler, userInfors, listUserAccess, permissions[3]);

    // set all user into grid
    ListAccess<NewsletterUserInfor> userInfoList = new ListAccessImpl<NewsletterUserInfor>(NewsletterUserInfor.class,
                                                                                           userInfors);
    LazyPageList<NewsletterUserInfor> dataPageList = new LazyPageList<NewsletterUserInfor>(userInfoList,
                                                                                          5);
    UIGrid uiGrid = this.getChildById(UIGRID_MANAGER_MODERATOR);
    UIPageIterator uiIterator_ = uiGrid.getUIPageIterator();
    uiIterator_.setPageList(null);
    uiIterator_.setPageList(dataPageList);

    this.setSelectedTab(UIGRID_MANAGER_USERS);
  }

  /**
   * Instantiates a new uI manager users.
   *
   * @throws Exception the exception
   */
  public UIManagerUsers() throws Exception{
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    managerUserHandler = newsletterManagerService.getManageUserHandler();

    permissions = new String[]{"Administrator","Moderator", "Redactor", "User"};
    // add public user grid
    UIGrid uiGrid = createUIComponent(UIGrid.class, null, UIGRID_MANAGER_USERS);
    uiGrid.getUIPageIterator().setId("UsersIterator");
    uiGrid.configure("Mail", TITLE_, ACTIONS_);
    addChild(uiGrid);
  }

  /**
   * Sets the infor.
   *
   * @param categoryName the category name
   * @param subscriptionName the subscription name
   */
  public void setInfor(String categoryName, String subscriptionName) {
    this.categoryName = categoryName;
    this.subscriptionName = subscriptionName;
    if (categoryName == null) {
      // add public user grid
      try {
        // set all user into grid
        UIGrid uiGrid = createUIComponent(UIGrid.class, null, UIGRID_MANAGER_MODERATOR);
        UIPageIterator uiIterator_ = uiGrid.getUIPageIterator();
        uiIterator_.setId("ModeratorsIterator");
        uiGrid.configure("UserName", MEMBER_TITLE_, MEMBER_ACTIONS_);
        addChild(uiGrid);
        isViewTab = true;
        this.setSelectedTab(UIGRID_MANAGER_USERS);

        updateListUser();
      } catch (Exception ex) {
        Utils.createPopupMessage(this,
                                 "UIManagerUsers.msg.set-infor-users",
                                 null,
                                 ApplicationMessage.ERROR);
      }
    }
  }

  /**
   * The listener interface for receiving unBanUserAction events.
   * The class that is interested in processing a unBanUserAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addUnBanUserActionListener<code> method. When
   * the unBanUserAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see UnBanUserActionEvent
   */
  static  public class UnBanUserActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      String email = event.getRequestContext().getRequestParameter(OBJECTID);
      managerUsers.managerUserHandler.changeBanStatus(Utils.getSessionProvider(),
                                                      NewsLetterUtil.getPortalName(),
                                                      email,
                                                      false);
      event.getRequestContext().addUIComponentToUpdateByAjax(managerUsers);
    }
  }

  /**
   * The listener interface for receiving banUserAction events.
   * The class that is interested in processing a banUserAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addBanUserActionListener<code> method. When
   * the banUserAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see BanUserActionEvent
   */
  static  public class BanUserActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      String email = event.getRequestContext().getRequestParameter(OBJECTID);
      managerUsers.managerUserHandler.changeBanStatus(Utils.getSessionProvider(), NewsLetterUtil.getPortalName(), email, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(managerUsers) ;
    }
  }

  /**
   * The listener interface for receiving deleteUserAction events.
   * The class that is interested in processing a deleteUserAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteUserActionListener<code> method. When
   * the deleteUserAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DeleteUserActionEvent
   */
  static  public class DeleteUserActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      String email = event.getRequestContext().getRequestParameter(OBJECTID);
      managerUsers.managerUserHandler.delete(Utils.getSessionProvider(), NewsLetterUtil.getPortalName(), email);
      event.getRequestContext().addUIComponentToUpdateByAjax(managerUsers) ;
    }
  }

  /**
   * The listener interface for receiving addAdministratorAction events.
   * The class that is interested in processing a addAdministratorAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddAdministratorActionListener<code> method. When
   * the addAdministratorAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddAdministratorActionEvent
   */
  static  public class AddAdministratorActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      managerUsers.managerUserHandler.addAdministrator(Utils.getSessionProvider(),
                                                       NewsLetterUtil.getPortalName(),
                                                       userId);
      managerUsers.updateListUser();
      event.getRequestContext()
           .addUIComponentToUpdateByAjax(managerUsers.getChildById(managerUsers.UIGRID_MANAGER_MODERATOR));
      event.getRequestContext().addUIComponentToUpdateByAjax(managerUsers);
    }
  }

  /**
   * The listener interface for receiving deleteAdministratorAction events.
   * The class that is interested in processing a deleteAdministratorAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteAdministratorActionListener<code> method. When
   * the deleteAdministratorAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DeleteAdministratorActionEvent
   */
  static  public class DeleteAdministratorActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      String superUserId = WCMCoreUtils.getService(UserACL.class).getSuperUser();
      if (superUserId != null && superUserId.equalsIgnoreCase(userId)) {
        Utils.createPopupMessage(managerUsers,
                                 "UIManagerUsers.msg.remove-admin-role-of-root",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      }
      managerUsers.managerUserHandler.deleteUserAddministrator(Utils.getSessionProvider(),
                                                               NewsLetterUtil.getPortalName(),
                                                               userId);
      managerUsers.updateListUser();
      event.getRequestContext()
           .addUIComponentToUpdateByAjax(managerUsers.getChildById(managerUsers.UIGRID_MANAGER_MODERATOR));
      event.getRequestContext().addUIComponentToUpdateByAjax(managerUsers);
    }
  }

  /**
   * The listener interface for receiving closeAction events. The class that is
   * interested in processing a closeAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  static  public class CloseActionListener extends EventListener<UIManagerUsers> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIManagerUsers> event) throws Exception {
      UIManagerUsers managerUsers = event.getSource();
      Utils.closePopupWindow(managerUsers, UINewsletterConstant.MANAGER_USERS_POPUP_WINDOW);
    }
  }
}
