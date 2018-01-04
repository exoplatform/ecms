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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * Feb 27, 2006
 */
public class DriveData implements Comparable<DriveData>, Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -1483463576328793278L;
  private String name ;
  private String workspace ;
  private String permissions ;
  private String homePath ;
  private String icon ;
  private String views ;
  private boolean viewPreferences ;
  private boolean viewNonDocument ;
  private boolean viewSideBar ;
  private boolean showHiddenNode ;
  private String allowCreateFolders ;
  private String allowNodeTypesOnTree;
  private Map<String, String> parameters = new HashMap<>();

  public  DriveData(){}

  /**
   * Clone object
   *
   * @return cloned object
   */
  public DriveData clone() {
    DriveData driveData = new DriveData();
    driveData.setAllowCreateFolders(getAllowCreateFolders());
    driveData.setAllowNodeTypesOnTree(getAllowNodeTypesOnTree());
    driveData.setHomePath(getHomePath());
    driveData.setIcon(getIcon());
    driveData.setName(getName());
    driveData.setPermissions(getPermissions());
    driveData.setShowHiddenNode(getShowHiddenNode());
    driveData.setViewNonDocument(getViewNonDocument());
    driveData.setViewPreferences(getViewPreferences());
    driveData.setViewSideBar(getViewSideBar());
    driveData.setViews(getViews());
    driveData.setWorkspace(getWorkspace());
    driveData.setParameters(getParameters());
    return driveData;
  }

  /**
   *
   * @return the name of drive
   */
  public String getName() { return name ; }
  /**
   * Register drive name
   * @param name  the name of DriveData
   */
  public void setName(String name) { this.name = name ; }

  /**
   *
   * @return the name of workspace
   */
  public String getWorkspace() { return workspace ; }
  /**
   * Register workspace to drive
   * @param ws the workspace name
   */
  public void setWorkspace(String ws) { workspace = ws ; }

  /**
   *
   * @return the permissions of drive
   */
  public String getPermissions() { return this.permissions ; }
  /**
   * Register permission to drive
   * @param permissions
   */
  public void setPermissions(String permissions) { this.permissions = permissions ; }

  /**
   *
   * @return the home path of drive
   */
  public String getHomePath() { return homePath ; }
  /**
   * Register home path to drive
   * @param path the home path of drive
   */
  public void setHomePath(String path) { homePath = path ; }

  /**
   *
   * @return icon path
   */
  public String getIcon() { return icon ; }
  /**
   * Register icon to drive
   * @param ico icon path
   */
  public void setIcon(String ico) { icon = ico ; }

  /**
   *
   * @return the folder type of drive
   */
  public String getAllowCreateFolders() { return allowCreateFolders ; }
  /**
   * Register folder type to drive
   * @param allowCreateFolders folder type
   */
  public void setAllowCreateFolders(String allowCreateFolders) { this.allowCreateFolders = allowCreateFolders ; }

  public String getAllowNodeTypesOnTree() { return allowNodeTypesOnTree ; }

  public void setAllowNodeTypesOnTree(String allowNodeTypesOnTree) { this.allowNodeTypesOnTree = allowNodeTypesOnTree ; }

  /**
   *
   * @return  the views of drive
   */
  public String getViews() { return views ; }
  /**
   * Register views to drive
   * @param v view name
   */
  public void setViews(String v) { views = v ; }

  /**
   *
   * @return the state of view preference drive
   */
  public boolean getViewPreferences() { return viewPreferences ; }

  /**
   * Register the state of view preference to drive
   * @param b  the state of view preference
   */
  public void setViewPreferences(boolean b) { viewPreferences = b ; }

  /**
   *
   * @return the state of view non document node type of drive
   */
  public boolean getViewNonDocument() { return viewNonDocument ; }
  /**
   * Register state of view non document to drive
   * @param b the state of view non document node type
   */
  public void setViewNonDocument(boolean b) { viewNonDocument = b ; }
  /**
   *
   * @return the state of view side bar of drive
   */
  public boolean getViewSideBar() { return viewSideBar ; }
  /**
   * Register state of view side bar to drive
   * @param b state of view side bar
   */
  public void setViewSideBar(boolean b) { viewSideBar = b ; }

  /**
   *
   * @return the state of show hidden node of drive
   */
  public boolean getShowHiddenNode() { return showHiddenNode ; }
  /**
   * Register state of show hidden node to drive
   * @param b state of show hidden node
   */
  public void setShowHiddenNode(boolean b) { showHiddenNode = b ; }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  /**
   *
   * @return  the array of permission
   */
  public String[] getAllPermissions() {
    return permissions.split(",") ;
  }

  /**
   * Check the state of permission is existing or not
   * @param allPermissions  the string array permission of drive
   * @param permission  permission name
   * @return the state of permission is existing or not.
   */
  public boolean hasPermission(String[] allPermissions, String permission) {
    List<String> permissionList = new ArrayList<String>() ;
    for(String per : allPermissions){
      permissionList.add(per.trim()) ;
    }
    if(permission == null) return false ;
    if(permission.indexOf(":/") > -1){
      String[] array = permission.split(":/") ;
      if(array == null || array.length < 2) return false ;
      if(permissionList.contains("*:/"+array[1])) return true ;
      if(array[0].equals("*")) {
        String[] arrPer = {};
        for(String per : permissionList) {
          arrPer = per.split(":/");
          if(arrPer.length == 2 && arrPer[1].equals(array[1])) return true;
        }
      }
    }
    return permissionList.contains(permission) ;
  }

  public String getResolvedHomePath() {
    String resolvedHomePath = homePath;
    if (parameters != null) {
      if (parameters.containsKey("userId") && homePath.contains("${userId}")) {
        resolvedHomePath = resolvedHomePath.replaceAll(Pattern.quote("${userId}"), parameters.get("userId"));
      }
      if (parameters.containsKey("groupId") && homePath.contains("${groupId}")) {
        resolvedHomePath = resolvedHomePath.replaceAll(Pattern.quote("${groupId}"), parameters.get("groupId"));
      }
    }
    return resolvedHomePath;
  }

  public int compareTo(DriveData arg) {
    return name.compareToIgnoreCase(arg.getName()) ;
  }

  @Override
  public boolean equals(Object obj) {
     if (obj == this) {
        return true;
     }
     if (obj instanceof DriveData) {
        DriveData that = (DriveData)obj;
        return name.equals(that.name) ;
     }
     return false;
  }
}
