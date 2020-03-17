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

import org.exoplatform.services.cms.documents.exception.PermissionValidationException;
import org.exoplatform.services.security.Identity;


/**
 * The Interface DocumentEditorProvider.
 */
public interface DocumentEditorProvider extends DocumentEditor {
  

  /**
   * Update permissions.
   *
   * @param permissions the permissions
   * @throws PermissionValidationException the permission validation exception
   */
  void updatePermissions(List<String> permissions) throws PermissionValidationException;
  
  /**
   * Update active.
   *
   * @param active the active
   */
  void updateActive(boolean active);
  
  
  /**
   * Checks if is available for user.
   *
   * @param identity the identity
   * @return true, if is available for user
   */
  boolean isAvailableForUser(Identity identity);
  
  
  /**
   * Gets the editor class.
   *
   * @return the editor class
   */
  Class<? extends DocumentEditor> getEditorClass();

}
