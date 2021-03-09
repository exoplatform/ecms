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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.picocontainer.Startable;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

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
  private static String WORKSPACE = "exo:workspace";

  private static String ALL_DRIVES_CACHED = "allDrives";

  private static String ALL_DRIVES_CACHED_WITH_VIRTUAL = "allDrives_withVirtual";

  private static String ALL_DRIVES_CACHED_BY_ROLES = "_allDrivesByRoles";

  private static String ALL_MAIN_CACHED_DRIVE = "_mainDrives";

  private static String ALL_PERSONAL_CACHED_DRIVE = "_personalDrives";

  private static String ALL_GROUP_CACHED_DRIVES = "_groupDrives";
  
  private static String ALL_GROUP_PERMISSION = "*:${groupId}";

  private static String ALL_USER_PERMISSION = "${userId}";
  /**
   * Name of property PERMISSIONS
   */
  private static String PERMISSIONS = "exo:accessPermissions" ;

  /**
   * Name of property VIEWS
   */
  private static String VIEWS = "exo:views" ;

  /**
   * Name of property ICON
   */
  private static String ICON = "exo:icon" ;

  /**
   * Name of property PATH
   */
  private static String PATH = "exo:path" ;

  /**
   * Name of property VIEW_REFERENCES
   */
  private static String VIEW_REFERENCES = "exo:viewPreferences" ;

  /**
   * Name of property VIEW_NON_DOCUMENT
   */
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument" ;

  /**
   * Name of property VIEW_SIDEBAR
   */
  private static String VIEW_SIDEBAR = "exo:viewSideBar" ;

  /**
   * Name of property SHOW_HIDDEN_NODE
   */
  private static String SHOW_HIDDEN_NODE = "exo:showHiddenNode" ;

  /**
   *  Name of property ALLOW_CREATE_FOLDER
   */
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolders" ;
  private static String ALLOW_NODETYPES_ON_TREE = "exo:allowNodeTypesOnTree";
  public static final String GROUPS_DRIVE_NAME = "Groups";
  public static final String GROUPS_DRIVE_ROOT_NODE = "Groups";
  public static final String PERSONAL_DRIVE_NAME = "Personal Documents";
  public static final String PERSONAL_DRIVE_PUBLIC_FOLDER_NAME = "Public";
  public static final String PERSONAL_DRIVE_PRIVATE_FOLDER_NAME = "Private";
  public static final String USER_DRIVE_NAME = "User Documents";
  public static final String PERSONAL_DRIVE_ROOT_NODE = "Users";
  public static final String DRIVE_PARAMATER_USER_ID = "userId";
  public static final String DRIVE_PARAMATER_GROUP_ID = "groupId";

  private static final String CACHE_NAME = "ecms.drive";
  private static final Log LOG  = ExoLogger.getLogger(ManageDriveServiceImpl.class.getName());
  private static final String DELETED_DRIVE_NAMES= "DeletedDriveNames";

  /**
   * List of ManageDrivePlugin
   */
  private final List<ManageDrivePlugin> drivePlugins  = new ArrayList<ManageDrivePlugin>();

  /**
   * RepositoryService object
   */
  private final RepositoryService repositoryService;

  /**
   * OrganizationService object
   */
  private final OrganizationService organizationService;

  /**
   * Path to drive home directory
   */
  private final String baseDrivePath;

  /**
   * NodeHierarchyCreator object
   */
  private final NodeHierarchyCreator nodeHierarchyCreator;

  /** The DMS configuration. */
  private final DMSConfiguration dmsConfiguration;
  
  /**
   * Keep the drives of repository
   */
  private final ExoCache<Serializable, Object> drivesCache;
  
  /** The groups path. */
  private final String groupsPath;
  
  /** The users path. */
  private final String usersPath;


  /** The group drive template. */
  private DriveData groupDriveTemplate = null ;

  /**
   * Keep the state when a new role added
   */
  private boolean newRoleUpdated = false;

  /**
   * Instantiates a new {@link ManageDriveService} implementation.
   *
   * @param jcrService the jcr service
   * @param nodeHierarchyCreator the node hierarchy creator
   * @param dmsConfiguration the dms configuration
   * @param caService the ca service
   * @param organizationService the organization service
   * @throws Exception the exception
   */
  public ManageDriveServiceImpl(RepositoryService jcrService,
                                NodeHierarchyCreator nodeHierarchyCreator, 
                                DMSConfiguration dmsConfiguration,
                                CacheService caService,
                                OrganizationService organizationService) throws Exception{
    this.repositoryService = jcrService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.baseDrivePath = nodeHierarchyCreator.getJcrPath(BasePath.EXO_DRIVES_PATH);
    this.dmsConfiguration = dmsConfiguration;
    this.drivesCache = caService.getCacheInstance(CACHE_NAME);
    this.organizationService = organizationService;
    
    // Pre-read configured paths
    this.usersPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    this.groupsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    // init all ManageDrivePlugin
    try {
      for (ManageDrivePlugin plugin : drivePlugins) {
        plugin.init();
      }
      getAllDrives(true);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error starting ManageDriveServiceImpl: {}", e.getMessage());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
    // Nothing
  }

  /**
   * Init drive node with specified repository
   */
  public void init() throws Exception {
    for (ManageDrivePlugin plugin : drivePlugins) {
      plugin.init();
    }
    getAllDrives();
  }

  /**
   * Add new ManageDrivePlugin to drivePlugins
   * @param drivePlugin
   */
  public void setManageDrivePlugin(ManageDrivePlugin drivePlugin) {
    drivePlugins.add(drivePlugin) ;
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getAllDrives() throws Exception {
    return getAllDrives(false);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public List<DriveData> getAllDrives(boolean withVirtualDrives) throws Exception {
    // Try to get from cache
    List<DriveData> allDrives;
    if (withVirtualDrives) {
      allDrives = (List<DriveData>) drivesCache.get(getRepoName() + "_" + ALL_DRIVES_CACHED_WITH_VIRTUAL);
    } else {
      allDrives = (List<DriveData>) drivesCache.get(getRepoName() + "_" + ALL_DRIVES_CACHED);
    }
    if ((allDrives != null) && (allDrives.size() > 0) && (groupDriveTemplate != null)) {
      return cloneArrayList(allDrives);
    }

    // Get from jcr
    Session session = getSession() ;
    Node driveHome = (Node)session.getItem(baseDrivePath);
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
      data.setAllowNodeTypesOnTree(drive.getProperty(ALLOW_NODETYPES_ON_TREE).getString());
      if (GROUPS_DRIVE_NAME.equals(data.getName())) {
        groupDriveTemplate = data.clone();
        // Include group drive template if necessary
        if (withVirtualDrives) {
          driveList.add(data.clone());
        }
      } else {
        driveList.add(data.clone());
      }
    }

    // Put drives to cache
    if (withVirtualDrives) {
      drivesCache.put(getRepoName() + "_" + ALL_DRIVES_CACHED_WITH_VIRTUAL, driveList);
    } else {
      drivesCache.put(getRepoName() + "_" + ALL_DRIVES_CACHED, driveList);
    }
    session.logout();
    return cloneArrayList(driveList);
  }

  /**
   * {@inheritDoc}
   */
  public DriveData getDriveByName(String name) throws Exception{
    if (name.startsWith(".")) {
      String groupName = name.replace(".", "/");
      if (groupDriveTemplate == null) return null;
      DriveData drive = groupDriveTemplate.clone();
      drive.setHomePath(groupDriveTemplate.getHomePath().replace("${groupId}", groupName));
      drive.setName(name);
      drive.getParameters().put(ManageDriveServiceImpl.DRIVE_PARAMATER_GROUP_ID, name);
      drive.setPermissions("*:" + groupName);
      return drive;
    }

    Session session = getSession() ;
    Node driveHome = (Node)session.getItem(baseDrivePath);
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
      try {
        data.setAllowNodeTypesOnTree(drive.getProperty(ALLOW_NODETYPES_ON_TREE).getString());
      } catch(PathNotFoundException e) {
        data.setAllowNodeTypesOnTree("*");
      }
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
                       boolean viewSideBar, boolean showHiddenNode, String allowCreateFolder, String allowNodeTypesOnTree) throws Exception {
    Session session = getSession();
    Node driveHome = (Node)session.getItem(baseDrivePath) ;
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
      driveNode.setProperty(ALLOW_NODETYPES_ON_TREE, allowNodeTypesOnTree);
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
      driveNode.setProperty(ALLOW_NODETYPES_ON_TREE, allowNodeTypesOnTree);
      driveNode.save() ;
    }
    drivesCache.clearCache();
    session.save() ;
    session.logout();
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getAllDriveByPermission(String permission) throws Exception {
    List<DriveData> driveByPermission = new ArrayList<DriveData>() ;
    try{
      List<DriveData> driveList = getAllDrives();
      for(DriveData drive : driveList) {
        if(drive.hasPermission(drive.getAllPermissions(), permission)){
          driveByPermission.add(drive) ;
        }
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return driveByPermission ;
  }

  /**
   * {@inheritDoc}
   */
  public void removeDrive(String driveName) throws Exception {
    Session session = getSession();
    Node driveHome = (Node)session.getItem(baseDrivePath) ;
    if(driveHome.hasNode(driveName)){
      driveHome.getNode(driveName).remove() ;
      driveHome.save() ;
    }
    drivesCache.clearCache();
    session.logout();

    this.updateDeletedDrivesLog(driveName);
  }

  /**
   * Get session from repository in SystemWorkspace name
   * @return session
   * @throws Exception
   */
  private Session getSession() throws Exception {
    ManageableRepository manaRepository = repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return manaRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;
  }

  /**
   * Get session from repository in SystemWorkspace name
   * @return session
   * @throws Exception
   */
  private String getRepoName() {
    try {
      String repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
      if (LOG.isDebugEnabled()) {
        LOG.debug("The repository name is: " + repositoryName);
      }
      return repositoryName;
    } catch (RepositoryException e) {
      if (e.getMessage().equalsIgnoreCase("Repository 'null' not found.")) {
        String repositoryName = System.getProperty("gatein.tenant.repository.name");
        return repositoryName;
      }
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository exception occurs:", e);
      }
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUsedView(String viewName) throws Exception {
    Session session = getSession();
    Node driveHome = (Node)session.getItem(baseDrivePath) ;
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

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getDriveByUserRoles(String userId, List<String> userRoles) throws Exception {
    Object drivesByRoles = drivesCache.get(getRepoName() + "_" + userId + ALL_DRIVES_CACHED_BY_ROLES);
    if (drivesByRoles != null)
      return cloneArrayList(drivesByRoles);
    List<DriveData> driveList = new ArrayList<DriveData>();
    if (userId != null) {
      // We will improve ManageDrive service to allow getAllDriveByUser
      for (DriveData drive : getAllDrives()) {
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
          if (flag) continue;
          for (String rolse : userRoles) {
            if (drive.hasPermission(allPermission, rolse)) {
              if(!driveList.contains(drive)) driveList.add(drive);
              break;
            }
          }
        }
      }
      for (DriveData drive : getGroupDrives(userId, userRoles)) {
        if(!driveList.contains(drive)) driveList.add(drive); 
      }
    } else {
      for (DriveData drive : getAllDrives()) {
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
    drivesCache.put(getRepoName() + "_" + userId + ALL_DRIVES_CACHED_BY_ROLES, driveList);
    return cloneArrayList(driveList);
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getGroupDrives(String userId, List<String> userRoles) throws Exception {
    Object drives = drivesCache.get(getRepoName() + "_" + userId + ALL_GROUP_CACHED_DRIVES);
    if (drives != null)
      return cloneArrayList(drives);
    List<DriveData> groupDrives = new ArrayList<DriveData>();
    DriveData groupDrive = getDriveByName(GROUPS_DRIVE_NAME);
    if(groupDrive == null){
      return cloneArrayList(groupDrives);
    }

    boolean flag = hasPermissionOnDrive(groupDrive, userRoles);
    if(flag){
      for (String role : userRoles) {
        String group = role.substring(role.indexOf(":")+1);
        if (groupDriveTemplate != null && group.charAt(0)=='/') {
          DriveData drive = groupDriveTemplate.clone();
          drive.setHomePath(groupDriveTemplate.getHomePath().replace("${groupId}", group));
          drive.setName(group.replace("/", "."));
          drive.setPermissions("*:"+group);
          if (!groupDrives.contains(drive))
            groupDrives.add(drive);
        }
      }
    }  
    Collections.sort(groupDrives);
    drivesCache.put(getRepoName() + "_" + userId + ALL_GROUP_CACHED_DRIVES, groupDrives);
    return cloneArrayList(groupDrives);
  }

  /**
   * {@inheritDoc}
   */
  public DriveData getGroupDriveTemplate() {
    return groupDriveTemplate;
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getMainDrives(String userId, List<String> userRoles) throws Exception {
    Object drives = drivesCache.get(getRepoName() + "_" + userId + ALL_MAIN_CACHED_DRIVE);
    if(drives != null) return cloneArrayList(drives);
    List<DriveData> generalDrives = new ArrayList<DriveData>();
    List<DriveData> groupDrives = this.getGroupDrives(userId, userRoles);
    List<DriveData> personalDrives = this.getPersonalDrives(userId);
    for(DriveData drive : getDriveByUserRoles(userId, userRoles)) {
      if (!groupDrives.contains(drive) && !personalDrives.contains(drive)) {
        generalDrives.add(drive);
      }
    }
    Collections.sort(generalDrives);
    drivesCache.put(getRepoName() + "_" + userId + ALL_MAIN_CACHED_DRIVE, generalDrives);
    return cloneArrayList(generalDrives);
  }

  /**
   * {@inheritDoc}
   */
  public List<DriveData> getPersonalDrives(String userId) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    Object drives = drivesCache.get(getRepoName() + "_" + userId + ALL_PERSONAL_CACHED_DRIVE);
    if(drives != null) return cloneArrayList(drives);

    String cmsUserPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    List<String> memberships = getUserMemberships(userId);
    List<DriveData> personalDrives = new ArrayList<>();
    String userPath = userNode.getPath();
    for(DriveData drive : getAllDrives()) {
      if((drive.getHomePath().startsWith(cmsUserPath + "/${userId}")
              || drive.getHomePath().startsWith(userPath + "/"))
              && !USER_DRIVE_NAME.equals(drive.getName())
              && hasPermissionOnDrive(drive, memberships)) {
        personalDrives.add(drive);
      }
    }
    Collections.sort(personalDrives);
    drivesCache.put(getRepoName() + "_" + userId + ALL_PERSONAL_CACHED_DRIVE, personalDrives);
    return cloneArrayList(personalDrives);
  }
  
  /**
   * {@inheritDoc}
   */
  public DriveData getUserDrive(String userId) throws Exception {
    DriveData userDrive = null;
    String homePath = null;
    for (Iterator<DriveData> diter = getPersonalDrives(userId).iterator(); diter.hasNext();) {
      DriveData drive = diter.next();
      String path = drive.getHomePath();
      if (path.startsWith(usersPath) && path.endsWith("/Private")) {
        homePath = path;
        userDrive = drive;
        // we prefer drives with already defined real user path as home
        if (homePath.indexOf("${userId}") < 0) {
          break;
        } else if (!diter.hasNext()) {
          // if no real home path found and no more drives, do this job here
          homePath = Utils.getPersonalDrivePath(homePath, userId);
          userDrive.setHomePath(homePath);
          break;
        }
      }
    }
    return userDrive;
  }

  /**
   * {@inheritDoc}
   */
  public DriveData getGroupDrive(String groupId) throws Exception {
    return getDriveByName(groupId.replace('/', '.'));
  }
  
  /**
   * {@inheritDoc}
   */
  public boolean isGroupDrive(DriveData drive) {
    return isGroupPath(drive.getHomePath());
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUserDrive(DriveData drive) {
    return isUserPath(drive.getHomePath());
  }
  
  /**
   * Checks is it a group path.
   *
   * @param path the path
   * @return true, if is group path
   */
  public boolean isGroupPath(String path) {
    return path.startsWith(groupsPath);
  }

  /**
   * Checks is it a user path.
   *
   * @param path the path
   * @return true, if is user path
   */
  public boolean isUserPath(String path) {
    return path.startsWith(usersPath);
  }

  protected boolean hasPermissionOnDrive(DriveData drive, List<String> userMemberships) {
    String[] allPermission = drive.getAllPermissions();
    if(ALL_GROUP_PERMISSION.equals(allPermission[0]) || ALL_USER_PERMISSION.equals(allPermission[0])) {
      return true;
    }
    for (String membership : userMemberships) {
      if (drive.hasPermission(allPermission, membership)) {
        return true;
      }
    }
    return false;
  }

  protected List<String> getUserMemberships(String userId) throws Exception {
    List<String> memberships = null;
    String currentUser = ConversationState.getCurrent() != null ? ConversationState.getCurrent().getIdentity().getUserId() : null;
    if(currentUser != null && currentUser.equals(userId)) {
      memberships = Utils.getMemberships();
    } else {
      Collection<Membership> colMemberships = organizationService.getMembershipHandler().findMembershipsByUser(userId);
      memberships = colMemberships.stream().map(m -> m.getMembershipType() + ":" + m.getGroupId()).collect(Collectors.toList());
    }
    return memberships;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isVitualDrive(String driveName) {
    if (groupDriveTemplate == null) return false;
    return groupDriveTemplate.getName().equals(driveName);
  }

  /**
   * {@inheritDoc}
   */
  public void clearAllDrivesCache() {
    drivesCache.clearCache();
  }

  /**
   * {@inheritDoc}
   */
  public void clearGroupCache(String userId) {
    drivesCache.remove(getRepoName() + "_" + userId + ALL_GROUP_CACHED_DRIVES);
    drivesCache.remove(getRepoName() + "_" + userId + ALL_DRIVES_CACHED_BY_ROLES);
    drivesCache.remove(getRepoName() + "_" + userId + ALL_MAIN_CACHED_DRIVE);
  }

  /**
   * {@inheritDoc}
   */
  public boolean newRoleUpdated() {
    return newRoleUpdated;
  }

  /**
   * {@inheritDoc}
   */
  public void setNewRoleUpdated(boolean newRoleUpdated) {
    this.newRoleUpdated = newRoleUpdated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDriveOfDefaultWorkspace() throws Exception {
    DriveData collaDrive = getDriveByName(COLLABORATION_DRIVE);
    if(collaDrive != null && collaDrive.getHomePath().equals("/")) {
      return COLLABORATION_DRIVE;
    }
    List<DriveData> listDrive = getAllDrives();
    String defautlWsName = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    for(DriveData drive : listDrive) {
      if(drive.getWorkspace().equals(defautlWsName) && drive.getHomePath().equals("/")) {
        return drive.getName();
      }
    }
    return null;
  }

  @Override
  public Set<String> getDeletedDriveNames() throws Exception {
    return Utils.getAllEditedConfiguredData(this.getClass().getSimpleName(), DELETED_DRIVE_NAMES, true);
  }

  /**
   * Mark deleted drive to log file.
   *
   * @param driveName Drive Name
   * @throws Exception
   */
  private void updateDeletedDrivesLog(String driveName) throws Exception {
    Utils.addEditedConfiguredData(driveName, this.getClass().getSimpleName(), DELETED_DRIVE_NAMES, true);
  }

  private List<DriveData> cloneArrayList(Object drivesByRoles) {
    @SuppressWarnings("unchecked")
    ArrayList<DriveData> drivesByRolesList = (ArrayList<DriveData>) drivesByRoles;
    List<DriveData> drivesList = new ArrayList<>();
    for (DriveData driveData : drivesByRolesList) {
      drivesList.add(driveData.clone());
    }
    return drivesList;
  }
}
