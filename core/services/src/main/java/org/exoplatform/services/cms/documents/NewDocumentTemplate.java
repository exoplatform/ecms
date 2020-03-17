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
 * The Class NewDocumentTemplate represents a document template with it's properties.
 */
public class NewDocumentTemplate {

  /** The extension. */
  protected final String extension;

  /** The path. */
  protected final String path;

  /** The name. */
  protected final String name;

  /** The mime type. */
  protected final String mimeType;

  /** The icon. */
  protected final String icon;


  /**
   * Instantiates a new document template.
   *
   * @param config the config
   */
  public NewDocumentTemplate(NewDocumentTemplateConfig config) {
    this.extension = config.getExtension();
    this.path = config.getPath();
    this.name = config.getName();
    this.mimeType = config.getMimeType();
    this.icon = config.getIcon();
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
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
   * Gets the mime type.
   *
   * @return the mime type
   */
  public String getMimeType() {
    return mimeType;
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
   * Gets the extension.
   *
   * @return the extension
   */
  public String getExtension() {
    return extension;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "NewDocumentTemplate [extension=" + extension + ", path=" + path + ", name=" + name + ", mimeType=" + mimeType + ", icon="
        + icon + "]";
  }

}
