/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents;

import java.util.List;

/**
 * The Interface DocumentEditorPlugin provides API for handling such events from New Document feature: 
 * the document is going to be created, the document has been created. 
 * Also allows to get the provider and editor link.
 */
public interface DocumentEditorPlugin {

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  String getProviderName();

  /**
   * This handler is invoked before creation a new document via NewDocumentTemplatePlugin.
   *
   * @param template the document template
   * @param parentPath the parent path
   * @param title the title
   * @throws Exception the exception
   */
  void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title) throws Exception ;

  /**
   * This handler is invoked after creation a new document via NewDocumentTemplatePlugin.
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  void onDocumentCreated(String workspace, String path) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the activity is being rendered.
   *
   * @param uuid the uuid
   * @param workspace workspace
   * @param activityId the activity id
   * @param context the context
   * @throws Exception the exception
   */
  void initActivity(String uuid, String workspace, String activityId, String context) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the preview is being rendered.
   * 
   * @param uuid the uuid
   * @param workspace workspace
   * @param activityId the activity id
   * @param context the context
   * @param index the index
   * @throws Exception the exception
   */
  void initPreview(String uuid, String workspace, String activityId, String context, int index) throws Exception;

 
  /**
   * Gets the config.
   *
   * @return the config
   */
  ProviderConfig getConfig();
  
  /**
   * The Class ProviderConfig is used to set default permissions and status for the editor provider.
   */
  public static class ProviderConfig {
    
    /** The permissions. */
    protected List<String> permissions;
    
    /** The active. */
    protected Boolean active;
    
    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public List<String> getPermissions() {
      return permissions;
    }
    
    /**
     * Sets the permissions.
     *
     * @param permissions the new permissions
     */
    public void setPermissions(List<String> permissions) {
      this.permissions = permissions;
    }
    
    /**
     * Gets the active.
     *
     * @return the active
     */
    public Boolean getActive() {
      return active;
    }
    
    /**
     * Sets the active.
     *
     * @param active the new active
     */
    public void setActive(Boolean active) {
      this.active = active;
    }

  }
}
