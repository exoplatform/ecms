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
package org.exoplatform.services.cms.drives.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * Feb 27, 2006
 */
public class ManageDriveServiceImpl implements ManageDriveService, Startable {

  /**
   * Name of property WORKSPACE
   */
  private static String WORKSPACE = "exo:workspace".intern() ;
  
  private static String ALL_DRIVES_CACHED = "allDrives".intern();
  
  private static String ALL_DRIVES_CACHED_BY_ROLES = "_allDrivesByRoles".intern();
  
  private static String ALL_MAIN_CACHED_DRIVE = "_mainDrives".intern();
  
  private static String ALL_PERSONAL_CACHED_DRIVE = "_personalDrives".intern();
  
  private static String ALL_GROUP_CACHED_DRIVES = "_groupDrives".intern();
  /**
   * Name of property PERMISSIONS
   */
  private static String PERMISSIONS = "exo:accessPermissions".intern() ;
  
  /**
   * Name of property VIEWS
   */
  private static String VIEWS = "exo:views".intern() ;
  
  /**
   * Name of property ICON
   */
  private static String ICON = "exo:icon".intern() ;
  
  /**
   * Name of property PATH
   */
  private static String PATH = "exo:path".intern() ;
  
  /**
   * Name of property VIEW_REFERENCES
   */
  private static String VIEW_REFERENCES = "exo:viewPreferences".intern() ;
  
  /**
   * Name of property VIEW_NON_DOCUMENT
   */
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument".intern() ;
  
  /**
   * Name of property VIEW_SIDEBAR
   */
  private static String VIEW_SIDEBAR = "exo:viewSideBar".intern() ;
  
  /**
   * Name of property SHOW_HIDDEN_NODE
   */
  private static String SHOW_HIDDEN_NODE = "exo:showHiddenNode".intern() ;
  
  /**
   *  Name of property ALLOW_CREATE_FOLDER
   */
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolders".intern() ;

  /**
   * List of ManageDrivePlugin
   */
  private List<ManageDrivePlugin> drivePlugins_  = new ArrayList<ManageDrivePlugin> ();
  
  /**
   * RepositoryService object
   */
  private RepositoryService repositoryService_ ;
  
  /**
   * Path to drive home directory
   */
  private String baseDrivePath_ ;
  
  /**
   * NodeHierarchyCreator object 
   */
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  
  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(ManageDriveServiceImpl.class);
  
  /**
   * Keep the drives of repository
   */
  private ExoCache drivesCache_ ;

