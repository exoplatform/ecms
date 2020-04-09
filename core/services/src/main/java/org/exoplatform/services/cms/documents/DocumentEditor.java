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

import java.util.Arrays;
import java.util.List;

/**
 * The Interface DocumentEditor.
 */
public interface DocumentEditor {

  /**
   * This handler is invoked before creation a new document via NewDocumentTemplatePlugin.
   *
   * @param template the document template
   * @param parentPath the parent path
   * @param title the title
   * @throws Exception the exception
   */
  void beforeDocumentCreate(NewDocumentTemplate template, String parentPath, String title) throws Exception;

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
   * @param activityId the activityId
   * @throws Exception the exception
   */
  void initActivity(String uuid, String workspace, String activityId) throws Exception;

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
   * Gets the provider name.
   *
   * @return the provider name
   */
  String getProviderName();
  
  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  default List<String> getPermissions() {
    return Arrays.asList("*");
  }

  /**
   * Checks if is active.
   *
   * @return true, if is active
   */
  default boolean isActive() {
    return true;
  }
}
