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

/**
 * The Class DocumentTemplateConfig.
 */
public class NewDocumentTemplateConfig {

  /** The extension. */
  protected String extension;

  /** The path. */
  protected String path;

  /** The name. */
  protected String name;

  /** The mime type. */
  protected String mimeType;

  /** The icon. */
  protected String icon;

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path the new path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the mime type.
   *
   * @return the mime type
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets the mime type.
   *
   * @param mimeType the new mime type
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Gets the icon.
   *
   * @return the icon
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Sets the icon.
   *
   * @param icon the new icon
   */
  public void setIcon(String icon) {
    this.icon = icon;
  }

  /**
   * Gets the extension.
   *
   * @return the extension
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Sets the extension.
   *
   * @param extension the new extension
   */
  public void setExtension(String extension) {
    this.extension = extension;
  }

}