  /**
   * Constructor method
   * Construcs RepositoryService, NodeHierarchyCreator, baseDrivePath_
   * @param jcrService
   * @param nodeHierarchyCreator
   * @throws Exception
   */
  public ManageDriveServiceImpl(RepositoryService jcrService, 
      NodeHierarchyCreator nodeHierarchyCreator, DMSConfiguration dmsConfiguration, 
      CacheService caService) throws Exception{
    repositoryService_ = jcrService ;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
    baseDrivePath_ = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_DRIVES_PATH);
    dmsConfiguration_ = dmsConfiguration;
    drivesCache_ = caService.getCacheInstance(ManageDriveService.class.getName());
  }

  /**
   * Implemented method from Startable class
   * init all ManageDrivePlugin
   */
  public void start() {
    try{
      for(ManageDrivePlugin plugin : drivePlugins_) {
        plugin.init() ;
      }
    }catch(Exception e) {      
    }    
  }

  /**
   * Implemented method from Startable class
   */
  public void stop() { }

  /**
   * Init drive node with specified repository
   */
  public void init(String repository) throws Exception {
    for(ManageDrivePlugin plugin : drivePlugins_) {
      plugin.init(repository) ;
    }
  }

  /**
   * Add new ManageDrivePlugin to drivePlugins_
   * @param drivePlugin
   */
  public void setManageDrivePlugin(ManageDrivePlugin drivePlugin) {
    drivePlugins_.add(drivePlugin) ;
  }    
  
  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public List<DriveData> getAllDrives(String repository) throws Exception {
    List<DriveData> allDrives = (List<DriveData>) drivesCache_.get(ALL_DRIVES_CACHED);
    if ((allDrives != null) && (allDrives.size() > 0)) return allDrives;
    Session session = getSession(repository) ;    
    Node driveHome = (Node)session.getItem(baseDrivePath_);
    NodeIterator itr = driveHome.getNodes() ;
    List<DriveData> driveList = new ArrayList<DriveData>() ;
    DriveData data = null;
    Node drive = null;
    while(itr.hasNext()) {
      data = new DriveData() ;
      drive = itr.nextNode() ;
      data.setName(drive.getName()) ;
      data.setWorkspace(drive.getProperty(WORKSPACE).getString()) ;
      data.setHomePath(drive.getProperty(PATH).getString()) ;
      data.setPermissions(drive.getProperty(PERMISSIONS).getString()) ;
      data.setViews(drive.getProperty(VIEWS).getString()) ;
      data.setIcon(drive.getProperty(ICON).getString()) ;
      data.setViewPreferences(Boolean.parseBoolean(drive.getProperty(VIEW_REFERENCES).getString())) ;
      data.setViewNonDocument(Boolean.parseBoolean(drive.getProperty(VIEW_NON_DOCUMENT).getString())) ;
      data.setViewSideBar(Boolean.parseBoolean(drive.getProperty(VIEW_SIDEBAR).getString())) ;
      data.setShowHiddenNode(Boolean.parseBoolean(drive.getProperty(SHOW_HIDDEN_NODE).getString())) ;
      data.setAllowCreateFolders(drive.getProperty(ALLOW_CREATE_FOLDER).getString()) ;
      driveList.add(data) ;
    }
    drivesCache_.put(ALL_DRIVES_CACHED, driveList);
    session.logout();
    return driveList ;    
  }

  /**
   * {@inheritDoc}
   */
  public DriveData getDriveByName(String name, String repository) throws Exception{  
    Session session = getSession(repository) ;    
    Node driveHome = (Node)session.getItem(baseDrivePath_);
    if (driveHome.hasNode(name)){
      Node drive = driveHome.getNode(name) ;
      DriveData data = new DriveData() ;
      data.setName(drive.getName()) ;
      data.setWorkspace(drive.getProperty(WORKSPACE).getString()) ;
      data.setHomePath(drive.getProperty(PATH).getString()) ;
      data.setPermissions(drive.getProperty(PERMISSIONS).getString()) ;
      data.setViews(drive.getProperty(VIEWS).getString()) ;
      data.setIcon(drive.getProperty(ICON).getString()) ;
      data.setViewPreferences(Boolean.parseBoolean(drive.getProperty(VIEW_REFERENCES).getString())) ;
      data.setViewNonDocument(Boolean.parseBoolean(drive.getProperty(VIEW_NON_DOCUMENT).getString())) ;
      data.setViewSideBar(Boolean.parseBoolean(drive.getProperty(VIEW_SIDEBAR).getString())) ;
      data.setShowHiddenNode(Boolean.parseBoolean(drive.getProperty(SHOW_HIDDEN_NODE).getString())) ;
      data.setAllowCreateFolders(drive.getProperty(ALLOW_CREATE_FOLDER).getString()) ;
      session.logout();
      return data ;
    }    
    session.logout();
    return null ;    
  }

  /**
   * {@inheritDoc}
   */
  public void addDrive(String name, String workspace, String permissions, String homePath, 
      String views, String icon, boolean viewReferences, boolean viewNonDocument, 
      boolean viewSideBar, boolean showHiddenNode, String repository, String allowCreateFolder) throws Exception {    
    Session session = getSession(repository);
    Node driveHome = (Node)session.getItem(baseDrivePath_) ;
    if (!driveHome.hasNode(name)){
      Node driveNode = driveHome.addNode(name, "exo:drive");
      driveNode.setProperty(WORKSPACE, workspace) ;
      driveNode.setProperty(PERMISSIONS, permissions) ;
      driveNode.setProperty(PATH, homePath) ;      
      driveNode.setProperty(VIEWS, views) ;
      driveNode.setProperty(ICON, icon) ;
      driveNode.setProperty(VIEW_REFERENCES, Boolean.toString(viewReferences)) ;
      driveNode.setProperty(VIEW_NON_DOCUMENT, Boolean.toString(viewNonDocument)) ;
      driveNode.setProperty(VIEW_SIDEBAR, Boolean.toString(viewSideBar)) ;
      driveNode.setProperty(ALLOW_CREATE_FOLDER, allowCreateFolder) ;
      driveNode.setProperty(SHOW_HIDDEN_NODE, Boolean.toString(showHiddenNode)) ;
      driveHome.save() ;
    } else{
      Node driveNode = driveHome.getNode(name);
      driveNode.setProperty(WORKSPACE, workspace) ;
      driveNode.setProperty(PERMISSIONS, permissions) ;
      driveNode.setProperty(PATH, homePath) ;      
      driveNode.setProperty(VIEWS, views) ;
      driveNode.setProperty(ICON, icon) ;
      driveNode.setProperty(VIEW_REFERENCES, Boolean.toString(viewReferences)) ;
      driveNode.setProperty(VIEW_NON_DOCUMENT, Boolean.toString(viewNonDocument)) ;
      driveNode.setProperty(VIEW_SIDEBAR, Boolean.toString(viewSideBar)) ;
      driveNode.setProperty(ALLOW_CREATE_FOLDER, allowCreateFolder) ;
      driveNode.setProperty(SHOW_HIDDEN_NODE, Boolean.toString(showHiddenNode)) ;
      driveNode.save() ;
    }
    drivesCache_.clearCache();
    session.save() ;
    session.logout();
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getAllDriveByPermission(String permission, String repository) throws Exception {
    List<DriveData> driveByPermission = new ArrayList<DriveData>() ;
    try{
      List<DriveData> driveList = getAllDrives(repository);    
      for(DriveData drive : driveList) {
        if(drive.hasPermission(drive.getAllPermissions(), permission)){
          driveByPermission.add(drive) ;
        } 
      }
      if(getDriveByName("Private", repository) != null) {
        driveByPermission.add(getDriveByName("Private", repository)) ;
      }
    } catch(Exception e) {
      LOG.error("Unexpected error", e);
    }    
    return driveByPermission ;
  }

  /**
   * {@inheritDoc}
   */
  public void removeDrive(String driveName, String repository) throws Exception {
    Session session = getSession(repository);
    Node driveHome = (Node)session.getItem(baseDrivePath_) ;
    if(driveHome.hasNode(driveName)){
      driveHome.getNode(driveName).remove() ;
      driveHome.save() ;
    }
    drivesCache_.clearCache();
    session.logout();
  } 

  /**
   * Get session from repository in SystemWorkspace name
   * @param repository    repository name
   * @return session
   * @throws Exception
   */
  private Session getSession(String repository) throws Exception{    
    ManageableRepository manaRepository = repositoryService_.getRepository(repository) ;
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return manaRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;          
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUsedView(String viewName, String repository) throws Exception {
    Session session = getSession(repository);      
    Node driveHome = (Node)session.getItem(baseDrivePath_) ;
    NodeIterator iter = driveHome.getNodes() ;
    while(iter.hasNext()) {
      Node drive = iter.nextNode() ;
      String[] views = drive.getProperty("exo:views").getString().split(",") ;
      for(String view : views) {
        if(viewName.equals(view)) {
          session.logout();
          return true ;
        }
      }
    }
    session.logout();
    return false;
  }

  @SuppressWarnings("unchecked")
  public List<DriveData> getDriveByUserRoles(String repository, String userId, List<String> userRoles) throws Exception {
    Object drivesByRoles = drivesCache_.get(userId + ALL_DRIVES_CACHED_BY_ROLES);
    if(drivesByRoles != null) return (List<DriveData>) drivesByRoles;
    List<DriveData> driveList = new ArrayList<DriveData>();
    if (userId != null) {
      // We will improve ManageDrive service to allow getAllDriveByUser
      for (DriveData drive : getAllDrives(repository)) {
        String[] allPermission = drive.getAllPermissions();
        boolean flag = false;
        for (String permission : allPermission) {
          if (permission.equalsIgnoreCase("${userId}")) {
            if(!driveList.contains(drive)) driveList.add(drive);
            flag = true;
            break;
          }
          if (permission.equalsIgnoreCase("*")) {
            if(!driveList.contains(drive)) driveList.add(drive);
            flag = true;
            break;
          }
          if (flag)
            continue;
          for (String rolse : userRoles) {
            if (drive.hasPermission(allPermission, rolse)) {
              if(!driveList.contains(drive)) driveList.add(drive);
              break;
            }
          }
        }
      }
    } else {
      for (DriveData drive : getAllDrives(repository)) {
        String[] allPermission = drive.getAllPermissions();
        for (String permission : allPermission) {
          if (permission.equalsIgnoreCase("*")) {
            driveList.add(drive);
            break;
          }
        }
      }
    }
    Collections.sort(driveList);
    drivesCache_.put(userId + ALL_DRIVES_CACHED_BY_ROLES, driveList);
    return driveList;
  }

  @SuppressWarnings("unchecked")
  public List<DriveData> getGroupDrives(String repository, String userId, List<String> userRoles, 
      List<String> groups) throws Exception {
    Object drives = drivesCache_.get(userId + ALL_GROUP_CACHED_DRIVES);
    if(drives != null) return (List<DriveData>) drives;
    List<DriveData> groupDrives = new ArrayList<DriveData>();
    String groupPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_GROUPS_PATH);
    for(DriveData drive : getDriveByUserRoles(repository, userId, userRoles)) {
      if(! drive.getHomePath().equals(groupPath) && drive.getHomePath().startsWith(groupPath)) {
        for(String group : groups) {
          if(drive.getHomePath().equals(groupPath + group)) {
            groupDrives.add(drive);
            break;
          }
        }
        for(String permission : drive.getAllPermissions()) {
          String[] arrPer = permission.split(":/");
          if(groups.contains("/" + arrPer[1]) && !groupDrives.contains(drive)) {
            groupDrives.add(drive);
            break;
          }
        }
      } 
    }
    Collections.sort(groupDrives);
    drivesCache_.put(userId + ALL_GROUP_CACHED_DRIVES, groupDrives);
    return groupDrives;
  }

  @SuppressWarnings("unchecked")
  public List<DriveData> getMainDrives(String repository, String userId, 
      List<String> userRoles) throws Exception {
    Object drives = drivesCache_.get(userId + ALL_MAIN_CACHED_DRIVE);
    if(drives != null) return (List<DriveData>) drives;
    List<DriveData> generalDrives = new ArrayList<DriveData>();
    String userPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USERS_PATH);
    String groupPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_GROUPS_PATH);
    for(DriveData drive : getDriveByUserRoles(repository, userId, userRoles)) {
      if((!drive.getHomePath().startsWith(userPath) && !drive.getHomePath().startsWith(groupPath)) 
          || drive.getHomePath().equals(userPath) || drive.getHomePath().equals(groupPath) ) {
        generalDrives.add(drive);
      }
    }
    Collections.sort(generalDrives);
    drivesCache_.put(userId + ALL_MAIN_CACHED_DRIVE, generalDrives);
    return generalDrives;
  }

  @SuppressWarnings("unchecked")
  public List<DriveData> getPersonalDrives(String repository, String userId, 
      List<String> userRoles) throws Exception {
    Object drives = drivesCache_.get(userId + ALL_PERSONAL_CACHED_DRIVE);
    if(drives != null) return (List<DriveData>) drives;
    List<DriveData> personalDrives = new ArrayList<DriveData>();
    String userPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USERS_PATH);
    for(DriveData drive : getDriveByUserRoles(repository, userId, userRoles)) {
      if(drive.getHomePath().startsWith(userPath + "/${userId}/")) {
        personalDrives.add(drive);
      }
    }
    Collections.sort(personalDrives);
    drivesCache_.put(userId + ALL_PERSONAL_CACHED_DRIVE, personalDrives);
    return personalDrives;
  }
}
