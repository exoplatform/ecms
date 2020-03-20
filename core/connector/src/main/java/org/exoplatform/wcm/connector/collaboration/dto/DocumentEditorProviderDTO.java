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
package org.exoplatform.wcm.connector.collaboration.dto;

import java.util.List;

/**
 * The Class DocumentEditorProviderDTO.
 */
public class DocumentEditorProviderDTO extends ResourceSupport {

  /** The provider. */
  private String           provider;

  /** The active. */
  private Boolean          active;

  /** The permissions. */
  private List<Permission> permissions;

  
  /**
   * Instantiates a new document editor provider DTO.
   */
  public DocumentEditorProviderDTO() {

  }

  /**
   * Instantiates a new document editor provider DTO.
   *
   * @param provider the provider
   * @param active the active
   * @param permissions the permissions
   */
  public DocumentEditorProviderDTO(String provider, boolean active, List<Permission> permissions) {
    this.provider = provider;
    this.active = active;
    this.permissions = permissions;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
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
   * Sets the provider.
   *
   * @param provider the new provider
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   * Sets the active.
   *
   * @param active the new active
   */
  public void setActive(Boolean active) {
    this.active = active;
  }

  /**
   * Sets the permissions.
   *
   * @param permissions the new permissions
   */
  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  public List<Permission> getPermissions() {
    return permissions;
  }

}
