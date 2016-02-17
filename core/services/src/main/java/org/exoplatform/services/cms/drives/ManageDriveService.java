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
package org.exoplatform.services.cms.drives;

import java.util.List;
import java.util.Set;

/**
 * Function to manage Drive to be able to add, remove or manage them.
 *
 * @LevelAPI Experimental
 */
public interface ManageDriveService {
  
  public static String COLLABORATION_DRIVE = "Collaboration";

  /**
   * Get Drive name which point to root folder of default workspace
   * @return String Name of drive.
   */
  public String getDriveOfDefaultWorkspace() throws Exception;

  /**
   * Register a new drive to workspace or update if the drive is existing
   *
   * @param name drive name
   * @param workspace the workspace name where will store the drive
   * @param permissions specify who can access to this drive
   * @param homePath specify the location of drive
   * @param views include all views can see in drive
   * @param icon the drive icon which can see in drive browser
   * @param viewReferences the boolean to set default for drive can view
   *          references node or not
   * @param viewNonDocument the boolean to set default for drive can view non
   *          document node or not
   * @param viewSideBar the boolean to set default for drive can view side bar
   *          or not
   * @param showHiddenNode the boolean to set default for drive can see hidden
   *          node or not
   * @param allowCreateFolder the string to specify which type of folder can add
   *          in the drive
   * @param allowNodeTypesOnTree
   * @throws Exception
   */
  public void addDrive(String name,
                       String workspace,
                       String permissions,
                       String homePath,
                       String views,
                       String icon,
                       boolean viewReferences,
                       boolean viewNonDocument,
                       boolean viewSideBar,
                       boolean showHiddenNode,
                       String allowCreateFolder,
                       String allowNodeTypesOnTree) throws Exception;

  /**
   * Return an DriveData Object
   * @param driveName  the string contain the drive name
   * @see DriveData
   * @return  DriveData with specified drive name and repository
   * @throws Exception
   */
  public DriveData getDriveByName(String driveName) throws Exception;

  /**
   * Return the list of DriveData
   * This method will look up in all workspaces of repository to find DriveData with
   * specified permission
   * @param permission  the string contain the permission
   * @return  list of DriveData with specified repository and permission
   * @see DriveData
   * @throws Exception
   */
  public List<DriveData> getAllDriveByPermission(String permission) throws Exception;

  /**
   * Remove drive with specified drive name and repository
   * @param driveName  drive name
   * @throws Exception
   */
  public void removeDrive(String driveName) throws Exception;

  /**
   * This method will look up in all workspaces of current repository to find DriveData
   *
   * @param withVirtualDrives true: include Virtual Drives, false: not include Virtual Drives
   * @return list of DriveData with specified repository
   * @throws Exception
   */
  public List<DriveData> getAllDrives(boolean withVirtualDrives) throws Exception;

  /**
   * This method will look up in all workspaces of current repository to find DriveData
   * @return list of DriveData with specified repository
   * @throws Exception
   */
  public List<DriveData> getAllDrives() throws Exception;

  /**
   * This method will check to make sure the view is not in used before remove this view
   * @param viewName view name
   * @return the status of current view is in used or not
   * @throws Exception
   */
  public boolean isUsedView(String viewName) throws Exception;

  /**
   * Register all drive plugins
   *
   * @throws Exception
   */
  public void init() throws Exception;

  /**
   * Get all drives by user roles
   * @param userId User name
   * @param roles Roles of user
   * @return list of drives
   * @throws Exception
   */
  public List<DriveData> getDriveByUserRoles(String userId, List<String> roles) throws Exception;

  /**
   * Get all main drives.
   *
   * @param userId Name of user
   * @param userRoles Roles of user
   * @return list of drives
   * @throws Exception
   */
  public List<DriveData> getMainDrives(String userId, List<String> userRoles) throws Exception;

  /**
   * Get all personal drives.
   *
   * @param userId Name of user
   * @return list of drives
   * @throws Exception
   */
  public List<DriveData> getPersonalDrives(String userId) throws Exception;

  /**
   * Get all group drives
   * @param userId Name of user
   * @param userRoles Roles of user
   * @return list of drives
   * @throws Exception
   */
  public List<DriveData> getGroupDrives(String userId, List<String> userRoles) throws Exception;

  /**
   * Check if a drive is vitual(Group Drive Template)
   * @param driveName  the string contain the drive name
   * @return true: is Virtual Drive, false: not is Virtual Drive
   */
  public boolean isVitualDrive(String driveName);

  /**
   * Clear all drives cache
   */
  public void clearAllDrivesCache();

  /**
   * Clear group drives cache
   * @param userId User name of current user
   */
  public void clearGroupCache(String userId);

  /**
   * Inform when have new role added
   * @return Boolean
   */
  public boolean newRoleUpdated();

  /**
   * Set the status of new added role
   * @param newRoleUpdated
   */
  public void setNewRoleUpdated(boolean newRoleUpdated);

  /**
   * Get deleted drive names.
   *
   * @return list of deleted drive names
   * @throws Exception
   */
  public Set<String> getDeletedDriveNames() throws Exception;
}
