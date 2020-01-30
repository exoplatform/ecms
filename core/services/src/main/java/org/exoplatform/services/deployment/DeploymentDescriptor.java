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
package org.exoplatform.services.deployment;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class DeploymentDescriptor {

  private Target target;
  private String sourcePath;
  private Boolean cleanupPublication = false;
  private String versionHistoryPath;
  
  /**
   * @return the target where data will be stored
   */
  public Target getTarget() { return target; }


  /**
   * @param target the target to set
   */
  public void setTarget(Target target) { this.target = target; }


  /**
   * @return the sourcePath of data the will be stored
   * sourcePath should point out where resource is located example: war:/,jar:/,file:/, http://...
   * the deployment plugin will base of the scheme to load resource
   */
  public String getSourcePath() { return sourcePath; }


  /**
   * @param sourcePath the sourcePath to set
   */
  public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }



  /**
   *
   * @return true if cleanup is enabled
   */
  public Boolean getCleanupPublication() {
  return cleanupPublication;
  }

  /**
   * This method allows to cleanup the publication lifecycle in the target
   * folder after importing the data. By using this, the publication live
   * revision property will be re-initialized and the content will be set as
   * published directly. Thus, the content will be visible in front side.
   *
   * @param cleanupPublication
   */
  public void setCleanupPublication(Boolean cleanupPublication) {
    this.cleanupPublication = cleanupPublication;
  }

  /**
  * get the path of version history file which we want to import
  * @return the path of version history file which we want to import
  */
  public String getVersionHistoryPath() {
    return versionHistoryPath;
  }
   
  /**
  * set the path of version history file which we want to import
  * @param versionHistoryPath the path of version history file which we want to import
  */
  public void setVersionHistoryPath(String versionHistoryPath) {
    this.versionHistoryPath = versionHistoryPath;
  }



public static class Target {
    private String repository;
    private String workspace;
    private String nodePath;

    /**
     * @return the repository
     */
    public String getRepository() { return repository; }
    /**
     * @param repository the repository to set
     */
    public void setRepository(String repository) { this.repository = repository; }
    /**
     * @return the workspace
     */
    public String getWorkspace() { return workspace; }
    /**
     * @param workspace the workspace to set
     */
    public void setWorkspace(String workspace) { this.workspace = workspace; }
    /**
     * @return the nodePath
     */
    public String getNodePath() { return nodePath; }
    /**
     * @param nodePath the nodePath to set
     */
    public void setNodePath(String nodePath) { this.nodePath = nodePath; }
  }

}
