/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.cms.taxonomy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Apr 3, 2009
 */
public class TaxonomyTreeData implements Comparable<TaxonomyTreeData> {

  private String             taxoTreeName;

  private String             repository;

  private String             taxoTreeWorkspace;

  private String             taxoTreePermissions;

  private String             taxoTreeHomePath;

  private String             taxoTreeActionName;

  private String             taxoTreeActionTargetPath;

  private String             taxoTreeActionTargetWorkspace;

  private String             taxoTreeActionTypeName;

  private boolean            edit               = false;

  public static final String ACTION_TAXONOMY_TREE = "exo:taxonomyAction";

  public TaxonomyTreeData() {

  }

  /**
   * @return the name of repository
   */
  public String getRepository() {
    return repository;
  }

  /**
   * Register repository to drive
   *
   * @param rp repository name
   */
  public void setRepository(String rp) {
    repository = rp;
  }

  /**
   * @return the permissions of drive
   */
  public String getPermissions() {
    return this.taxoTreePermissions;
  }

  /**
   * Check the state of permission is existing or not
   * @param allPermissions the string array permission of drive
   * @param permission permission name
   * @return the state of permission is existing or not.
   */
  public boolean hasPermission(String[] allPermissions, String permission) {
    List<String> permissionList = new ArrayList<String>();
    for (String per : allPermissions) {
      permissionList.add(per.trim());
    }
    if (permission == null)
      return false;
    if (permission.indexOf(":/") > -1) {
      String[] array = permission.split(":/");
      if (array == null || array.length < 2)
        return false;
      if (permissionList.contains("*:/" + array[1]))
        return true;
    }
    return permissionList.contains(permission);
  }

  public int compareTo(TaxonomyTreeData arg) {
    return taxoTreeName.compareToIgnoreCase(arg.getTaxoTreeName());
  }


  /**
   * Get taxonomy tree home path
   * @return taxoTreeHomePath
   */
  public String getTaxoTreeHomePath() {
    return taxoTreeHomePath;
  }

  /**
   * Register home path to taxonomy Tree
   * @param taxoTreeHomePath the home path of drive
   */
  public void setTaxoTreeHomePath(String taxoTreeHomePath) {
    this.taxoTreeHomePath = taxoTreeHomePath;
  }

  /**
   * get taxonomy tree name
   */
  public String getTaxoTreeName() {
    return taxoTreeName;
  }

  /**
   * Register taxonomy tree name
   * @param taxoTreeName the name of taxonomy tree
   */
  public void setTaxoTreeName(String taxoTreeName) {
    this.taxoTreeName = taxoTreeName;
  }

  public String getTaxoTreePermissions() {
    return taxoTreePermissions;
  }

  /**
   * Register permission to taxonomy tree
   * @param permission
   */
  public void setTaxoTreePermissions(String permission) {
    this.taxoTreePermissions = permission;
  }

  /**
   * @return the name of workspace
   */
  public String getTaxoTreeWorkspace() {
    return taxoTreeWorkspace;
  }

  /**
   * Register workspace to tree
   * @param taxoTreeWorkspace the workspace name
   */
  public void setTaxoTreeWorkspace(String taxoTreeWorkspace) {
    this.taxoTreeWorkspace = taxoTreeWorkspace;
  }

  /**
   * Get action name of Taxonomy tree
   */
  public String getTaxoTreeActionName() {
    return taxoTreeActionName;
  }

  /**
   * Set action name of Taxonomy tree
   * @param taxoTreeActionName
   */
  public void setTaxoTreeActionName(String taxoTreeActionName) {
    this.taxoTreeActionName = taxoTreeActionName;
  }

  public String getTaxoTreeActionTypeName() {
    return taxoTreeActionTypeName;
  }

  public void setTaxoTreeActionTypeName(String taxoTreeActionTypeName) {
    this.taxoTreeActionTypeName = taxoTreeActionTypeName;
  }

  /**
   * Check data in edited state or not
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * Set edit state
   * @param edit
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }
}
