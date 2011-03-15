/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.workflow.impl.bonita;

//import hero.user.ImmutableException;
//import hero.user.UserBase;
//import hero.user.UserBaseException;
//import hero.util.BonitaConfig;


/**
 * eXo User Management
 * This class communicates with the eXo Organization service to retrieve
 * and update users in the Portal.
 *
 * Created by Bull R&D
 * @author Fouad Allaoui
 * May 15, 2006
 */
//public class ExoUserBase implements UserBase {
//
//  public final static String EXO_PORTALNAME = "exo.Portalname";
//
//  private String url;
//  private String user;
//  private String password;
//  private String driver;
//
//  public ExoUserBase() {
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#getUserName(java.lang.String)
//   */
//  public String getUserName(String userId) throws UserBaseException {
//    return userId;
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#getUserInfos(java.lang.String)
//   */
//  public Map getUserInfos(String userId) throws UserBaseException {
//    try {
//      Hashtable uinfos=new Hashtable();
//      boolean checkpoint = false;
//
//      // Lookup the eXo Organization service
//      PortalContainer container = PortalContainer.getInstance();
//      OrganizationService organization = null;
//
//      if (container==null) {
//        BonitaConfig instance = BonitaConfig.getInstance();
//        String portalName = instance.getProperty(EXO_PORTALNAME);
//        container = RootContainer.getInstance().getPortalContainer(portalName);
//        PortalContainer.setInstance(container);
//        checkpoint = true;
//      }
//      organization = (OrganizationService)
//      container.getComponentInstanceOfType(OrganizationService.class);
//
//      // Retrieve all the users contained by the specified group
//      UserHandler userHandler_ = organization.getUserHandler();
//      // Retrive the user match with userId
//      User user = userHandler_.findUserByName(userId);
//
//      uinfos.put("name",userId);
//      if (user.getPassword()!=null)
//        uinfos.put("password",user.getPassword());
//      if (user.getEmail()!=null)
//        uinfos.put("email",user.getEmail());
//        uinfos.put("jabber","TODO");
//
//      if (checkpoint){
//        PortalContainer.setInstance(null);
//        checkpoint=false;
//      }
//
//      return uinfos;
//    } catch (Exception ne) {
//        throw new UserBaseException(ne.getMessage());
//    }
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#getUsers()
//   */
//  public Collection getUsers() throws UserBaseException {
//    try {
//      Collection allUsers = new ArrayList();
//      boolean checkpoint = false;
//
//      // Lookup the eXo Organization service
//      PortalContainer container = PortalContainer.getInstance();
//      OrganizationService organization = null;
//
//      if (container==null) {
//        BonitaConfig instance = BonitaConfig.getInstance();
//        String portalName = instance.getProperty(EXO_PORTALNAME);
//        container = RootContainer.getInstance().getPortalContainer(portalName);
//        PortalContainer.setInstance(container);
//        checkpoint = true;
//      }
//      organization = (OrganizationService)
//      container.getComponentInstanceOfType(OrganizationService.class);
//
//      // Retrieve all the users contained by the specified group
//      UserHandler userHandler_ = organization.getUserHandler();
//      // Get all users and put it in a PageList
//      Query query = new Query() ;
//      PageList users =  userHandler_.findUsers(query) ;
//
//      List list  = users.getPage( 1);
//      for( Object ele : list){
//        Hashtable uinfos=new Hashtable();
//        User u = (User)ele;
//        uinfos.put("name",u.getUserName());
//        allUsers.add(uinfos);
//      }
//
//      if (checkpoint){
//        PortalContainer.setInstance(null);
//        checkpoint=false;
//      }
//
//      return allUsers;
//    } catch (Exception ne) {
//        throw new UserBaseException(ne.getMessage());
//    }
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#handle(java.lang.String)
//   */
//  public boolean handle(String userId) throws UserBaseException {
//    return false;
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#isMutable()
//   */
//  public boolean isMutable() {
//    return true;
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#create(java.lang.String, java.lang.String,
//   * java.lang.String, java.lang.String, java.util.Map)
//   */
//  public void create(String userId, String password, String userName,
//    String userEmail, Map userInfos) throws UserBaseException,
//    ImmutableException {
//    try {
//      boolean checkpoint = false;
//
//      // Lookup the eXo Organization service
//      PortalContainer container = PortalContainer.getInstance();
//      OrganizationService organization = null;
//
//      if (container==null) {
//        BonitaConfig instance = BonitaConfig.getInstance();
//        String portalName = instance.getProperty(EXO_PORTALNAME);
//        container = RootContainer.getInstance().getPortalContainer(portalName);
//        PortalContainer.setInstance(container);
//        checkpoint = true;
//      }
//      organization = (OrganizationService)
//      container.getComponentInstanceOfType(OrganizationService.class);
//
//      // Retrieve all the users contained by the specified group
//      UserHandler userHandler_ = organization.getUserHandler();
//      // Create user
//      User user = organization.getUserHandler().createUserInstance();
//      user.setUserName(userId);
//      user.setPassword(password);
//      user.setFirstName(userName) ;
//      user.setLastName(userEmail) ;
//      user.setEmail(userEmail);
//      organization.getUserHandler().createUser(user, true);
//
//      if (checkpoint){
//        PortalContainer.setInstance(null);
//        checkpoint=false;
//      }
//
//    } catch (Exception ne) {
//      throw new UserBaseException(ne.getMessage());
//    }
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#edit(java.lang.String, java.lang.String,
//   * java.lang.String, java.lang.String, java.util.Map)
//   */
//  public void edit(String userId, String password, String userName,
//    String userEmail, Map userInfos) throws UserBaseException,
//    ImmutableException {
//    try {
//      boolean checkpoint = false;
//
//      // Lookup the eXo Organization service
//      PortalContainer container = PortalContainer.getInstance();
//      OrganizationService organization = null;
//
//      if (container==null) {
//        BonitaConfig instance = BonitaConfig.getInstance();
//        String portalName = instance.getProperty(EXO_PORTALNAME);
//        container = RootContainer.getInstance().getPortalContainer(portalName);
//        PortalContainer.setInstance(container);
//        checkpoint = true;
//      }
//      organization = (OrganizationService)
//      container.getComponentInstanceOfType(OrganizationService.class);
//
//      // Retrieve all the users contained by the specified group
//      UserHandler userHandler_ = organization.getUserHandler();
//      // Retrive the user match with userId
//      User user = userHandler_.findUserByName(userId);
//      user.setUserName(userId);
//      user.setPassword(password);
//      user.setFirstName(userName) ;
//      user.setLastName(userEmail) ;
//      user.setEmail(userEmail);
//      organization.getUserHandler().saveUser(user,true);
//
//      if (checkpoint){
//        PortalContainer.setInstance(null);
//        checkpoint=false;
//      }
//
//    } catch (Exception ne) {
//      throw new UserBaseException(ne.getMessage());
//    }
//  }
//
//  /* (non-Javadoc)
//   * @see hero.util.user.UserBase#deleteUser(java.lang.String)
//   */
//  public void deleteUser(String userId) throws UserBaseException, ImmutableException {
//    try {
//      boolean checkpoint = false;
//
//      // Lookup the eXo Organization service
//      PortalContainer container = PortalContainer.getInstance();
//      OrganizationService organization = null;
//
//      if (container==null) {
//        BonitaConfig instance = BonitaConfig.getInstance();
//        String portalName = instance.getProperty(EXO_PORTALNAME);
//        container = RootContainer.getInstance().getPortalContainer(portalName);
//        PortalContainer.setInstance(container);
//        checkpoint = true;
//      }
//      organization = (OrganizationService)
//      container.getComponentInstanceOfType(OrganizationService.class);
//      organization.getUserHandler().removeUser(userId, true);
//
//      if (checkpoint){
//        PortalContainer.setInstance(null);
//        checkpoint=false;
//      }
//
//    } catch (Exception ne) {
//      throw new UserBaseException(ne.getMessage());
//    }
//  }
//}
